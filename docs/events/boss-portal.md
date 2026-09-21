# Boss Portal Event

`boss-portal.yml` is a bundled, executable reference for a timed visual event that releases a
tracked boss. It uses the existing generic timer, model animation, boss spawn, entity-death routing,
loot, cleanup, and recovery behavior; it does not introduce a `BossPortalManager`.

## Lifecycle

```text
opening
  └─ spawn model + play opening animation + 2-second timer
       ↓
active
  └─ spawn event-owned Portal Guardian
       ↓ entity_death
defeated
  └─ drop loot + remove bossbar/model + complete event
```

## What it uses

| Concern | Shared contract used by Boss Portal |
| --- | --- |
| Visual asset | `models/boss-portal.yml` (`dev_boss_portal_model`) and named `opening` animation |
| Transition | `timer_elapsed` moves the event to the active phase |
| Boss ownership | `spawn_boss` marks the guardian as event-owned so the platform death router can emit `entity_death` |
| Completion | YAML owns rewards and cleanup; `complete_event` invokes standard lifecycle cleanup |
| Restart | The shared persistent phase/timer state and resource reconciliation apply without portal-specific code |

## Operator workflow

1. Start `boss_portal` with `/event event start boss_portal`.
2. Wait for the opening timer and confirm that the guardian appears.
3. Defeat the guardian, then confirm drops, bossbar removal, model removal, and a completed instance.
4. Copy the YAML with a new underscore-only ID to alter the delay, boss, model, or loot.

The bundled asset is a native YAML model. Administrators may replace it with an imported Blockbench
model and keep the same `spawn_model` and `play_animation` references.
