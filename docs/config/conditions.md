# Conditions

Conditions act as gates for `Triggers` or `Actions`. If a condition evaluates to false, the transition or action does not occur.

## Condition Catalog

| Condition ID | Description | Example Configuration |
| :--- | :--- | :--- |
| `not_locked` | Allows a transition only when the event lock has expired. | `type: not_locked` |
| `is_locked` | Allows a transition only while the event is locked. | `type: is_locked` |

Platform integrations may add resolver-backed conditions. Names such as `permission`,
`players-online`, and `distance` remain design candidates until an integration registers and tests
them; they must not be presented as available v1 behavior.

## Complexity
The expression language is intentionally kept simple for V1. Complex logical operators (`AND`, `OR`, `NOT` nesting) may be added if a strong use case emerges, but currently, a list of conditions implies a logical `AND`.
