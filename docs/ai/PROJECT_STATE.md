# SpectraEvents Project State

## Current Milestone

1.0 implementation in progress — the shared large-encounter foundation includes zones,
per-definition capacity, tracked waves, contribution checkpoints, participant checkpoints and a
durable reward mailbox. Meteor, Airdrop, Metin, Piñata and Boss Portal are shipped as inactive
presets in `events/presets/`; an administrator explicitly copies a preset into `events/` to activate
it. The retired public `examples/events` catalogue no longer participates in validation.

## Current Target

Data-driven 3D model engine for Paper, Purpur, Spigot, and CraftBukkit across Minecraft versions
26.1, 26.2, and 26.3, plus Folia 26.1–26.2. Folia 26.3 is refused before initialization.

## Asset Workflow Status

- Native server-side model definitions and Display/Interaction rendering are active.
- Blockbench `.bbmodel` is the canonical official visual source; compatible signed `.spectra.zip`
  bundles also import into the normal model/animation registries and build a local verified
  resource-pack ZIP.
- Delivery is disabled by default. Both platform families accept an administrator-owned HTTPS ZIP
  with SHA-1 or a configured Modrinth resource-pack project; blank `modrinthVersionId` selects the
  matching `<plugin-version>+<Minecraft release line>` automatically. Descriptor resolution failure
  never reports ready.
- Real-client delivery acceptance is still required for each hosted pack and target profile.

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
- [x] Purpur and Folia aligned as Paper-family targets using the Paper distribution and region/entity schedulers; Folia 26.1–26.2 is supported and Folia 26.3+ is explicitly refused before startup.
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
  - Paper artifact (`SpectraEvents-<version>-paper.jar`) supports Paper/Purpur 26.1–26.3 and Folia 26.1–26.2.
  - Spigot artifact (`SpectraEvents-<version>-spigot.jar`) supports Spigot and Bukkit-compatible servers.
  - Sponge: NOT SUPPORTED, NO ADAPTER, NO ARTIFACT, NO RELEASE. Completely removed per product decision.
  - Release workflow targets the 26.1–26.3 compatibility band and must block publication unless its real-server matrix passes.
  - Configured a platform-specific Modrinth release pipeline: Paper/Purpur, Spigot/CraftBukkit,
    and Folia each receive a dedicated version with exactly one compatible JAR, plus SHA-256
    verification on GitHub Releases.
- [x] Professional 3D Model Runtime:
  - Built platform-neutral domain model & math primitives (`Vector3`, `Quaternion`, `EulerRotation`, `ModelTransform`, `ModelDefinition`, `ModelPartDefinition`, `InteractionDefinition`).
  - Hierarchical matrix, pivot, scale, and intrinsic Z-X-Y Euler-to-Quaternion rotation math.
  - Blockbench asset pipeline (`plugins/SpectraEvents/assets/source/*.bbmodel|*.spectra.zip` -> imported document -> `ModelCompiler` -> compiled immutable `ModelDefinition`).
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
  - Opt-in HTTPS/SHA-1 or Modrinth delivery configuration with cache, reconnect, decline and failure
    handling covered by JVM tests; release CI builds and publishes one pack per supported release line.
- [x] M3 reference events:
  - Meteor, Airdrop, Metin, Piñata, and Boss Portal are bundled YAML definitions with paired model
    YAML, user documentation, configuration/model validation, and shared cleanup/recovery behavior.
  - Added the platform-neutral durable `HitCounter` primitive. Piñata uses `increment_hits` and
    `hits_reached`; a SQLite recovery test verifies the counter survives a restart.
  - Boss Portal composes the shared model animation, timer, boss spawn, entity-death routing, reward,
    and cleanup contracts without an event-specific coordinator.
- [x] M5 admin GUI and documentation:
  - Dashboard, Active Events, Definitions, Configuration, and Locations screens with full pagination.
  - Configuration detail screen with per-parameter override editing, persisted beside the event in
    `events/overrides/<id>.yml`; global `config.yml` owns the default locale only.
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
- All five reference event definitions (Meteor, Airdrop, Metin, Piñata, and Boss Portal) use the
  data-driven phased contract. They remain inactive until copied from `events/presets/` into `events/`.
- Blockbench `.bbmodel` is the canonical visual source. Resource-pack delivery defaults to disabled
  until an administrator configures a verified HTTPS ZIP/SHA-1 or the real Modrinth resource-pack
  project; startup remains available for configuration and diagnostics.
- The global Event BossBar Manager is fully implemented and handles up to three concurrent bossbars with one-second refreshes.
- ADR 0008 supersedes the manual-publication part of ADR 0007: CI publishes distinct Modrinth packs
  per release line, while player delivery still stays disabled until real-client verification.
- Release eligibility checks artifacts, the CI runtime matrix and the resource-pack evidence manifest.
  Complete current-build runtime evidence and, when delivery is enabled, real-client acceptance remain
  required before publication.

## Next Planned Milestone

V1 release candidate: confirm the real-server matrix on the current build, prepare the worktree (confirm deletions, split commits), cut the release, and publish artifacts. No public addon API is planned without a concrete demonstrated use case; ADR 0005 remains in effect.

## Important Active Decisions

- Official platform scope is Paper/Purpur 26.1–26.3, Folia 26.1–26.2, and Spigot/CraftBukkit 26.1–26.3.
- Sponge is NOT supported; all Sponge modules, artifacts, and release tasks are removed.
- Dependency direction is strictly `platform -> application -> core`.
- Core and application have zero Bukkit, Paper, NMS, or CraftBukkit dependencies. The premature empty public API module was removed by ADR 0005.
- Infrastructure (SQLite storage, HTTP update client) resides in platform-neutral `adapters/*` modules.
- Concrete platform implementations are prefixed by their family name (e.g., `PaperActionAdapter`).
- Purpur and supported Folia versions use the Paper distribution without duplicate adapter code.
- All event execution is 100% event-driven without global tick loops.
- SQLite persistence uses a Single-Writer asynchronous queue pattern, strictly isolating disk I/O from server thread pools while eliminating SQLITE_BUSY deadlocks.
- Event animation callbacks use a dedicated global scheduler channel and are cancelled with platform shutdown cleanup; they are never scheduled with a null event-map key.

## Last Updated

2026-10-03
