# Release Management & Publishing Standards

This document defines the release lifecycle, packaging invariants, platform publishing standards, and incident prevention rules for the **SpectraEvents** project.

---

## 1. Golden Packaging Invariant (Universal Rule)

Every official release produces **EXACTLY TWO production JARs** built via `./gradlew build`:
1. **EXACTLY ONE Paper JAR** (`SpectraEvents-<version>-paper.jar`)
2. **EXACTLY ONE Spigot JAR** (`SpectraEvents-<version>-spigot.jar`)

Never bundle both server platforms into a single monolithic JAR or fragment releases into multiple build scripts.

---

## 2. Release Branches & SemVer Strategy

- Releases must originate from `main` or active `release/*` branches.
- Version numbers strictly follow Semantic Versioning (`MAJOR.MINOR.PATCH` with optional `-beta.<N>` suffixes):
  - `PATCH`: Bug fixes, minor adjustments, compatibility metadata updates.
  - `MINOR`: New actions, conditions, triggers, event templates, or platform integrations (backwards compatible).
  - `MAJOR`: Breaking changes to definition YAML schema or database storage layout.

---

## 3. Automated Release Script

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

The CI/CD pipeline automatically deploys the release to:
1. **GitHub Releases:** Attaches both distribution JARs, checksum files, and curated release notes.
2. **Modrinth:** Publishes Paper, Purpur, Folia, Spigot, and Bukkit distributions, plus client resource packs.
3. **CurseForge:** Publishes server distribution packages for configured project IDs (`CURSEFORGE_PROJECT_ID`).

---

## 4. Release Qualification Gates

Before any tag or release can be accepted, all quality gates must pass cleanly:
- **Zero `@SuppressWarnings`:** Enforced by `validate-source-hygiene.sh`.
- **Zero Markers:** No `TODO`, `FIXME`, or `XXX` tags in Java sources.
- **Test Coverage:** $\ge 85\%$ instruction coverage enforced by `jacocoTestCoverageVerification`.
- **Artifact Verification:** Verified by `verifyDistributionArtifacts`.
