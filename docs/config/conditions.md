# Conditions

Conditions act as gates for `Triggers` or `Actions`. If a condition evaluates to false, the transition or action does not occur.

## Condition Catalog

| Condition ID | Description | Example Configuration |
| :--- | :--- | :--- |
| `permission` | Player must have a specific permission node. | `node: my.custom.perm` |
| `players-online` | Minimum or maximum number of players online. | `min: 5`, `max: 100` |
| `time-of-day` | In-game time requirements. | `min: 13000`, `max: 23000` |
| `world` | Must be in a specific world or avoid certain worlds. | `allowed: [world, world_nether]` |
| `biome` | Must be in a specific biome. | `allowed: [plains, desert]` |
| `distance` | Player must be within X blocks of the instance. | `max: 50` |
| `item-held` | Player must hold a specific item. | `material: DIAMOND_SWORD` |
| `score-threshold`| A player's objective score or custom stat. | `objective: kills`, `min: 10` |
| `cooldown` | Prevents rapid firing of a trigger. | `duration: 500ms` |

## Complexity
The expression language is intentionally kept simple for V1. Complex logical operators (`AND`, `OR`, `NOT` nesting) may be added if a strong use case emerges, but currently, a list of conditions implies a logical `AND`.
