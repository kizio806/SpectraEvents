# Interaction Specification

Interactions map physical player inputs (left-click, right-click, projectile hits) to logical domain triggers.

## Mechanisms
SpectraEvents utilizes the native Minecraft `Interaction` entity to capture clicks and damage events without relying on complex armor stand raytracing.

## Configuration
Interactions are attached to models or exist globally for the instance.

```yaml
interactions:
  main_hitbox:
    width: 3.0
    height: 3.0
    offset: [0.0, 0.0, 0.0]
    on-left-click:
      - action: play-sound
        sound: block.stone.hit
```

## Internal Mapping
When the `InteractionComponent` receives a Bukkit `EntityDamageByEntityEvent` targeting its interaction entity, it does the following:
1. Validates the source (is it a player or a player's projectile?).
2. Emits a `player-interact` domain trigger.
3. If the phase has a `HealthComponent`, the engine routes the Bukkit damage value to the health pool.
4. If the phase has a `HitCounterComponent`, the engine routes a `1` to the hit counter.

Core domain logic remains completely insulated from the Bukkit event.
