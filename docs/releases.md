# Release Management & Versioning Standards

This document defines the authoritative versioning architecture, release lifecycle, multi-platform packaging invariants, platform publishing standards, and incident prevention rules for the **SpectraEvents** project.

---

## 1. Authoritative Versioning Policy

SpectraEvents follows an engineering-grade versioning policy based on [Semantic Versioning 2.0.0](https://semver.org/) combined with SemVer build metadata for Minecraft compatibility lines.

### Version Format

```text
MAJOR.MINOR.PATCH+mc<MINECRAFT_RANGE>
```

Example for active `main` branch:
```text
1.0.0+mc26.x
```

### Increment Semantics

- **MAJOR (`X.0.0`)**: Incremented when introducing breaking changes affecting operators, event YAML schemas, public APIs, or SQLite database layouts.
- **MINOR (`x.Y.0`)**: Incremented when introducing backward-compatible new event actions, conditions, triggers, templates, or integrations.
- **PATCH (`x.y.Z`)**: Incremented for backward-compatible bug fixes, performance optimizations, security hardening, and maintenance updates.

*Note:* Minecraft version changes or internal refactoring alone do **NOT** alter the base `MAJOR.MINOR.PATCH` sequence. Minecraft compatibility is captured exclusively in the `+mc<RANGE>` metadata suffix.

---

## 2. Critical Global Synchronization Rule

The defining principle of this versioning architecture is **global base version synchronization**:

> **Every active support branch must use the exact same base project version.**

When future support branches (e.g. `support/mc-1.21.x`) are created alongside `main` (`1.0.0+mc26.x`):
- All active compatibility lines must share the identical base version (`1.0.0`).
- Base versions are never incremented independently on a single branch.
- Upgrades (e.g. `1.0.1` or `1.1.0`) are applied across all active support branches in lockstep.

---

## 3. Golden Packaging Invariant (Universal Rule)

Every official release produces **EXACTLY TWO production distribution JARs** built via Gradle:

1. **Paper Family JAR**:
   `spectra-events-<BASE_VERSION>-<MINECRAFT_RANGE>-paper-purpur-folia.jar`
   - Example: `spectra-events-1.0.0-mc26.x-paper-purpur-folia.jar`
   - Platform Compatibility: **Paper**, **Purpur**, and **Folia** (Minecraft 26.1–26.3).
   - Features: Native Adventure MiniMessage, asynchronous chunk loading, Folia region scheduling via `RegionTaskScheduler`, and Brigadier commands.

2. **Spigot Family JAR**:
   `spectra-events-<BASE_VERSION>-<MINECRAFT_RANGE>-spigot-bukkit.jar`
   - Example: `spectra-events-1.0.0-mc26.x-spigot-bukkit.jar`
   - Platform Compatibility: **Spigot** and **CraftBukkit** (Minecraft 26.1–26.3).
   - Features: Dedicated Spigot task scheduling, Spigot PDC persistence keys, Bukkit Admin GUI, and modular administration subcommands.

Never bundle both server platforms into a single monolithic JAR, and never produce separate fragmented JARs for Folia or Purpur.

---

## 4. Platform Publishing Standards (Modrinth & CurseForge)

Every official release deploys to Modrinth and CurseForge with **EXACTLY TWO distribution version entries** in the main plugin project, alongside versioned client resource packs in the dedicated assets project:

### Main Plugin Project Releases
1. **Paper, Purpur, and Folia**:
   - Version ID / Number: `<BASE_VERSION>+mc<MINECRAFT_RANGE>.paper` (e.g. `1.0.0+mc26.x.paper`)
   - Supported Loaders: `paper`, `purpur`, `folia`
   - Supported Game Versions: `26.1`, `26.2`, `26.3`
   - File: `spectra-events-1.0.0-mc26.x-paper-purpur-folia.jar`
2. **Spigot and CraftBukkit**:
   - Version ID / Number: `<BASE_VERSION>+mc<MINECRAFT_RANGE>.spigot` (e.g. `1.0.0+mc26.x.spigot`)
   - Supported Loaders: `spigot`, `bukkit`
   - Supported Game Versions: `26.1`, `26.2`, `26.3`
   - File: `spectra-events-1.0.0-mc26.x-spigot-bukkit.jar`

### Dedicated Resource Pack Releases (`spectraevents-assets`)
Release automation publishes independent client resource packs to the dedicated Modrinth assets project before deploying plugin distributions:
- `SpectraEvents Assets <version> — Minecraft 26.1` (`<version>+26.1`, file: `spectraevents-profile_26_1.zip`)
- `SpectraEvents Assets <version> — Minecraft 26.2` (`<version>+26.2`, file: `spectraevents-profile_26_2.zip`)
- `SpectraEvents Assets <version> — Minecraft 26.3` (`<version>+26.3`, file: `spectraevents-profile_26_3.zip`)

### ⚠️ Prohibited Publishing Anti-Patterns
- **NEVER publish Folia as a separate Modrinth version entry.** Folia is natively supported by the Paper distribution artifact (`spectra-events-<base>-<range>-paper-purpur-folia.jar`). Duplicating the Paper JAR as a separate `+folia` release entry fragments download metrics, clutters operator notifications, and creates launcher confusion.
- **NEVER bundle Paper and Spigot into a single hybrid JAR.** Paper bytecode optimizations and Folia region schedulers rely on platform APIs that fail classloading on vanilla Spigot runtimes.
- **NEVER tag or publish a release without curated release notes.** GitHub Actions strictly verifies the existence of `docs/releases/<version>.md` before executing publication.

---

## 5. Git Tags and GitHub Releases

Every release is triggered by pushing an annotated, cryptographically signed Git tag:

### Git Tag Convention
```text
v<BASE_VERSION>+mc<MINECRAFT_RANGE>
```
Example: `v1.0.0+mc26.x`

### GitHub Release Title Convention
```text
v<BASE_VERSION> — Minecraft <MINECRAFT_RANGE>
```
Example: `v1.0.0 — Minecraft 26.x`

Each GitHub Release contains:
- 1 Paper/Purpur/Folia JAR (`spectra-events-1.0.0-mc26.x-paper-purpur-folia.jar`)
- 1 Spigot/Bukkit JAR (`spectra-events-1.0.0-mc26.x-spigot-bukkit.jar`)
- Verified SHA-256 checksums file (`SHA256SUMS.txt`)

---

## 6. Curated Release Notes Requirement (`docs/releases/<version>.md`)

Before tagging any release, an engineer or release manager must author the corresponding release notes document at:
```text
docs/releases/<version>.md
```
(For example: `docs/releases/1.0.0+mc26.x.md`).

This curated document is read directly by `.github/workflows/release.yml` and injected into:
- The GitHub Release body.
- The Modrinth version changelog.
- The CurseForge version changelog.

---

## 7. Automated Validation & Safety Gates

Continuous integration enforces release readiness and prevents accidental divergence:

```bash
# 1. Run full verification gates and test suites
./gradlew check

# 2. Build production distribution artifacts
./gradlew :distributions:paper:build :distributions:spigot:build

# 3. Test release readiness in dry-run mode
./scripts/release/release.sh --dry-run
```

The verification pipeline automatically enforces:
- SemVer compliance with mandatory `+mc<RANGE>` metadata (`verifySemverVersion`).
- Compatibility matrix configuration (`verifyCompatibilityMatrix`).
- Platform boundary isolation (`verifyPlatformBoundaries`).
- Deterministic production distribution artifact naming and existence (`verifyDistributionArtifacts`).
- Source hygiene, comment standards, and spotless formatting (`validateSourceHygiene`, `spotlessCheck`).
- Strict test coverage ($\ge 85\%$) (`jacocoRootCoverageVerification`).

---
