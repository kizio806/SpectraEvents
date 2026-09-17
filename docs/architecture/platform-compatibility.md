# Platform Compatibility & Taxonomy Matrix

## 1. Overview

SpectraEvents defines platform integration around **API Families** and **Server Implementations**, rather than treating every server jar as a unique ecosystem.

```text
                                  +-------------------+
                                  |    Bukkit API     |
                                  +---------+---------+
                                            |
                                  +---------v---------+
                                  |     Paper API     |
                                  +----+----------+---+
                                       |          |
                      +----------------+          +----------------+
                      |                                            |
            +---------v---------+                        +---------v---------+
            |      Purpur       |                        |       Folia       |
            | (Paper Distro)    |                        | (Paper Region Sched)
            +-------------------+                        +-------------------+
```

---

## 2. API Family vs Server Implementation Taxonomy

| Server / Platform | API Family | Primary Target Artifact | Lifecycle / Threading Model | Multi-Platform Status |
| :--- | :--- | :--- | :--- | :--- |
| **Paper 26.2** | `paper` | `distributions/paper/v26_2` | Region/Entity Task Schedulers, Adventure, PDC | **SUPPORTED / TESTED** |
| **Purpur** | `paper` | `distributions/paper/v26_2` | Pure Paper API binary compatible (uses Paper distro) | **PAPER-COMPATIBLE / NOT CERTIFIED** |
| **Folia** | `paper` | `distributions/paper/v26_2` | Region-aware Paper API (RegionTaskScheduler) | **PAPER-COMPATIBLE / NOT CERTIFIED** |
| **Spigot / Bukkit** | `bukkit` | `platforms/bukkit/common` *(Planned)* | Main thread loop + sync/async BukkitTask | **PORT-READY** |
| **Sponge** | `sponge` | `platforms/sponge/v10` *(Planned)* | Sponge CauseStackManager & Scheduler | **PORT-READY** |

---

## 3. Platform Capabilities & Requirements

### Paper Platform Family (`platforms/paper/*`)
- **Minimum Java**: Java 21 (Paper 26.2 targets Java 25 runtime).
- **Core Abstractions**: `PaperPlatformRegionTaskScheduler`, `PaperModelRenderer`, `PaperActionAdapter`, `PaperLifecycleReporter`.
- **Concurrency & Region Threading**: Direct usage of Paper `RegionScheduler` and `EntityScheduler` ensures native Folia region safety without code duplication.

### Spigot / Bukkit Platform Family (`platforms/bukkit/*`)
- **Minimum Java**: Java 21.
- **Scheduler Adaptation**: `BukkitSchedulerAdapter` using single-threaded tick tasks.
- **Model Rendering**: ItemDisplay falling back to ArmorStand head elements for legacy Bukkit versions.

### Sponge Platform Family (`platforms/sponge/*`)
- **Minimum Java**: Java 21.
- **Scheduler Adaptation**: `SpongeSchedulerAdapter` mapped to `Sponge.asyncScheduler()` and `server().scheduler()`.
- **Model Rendering**: Sponge Entity display components mapped to custom entity types.
