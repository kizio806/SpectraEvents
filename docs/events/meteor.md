# Meteor Event Specification

## Purpose
A high-impact, competitive public event where players race to damage a falling celestial object for ranked rewards.

## Player Experience
Players are notified of a falling meteor. They race to the location, wait for it to cool down (locked phase), and then attack it. Rewards are distributed based on damage dealt.

## Event Lifecycle
Follows the standard global `CREATED` -> `RUNNING` -> `COMPLETED` lifecycle.

## Phases

1. **ANNOUNCED** (Optional)
   - Triggers a server-wide broadcast and sets a timer before spawn.
2. **FALLING**
   - The meteor spawns high in the sky and descends toward the surface.
   - **Transitions**: When it hits the ground -> `IMPACT`.
3. **IMPACT**
   - Plays a massive sound and particle explosion.
   - **Transitions**: Immediate (0 ticks) -> `LOCKED`.
4. **LOCKED**
   - The meteor is too hot to touch. A countdown is displayed.
   - **Transitions**: Timer expires -> `ACTIVE`.
5. **ACTIVE**
   - The meteor takes damage from players.
   - **Transitions**: Health reaches zero -> `DESTROYED`.
6. **DESTROYED**
   - Destructive animation plays. Rewards are calculated.
   - **Transitions**: Animation finishes -> `CLEANUP`.
7. **CLEANUP**
   - Drops world items, removes visual models, transitions the global state to `COMPLETED`.

## Spawn
- **Strategy**: Random Surface (avoids water, lava, restricted biomes).

## Visuals
- A large, spherical 3D model (block displays).
- Trailing fire particles while falling.
- A Bossbar displaying health during the `ACTIVE` phase.
- Holograms displaying time remaining during `LOCKED`.

## Interactions
- **Hitbox**: Large interaction entity covering the model. Left-click/damage applies to the `HealthComponent`.

## Components
- `ModelComponent`, `AnimationComponent`, `TimerComponent`, `HealthComponent`, `LeaderboardComponent`, `BossBarComponent`.

## Triggers
- `timer-expired`, `health-zero`, `animation-finished`, `ground-collision`.

## Conditions
- Minimum players online (for auto-start).

## Actions
- `broadcast`, `play-sound`, `spawn-particles`, `give-reward`.

## Rewards
- **Distribution**: Top 3 damage dealers receive premium loot. All other participants receive basic loot.

## Leaderboard
- Tracks cumulative damage dealt per player.

## Event Area
- Prevents PvP while the meteor is `LOCKED`, enables PvP when `ACTIVE`.

## Persistence & Restart Recovery
- If the server restarts during `FALLING`, the meteor resumes falling from its saved height.
- If restarting during `ACTIVE`, health is restored, and the event continues.

## Chunk Behavior
- The event chunk is force-loaded while the event is `RUNNING`.

## Cleanup
- All display entities are strictly removed. Global state becomes `COMPLETED`.

## Failure Cases
- No valid spawn location found (transitions to `FAILED`).

## Abuse / Anti-Dupe Considerations
- Damage from projectiles must accurately attribute to the shooter.
- Prevent players from blocking the meteor's fall with obsidian.

## Required Engine Primitives
- Falling mechanics (interpolation).
- `HealthComponent`.
- Ranked `LeaderboardComponent`.
