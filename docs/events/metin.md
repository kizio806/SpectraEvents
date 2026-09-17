# Metin Event Specification

## Purpose
A stationary object that must be destroyed. As it takes damage and crosses specific health thresholds, it spawns defending mob waves. A final boss spawns before it can be destroyed.

## Player Experience
Players find a Metin stone and start attacking it. At 75%, 50%, and 25% health, waves of monsters spawn to defend it. At 5% health, the stone becomes invulnerable until a boss is defeated.

## Event Lifecycle
Standard global lifecycle.

## Phases

1. **SPAWNING**
   - The Metin stone appears with an introductory animation (e.g., rising from ground).
   - **Transitions**: Animation finished -> `ACTIVE`.
2. **ACTIVE**
   - The stone takes damage.
   - **Transitions**:
     - Health drops below threshold (e.g., 5%) -> `ENRAGED`.
     - Health reaches zero (if boss skipped) -> `DEFEATED`.
3. **ENRAGED**
   - The stone gains invulnerability (`Condition` blocks damage). A Boss mob is spawned.
   - **Transitions**: Boss mob killed trigger -> `ACTIVE` (or `DEFEATED` directly).
4. **DEFEATED**
   - The stone crumbles. Rewards are distributed.
   - **Transitions**: Immediate -> `CLEANUP`.
5. **CLEANUP**
   - Standard instance removal.

## Spawn
- **Strategy**: Fixed location or Random Region.

## Visuals
- Stationary tall block structure (Model).
- Particles emitted based on current health percentage (more smoke as health gets lower).

## Interactions
- Left-click/damage applies to the `HealthComponent`.

## Components
- `ModelComponent`, `HealthComponent`, `MobWaveComponent`, `LeaderboardComponent`.

## Triggers
- `health-threshold` (e.g., fires at 75%, 50%, 25%).
- `mob-killed` (tracking the specific spawned boss).
- `health-zero`.

## Conditions
- Invulnerability condition applied during the `ENRAGED` phase.

## Actions
- `spawn-mob` (for waves and boss).
- `give-reward`.

## Rewards
- **Distribution**: Damage leaderboard ranking, plus a participation threshold (e.g., must have dealt at least 1% of total health to get rewards).

## Leaderboard
- Tracks cumulative damage.

## Event Area
- Keeps mobs tethered to the stone (prevents kiting the boss across the map).

## Persistence & Restart Recovery
- Retains current health and active phase. Mobs may need to be respawned or recovered if despawned by the server.

## Chunk Behavior
- Force-loaded while `ACTIVE`.

## Failure Cases
- Boss falls into the void -> fails safe by auto-transitioning back to `ACTIVE`.

## Abuse / Anti-Dupe Considerations
- Mobs spawned by the event should not drop vanilla loot or XP to prevent farming.

## Required Engine Primitives
- `HealthComponent`.
- `health-threshold` triggers.
- `MobWaveComponent` / generic entity tracking.
