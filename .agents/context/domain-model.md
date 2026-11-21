# Domain Model Context — SpectraEvents

## Core Entities & Concepts

### 1. EventDefinition
The declarative template defining a world event:
- Unique identifier (`id`)
- Display metadata (name, description, tags)
- Configuration parameters and variable overrides
- Component attachments (3D models, hitboxes, particle emitters, sound loops)
- Phase graph (ordered sequence or directed transitions)

### 2. EventInstance
A live, executing event instance situated in the Minecraft world:
- Assigned unique instance UUID
- Current phase reference
- Progress counters, health pools, and participant scores
- Active display entity UUIDs and hitbox entity tracking

### 3. EventPhase & PhaseTransition
- `EventPhase`: Individual stage of an event with specific objectives (e.g. `SPAWN`, `COMBAT`, `PUZZLE`, `VICTORY`).
- `Trigger`: Condition evaluator (e.g. timer elapsed, damage dealt, mob defeated, player interaction).
- `Action`: Consequence executed on transition (e.g. spawn mobs, play cinematic, trigger particles, grant rewards).

### 4. RewardMailbox & Transactional Recovery
- In-memory event rewards are persisted to transactional SQLite mailbox tables.
- Offline players receive pending rewards automatically upon reconnecting.
- Active events recover state cleanly upon server restarts without entity duplication.
