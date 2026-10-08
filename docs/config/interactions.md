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
3. Emits the generic `interaction` trigger with the player as its actor.
4. Executes only the YAML actions declared for that trigger. For a Piñata this is `increment_hits`,
   which always records one hit unless the author deliberately supplies another positive `amount`.

Core domain logic remains completely insulated from the Bukkit event.
