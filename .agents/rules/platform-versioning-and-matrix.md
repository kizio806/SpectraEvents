# Platform Versioning and Matrix Contract

## Purpose
SpectraEvents targets modern Minecraft server software while preserving platform compatibility and runtime safety.

## Standard
- **Java Target:** Platform adapters (`platforms/*`) and distributions (`distributions/*`) target Java 25. Core and application modules target Java 21 baseline.
- **Server Support Band:**
  - Paper / Purpur 26.1–26.3 (`SpectraEvents-<version>-paper.jar`)
  - Spigot / CraftBukkit 26.1–26.3 (`SpectraEvents-<version>-spigot.jar`)
- **Folia Compatibility Policy:** Folia 26.1–26.3 is supported via `RegionTaskScheduler`. Folia is automatically detected at bootstrap and all tick-bound operations are routed through the region scheduler.
- **Golden Packaging Invariant:** Every release build produces **EXACTLY TWO production JARs**:
  1. Paper distribution: `distributions/paper/build/libs/SpectraEvents-<version>-paper.jar`
  2. Spigot distribution: `distributions/spigot/build/libs/SpectraEvents-<version>-spigot.jar`

## Verification:
```bash
./gradlew verifyCompatibilityMatrix
./gradlew verifyDistributionArtifacts
```
