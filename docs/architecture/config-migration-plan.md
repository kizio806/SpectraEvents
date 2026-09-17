# Configuration Migration Plan

This document maps the hardcoded Java coordinators (`DevMeteorCoordinator`, `DevAirdropCoordinator`, `DevMetinCoordinator`) to the new data-driven Engine model (`EventSpec`).

## Core Principles

- **No Custom Coordinators**: All event logic will be defined via `EventSpec` and executed by the `EventRuntimeKernel`.
- **Domain Triggers**: Platform adapters map Bukkit events (e.g. `EntityDamageEvent`) into domain triggers (`HealthThresholdCrossed`, `EntityDeath`).
- **Data-Driven Actions**: Instead of calling Bukkit API directly, actions produce Domain commands or delegate to `PlatformActionPort` to spawn entities, play sounds, or apply damage.

---

## Status Overview

- [x] **Meteor**: Fully migrated to config-driven YAML runtime execution (`events/meteor.yml`). `DevMeteorCoordinator` removed.
- [ ] **Airdrop**: Next migration milestone target.
- [ ] **Metin**: Future migration milestone target.

---

## 1. Meteor (Completed)

### Schema & Execution
Meteor is driven entirely by `events/meteor.yml` and executed via `EventExecutionEngine`:
- `falling`: `on-enter` initializes health & spawns model with height offset. Transition via `timer_elapsed` (3s) -> `impact`.
- `impact`: `on-enter` moves model to ground, plays explosion sound, spawns particles. Transition via `timer_elapsed` (1s) -> `locked`.
- `locked`: `on-enter` sets lock duration (10s). Transition via `timer_elapsed` (10s) -> `active`. `interaction` returns locked feedback.
- `active`: `interaction` applies damage (1 HP per hit). `health_depleted` (0 HP) triggers transition -> `destroyed`.
- `destroyed`: `on-enter` removes model and completes event instance (`complete_event`).

### Future Schema
```json
{
  "id": "meteor",
  "initialPhase": "falling",
  "phases": {
    "falling": {
      "onEnter": [
        { "type": "spawn_model", "parameters": { "model": "meteor_falling", "pdcId": "meteor_core" } },
        { "type": "start_temporal_task", "parameters": { "id": "meteor_descend", "interval": "1t" } }
      ],
      "transitions": [
        {
          "trigger": { "type": "altitude_crossed" },
          "conditions": [ { "type": "y_less_than_or_equal", "parameters": { "y": 64 } } ],
          "targetPhase": "impact"
        }
      ]
    },
    "impact": {
      "onEnter": [
        { "type": "stop_temporal_task", "parameters": { "id": "meteor_descend" } },
        { "type": "spawn_particles", "parameters": { "type": "explosion" } },
        { "type": "spawn_block", "parameters": { "type": "ancient_debris" } }
      ],
      "transitions": [
        {
          "trigger": { "type": "timer_elapsed", "parameters": { "duration": "0s" } },
          "targetPhase": "completed"
        }
      ]
    },
    "completed": {}
  }
}
```

---

## 2. Airdrop

### Current
- `falling`: Waits until y=64, moves armor stand.
- `locked`: 10-second countdown, displaying boss bar.
- `open`: Accepts interaction from player.
- `claimed`: Distributes reward, transitions to completed.

### Future Schema
```json
{
  "id": "airdrop",
  "initialPhase": "falling",
  "phases": {
    "falling": {
      // similar to meteor, transitions to locked at y=64
    },
    "locked": {
      "onEnter": [
        { "type": "start_boss_bar", "parameters": { "title": "Unlocking Airdrop...", "duration": "10s" } }
      ],
      "transitions": [
        {
          "trigger": { "type": "timer_elapsed", "parameters": { "duration": "10s" } },
          "targetPhase": "open"
        }
      ]
    },
    "open": {
      "onEnter": [
        { "type": "spawn_interaction_hitbox" }
      ],
      "transitions": [
        {
          "trigger": { "type": "player_interact" },
          "targetPhase": "claimed"
        }
      ]
    },
    "claimed": {
      "onEnter": [
        { "type": "distribute_reward" }
      ]
    }
  }
}
```

---

## 3. Metin

### Current
- Spawns an end crystal (Metin) with health and damage tracking.
- Phases based on health thresholds (100% -> 60% -> 25%).
- Spawns mobs/bosses at thresholds.
- Completes on death.

### Future Schema
```json
{
  "id": "metin",
  "initialPhase": "spawning",
  "phases": {
    "spawning": {
      "onEnter": [
        { "type": "spawn_entity", "parameters": { "type": "end_crystal", "health": 1000 } }
      ],
      "transitions": [
        { "trigger": { "type": "entity_spawned" }, "targetPhase": "active" }
      ]
    },
    "active": {
      "transitions": [
        {
          "trigger": { "type": "health_threshold_crossed", "parameters": { "threshold": 60 } },
          "targetPhase": "enraged"
        },
        {
          "trigger": { "type": "health_depleted" },
          "targetPhase": "completed"
        }
      ]
    },
    "enraged": {
      "onEnter": [
        { "type": "spawn_mob_wave" },
        { "type": "play_sound", "parameters": { "sound": "entity.wither.spawn" } }
      ],
      "transitions": [
        {
          "trigger": { "type": "health_threshold_crossed", "parameters": { "threshold": 25 } },
          "targetPhase": "boss"
        },
        {
          "trigger": { "type": "health_depleted" },
          "targetPhase": "completed"
        }
      ]
    }
  }
}
```
