# Scope

## V1 Scope
The V1 release will focus on delivering a stable, data-driven foundation capable of running several distinct event types (e.g., Meteor, Airdrop, Metin, Pinata) natively.

Key V1 inclusions:
- Data-driven event definitions via YAML.
- Event lifecycle and phase transitions.
- Timers and health/hit mechanics.
- Native display-based visuals (Item/Block/Text Displays, Interactions).
- Bossbars and holograms.
- Simple linear animations.
- Loot and reward distribution logic.
- Leaderboards (damage, interactions).
- Spawn strategies (random surface, fixed location).
- Event areas (basic entry/exit and rule enforcement).
- Persistence and recovery.
- Robust debug tooling and admin commands.
- Primary target: Paper 26.2 (Java 25).

## Post-V1 Scope
These features are highly desirable but deferred until the V1 foundation is proven:
- Full GUI inventory editor (frontend for configuration).
- Complex skeletal animations and external resource-pack compilation.
- Database persistence (SQLite, PostgreSQL).
- Advanced placeholder integration (PlaceholderAPI) for all internal states.
- Cross-server synchronization (Redis/velocity support).
- Deep third-party integrations (Vault, Oraxen, Nexo, ItemsAdder).
- Complex boss AI and full mob wave scripting.

## Explicitly Out of Scope
- Complete replacement of standard region management (WorldGuard).
- Full economy or RPG leveling systems.
- Scripting engines (Nashorn/GraalVM JS) for event logic.
- 1.8 - 1.20 support (the engine targets modern Paper API exclusively).
