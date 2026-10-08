# Scope

## V1 Scope
The V1 release focuses on a stable, data-driven foundation capable of running several distinct event
types through the same runtime. Meteor, Airdrop, Metin, Piñata and Boss Portal are shipped as
inactive presets, not hard-coded mechanics.

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
- Paper/Purpur and Spigot/CraftBukkit distributions across 26.1–26.3, plus Folia 26.1–26.2, with
  Java 25. Every published version still requires its complete real-server matrix.

## Post-V1 Scope
These features are highly desirable but deferred until the V1 foundation is proven:
- Full GUI inventory editor (frontend for configuration).
- Complex skeletal animation and arbitrary mesh rendering. The current resource-pack generator emits
  cuboid geometry.
- PostgreSQL storage; the current durable storage contract is SQLite.
- Advanced placeholder integration (PlaceholderAPI) for all internal states.
- Cross-server synchronization (Redis/velocity support).
- Deep third-party integrations (Vault, Oraxen, Nexo, ItemsAdder).
- Complex boss AI and full mob wave scripting.

## Explicitly Out of Scope
- Complete replacement of standard region management (WorldGuard).
- Full economy or RPG leveling systems.
- Scripting engines (Nashorn/GraalVM JS) for event logic.
- Legacy Minecraft support; the engine targets the declared modern Paper-family and Spigot-family
  range only.
