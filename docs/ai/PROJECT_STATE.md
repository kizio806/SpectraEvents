# SpectraEvents Project State

## Current Milestone

1.0 implementation in progress — the shared large-encounter foundation now includes zones,
per-definition capacity, tracked waves, contribution checkpoints, participant checkpoints and a
durable reward mailbox. Metin, Airdrop, Meteor, Piñata and Boss Portal are bundled definitions;
Piñata and Boss Portal are being migrated to their final phased contracts. The retired public
`examples/events` catalogue is intentionally removed and no longer participates in validation.

## Current Target

Data-driven 3D model engine for Paper and Spigot platform families across Minecraft versions 26.1, 26.2, 26.3.

## Asset Workflow Status

- Native server-side model definitions and Display/Interaction rendering are active.
- Signed `.spectra.zip` Generic Model bundles import into the normal model/animation registries and
  build a local verified resource-pack ZIP.
- Delivery now accepts a pinned Modrinth version ID on both platform families; required-delivery
  failure or refusal disconnects the player. Publication and full runtime certification remain open.
- The operator confirmed custom-model-data rendering and the real-player delivery workflow after M2.
  Exact server/client versions were not recorded and are not inferred retrospectively.

## Completed

- Repository initialized
- [x] Multi-module Gradle build
- [x] Event Runtime Kernel & Phase Model
- [x] Three full event vertical slices built and migrated to 100% config-driven execution: Meteor, Airdrop, Metin.
- [x] All hardcoded dev coordinators removed.
- [x] Generic `EventExecutionEngine` with engine-owned durable at-most-once claim acceptance, damage tracking (`apply_damage`), health thresholds (`health_threshold_crossed`), entity death routing (`entity_death`), item rewards (`give_item`), and boss spawning (`spawn_boss`). External delivery remains crash-ambiguous and requires operator reconciliation.
- [x] Decoupled platform-neutral infrastructure: `adapters/storage-sqlite` and `adapters/update-http` created with zero Bukkit/Minecraft dependencies.
- [x] Refactored package boundaries:
  - Core: `io.github.kizio806.spectraevents.core.event.runtime`, `io.github.kizio806.spectraevents.core.event.lifecycle`, `io.github.kizio806.spectraevents.core.visual.model`, `io.github.kizio806.spectraevents.core.gameplay.contribution`.
  - Application: `io.github.kizio806.spectraevents.application.config.spec`, `io.github.kizio806.spectraevents.application.config.compiled`, `io.github.kizio806.spectraevents.application.config.loader`.
- [x] Platform family layout created: `platforms/paper/common` and `platforms/spigot/common`, with separate `distributions/paper` and `distributions/spigot` artifacts.
- [x] Purpur and Folia aligned as Paper-family targets using the Paper distribution and region/entity schedulers; Folia's unsupported Bukkit scoreboard API is surfaced as an explicit capability warning.
- [x] Decomposed command tree (`SpectraMainCommand`, `EventCommandHandler`, `DefinitionCommandHandler`, `UpdateCommandHandler`, `DiagnosticsCommandHandler`, `IntegrationCommandHandler`, `SpectraDebugCommand`, `ModelCommandHandler`).
- [x] Decomposed Admin GUI (`AdminGuiController`, `MainScreen`, `ActiveEventsScreen`, `DefinitionsScreen`, `IntegrationsScreen`, `UpdatesScreen`).
- [x] Composition Root extracted into `PaperBootstrap.java` so plugin entrypoint `SpectraEventsPlugin.java` is a thin wrapper.
- [x] Multi-platform architecture & compatibility documentation added (`platform-compatibility.md`, `platform-versioning.md`, ADR 0004).
- [x] Comprehensive 40-point architecture and platform compatibility audit completed and verified.
- [x] Production Load Testing & Persistence Hardening:
  - SQLite backend fortified with Single-Writer Persistence Executor and WAL mode.
  - Event runtime state and initial instance state are durably persisted through the single writer; external Minecraft side effects cannot be made transactionally exactly-once with SQLite.
  - Demonstrated full correctness under intense concurrent load via `SQLiteConcurrencyBenchmarkTest` and `AirdropClaimRaceTest`.
  - Platform actions use `GlobalRegionScheduler`, `RegionScheduler`, and `EntityScheduler` according to ownership; release claims remain gated by the real-server matrix.
