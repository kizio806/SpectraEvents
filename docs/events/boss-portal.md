# Boss Portal Event Specification

## Purpose
An event where players must deliver items to a portal to charge it, which then spawns a boss.

## Player Experience
A dormant portal spawns. Players must throw or deliver 100 specific items (e.g., "Corrupted Souls") into the portal. Once charged, it opens, and a boss steps out. Players must defeat the boss.

## Event Lifecycle
Standard global lifecycle.

## Phases

1. **DORMANT**
   - The portal frame exists, but is unlit.
   - **Transitions**: Player delivers item -> `CHARGING`.
2. **CHARGING**
   - Players deliver items. A counter tracks progress (0/100).
   - **Transitions**: Counter reaches 100 -> `OPEN`.
3. **OPEN**
   - Portal lighting animation. Boss entity spawns.
   - **Transitions**: Boss killed -> `BOSS_DEFEATED`.
4. **BOSS_ACTIVE** (Parallel to OPEN, or subsumed by it)
   - The boss fights the players.
5. **BOSS_DEFEATED**
   - Boss death animation.
   - **Transitions**: Immediate -> `CLOSING`.
6. **CLOSING**
   - Distributes rewards. Portal closes.
   - **Transitions**: Immediate -> `CLEANUP`.
7. **CLEANUP**
   - Standard instance removal.

## Spawn
- **Strategy**: Fixed location.

## Visuals
- Portal frame model.
- Swirling particle vortex inside the frame when `CHARGING` and `OPEN`.

## Interactions
- `item-delivered` (via dropping item in hitbox or right-clicking with item).

## Components
- `ModelComponent`, `InteractionComponent` (Delivery), `MobWaveComponent`.

## Triggers
- `item-delivered`.
- `delivery-target-reached`.
- `mob-killed`.

## Conditions
- `item-held` or `item-dropped` matches the required definition.

## Actions
- `consume-item`, `spawn-mob`, `spawn-particles`.

## Rewards
- **Distribution**: Two-tiered.
  - Participation reward based on items delivered.
  - Boss loot based on damage dealt to the boss.

## Leaderboard
- Two leaderboards: Delivery count and Damage count.

## Event Area
- Prevents players from building traps around the portal.

## Persistence & Restart Recovery
- Delivery count must persist across restarts.

## Required Engine Primitives
- Item delivery / consumption mechanics.
- Multiple active leaderboards per instance.
