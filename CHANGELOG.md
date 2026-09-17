# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0-beta.1] - Unreleased

### Added
- **Config-Driven Event Engine**: Purely event-driven, data-driven 3D event kernel for Minecraft Paper servers.
- **Built-in Event Definitions**: Ships with production-ready YAML event definitions for `Meteor`, `Airdrop`, and `Metin`.
- **Atomic Gameplay Actions**: Support for `try_claim`, `apply_damage`, `spawn_boss`, `give_item`, `give_money`, and `health_threshold_crossed` triggers.
- **Asynchronous SQLite Persistence**: High-performance SQLite persistence with Single-Writer queue architecture and crash recovery.
- **Restart & Crash Recovery**: Automatic state recovery for active events, boss entity reconciliation, and pending timer recovery.
- **Admin Tooling & GUI**: Full admin command suite (`/spectra`) and interactive inventory GUI for managing events, definitions, and updates.
- **Optional Plugin Integrations**: Safe integration adapters for `LuckPerms`, `WorldGuard`, `Vault`, `PlaceholderAPI`, `MiniPlaceholders`, `Nexo`, `Oraxen`, and `ItemsAdder`.
- **Folia Support**: Native region-aware scheduling foundation (`RegionTaskScheduler`, `PaperRegionTaskScheduler`).
- **Update Checking System**: SemVer-aware GitHub releases update check provider.

### Removed
- **Sponge Platform Support**: Removed the experimental Sponge platform adapter, distribution module, descriptors, and release workflows to focus official support exclusively on the Paper and Spigot ecosystems.

### Changed
- **Namespace Migration**: Standardized Java package root to `io.github.kizio806.spectraevents` and Gradle group to `io.github.kizio806`.
- **Platform Separation**: Strictly decoupled `platform -> application -> core` architecture with zero platform dependencies in core logic.
