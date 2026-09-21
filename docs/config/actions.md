# Actions

Actions are one-shot operations executed by the engine. They can fire when entering a phase, exiting a phase, or reacting to a trigger within a phase.

## Action Catalog (schema v1)

| Action ID | Description |
| :--- | :--- |
| `spawn_model` | Spawns a referenced visual model into the world. |
| `move_model` | Moves event-owned models to the current event location. |
| `remove_model` | Despawns event-owned models and stops their animations. |
| `play_animation` | Starts a named animation on event-owned models. `play-animation` is a compatibility alias. |
| `play_sound` | Plays a sound at the event location. |
| `spawn_particles` | Spawns particles at the event location. |
| `broadcast_message` / `broadcast` | Sends a message to the entire server. |
| `send_message` | Sends a message to the current command/player context. |
| `give_item` | Gives an item to the interaction actor. |
| `drop_loot` | Drops configured item entries at the event location. |
| `spawn_entity` / `spawn_boss` | Spawns an event-owned entity. |
| `spawn_mobs` / `spawn_wave` | Spawns an event-owned mob wave. |
| `show_bossbar` / `update_bossbar` / `remove_bossbar` | Manages the event bossbar. |
| `show_scoreboard` / `update_scoreboard` / `remove_scoreboard` | Manages the event scoreboard. |
| `initialize_health` | Creates or replaces event health. |
| `initialize_hit_counter` | Creates an equal-value interaction counter. |
| `set_locked` | Locks the event for a duration. |
| `try_claim` | Atomically claims a reward transition. |
| `apply_damage` | Applies damage and emits health triggers. |
| `initialize_hit_counter` | Domain | Initializes a per-instance equal-value hit counter | `max` |
| `increment_hits` | Domain | Records hits and emits `hits_reached` on the configured target | optional `amount` (default `1`) |
| `complete_event` / `cancel_event` | Ends the event and invokes cleanup. |

## Implemented Actions (Config-Driven Runtime)

| Action ID | Category | Description | Parameters |
| :--- | :--- | :--- | :--- |
| `spawn_model` | Platform | Spawns a visual model entity structure | `model`, `height_offset` |
| `move_model` | Platform | Moves spawned model to the current event location | None |
| `remove_model` | Platform | Removes model entities | None |
| `play_animation` | Platform | Starts a named animation on matching models owned by the event | `animation`, optional `model`, `speed`, `loop`, `max-loops` |
| `play_sound` | Platform | Plays audio at event location | `sound`, `volume`, `pitch` |
| `spawn_particles` | Platform | Spawns particle effects | `particle`, `count` |
| `initialize_health` | Domain | Initializes per-instance `Health` component | `max` |
| `set_locked` | Domain | Sets locked duration for `is_locked` condition | `duration` |
| `apply_damage` | Domain | Damages event `Health` (emits `health_depleted` at 0) | `amount` |
| `complete_event` | Domain | Transitions event instance state to `COMPLETED` | None |
| `cancel_event` | Domain | Transitions event instance state to `CANCELLED` | None |

## Failure Semantics
Actions distinguish between **Fatal** and **Non-fatal** failures:
- **Fatal Failure**: Critical platform failures (e.g. required model entity spawn fails) fail the event instance cleanly (`FAILED` state), cancel active timers, clean up allocated platform resources, and log an error.
- **Non-fatal Failure**: Minor side-effect failures (e.g. invalid sound or particle effect) log a diagnostic warning while execution continues cleanly without leaving state corrupt.

## Model animation example

Animations are started explicitly after the model is spawned. This keeps the model lifecycle and
timeline lifecycle deterministic; `spawn_model.animate` is not an implicit or supported shortcut.

```yaml
on-enter:
  - type: spawn_model
    model: meteor
  - type: play_animation
    model: meteor
    animation: spin
    loop: true
    speed: 1.0
```

`model` is optional. When omitted, the action targets every model owned by the current event. The
supported `loop` values are `true`, `false`, `ONCE`, `LOOP`, and `PING_PONG`. `max-loops: -1`
means unlimited loops. An unknown animation or an event without a matching active model is a
fatal action error and the event is failed and cleaned up according to the normal lifecycle rules.
