# Architecture Assessment

Assessment date: `2026-11-01`

## Executive Verdict

`SpectraEvents` has a technically sound hexagonal architecture engineered as a modular, data-driven 3D event engine for modern Paper and Spigot servers.

Why:

- core domain models and 3D math remain pure Java 21 in `spectraevents-core`
- application orchestration and model services reside in `spectraevents-application` (Java 21)
- common presentation and Bukkit primitives are unified in `:platforms:bukkit`
- Paper and Spigot platform modules act as focused adapters
- release and compatibility gates are automated through Gradle + CI
- test, metadata, structure, and publication checks pass on the current matrix

## Scope And Validation

This assessment was validated locally with:

```bash
./gradlew clean check build
./scripts/release/release.sh --dry-run --allow-existing-tag
```

Result on `2026-11-01`: both commands completed successfully with 0 warnings, 0 `@SuppressWarnings`, and $\ge 85\%$ test coverage across domain modules.

## Repository Consistency & Structural Standards

This repository maintains strict engineering consistency through:

- canonical maintainer, release, and security policies (`CONTRIBUTING.md`, `SECURITY.md`)
- unified documentation and wiki layout (`docs/` and `docs/wiki/`)
- AI agent operating rules, workflows, and skills (`.agents/`)
- automated compatibility matrix verification (`verifyCompatibilityMatrix`)
- pre-publication structure validation gate (`validateStructure`)
- source hygiene validation gate (`validateSourceHygiene`)
- explicit formatting rules via Spotless Google Java Format

## Platform Artifact & Packaging Contract

SpectraEvents enforces a strict **Single JAR per Server Family** architectural invariant:
- **Module Separation:** Domain logic (`core`) and application services (`application`) reside exclusively as pure Java (Java 21 baseline). No Bukkit, Paper, or Minecraft classes may be imported in these modules.
- **Shared Bukkit Presentation:** `:platforms:bukkit` unifies Display entity tracking (`BukkitPdcKeys`), shared container listeners (`BukkitSharedLootListener`), and custom item providers across server implementations.
- **Thin Platform Adapters:** `:platforms:paper:common` and `:platforms:spigot:common` provide only platform-specific event routing, scheduler bridges (`RegionTaskScheduler`), and chat/component serialization.
- **Single-JAR Platform Packaging:** Rather than fragmenting releases into multiple patch-specific JARs, each distribution module compiles a single production artifact (`SpectraEvents-<version>-paper.jar` and `SpectraEvents-<version>-spigot.jar`).
- **Platform Publication Parity:** Modrinth and GitHub Releases always publish matching Paper and Spigot artifacts per release tag.

## Technical Strengths

- pure hexagonal boundary isolation between domain logic and Minecraft APIs
- native Minecraft 1.19.4+ Display/Interaction 3D model hierarchy and keyframed timeline engine
- single-writer asynchronous SQLite persistence with WAL mode and crash recovery
- durable mailbox claim lifecycle with idempotent reward delivery
- comprehensive unit test coverage ($\ge 95\%$ on core, $\ge 85\%$ on application)
