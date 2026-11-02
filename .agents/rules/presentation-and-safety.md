# Presentation, Display Entities, and Cleanup Safety

## Purpose
SpectraEvents renders custom 3D models using native Minecraft Display and Interaction entities. All spawned entities must be tracked, reconciled, and cleanly destroyed upon event termination or server shutdown.

## Standard
- **PDC Ownership:** Every entity spawned by SpectraEvents must be tagged with `BukkitPdcKeys.EVENT_INSTANCE_ID` and `BukkitPdcKeys.DISPLAY_ROLE`.
- **Zero Orphaned Entities:** Server restarts and crash recoveries run `PaperEntityReconciler` / `SpigotEntityReconciler` to purge abandoned Display entities.
- **Display Interpolation:** Model transformations utilize Display entity interpolation duration (`setInterpolationDuration`) to ensure fluid client-side bone animations without network packet flooding.
- **Fail-Safe Cleanup:** In the event of an unhandled runtime error during an event phase, the cleanup handler must execute immediately, closing shared loot inventories, stopping animations, and removing spawned entities.

## Verification:
```bash
./gradlew :platforms:bukkit:test :platforms:paper:common:test :platforms:spigot:common:test
```
