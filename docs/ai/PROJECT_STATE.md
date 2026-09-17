# SpectraEvents Project State

## Current Milestone

SpectraEvents Production Load Testing & Persistence Hardening

## Current Target

Paper 26.2 (Java 25 runtime); Platform-neutral core and application target Java 21.

## Completed

- Repository initialized
- [x] Multi-module Gradle build
- [x] Event Runtime Kernel & Phase Model
- [x] Three full event vertical slices built and migrated to 100% config-driven execution: Meteor, Airdrop, Metin.
- [x] All hardcoded dev coordinators removed.
- [x] Generic `EventExecutionEngine` stabilized with atomic claims (`try_claim`), damage tracking (`apply_damage`), health thresholds (`health_threshold_crossed`), entity death routing (`entity_death`), item rewards (`give_item`), and boss spawning (`spawn_boss`).
- [x] Decoupled platform-neutral infrastructure: `adapters/storage-sqlite` and `adapters/update-http` created with zero Bukkit/Minecraft dependencies.
- [x] Refactored package boundaries:
  - Core: `dev.spectraevents.core.event.runtime`, `dev.spectraevents.core.event.lifecycle`, `dev.spectraevents.core.visual.model`, `dev.spectraevents.core.gameplay.contribution`.
  - Application: `dev.spectraevents.application.config.spec`, `dev.spectraevents.application.config.compiled`, `dev.spectraevents.application.config.loader`.
- [x] Platform family layout created: `platforms/paper/common` and `platforms/paper/v26_2`.
- [x] Purpur and Folia aligned as native Paper platform family targets (using Paper distribution and RegionTaskScheduler).
- [x] Decomposed command tree (`SpectraMainCommand`, `EventCommandHandler`, `DefinitionCommandHandler`, `UpdateCommandHandler`, `DiagnosticsCommandHandler`, `IntegrationCommandHandler`, `SpectraDebugCommand`).
- [x] Decomposed Admin GUI (`AdminGuiController`, `MainScreen`, `ActiveEventsScreen`, `DefinitionsScreen`, `IntegrationsScreen`, `UpdatesScreen`).
- [x] Composition Root extracted into `PaperBootstrap.java` so plugin entrypoint `SpectraEventsPlugin.java` is a thin wrapper.
- [x] Multi-platform architecture & compatibility documentation added (`platform-compatibility.md`, `platform-versioning.md`, ADR 0004).
- [x] Comprehensive 40-point architecture and platform compatibility audit completed and verified.
- [x] Production Load Testing & Persistence Hardening:
  - SQLite backend fortified with Single-Writer Persistence Executor and WAL mode.
  - Event Runtime State is fully synchronized with SQLite for zero-data-loss crash recovery.
  - Demonstrated full correctness under intense concurrent load via `SQLiteConcurrencyBenchmarkTest` and `AirdropClaimRaceTest`.
  - Thorough Folia threading model audit confirms correctness (`GlobalRegionScheduler` for timers, `RegionScheduler`/`EntityScheduler` for actions).

## In Progress

None.

## Next Planned Milestone

SpectraEvents Addon API & Custom Content Expansion (Pinata, Vault, Boss Portal Events)

## Important Active Decisions

- Dependency direction is strictly `platform -> application -> core`.
- Core, application, and public API have zero Bukkit, Paper, NMS, or CraftBukkit dependencies.
- Infrastructure (SQLite storage, HTTP update client) resides in platform-neutral `adapters/*` modules.
- Concrete platform implementations are prefixed by their family name (e.g., `PaperActionAdapter`).
- Purpur and Folia use the Paper distribution without duplicate adapter code.
- All event execution is 100% event-driven without global tick loops.
- SQLite persistence uses a Single-Writer asynchronous queue pattern, strictly isolating disk I/O from server thread pools while eliminating SQLITE_BUSY deadlocks.

## Last Updated

2026-09-17
