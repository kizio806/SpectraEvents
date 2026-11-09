# Release Management & Publishing Standards

This document defines the release lifecycle, multi-platform packaging invariants, distribution publishing standards, and incident prevention rules for the **SpectraEvents** project.

---

## 1. Golden Packaging Invariant (Universal Rule)

Every official release produces **EXACTLY TWO production JARs** built via `./gradlew build`:
1. **EXACTLY ONE Paper JAR** (`SpectraEvents-<version>-paper.jar`)
2. **EXACTLY ONE Spigot JAR** (`SpectraEvents-<version>-spigot.jar`)

### Cross-Platform Matrix Alignment
- **Paper Distribution Family (`SpectraEvents-<version>-paper.jar`):**
  - Targets **Paper**, **Purpur**, and **Folia** (Minecraft 26.1–26.3).
  - Uses native Adventure MiniMessage, asynchronous chunk loading, Folia region scheduling via `RegionTaskScheduler`, and Brigadier commands.
- **Spigot Distribution Family (`SpectraEvents-<version>-spigot.jar`):**
  - Targets **Spigot** and **CraftBukkit** (Minecraft 26.1–26.3).
  - Uses dedicated Spigot task scheduling, Spigot PDC persistence keys, Bukkit Admin GUI, and modular administration subcommands.

Never bundle both server platforms into a single monolithic JAR, and never produce separate fragmented JARs for Folia or Purpur.

---

## 2. Platform Publishing Invariants (Modrinth & CurseForge)

Every official release deploys to Modrinth and CurseForge with **EXACTLY TWO distribution version entries** in the main plugin project, alongside versioned client resource packs in the dedicated assets project:

### Main Plugin Project Releases
1. **`SpectraEvents <version> — Paper, Purpur, and Folia`**
   - Version ID / Number: `<version>+paper` (e.g. `0.1.1-beta.2+paper`)
   - Supported Loaders: `paper`, `purpur`, `folia`
   - Supported Game Versions: `26.1`, `26.2`, `26.3`
   - File: Single Paper JAR (`SpectraEvents-<version>-paper.jar`)
2. **`SpectraEvents <version> — Spigot and CraftBukkit`**
   - Version ID / Number: `<version>+spigot` (e.g. `0.1.1-beta.2+spigot`)
   - Supported Loaders: `spigot`, `bukkit`
   - Supported Game Versions: `26.1`, `26.2`, `26.3`
   - File: Single Spigot JAR (`SpectraEvents-<version>-spigot.jar`)

### Dedicated Resource Pack Releases (`spectraevents-assets` / `Rg1nw8IW`)
Release automation publishes independent client resource packs to the dedicated Modrinth assets project before deploying plugin distributions:
- `SpectraEvents Assets <version> — Minecraft 26.1` (`<version>+26.1`, file: `spectraevents-profile_26_1.zip`)
- `SpectraEvents Assets <version> — Minecraft 26.2` (`<version>+26.2`, file: `spectraevents-profile_26_2.zip`)
- `SpectraEvents Assets <version> — Minecraft 26.3` (`<version>+26.3`, file: `spectraevents-profile_26_3.zip`)

### ⚠️ Prohibited Publishing Anti-Patterns
- **NEVER publish Folia as a separate Modrinth version entry.** Folia is natively supported by the Paper distribution artifact (`SpectraEvents-<version>-paper.jar`). Duplicating the Paper JAR as a separate `+folia` release entry fragments download metrics, clutters operator notifications, and creates launcher confusion.
- **NEVER bundle Paper and Spigot into a single hybrid JAR.** Paper bytecode optimizations and Folia region schedulers rely on platform APIs that fail classloading on vanilla Spigot runtimes.
- **NEVER tag or publish a release without curated release notes.** GitHub Actions strictly verifies the existence of `docs/releases/<version>.md` before executing publication.

---

## 3. Curated Release Notes Requirement (`docs/releases/<version>.md`)

Before tagging any release, an engineer or release manager must author the corresponding release notes document at:
```text
docs/releases/<version>.md
```
(For example: `docs/releases/0.1.1-beta.2.md`).

This curated document is read directly by `.github/workflows/release.yml` and injected into:
- The GitHub Release body.
- The Modrinth version changelog.
- The CurseForge version changelog.

---

## 4. Release Branches & SemVer Strategy

- Releases must originate from `main` or active `support/mc-<gen>` backport branches.
- Version numbers strictly follow Semantic Versioning (`MAJOR.MINOR.PATCH` with optional `-rc.<N>` pre-release identifiers).
  - `PATCH`: Bug fixes, minor adjustments, compatibility metadata updates.
  - `MINOR`: New actions, conditions, triggers, event templates, or platform integrations (backwards compatible).
  - `MAJOR`: Breaking changes to definition YAML schema or database storage layout.
- See [Versioning Strategy](versioning.md) for full branch lifecycle, backport strategy, and Minecraft generation support details.

---

## 5. Automated Release Script

SpectraEvents provides an automated release execution script at `scripts/release/release.sh`:

### Dry-Run Verification (Mandatory Pre-Release Step)
```bash
./scripts/release/release.sh --dry-run
```
Verifies git tree cleanliness, branch consistency, source hygiene, repository structure, test suites, static analysis, and builds production distribution artifacts.

### Official Publication
```bash
./scripts/release/release.sh --publish
```
Creates the annotated release tag, pushes to origin, and triggers the GitHub Actions release workflow.

The CI/CD pipeline automatically:
1. Runs full test suites, static analysis, and distribution builds.
2. Validates client resource-pack delivery evidence.
3. Executes real-server runtime smoke testing across all 15 matrix combinations (Paper, Purpur, Folia, Spigot, CraftBukkit on 26.1, 26.2, 26.3).
4. Publishes matching resource-pack profiles to the Modrinth assets project.
5. Deploys the Paper and Spigot plugin distributions to Modrinth and CurseForge.
6. Publishes the GitHub Release with attached JARs and SHA-256 checksums.

---

## 6. Release Qualification Gates

Before any tag or release can be accepted, all quality gates must pass cleanly:
- **Zero `@SuppressWarnings`:** Enforced by `validate-source-hygiene.sh`.
- **Zero Markers:** No `TODO`, `FIXME`, or `XXX` tags in Java sources.
- **Test Coverage:** $\ge 85\%$ instruction coverage enforced by `jacocoTestCoverageVerification`.
- **Artifact Verification:** Verified by `verifyDistributionArtifacts` (exactly two JARs).
- **Curated Release Notes:** Verified by `docs/releases/<version>.md` existence check.
