# SpectraEvents Project State

## Current Milestone

Professional 3D Model Runtime (COMPLETED)

## Current Target

Data-driven 3D model engine for Paper and Spigot platform families across Minecraft versions 26.1.1, 26.1.2, 26.2, 26.3.

## Completed

- Repository initialized
- [x] Multi-module Gradle build
- [x] Event Runtime Kernel & Phase Model
- [x] Three full event vertical slices built and migrated to 100% config-driven execution: Meteor, Airdrop, Metin.
- [x] All hardcoded dev coordinators removed.
- [x] Generic `EventExecutionEngine` stabilized with atomic claims (`try_claim`), damage tracking (`apply_damage`), health thresholds (`health_threshold_crossed`), entity death routing (`entity_death`), item rewards (`give_item`), and boss spawning (`spawn_boss`).
- [x] Decoupled platform-neutral infrastructure: `adapters/storage-sqlite` and `adapters/update-http` created with zero Bukkit/Minecraft dependencies.
- [x] Refactored package boundaries:
  - Core: `io.github.kizio806.spectraevents.core.event.runtime`, `io.github.kizio806.spectraevents.core.event.lifecycle`, `io.github.kizio806.spectraevents.core.visual.model`, `io.github.kizio806.spectraevents.core.gameplay.contribution`.
  - Application: `io.github.kizio806.spectraevents.application.config.spec`, `io.github.kizio806.spectraevents.application.config.compiled`, `io.github.kizio806.spectraevents.application.config.loader`.
- [x] Platform family layout created: `platforms/paper/common` and `platforms/paper/v26_2`.
- [x] Purpur and Folia aligned as native Paper platform family targets (using Paper distribution and RegionTaskScheduler).
- [x] Decomposed command tree (`SpectraMainCommand`, `EventCommandHandler`, `DefinitionCommandHandler`, `UpdateCommandHandler`, `DiagnosticsCommandHandler`, `IntegrationCommandHandler`, `SpectraDebugCommand`, `ModelCommandHandler`).
- [x] Decomposed Admin GUI (`AdminGuiController`, `MainScreen`, `ActiveEventsScreen`, `DefinitionsScreen`, `IntegrationsScreen`, `UpdatesScreen`).
- [x] Composition Root extracted into `PaperBootstrap.java` so plugin entrypoint `SpectraEventsPlugin.java` is a thin wrapper.
- [x] Multi-platform architecture & compatibility documentation added (`platform-compatibility.md`, `platform-versioning.md`, ADR 0004).
- [x] Comprehensive 40-point architecture and platform compatibility audit completed and verified.
- [x] Production Load Testing & Persistence Hardening:
  - SQLite backend fortified with Single-Writer Persistence Executor and WAL mode.
  - Event Runtime State is fully synchronized with SQLite for zero-data-loss crash recovery.
  - Demonstrated full correctness under intense concurrent load via `SQLiteConcurrencyBenchmarkTest` and `AirdropClaimRaceTest`.
  - Thorough Folia threading model audit confirms correctness (`GlobalRegionScheduler` for timers, `RegionScheduler`/`EntityScheduler` for actions).
- [x] Production Hardening & Integrations:
  - Added full dynamic integration resolver via `IntegrationRegistry`.
  - Integrated Optional dependencies: `LuckPerms`, `WorldGuard`, `Vault`, `PlaceholderAPI`, `MiniPlaceholders`.
  - Implemented `CustomItemProvider` with `Nexo`, `Oraxen`, and `ItemsAdder` adapters for the `give_item` action.
  - Hardened absolute timer persistence allowing recovery of queued actions after a crash or restart.
  - Built comprehensive `EntityReconciliationService` to garbage-collect orphaned models and entities at boot or reconnect them to running events.
  - Finalized `/spectra doctor` diagnostics exposing missing optional dependencies and integration status gracefully.
- [x] Multi-Platform Expansion & Sponge Scope Reduction:
  - Official platform families: Paper, Spigot.
  - Paper artifact (`SpectraEvents-<version>-paper.jar`) supports Paper, Purpur, and Folia.
  - Spigot artifact (`SpectraEvents-<version>-spigot.jar`) supports Spigot and Bukkit-compatible servers.
  - Sponge: NOT SUPPORTED, NO ADAPTER, NO ARTIFACT, NO RELEASE. Completely removed per product decision.
  - Verified 26.1.1 – 26.3 compatibility band for Paper and Spigot.
  - Configured multi-artifact release pipeline with separate Modrinth versions (`-paper`, `-spigot`) and SHA-256 verification.
- [x] Professional 3D Model Runtime:
  - Built platform-neutral domain model & math primitives (`Vector3`, `Quaternion`, `EulerRotation`, `ModelTransform`, `ModelDefinition`, `ModelPartDefinition`, `InteractionDefinition`).
  - Hierarchical matrix, pivot, scale, and intrinsic Z-X-Y Euler-to-Quaternion rotation math.
  - Data-driven YAML authoring pipeline (`plugins/SpectraEvents/models/*.yml` -> `ModelSpec` -> `ModelCompiler` -> compiled immutable `ModelDefinition`).
  - Strict validation: DFS cycle detection ($A \to B \to A$), missing parent validation, duplicate ID checks, depth limits (64).
  - Built `ModelDefinitionRegistry` and `ModelRuntimeService` with atomic spawn, rollback on failure, and idempotent removal.
  - Production platform renderers (`PaperModelRenderer` and `SpigotModelRenderer`) utilizing native `ItemDisplay`, `BlockDisplay`, `TextDisplay`, and `Interaction` entities with PDC ownership tagging.
  - Event actions (`spawn_model`, `remove_model`) and events (Meteor, Airdrop, Metin) fully migrated to the new 3D model runtime.
  - Developer/Admin CLI tool commands (`/spectra model list`, `info`, `validate`, `spawn`, `remove`) and `/spectra doctor` 3D metrics.

## In Progress

- Next milestone preparation.

## Next Planned Milestone

Professional 3D Animation & Timeline Engine (keyframes, timeline, interpolation curves, play/pause/resume, animation state machines).

## Important Active Decisions

- Official platform scope is strictly limited to Paper Family (Paper, Purpur, Folia) and Spigot Family (Spigot, Bukkit).
- Sponge is NOT supported; all Sponge modules, artifacts, and release tasks are removed.
- Dependency direction is strictly `platform -> application -> core`.
- Core, application, and public API have zero Bukkit, Paper, NMS, or CraftBukkit dependencies.
- Infrastructure (SQLite storage, HTTP update client) resides in platform-neutral `adapters/*` modules.
- Concrete platform implementations are prefixed by their family name (e.g., `PaperActionAdapter`).
- Purpur and Folia use the Paper distribution without duplicate adapter code.
- All event execution is 100% event-driven without global tick loops.
- SQLite persistence uses a Single-Writer asynchronous queue pattern, strictly isolating disk I/O from server thread pools while eliminating SQLITE_BUSY deadlocks.

## Last Updated

2026-09-17
