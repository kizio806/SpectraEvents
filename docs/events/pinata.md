# Pinata Event Specification

## Purpose
A lighthearted event where players rapidly hit an object. Progress is measured by the sheer number of hits, not the amount of damage dealt.

## Player Experience
A Pinata spawns. Players gather and click/punch it rapidly. It drops minor items on some hits, and breaks after a total number of hits is reached, dropping a large reward.

## Event Lifecycle
Standard global lifecycle.

## Phases

1. **SPAWN**
   - The Pinata appears hanging from a fixed point or hovering.
   - **Transitions**: Immediate -> `ACTIVE`.
2. **ACTIVE**
   - The Pinata registers discrete hits.
   - **Transitions**: Hit counter reaches max -> `BROKEN`.
3. **BROKEN**
   - The model explodes into particles and items.
   - **Transitions**: Animation finishes -> `REWARDING`.
4. **REWARDING**
   - Final rewards are distributed.
   - **Transitions**: Immediate -> `CLEANUP`.
5. **CLEANUP**
   - Standard instance removal.

## Spawn
- **Strategy**: Administrator triggered at a specific location.

## Visuals
- A colorful Model.
- Bobs or swings when hit (animation).

## Interactions
- Left-click triggers the `HitCounterComponent`. Damage values from swords vs. bare hands are irrelevant; every hit counts as 1.

## Components
- `ModelComponent`, `HitCounterComponent`, `AnimationComponent`.

## Triggers
- `hits-reached` (thresholds for minor drops, and max for breaking).
- `player-interact` (for per-hit feedback).

## Conditions
- Cooldown condition (e.g., max 2 hits per second per player) to prevent autoclicker abuse.

## Actions
- `play-sound`, `spawn-particles`, `give-reward` (minor drop).

## Rewards
- **Distribution**:
  - Random drops scattered on the floor during the `ACTIVE` phase.
  - Optional `last-hit` reward for the player who breaks it.
  - `participation` reward for anyone with > 10 hits.

## Leaderboard
- Optional (most hits).

## Event Area
- Small area, PvP disabled.

## Persistence & Restart Recovery
- Retains current hit count.

## Chunk Behavior
- Standard.

## Required Engine Primitives
- `HitCounterComponent` (distinct from `HealthComponent`).
- High-frequency interaction handling.
