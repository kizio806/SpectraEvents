# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.0] - 2026-11-09

### Added
- **Paper Command Decomposition & Architecture Parity**: Extracted `ScheduleCommandHandler` and `RewardsCommandHandler` in `:platforms:paper:common:command`, achieving architectural symmetry with Spigot and removing cross-platform translation key coupling (`command.paper.*` keys added across all 6 locales).
- **Spec Math Deduplication**: Extracted `SpecMathParser` in `:spectraevents-application:model:compiler`, eliminating identical 3D vector, euler rotation, and quaternion parsing logic between `ModelCompiler` and `AnimationCompiler`.
- **Model Transform Teleport Deduplication**: Generalized model transform updates to template method `applyTransformTeleports` in `AbstractBukkitModelRenderer`, eliminating duplicated entity traversal and handle mapping between Paper and Spigot.
- **Source Hygiene & Clean Imports**: Cleared all inline fully qualified names (FQNs) across domain, application, and platform modules in favor of clean single-class imports. Restored `CODE_OF_CONDUCT.md` and cleaned PR template and CI workflows.
- **Full Integration & Custom Item Platform Parity**: Relocated third-party plugin integrations (Vault, LuckPerms, WorldGuard, PlaceholderAPI) and custom item providers (Nexo, Oraxen, ItemsAdder) from `:platforms:paper:common` to `:platforms:bukkit:integration`, establishing 100% integration parity for Spigot distributions with unified `BukkitIntegrationManager`.
- **Shared Entity Reconciler & Model Handles**: Extracted `AbstractBukkitEntityReconciler` into `:platforms:bukkit:lifecycle` and unified `BukkitModelInstanceHandle` in `:platforms:bukkit:render`, eliminating duplicate chunk scanning, corrupted entity handling, and parallel model handle classes.
- **Shared Schedule Bootstrap**: Introduced `BukkitScheduleBootstrapHelper` in `:platforms:bukkit:config`, deduplicating YAML schedule loading between platform bootstraps.
- **Comprehensive Platform Tests**: Added unit tests for `BukkitModelInstanceHandleTest`, `AbstractBukkitEntityReconcilerTest`, `BukkitScheduleBootstrapHelperTest`, `BukkitIntegrationManagerTest`, `SpigotBossBarManagerTest`, `BukkitBossBarHolderTest`, `BukkitBossBarProgressCalculatorTest`, `GuiTextTest`, `CommandTextTest`, `SpigotUpdateCommandHandlerTest`, and `SpigotAssetCommandHandlerTest`.

### Changed
- **Code Deduplication**: Eliminated duplicate `PaperModelInstanceHandle`, `SpigotModelInstanceHandle`, `PaperResourcePackDeliveryBootstrap`, `SpigotResourcePackDeliveryBootstrap`, and ghost class `PaperEventSettingsStore`, standardizing on canonical `BukkitEventSettingsStore` and direct bootstrap calls.
- **Naming Harmonization**: Renamed Paper manager classes and test fixtures to `PaperBossBarManager` and `PaperScoreboardManager` for symmetry with Spigot counterparts.
- **Extracted Shared BossBar Progress Calculator**: Centralized BossBar progress calculations in `:platforms:bukkit:bossbar:BukkitBossBarProgressCalculator`.
- **Spigot BossBar State Architecture**: Refactored `SpigotBossBarManager` to use unified atomic `BukkitBossBarHolder`, resolving multi-map concurrency hazards and fixing a bug where boss bar progress got reset to 1.0 on every HUD refresh.
- **i18n Neutrality**: Harmonized `command.gui-player-only` across all 6 localization catalogs (`en-US`, `pl-PL`, `de-DE`, `es-ES`, `fr-FR`, `pt-BR`) eliminating platform leakage in command messages.
- **Quality Pipeline Cleanliness**: Consolidated SonarCloud configuration as single source of truth in `build.gradle.kts`, removed redundant root `sonar-project.properties`, renamed to standard `.codecov.yml`, and included platforms in Codecov tracking.
- **Admin GUI Portability & Spigot Feature Parity**: Relocated the entire Admin GUI hierarchy (17 screens, view navigation, pagination) to `:platforms:bukkit:gui` with `GuiText.legacy` component bridging. Added modular Spigot administration subcommands (`location`, `model`, `asset`, `integration`, `update`, `admin`) achieving 100% parity with Paper.
- **Shared Model Renderer**: Introduced `AbstractBukkitModelRenderer` in `:platforms:bukkit:render`, consolidating common Display/Interaction entity math, transformation matrices, billboard settings, and PDC tagging while reducing boilerplate by ~800 lines.
- **Static Analysis & Hygiene Hardening**: Eliminated blanket rule suppressions in `spotbugs-exclude.xml` and `pmd.xml`. Resolved all potential null dereference paths, unclosed resources in unit/integration tests, and replaced generic `catch (RuntimeException)` blocks with typed domain exceptions (`FatalActionException`, `IllegalStateException`, `IllegalArgumentException`, `IOException`).
- **Toolchain & Dependency Locks**: Upgraded Gradle wrapper to 9.8.2 and JaCoCo to 0.8.15 with verified lockfile synchronization across all modules. Removed unused `gradle/compatibility.versions.toml`.
- **Unified Distribution Publishing**: Consolidated Modrinth/CurseForge deployment to exactly two release entries (Paper/Purpur/Folia and Spigot/CraftBukkit), eliminating redundant duplicate Folia upload entries while declaring native Folia 26.1–26.3 support. Added curated release notes at `docs/releases/0.1.1-beta.2.md`.
- **Zero Platform Duplication**: Extracted common Bukkit, Paper, and Spigot logic into `:platforms:bukkit`, eliminating duplicate configuration bootstraps, capability queries, lifecycle reporters, and resource-pack delivery adapters.
- **Test Coverage Transparency**: Removed all platform coverage exclusions from JaCoCo root aggregation, SonarCloud analysis, and Codecov configurations. All platform code is now included in testing and coverage gates.
- **Defensive API Contracts**: Enforced strict `Objects.requireNonNull` validation across platform action adapters, schedulers, and resource cleaners.

