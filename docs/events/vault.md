# Vault Event Specification

## Purpose
A multi-stage hacking/looting event requiring players to hold an area, perform sequential interactions, and defend their claim.

## Player Experience
Players find a locked Vault. They start a "hacking" process which takes time. Other players can interrupt them. Once open, the Vault contains high-tier loot.

## Event Lifecycle
Standard global lifecycle.

## Phases

1. **LOCKED**
   - The vault is closed.
   - **Transitions**: Player interact -> `HACKING`.
2. **HACKING**
   - A timer starts. A hologram shows progress.
   - **Transitions**:
     - Timer expires -> `OPEN`.
     - Hacking interrupted (e.g., player leaves area) -> `LOCKED` (reset).
3. **OPEN**
   - The vault door model opens.
   - **Transitions**: Player interacts -> `LOOTING`.
4. **LOOTING**
   - Distributes loot to the hacking party.
   - **Transitions**: Immediate -> `EMPTY`.
5. **EMPTY**
   - The vault remains open but is empty for a cooldown period.
   - **Transitions**: Timer expires -> `CLEANUP` (or `LOCKED` for recurring).
6. **CLEANUP**
   - Standard instance removal.

## Spawn
- **Strategy**: Fixed location (e.g., inside a custom generated structure).

## Visuals
- Multipart Model (Base vault, moving door).
- Hologram showing hacking progress bar.

## Interactions
- Right-click to begin hacking.
- Right-click to loot.

## Components
- `ModelComponent`, `TimerComponent`, `InteractionComponent`, `AreaComponent`.

## Triggers
- `player-interact`, `timer-expired`, `player-leave-area`.

## Conditions
- `player-in-area` required to continue hacking timer.

## Actions
- `play-animation` (open door), `give-reward`.

## Rewards
- **Distribution**: Given to the specific player or party that completed the hack.

## Leaderboard
- None.

## Event Area
- Very strict. If the hacker leaves the area, the `HACKING` phase is interrupted. PvP is enabled.

## Persistence & Restart Recovery
- If hacking is in progress, a restart resets it to `LOCKED`. If `EMPTY`, retains the cooldown timer.

## Required Engine Primitives
- Interruptible timers.
- Area membership tracking (`player-leave-area` trigger).
