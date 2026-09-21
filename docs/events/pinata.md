# Piñata Event

`pinata.yml` is a bundled, executable reference event. It proves that a fast interaction event does
not need to borrow combat health or an event-specific Java manager.

## What it uses

| Concern | Shared contract used by Piñata |
| --- | --- |
| Visual asset | `models/pinata.yml` (`dev_pinata_model`) with a native `Interaction` hitbox and looping `sway` animation |
| State | `initialize_hit_counter` creates a durable 20-hit counter |
| Input | The platform router emits generic `interaction`; YAML invokes `increment_hits` |
| Completion | The engine emits `hits_reached` exactly when the target is first reached |
| Rewards and cleanup | `drop_loot`, `remove_bossbar`, `remove_model`, then `complete_event` |
| Restart | The shared event state store persists the hit count; normal lifecycle recovery recreates phase-owned resources |

Every accepted interaction is worth one hit. Weapon damage is irrelevant: Piñata uses the dedicated
counter rather than `Health` or `apply_damage`.

## Operator workflow

1. Start `pinata` with `/event event start pinata` at the intended location.
2. Interact with the model until the twentieth accepted interaction.
3. Confirm the drops and that the model and bossbar disappear.
4. To customize it, copy `events/pinata.yml`, give it a new underscore-only `id`, and change the
   counter maximum, rewards, model ID, or messages. No Java is required.

The shipped model is native YAML so the example works without a mandatory custom pack. It can be
replaced by an imported Blockbench model while preserving the same event definition contract.
