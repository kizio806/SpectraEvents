# Architecture and Module Boundaries

## Purpose
SpectraEvents is built on clean hexagonal architecture with strict modularity. Dependencies flow in a single direction: `platform -> application -> core`. Platform-neutral domain and use-case code must never couple to server or loader APIs.

## Standard
- **Core Domain Isolation:** `spectraevents-core` is pure Java 21. It defines event models, phases, triggers, conditions, actions, rewards, and 3D mathematics (`Vector3`, `Quaternion`, `EulerRotation`). It must never depend on Bukkit, Paper, Spigot, NMS, or CraftBukkit.
- **Application Orchestration:** `spectraevents-application` coordinates event lifecycles, schedules, definitions, and model runtime services. It must remain platform-neutral (Java 21).
- **Platform Separation:** Minecraft APIs belong exclusively in platform adapters (`platforms/bukkit`, `platforms/paper`, `platforms/spigot`).
- **Shared Bukkit Layer:** Logic common to Paper and Spigot (Display entity tags, PDC keys, container holders, custom item providers) belongs in `:platforms:bukkit`. Do not duplicate adapter code between Paper and Spigot.
- **Hexagonal Ports:** Adapters implement SPI ports defined in `application` or `core` (`EventInstanceRepository`, `ModelProjectReader`, `ResourcePackSource`, `UpdateChecker`).

## Do:
```java
// Correct: Core domain calculation using pure Java
public record ModelTransform(Vector3 translation, Quaternion rotation, Vector3 scale) {
  public ModelTransform interpolate(ModelTransform target, float alpha) {
    return new ModelTransform(
        translation.lerp(target.translation(), alpha),
        rotation.slerp(target.rotation(), alpha),
        scale.lerp(target.scale(), alpha));
  }
}
```

## Don't:
```java
// Bad: Importing Bukkit/Paper in core or application
import org.bukkit.Location;
import org.bukkit.entity.Player;

// Bad: Using global singletons or service locators
SpectraEventsPlugin.getInstance().getEventService();
```

## Verification:
The repository validates module boundaries automatically:
```bash
./scripts/verification/validate-structure.sh
./gradlew verifyPlatformBoundaries
```
