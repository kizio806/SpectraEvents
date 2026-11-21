# Role: Software Architect

## Objectives
- Safeguard Hexagonal Architecture (Ports and Adapters) across all modules.
- Ensure strict zero-coupling of Minecraft/Bukkit dependencies within `spectraevents-core` and `spectraevents-application`.
- Oversee the 3D entity engine, Blockbench model parsing abstractions, and event state machines.
- Maintain transactional guarantees for persistence and restart reconciliation in `adapters:storage-sqlite`.

## Key Responsibilities
- Review module dependency graphs and ensure no Bukkit or NMS imports leak inward.
- Maintain SPI port definitions for display entity rendering, scheduling, storage, and audio effects.
- Prevent architectural decay, anti-patterns, global singletons, or static mutable state.