- [x] Production Hardening & Integrations:
  - Added full dynamic integration resolver via `IntegrationRegistry`.
  - Integrated Optional dependencies: `LuckPerms`, `WorldGuard`, `Vault`, `PlaceholderAPI`, `MiniPlaceholders`.
  - Implemented `CustomItemProvider` with `Nexo`, `Oraxen`, and `ItemsAdder` adapters for the `give_item` action.
  - Hardened absolute timer persistence allowing recovery of queued actions after a crash or restart.
  - Built comprehensive `EntityReconciliationService` to garbage-collect orphaned models and entities at boot or reconnect them to running events.
  - Finalized `/spectraevents doctor` diagnostics exposing missing optional dependencies and integration status gracefully.
- [x] Multi-Platform Expansion & Sponge Scope Reduction:
  - Official platform families: Paper, Spigot.
  - Paper artifact (`SpectraEvents-<version>-paper.jar`) supports Paper, Purpur, and Folia.
  - Spigot artifact (`SpectraEvents-<version>-spigot.jar`) supports Spigot and Bukkit-compatible servers.
  - Sponge: NOT SUPPORTED, NO ADAPTER, NO ARTIFACT, NO RELEASE. Completely removed per product decision.
  - Release workflow targets the 26.1–26.3 compatibility band and must block publication unless its real-server matrix passes.
  - Configured multi-artifact release pipeline with separate Modrinth versions (`-paper`, `-spigot`) and SHA-256 verification.
- [x] Professional 3D Model Runtime:
  - Built platform-neutral domain model & math primitives (`Vector3`, `Quaternion`, `EulerRotation`, `ModelTransform`, `ModelDefinition`, `ModelPartDefinition`, `InteractionDefinition`).
  - Hierarchical matrix, pivot, scale, and intrinsic Z-X-Y Euler-to-Quaternion rotation math.
  - Data-driven YAML authoring pipeline (`plugins/SpectraEvents/models/*.yml` -> `ModelSpec` -> `ModelCompiler` -> compiled immutable `ModelDefinition`).
  - Strict validation: DFS cycle detection ($A \to B \to A$), missing parent validation, duplicate ID checks, depth limits (64).
  - Built `ModelDefinitionRegistry` and `ModelRuntimeService` with atomic spawn, rollback on failure, and idempotent removal.
  - Production platform renderers (`PaperModelRenderer` and `SpigotModelRenderer`) utilizing native `ItemDisplay`, `BlockDisplay`, `TextDisplay`, and `Interaction` entities with PDC ownership tagging.
  - Event actions (`spawn_model`, `play_animation`, `move_model`, `remove_model`) and events (Meteor, Airdrop, Metin) use the shared 3D model runtime; named model animations can be started from YAML and are stopped before cleanup.
- [x] Professional 3D Animation & Timeline Engine:
  - Built platform-neutral animation math primitives (`AnimationTime`, `AnimationDuration`, `Easing`, `RotationMode`, `LoopMode`, `Vector3Keyframe`, `RotationKeyframe`, `ScaleKeyframe`, `TimelineCue`, `AnimationDefinition`).
  - Implemented Quaternion SLERP with shortest-path sign handling ($q$ vs $-q$) and continuous Euler angle lerp ($0^\circ \to 720^\circ$ multi-turn spins).
  - Built DTO specs (`AnimationSpec`, `KeyframeSpec`, `TimelineCueSpec`) and `AnimationCompiler` with precompiled segment breakdown (50ms sub-steps for non-linear easing curves).
  - Built `AnimationDefinitionRegistry`, `ActiveAnimationRegistry`, and `AnimationRuntimeService` for playback lifecycle control (`play`, `pause`, `resume`, `stop`, `seek`).
  - Native client-side Display interpolation (`setInterpolationDuration`) via `ModelRendererPort` updates (`PaperModelRenderer` and `SpigotModelRenderer`), running with zero server tick loops.
  - Event-driven timeline cue callbacks (`AnimationCueReached`) and recovery policy support (`RESTART`, `RESUME`, `STOP`).
  - Unit tests covering math, easing, compiler, playback state machine, and interpolation dispatch.
- [x] M1 Authoring Contract for Custom Events:
  - Stabilized the schema-v1 authoring contract for models, named animations, phases, triggers, conditions, actions, rewards, recovery, and cleanup.
  - Added source-path validation diagnostics, three verified YAML examples, and the copy/validate/reload/start/diagnose administrator guide.
  - Connected named model animations through `play_animation`, including cleanup and restart/recovery handling.
  - Running instances retain the definition snapshot captured at start when definitions are reloaded.
