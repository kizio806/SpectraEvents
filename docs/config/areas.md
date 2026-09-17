# Event Area Specification

The `AreaComponent` defines a physical boundary around an event instance. It tracks membership and enforces rules.

## Shape
Areas can be defined as spheres or cuboids relative to the event's center.

```yaml
components:
  area:
    shape: sphere
    radius: 50
    rules:
      pvp: false
      block-break: false
      flight: false
```

## Membership Tracking
The engine monitors player movement asynchronously. When a player crosses the boundary, the engine emits `player-enter-area` or `player-leave-area` triggers. This is crucial for events like Vault, where leaving the area interrupts the hacking process.

## Rule Enforcement
The engine intercepts Bukkit events (e.g., `EntityDamageByEntityEvent` for PvP, `BlockBreakEvent`) and cancels them if they occur within the Area and violate the configured rules.

**Note**: This is not a complete replacement for WorldGuard. Complex, persistent region management should remain in WorldGuard. `AreaComponent` is strictly for temporary rules active only while the EventInstance is `RUNNING`.
