# SpectraEvents — Versioning Strategy

This document defines the official versioning model, branch strategy, and Minecraft generation support policy for SpectraEvents.

---

## 1. Plugin Version Numbers (SemVer)

SpectraEvents strictly follows [Semantic Versioning](https://semver.org/):

```
MAJOR.MINOR.PATCH
```

| Segment | When to increment |
| :--- | :--- |
| `MAJOR` | Breaking changes to event definition YAML schema or database storage layout. Server operators must review and migrate their event definitions. |
| `MINOR` | New actions, conditions, triggers, event templates, or platform integrations. Fully backwards-compatible with existing YAML definitions. |
| `PATCH` | Bug fixes, performance improvements, compatibility metadata updates. No API or schema changes. |

Pre-release suffixes follow the pattern `-rc.N` (release candidates):

```
1.0.0-rc.1  →  1.0.0  →  1.0.1  →  1.1.0  →  2.0.0
```

---

## 2. Minecraft Generation Support Model

SpectraEvents supports a **range of Minecraft versions** within a single release build. One plugin version covers an entire Minecraft generation (e.g. `26.1–26.3`).

```
SpectraEvents 1.0.0
  ├── SpectraEvents-1.0.0-paper.jar   →  Paper / Purpur / Folia 26.1, 26.2, 26.3
  └── SpectraEvents-1.0.0-spigot.jar  →  Spigot / CraftBukkit  26.1, 26.2, 26.3
```

When a new Minecraft generation is released (e.g. `27.x`), a new `main` branch cycle begins targeting that generation.

---

## 3. Branch Strategy

| Branch | Purpose |
| :--- | :--- |
| `main` | Active development. Always targets the **current Minecraft generation**. |
| `support/mc-<gen>` | Long-term bugfix backports for a **previous Minecraft generation**. Created when `main` moves to a new generation. |

### Branch lifecycle example

```
1.0.0  released on main   →  MC 26.1–26.3
                               ↓
       MC 27.x released   →  main moves to MC 27.x
                               ↓
       support/mc-26.x    →  created from last 26.x tag
       bugfix committed   →  cherry-picked from main to support/mc-26.x
       1.0.1 released on support/mc-26.x (same version number, different MC range)
```

### Creating a support branch

```bash
# After main has moved to a new MC generation:
git checkout -b support/mc-26.x v1.2.3   # base on last stable tag for that generation
git push origin support/mc-26.x
```

---

## 4. Git Tags

Tags are placed on the branch where the release originates:

| Tag | Branch | Meaning |
| :--- | :--- | :--- |
| `v1.0.0` | `main` | Stable release for current MC generation |
| `v1.0.1` | `main` | Bugfix for current MC generation |
| `v1.0.1` | `support/mc-26.x` | Same plugin version backported to older MC generation |

The plugin version number is **not suffixed with the MC version**. Modrinth and CurseForge use their own version metadata fields to declare supported game versions.

---

## 5. Modrinth / CurseForge Publishing

Each release produces **exactly two Modrinth version entries** per platform:

| Modrinth Entry | Loaders | Game Versions |
| :--- | :--- | :--- |
| `SpectraEvents 1.0.0 — Paper, Purpur, Folia` | `paper`, `purpur`, `folia` | `26.1`, `26.2`, `26.3` |
| `SpectraEvents 1.0.0 — Spigot, CraftBukkit` | `spigot`, `bukkit` | `26.1`, `26.2`, `26.3` |

For support branch releases targeting an older MC generation, the same version number is published with the older game versions declared.

---

## 6. What Never Changes

- **Golden Packaging Invariant:** Every release always produces exactly **two** production JARs — one Paper, one Spigot. No exceptions.
- **SemVer:** Version numbers never include Minecraft version suffixes (e.g. `1.0.0-mc26` is forbidden).
- **CHANGELOG:** Every release has an entry in `CHANGELOG.md` and a curated `docs/releases/<version>.md` file.
