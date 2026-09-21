# Platform Compatibility & Taxonomy Matrix

## 1. Overview

SpectraEvents defines platform integration around **Compatibility Bands** and **Platform Families**, operating under a single-JAR-per-family release model.

```text
SPECTRAEVENTS COMPATIBILITY ARCHITECTURE
+-------------------------------------------------------------------------+
|                  Minecraft Compatibility Band: 26.1 – 26.3               |
+-------------------------------------------------------------------------+
|  Paper Family Artifact   |   Spigot Family Artifact  |
|  (Paper / Purpur / Folia)|      (Spigot / Bukkit)    |
+-------------------------------------------------------------------------+
```

---

## 2. API Family & Distribution Matrix

| Artifact | Target Platform | Factually Verified Compatible Minecraft Versions | Loaders | Baseline API | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Paper Family JAR** | Paper, Purpur | `26.1`, `26.2`, `26.3` | `paper`, `purpur` | Paper API `26.1` | Implemented; local real-server matrix passed |
| **Paper Family JAR** | Folia | `26.1`, `26.2` | `folia` | Paper API `26.1` | Implemented and locally verified; a Folia 26.3 startup is refused before data is loaded until this row is real-server verified |
| **Spigot Family JAR** | Spigot, CraftBukkit | `26.1`, `26.2`, `26.3` | `spigot`, `bukkit` | Spigot API `26.1` | Implemented; local real-server matrix passed |

---

## 3. Platform Capabilities & Requirements

### Paper Platform Family (`platforms/paper/common`)
- **Minimum Java**: Java 25 runtime for Paper 26.x series; Java 21 for core API.
- **Core Abstractions**: `PaperRegionTaskScheduler`, `PaperModelRenderer`, `PaperActionAdapter`, `PaperLifecycleReporter`.
- **Concurrency & Region Threading**: Direct usage of Paper `RegionScheduler` and `EntityScheduler` ensures native Folia region safety without code duplication.
- **Folia scoreboard limitation**: Folia does not implement Bukkit scoreboard creation. Scoreboard actions emit an explicit warning and continue without a sidebar; use bossbars for Folia-compatible event UI.
- **Folia version guard**: Folia is deliberately supported only for Minecraft 26.1–26.2. The Paper
  artifact logs a precise reason and disables itself on Folia 26.3 or a newer unverified version;
  Paper and Purpur remain supported through Minecraft 26.3.

### Spigot Platform Family (`platforms/spigot/common`)
- **Minimum Java**: Java 25 runtime.
- **Scheduler Adaptation**: `SpigotEventTaskScheduler` mapped to Bukkit sync/async scheduler.
- **Components & Text**: Kyori Adventure relocated into plugin namespace.
