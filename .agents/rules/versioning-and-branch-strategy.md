# Versioning and Branch Strategy

## Purpose
This rule defines how SpectraEvents handles plugin version numbers, Minecraft generation support,
branch management, and release tagging. All AI agents and engineers must follow this model precisely.

## Plugin Versioning — SemVer with Minecraft Metadata

SpectraEvents uses strict Semantic Versioning (`MAJOR.MINOR.PATCH+mc<RANGE>`):

- **MAJOR**: Breaking changes to event definition YAML schema or SQLite database layout.
  Server operators must review and migrate their event definitions.
- **MINOR**: New actions, conditions, triggers, templates, or integrations. Fully backwards-compatible.
- **PATCH**: Bug fixes and compatibility metadata updates. No schema or API changes.

Pre-release suffix: `-rc.N` before metadata (e.g. `1.0.0-rc.1+mc26.x`).

## Minecraft Generation Support Model

One plugin release covers an **entire Minecraft generation range** (e.g. `26.1–26.3`, tagged `mc26.x`).
A single `gradle.properties` version produces exactly two JARs supporting all versions in that range:

```text
spectra-events-<version>-paper-purpur-folia.jar   →  Paper / Purpur / Folia  (all versions in range)
spectra-events-<version>-spigot-bukkit.jar        →  Spigot / CraftBukkit    (all versions in range)
```

Base version (`MAJOR.MINOR.PATCH`) is synchronized across all active branches. Compatibility lines are tagged as `v<BASE>+mc<RANGE>` (e.g. `v1.0.0+mc26.x`).

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
- Tags follow the format `v<BASE_VERSION>+mc<MINECRAFT_RANGE>` (e.g. `v1.0.0+mc26.x`).
- Tags are always annotated and created by the release script — never manually.

## Modrinth / CurseForge Publishing

Every release publishes **exactly two Modrinth version entries** (Paper family + Spigot family):
- `<BASE_VERSION>+mc<MINECRAFT_RANGE>.paper`
- `<BASE_VERSION>+mc<MINECRAFT_RANGE>.spigot`
Both entries declare all supported game versions in their respective Minecraft generation range.
Never publish Folia as a separate entry — it is covered by the Paper distribution.

## Verification
Before every release, confirm in `gradle.properties`:
```
spectraevents.version=<MAJOR>.<MINOR>.<PATCH>+mc<MINECRAFT_RANGE>
```
Run the full release dry-run:
```bash
./scripts/release/release.sh --dry-run
```
