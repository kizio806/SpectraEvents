# Versioning and Branch Strategy

## Purpose
This rule defines how SpectraEvents handles plugin version numbers, Minecraft generation support,
branch management, and release tagging. All AI agents and engineers must follow this model precisely.

## Plugin Versioning — SemVer

SpectraEvents uses strict Semantic Versioning (`MAJOR.MINOR.PATCH`):

- **MAJOR**: Breaking changes to event definition YAML schema or SQLite database layout.
  Server operators must review and migrate their event definitions.
- **MINOR**: New actions, conditions, triggers, templates, or integrations. Fully backwards-compatible.
- **PATCH**: Bug fixes and compatibility metadata updates. No schema or API changes.

Pre-release suffix: `-rc.N` only (e.g. `1.0.0-rc.1`). Never use `-beta`, `-alpha`, or MC version suffixes.

## Minecraft Generation Support Model

One plugin release covers an **entire Minecraft generation range** (e.g. `26.1–26.3`).
A single `gradle.properties` version produces exactly two JARs supporting all versions in that range:

```
SpectraEvents-<version>-paper.jar   →  Paper / Purpur / Folia  (all versions in range)
SpectraEvents-<version>-spigot.jar  →  Spigot / CraftBukkit    (all versions in range)
```

Version numbers are **never** suffixed with Minecraft version identifiers. Modrinth declares game
versions via its own metadata fields, not via the plugin version string.

## Branch Strategy

| Branch | Purpose |
| :--- | :--- |
| `main` | Active development. Always targets the current Minecraft generation. |
| `support/mc-<gen>` | Bugfix backports for a previous Minecraft generation. |

A `support/mc-<gen>` branch is created when `main` moves to a new Minecraft generation.
Only bug fixes are cherry-picked to support branches — no new features.

### Creating a support branch
```bash
git checkout -b support/mc-26.x v<last-stable-tag-for-that-gen>
git push origin support/mc-26.x
```

## Git Tags

- Tags are placed on the originating branch (`main` or `support/mc-<gen>`).
- Same plugin version number (`v1.0.1`) may appear on both `main` and a support branch,
  representing the same fix for different Minecraft generations.
- Tags are always annotated and created by the release script — never manually.

## Modrinth / CurseForge Publishing

Every release publishes **exactly two Modrinth version entries** (Paper family + Spigot family).
Both entries declare all supported game versions in their respective Minecraft generation range.
Never publish Folia as a separate entry — it is covered by the Paper distribution.

## Verification
Before every release, confirm in `gradle.properties`:
```
spectraevents.version=<MAJOR>.<MINOR>.<PATCH>
```
Run the full release dry-run:
```bash
./scripts/release/release.sh --dry-run
```
