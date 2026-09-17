# Persistence Specification

To ensure server stability, SpectraEvents does not use a fully stateless model for running instances. Instances must be capable of surviving chunk unloads, server restarts, and crashes.

## Engine Responsibilities
The engine does NOT persist every tick. Instead, it takes **Event Snapshots** upon meaningful state changes (e.g., phase transitions, significant health drops, delivery milestones).

### Data Retained Across Restarts
- Instance ID (UUID).
- Definition ID and the hashed Version/Snapshot of the definition.
- Current Phase.
- 3D Location (World, X, Y, Z).
- Active Timers (Remaining duration).
- Health / Hit Counters.
- Validated Participants and their scores (Leaderboard data).
- Claimed Rewards (to prevent duping).

## Restart Recovery Flow
1. Server starts. Engine loads all `EventDefinition` configurations.
2. Engine loads all saved `EventSnapshot` records from disk (JSON/SQLite).
3. For each snapshot, the engine reconstructs the `EventInstance`.
4. The instance resumes its active Phase.
5. If the chunks are unloaded, the instance may "hibernate" or force-load the chunk depending on config.

## Entity Reconciliation
Display entities and interaction hitboxes must be properly linked.
- **Instance exists + Entities exist**: Reconnect via PersistentDataContainer (PDC).
- **Instance exists + Entities missing**: Respawn models.
- **Entities exist + Instance missing**: Orphan cleanup (despawn entities).
