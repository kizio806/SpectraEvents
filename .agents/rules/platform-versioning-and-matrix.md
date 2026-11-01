# Platform Versioning and Matrix Contract

## Purpose
SpectraEvents targets modern Minecraft server software while preserving platform compatibility and runtime safety.

## Standard
- **Java Target:** Platform adapters (`platforms/*`) and distributions (`distributions/*`) target Java 25. Core and application modules target Java 21 baseline.
- **Server Support Band:**
  - Paper / Purpur 26.1–26.3 (`SpectraEvents-<version>-paper.jar`)
  - Spigot / CraftBukkit 26.1–26.3 (`SpectraEvents-<version>-spigot.jar`)
- **Folia Compatibility Policy (ADR 0009):** Folia 26.1–26.2 is supported via `RegionTaskScheduler`. Folia 26.3 is explicitly blocked at bootstrap pending verification of Folia 26.3 region scheduling and entity thread bounds.
- **Golden Packaging Invariant:** Every release build produces **EXACTLY TWO production JARs**:
  1. Paper distribution: `distributions/paper/build/libs/SpectraEvents-<version>-paper.jar`
  2. Spigot distribution: `distributions/spigot/build/libs/SpectraEvents-<version>-spigot.jar`

## Verification:
```bash
./gradlew verifyCompatibilityMatrix
./gradlew verifyDistributionArtifacts
```
