# Spawn System Specification

The spawn configuration defines how the engine selects a location for a new `EventInstance`.

## Strategies

### `fixed`
Spawns the event at an exact, hardcoded coordinate.
- **Parameters**: `world`, `x`, `y`, `z`.

### `random-surface`
Selects a random X/Z coordinate within a radius and finds the highest non-air block.
- **Parameters**: `world`, `center-x`, `center-z`, `min-radius`, `max-radius`.

### `random-radius`
Selects a completely random 3D coordinate. (Useful for flying events).
- **Parameters**: `world`, `center`, `min-radius`, `max-radius`, `min-y`, `max-y`.

### `region`
Selects a random location exclusively within a predefined WorldGuard region or custom polygon.

## Constraints
All random strategies evaluate constraints before accepting a location. If constraints are violated, the engine tries again.

- `avoid-blocks`: e.g., `[WATER, LAVA, CACTUS]`
- `allowed-biomes`: e.g., `[PLAINS, DESERT]`
- `denied-biomes`: e.g., `[OCEAN, RIVER]`
- `min-distance-from-spawn`: Ensure it doesn't drop on the world spawn.
- `min-distance-from-other-events`: Ensure instances don't overlap.

## Failure Handling
The system has a `max-attempts` limit (e.g., 50). If no valid location is found, the spawn strategy fails. The engine catches this, logs an error, and transitions the instance to the `FAILED` lifecycle state gracefully.
