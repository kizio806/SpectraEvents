# Placeholder Specification

Placeholders allow dynamic text generation in Holograms, Bossbars, Titles, and Messages.

## Internal Templating
SpectraEvents supports internal placeholders resolved at runtime.

| Placeholder | Meaning |
| :--- | :--- |
| `{event_id}` | The definition ID (e.g., `meteor`). |
| `{event_name}` | The display name (e.g., `Meteor`). |
| `{instance_id}` | The UUID of the running instance. |
| `{phase}` | The current phase name. |
| `{time}` | Formatted time remaining on the active timer. |
| `{health}` | Current health of the instance. |
| `{health_max}` | Maximum health of the instance. |
| `{health_percent}`| Percentage (e.g., `55%`). |
| `{x}`, `{y}`, `{z}`| Coordinates of the instance. |
| `{participants}` | Total number of players who interacted. |
| `{top_1_name}` | Name of the highest-ranked player. |
| `{top_1_value}` | Score of the highest-ranked player. |

## PlaceholderAPI Integration
In a future update, these internal placeholders will be exposed to PlaceholderAPI (e.g., `%spectraevents_meteor_active_count%`) so external scoreboards can track event status.

**Note**: Internal `{}` placeholders are parsed natively by the engine to guarantee performance, as `String.replace` or MiniMessage tags are significantly faster than full PAPI resolution for rapidly updating holograms (e.g., ticking timers).
