# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Security
- **Atomic Concurrency**: Resolved TOCTOU race condition in `AnimationRuntimeService` by enforcing strict lock acquisition and double-checked registration state before animation step advancement.
- **Cryptographic Contract**: Resolved CodeQL SHA-1 weak cryptography alert in `ResourcePackBuilder` through explicit protocol-mandated pack hash method and Javadoc contract.
- **Encapsulation Hardening**: Eliminated internal representation leaks in `EngineContext` and `SpectraAssetFace`, replacing exposed internal maps and locks with encapsulated domain execution methods.
- **Exception Logging**: Ensured all caught exceptions in `EventLifecycleManager` log underlying cause and stack traces without silent omission.

### Changed
- **Zero Platform Duplication**: Extracted common Bukkit, Paper, and Spigot logic into `:platforms:bukkit`, eliminating duplicate configuration bootstraps, capability queries, lifecycle reporters, and resource-pack delivery adapters.
- **Test Coverage Transparency**: Removed all platform coverage exclusions from JaCoCo root aggregation, SonarCloud analysis, and Codecov configurations. All platform code is now included in testing and coverage gates.
- **Defensive API Contracts**: Enforced strict `Objects.requireNonNull` validation across platform action adapters, schedulers, and resource cleaners.

### Added
- **Platform Unit Test Suite**: Added comprehensive unit test suites for `:platforms:bukkit` (`BukkitSharedLootListenerTest`, `BukkitResourcePackDeliveryBootstrapTest`, `BukkitDefinitionConfigBootstrapTest`, `BukkitLifecycleReporterTest`), `:platforms:spigot:common` (command handlers and helpers), and `:platforms:paper:common` (`PaperResourceCleanerTest`, `PaperEntityReconcilerTest`, `PaperSchedulerTest`, `EventScoreboardManagerTest`).

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
