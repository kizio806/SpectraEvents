# Built-in event definitions

SpectraEvents bundles five executable, data-driven event definitions. They are ordinary YAML
definitions with shared engine primitives; no event has an event-specific coordinator. The server
extracts them to `plugins/SpectraEvents/events/` on first start.

| Definition | Role | Operator reference |
|---|---|---|
| `meteor.yml` | Large combat raid: protected HP gates, tracked waves, capped melee damage and durable public ground loot. | [Meteor raid](meteor.md) |
| `metin.yml` | Structure raid: phases, contributions, tracked waves and podium rewards. | [Metin event](metin.md) |
| `airdrop.yml` | Timed PvP point of interest with a durable, public shared crate. | [Airdrop event](airdrop.md) |
| `pinata.yml` | Interaction-counter reference event. | [Piñata event](pinata.md) |
| `boss-portal.yml` | Timer-to-boss-to-death-router reference event. | [Boss Portal event](boss-portal.md) |

## Shared operating rules

- Large encounters reserve a non-overlapping zone and are limited to three active events per
  world.
- Global event bossbars are refreshed once per second and include state and location; combat
  messages remain local to the zone, with starts and finales announced globally.
- Event state, contributions and shared loot are checkpointed to SQLite for restart recovery.
- YAML owns models, loot, waves, messages and phase graphs. The command and inventory GUI expose
  only explicitly declared bounded scalar parameters.

See [event definitions](../config/event-definition.md), [actions](../config/actions.md) and
[triggers](../config/triggers.md) for authoring contracts.