- [x] M2 implementation foundation:
  - One signed Blockbench Generic Model `.spectra.zip` format with checksums, strict layout and
    hostile-input limits.
  - Imported cubes, textures, group hierarchy, pivots and named animations compile into ordinary
    `spawn_model` / `play_animation` definitions.
  - Deterministic resource-pack ZIP output with generated manifest and custom-model-data mapping.
  - Opt-in HTTPS/SHA-1 delivery configuration with cache, reconnect, decline and failure handling
  covered by JVM tests; no automatic hosting or Modrinth publishing.
- [x] M3 reference events:
  - Meteor, Airdrop, Metin, Piñata, and Boss Portal are bundled YAML definitions with paired model
    YAML, user documentation, configuration/model validation, and shared cleanup/recovery behavior.
  - Added the platform-neutral durable `HitCounter` primitive. Piñata uses `increment_hits` and
    `hits_reached`; a SQLite recovery test verifies the counter survives a restart.
  - Boss Portal composes the shared model animation, timer, boss spawn, entity-death routing, reward,
    and cleanup contracts without an event-specific coordinator.
- [x] M5 admin GUI and documentation:
  - Dashboard, Active Events, Definitions, Configuration, and Locations screens with full pagination.
  - Configuration detail screen with profile cycling and per-parameter override editing (persisted
    to `event-settings.yml`).
  - Definition detail with phase overview and Start button using saved profile + named location.
  - Cancel confirmation, instance detail, and location remove confirmation screens.
  - Named location management via GUI and `/spectraevents location` commands.
  - `EventDefinitionProfileApplier` applying health/damage/hits/duration/lock-duration overrides.
  - `EventGuiPagination` utility with regression tests.
  - All five event definitions complete with bossbars, scoreboards, titles, HUD placeholders,
    health thresholds, spawn waves, loot drops, cleanup, and timeout phases.
  - Smoke test command prefix corrected from `event event` to `spectraevents event`.
  - `admin-workflow.md` updated to describe the shipped GUI instead of the old "Future GUI" note.
  - All docs migrated to `/spectraevents` — no `/event` alias references remain.

## Current State

- M5 admin GUI work remains complete.
- All five bundled event definitions (Meteor, Airdrop, Metin, Piñata, and Boss Portal) have been fully migrated to their final, data-driven phased contracts using zones, tracked waves, contribution checkpoints, and durable reward mailboxes.
- Resource-pack delivery is now strictly mandatory on all platforms (Paper and Spigot). The fallback YAML models have been removed, and startup is actively blocked if delivery is not configured.
- The global Event BossBar Manager is fully implemented and handles up to three concurrent bossbars with one-second refreshes.
- All out-of-date documentation (profiles, sidebars, old Metin phases, Modrinth delivery block) has been replaced. ADR 0006 now mandates Modrinth resource-pack delivery.
- M4 is complete: release eligibility is executable, artifacts/checksums and recovery procedures are documented, and Paper/Purpur + Spigot/CraftBukkit 26.1–26.3 and Folia 26.1–26.2 have real-server coverage.

## Next Planned Milestone

V1 release candidate: confirm the real-server matrix on the current build, prepare the worktree (confirm deletions, split commits), cut the release, and publish artifacts. No public addon API is planned without a concrete demonstrated use case; ADR 0005 remains in effect.

## Important Active Decisions

- Official platform scope is strictly limited to Paper Family (Paper, Purpur, Folia) and Spigot Family (Spigot, Bukkit).
- Sponge is NOT supported; all Sponge modules, artifacts, and release tasks are removed.
- Dependency direction is strictly `platform -> application -> core`.
- Core and application have zero Bukkit, Paper, NMS, or CraftBukkit dependencies. The premature empty public API module was removed by ADR 0005.
- Infrastructure (SQLite storage, HTTP update client) resides in platform-neutral `adapters/*` modules.
- Concrete platform implementations are prefixed by their family name (e.g., `PaperActionAdapter`).
- Purpur and Folia use the Paper distribution without duplicate adapter code.
- All event execution is 100% event-driven without global tick loops.
- SQLite persistence uses a Single-Writer asynchronous queue pattern, strictly isolating disk I/O from server thread pools while eliminating SQLITE_BUSY deadlocks.
- Event animation callbacks use a dedicated global scheduler channel and are cancelled with platform shutdown cleanup; they are never scheduled with a null event-map key.

## Last Updated

2026-09-27
