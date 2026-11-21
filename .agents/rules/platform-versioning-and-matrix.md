# Platform Versioning and Matrix Contract

## Purpose
SpectraEvents targets modern Minecraft server software while preserving platform compatibility and runtime safety.

## Standard
- **Java Target:** Platform adapters (`platforms/*`) and distributions (`distributions/*`) target Java 25. Core and application modules target Java 21 baseline.
- **Server Support Band:**
  - Paper / Purpur / Folia 26.1–26.3 (`spectra-events-<base>-<range>-paper-purpur-folia.jar`)
  - Spigot / CraftBukkit 26.1–26.3 (`spectra-events-<base>-<range>-spigot-bukkit.jar`)
- **Folia Compatibility Policy:** Folia 26.1–26.3 is supported natively by the Paper distribution via `RegionTaskScheduler`. Folia is automatically detected at bootstrap and all tick-bound operations are routed through the region scheduler.
- **Golden Packaging Invariant:** Every release build produces **EXACTLY TWO production JARs**:
  1. Paper distribution: `distributions/paper/build/libs/spectra-events-<base>-<range>-paper-purpur-folia.jar`
  2. Spigot distribution: `distributions/spigot/build/libs/spectra-events-<base>-<range>-spigot-bukkit.jar`

## Verification:
```bash
./gradlew verifyCompatibilityMatrix
./gradlew verifyDistributionArtifacts
```
