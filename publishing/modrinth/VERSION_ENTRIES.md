# Modrinth version entries

Do not publish while any required P0 is open. For an authorized version `<version>`, create separate
entries so every download has one exact platform contract.

## Server plugin entries

| Public name | Version number | Loaders | Game versions | File |
| --- | --- | --- | --- | --- |
| `SpectraEvents <version> — Paper / Purpur` | `<version>+paper` | `paper`, `purpur` | `26.1`, `26.2`, `26.3` | `SpectraEvents-<version>-paper.jar` |
| `SpectraEvents <version> — Spigot / CraftBukkit` | `<version>+spigot` | `spigot`, `bukkit` | `26.1`, `26.2`, `26.3` | `SpectraEvents-<version>-spigot.jar` |
| `SpectraEvents <version> — Folia` | `<version>+folia` | `folia` | `26.1`, `26.2` | `SpectraEvents-<version>-paper.jar` |

Environment: dedicated server only. Version type follows SemVer suffix: `alpha` for alpha,
`beta` for beta/RC, otherwise `release`. Use the matching `docs/releases/<version>.md` as changelog
only after reconciling it with the exact candidate diff and known limitations.

## Resource-pack entries

The resource-pack project receives one version per exact profile:

| Public name | Version number | Game version | File type and file |
| --- | --- | --- | --- |
| `SpectraEvents Assets <version> — Minecraft 26.1` | `<version>+26.1` | `26.1` | required resource pack: `spectraevents-profile_26_1.zip` |
| `SpectraEvents Assets <version> — Minecraft 26.2` | `<version>+26.2` | `26.2` | required resource pack: `spectraevents-profile_26_2.zip` |
| `SpectraEvents Assets <version> — Minecraft 26.3` | `<version>+26.3` | `26.3` | required resource pack: `spectraevents-profile_26_3.zip` |

Record the final SHA-1 and real-client acceptance for each ZIP before enabling automatic delivery.
