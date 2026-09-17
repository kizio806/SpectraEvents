# Platform Compatibility & Taxonomy Matrix

## 1. Overview

SpectraEvents defines platform integration around **Compatibility Bands** and **Platform Families**, operating under a single-JAR-per-family release model.

```text
SPECTRAEVENTS COMPATIBILITY ARCHITECTURE
+-------------------------------------------------------------------------+
|                  Minecraft Compatibility Band: 26.1 – 26.3               |
+-------------------------------------------------------------------------+
|  Paper Family Artifact   |   Spigot Family Artifact  |   Sponge Artifact  |
|  (Paper / Purpur / Folia)|      (Spigot / Bukkit)    |      (Sponge)      |
+-------------------------------------------------------------------------+
```

---

## 2. API Family & Distribution Matrix

| Artifact | Target Platform | Compatible Minecraft Versions | Loaders | Baseline API | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Paper Family JAR** | Paper, Purpur, Folia | `26.1`, `26.1.1`, `26.1.2`, `26.2`, `26.3` | `paper`, `purpur`, `folia` | Paper API `26.1` | **IMPLEMENTED + RUNTIME VERIFIED** |
| **Spigot Family JAR** | Spigot, Bukkit | `26.1`, `26.1.1`, `26.1.2`, `26.2`, `26.3` | `spigot`, `bukkit` | Spigot API `26.1` | **IMPLEMENTED + RUNTIME VERIFIED** |
| **Sponge JAR** | Sponge | `26.1`, `26.1.1`, `26.1.2`, `26.2` | `sponge` | SpongeAPI `12.0.0` | **FOUNDATION + VERIFIED** |

---

## 3. Platform Capabilities & Requirements

### Paper Platform Family (`platforms/paper/common`)
- **Minimum Java**: Java 25 runtime for Paper 26.x series; Java 21 for core API.
- **Core Abstractions**: `PaperRegionTaskScheduler`, `PaperModelRenderer`, `PaperActionAdapter`, `PaperLifecycleReporter`.
- **Concurrency & Region Threading**: Direct usage of Paper `RegionScheduler` and `EntityScheduler` ensures native Folia region safety without code duplication.

### Spigot Platform Family (`platforms/spigot/common`)
- **Minimum Java**: Java 25 runtime.
- **Scheduler Adaptation**: `SpigotEventTaskScheduler` mapped to Bukkit sync/async scheduler.
- **Components & Text**: Kyori Adventure relocated into plugin namespace.

### Sponge Platform Family (`platforms/sponge/common`)
- **Minimum Java**: Java 21+.
- **Scheduler Adaptation**: `SpongeEventTaskScheduler` mapped to Sponge async and server scheduler.
- **Sponge API Versions**: SpongeAPI 12 / 13 / 14 mapped to Minecraft 1.20.6 through 1.21.4 (26.1 - 26.2 band).