### Fixed
- **Resource Pack Status Mapping on Spigot (`SE-BUG-01`)**: Corrected handling of `DOWNLOADED`, `INVALID_URL`, `FAILED_RELOAD`, and `DISCARDED` resource pack statuses in `AbstractBukkitPlayerResourcePackAdapter`, preventing Spigot servers from erroneously kicking players upon downloading required packs.
- **Concurrency Invariants**: Thread-safe item providers using `CopyOnWriteArrayList` in `AbstractBukkitActionAdapter` and atomic `peakQueueDepth.accumulateAndGet(depth, Math::max)` in `SingleWriterPersistenceExecutor`.
- **Spigot Plugin Lifecycle**: Hardened `SpectraEventsSpigotPlugin` with safe shutdown and audience teardown on startup failure.

### Security
- **Atomic Concurrency**: Resolved TOCTOU race condition in `AnimationRuntimeService` by enforcing strict lock acquisition and double-checked registration state before animation step advancement.
- **Cryptographic Contract**: Resolved CodeQL SHA-1 weak cryptography alert in `ResourcePackBuilder` through explicit protocol-mandated pack hash method and Javadoc contract.
- **Encapsulation Hardening**: Eliminated internal representation leaks in `EngineContext` and `SpectraAssetFace`, replacing exposed internal maps and lists with defensive copies (`List.copyOf`) and encapsulated domain execution methods.
- **Exception Logging**: Ensured all caught exceptions in `EventLifecycleManager` log underlying cause and stack traces without silent omission.
- **CodeQL Deprecation Cleansing**: Addressed deprecations across Spigot adapters reflectively to ensure clean multi-module CodeQL builds without `@SuppressWarnings`.

## [0.1.1-beta.2] - 2026-10-08

### Added
- **Public beta release pipeline**: a signed release tag builds, verifies and publishes the Paper,
  Spigot and Folia-compatible distributions together with one version-specific resource pack for
  each supported Minecraft release line.
- **Official asset positioning**: the three generated resource-pack ZIPs are published first to the
  separate SpectraEvents Assets Modrinth project, allowing the project and its client assets to be
  reviewed independently while the plugin remains explicitly marked as beta software.

### Changed
- **Release metadata**: GitHub Release notes and Modrinth metadata now describe the supported
  server families, Java 25 requirement, full Folia 26.1–26.3 region-scheduling support, and the
  exact matching resource-pack profile for Minecraft 26.1, 26.2 and 26.3.

## [0.1.1-beta.1] - 2026-10-04

### Added
- **Zero-to-Metin onboarding**: Fresh installs expose an atomic bundled Metin template, validate its
  model and animations, generate the matching resource pack, and recover a running event after a
  clean restart.
- **Reference-event coverage**: Metin, Meteor, Airdrop, Piñata and Boss Portal use the shared DSL
  with lifecycle, cleanup and reward regression coverage.
- **Release resource packs**: CI publishes three deterministic client packs to the separate
  resource-pack project: one each for Minecraft 26.1, 26.2 and 26.3.

### Changed
- **Folia support**: The Paper-family JAR supports Folia 26.1 and 26.2. Folia 26.3 is refused at
  startup with an upgrade message rather than being claimed as compatible.
- **Blockbench assets**: Bundled models now use only geometry representable by the vanilla
  resource-pack generator; unsupported meshes fail validation before server startup.

## [0.1.0-beta.1] - Unreleased

### Changed
- **Administration Command**: Changed the primary administration command from `/spectra` to `/spectraevents`.
- **Quality Refactor**: Improved server console logging and developer code-comment standards.

### Added
- **Config-Driven Event Engine**: Purely event-driven, data-driven 3D event kernel for Minecraft Paper servers.
- **Built-in Event Definitions**: Ships with production-ready YAML event definitions for `Meteor`, `Airdrop`, and `Metin`.
- **Atomic Gameplay Actions**: Support for `try_claim`, `apply_damage`, `spawn_boss`, `give_item`, `give_money`, and `health_threshold_crossed` triggers.
- **Asynchronous SQLite Persistence**: High-performance SQLite persistence with Single-Writer queue architecture and crash recovery.
- **Restart & Crash Recovery**: Automatic state recovery for active events, boss entity reconciliation, and pending timer recovery.
- **Admin Tooling & GUI**: Full admin command suite (`/spectraevents`) and interactive inventory GUI for managing events, definitions, and updates.
- **Optional Plugin Integrations**: Safe integration adapters for `LuckPerms`, `WorldGuard`, `Vault`, `PlaceholderAPI`, `MiniPlaceholders`, `Nexo`, `Oraxen`, and `ItemsAdder`.
- **Folia Support**: Native region-aware scheduling foundation (`RegionTaskScheduler`, `PaperRegionTaskScheduler`).
- **Update Checking System**: SemVer-aware GitHub releases update check provider.

### Removed
- **Sponge Platform Support**: Removed the experimental Sponge platform adapter, distribution module, descriptors, and release workflows to focus official support exclusively on the Paper and Spigot ecosystems.

### Changed
- **Namespace Migration**: Standardized Java package root to `io.github.kizio806.spectraevents` and Gradle group to `io.github.kizio806`.
- **Platform Separation**: Strictly decoupled `platform -> application -> core` architecture with zero platform dependencies in core logic.
