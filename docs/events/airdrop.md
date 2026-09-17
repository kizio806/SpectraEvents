# Airdrop Event Specification

## Purpose
A loot crate drops into the world, requiring players to control the area and interact with it to claim the prize.

## Player Experience
A plane or crate descends slowly. Once it lands, players must secure the area and open the crate. It is a discrete interaction, not a combat boss.

## Event Lifecycle
Standard global lifecycle.

## Phases

1. **FALLING**
   - The crate descends with a parachute model.
   - **Transitions**: Hits ground -> `LANDED`.
2. **LANDED**
   - The parachute model swaps to a landed crate model. Emits a smoke beacon.
   - **Transitions**: Timer expires -> `LOCKED`.
3. **LOCKED**
   - A brief stabilization period.
   - **Transitions**: Timer expires -> `OPEN`.
4. **OPEN**
   - The crate can be interacted with.
   - **Transitions**: Player right-clicks -> `CLAIMED`.
5. **CLAIMED**
   - Loot is distributed.
   - **Transitions**: Immediate -> `CLEANUP`.
6. **CLEANUP**
   - Removes models, transitions to `COMPLETED`.

## Spawn
- **Strategy**: Random Surface.

## Visuals
- Falling crate with a parachute model.
- Smoke particle beacon indicating location.
- Hologram indicating status.

## Interactions
- Right-click to open. Requires `InteractionComponent`. No health involved.

## Components
- `ModelComponent`, `TimerComponent`, `InteractionComponent`.

## Triggers
- `ground-collision`, `timer-expired`, `player-interact`.

## Conditions
- Optional: Player must hold a specific "Airdrop Key" item to trigger the interaction.

## Actions
- `broadcast`, `give-reward`, `spawn-particles`.

## Rewards
- **Distribution**: First-come, first-served (the player who triggers `player-interact`).
- Alternatively, drops physical items on the ground (`world-drop`).

## Leaderboard
- Not required for standard Airdrop.

## Event Area
- PvP enabled.

## Persistence & Restart Recovery
- Restores state and location upon reboot. Parachute visual re-syncs.

## Chunk Behavior
- Force-loaded.

## Failure Cases
- Lands in void (fails safe, cancels event).

## Abuse / Anti-Dupe Considerations
- Ensure rapid right-clicking by multiple players only triggers the `CLAIMED` phase once.

## Required Engine Primitives
- `InteractionComponent` (Right-click).
- Single-claim resolution.
