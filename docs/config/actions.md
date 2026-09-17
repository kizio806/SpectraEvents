# Actions

Actions are one-shot operations executed by the engine. They can fire when entering a phase, exiting a phase, or reacting to a trigger within a phase.

## Action Catalog

| Action ID | Description |
| :--- | :--- |
| `spawn-model` | Spawns a referenced visual model into the world. |
| `remove-model` | Despawns a visual model. |
| `play-animation`| Starts a defined animation track on a model. |
| `play-sound` | Plays a Bukkit `Sound` at the instance's location. |
| `spawn-particles`| Spawns a configured particle effect. |
| `broadcast` | Sends a message to the entire server. |
| `message` | Sends a message only to nearby players/participants. |
| `title` | Displays a title/subtitle to players. |
| `actionbar` | Displays an actionbar message. |
| `show-bossbar` | Activates the Bossbar for the event. |
| `hide-bossbar` | Deactivates the Bossbar. |
| `show-hologram` | Spawns a text display hologram. |
| `spawn-mob` | Spawns a configured mob or wave. |
| `give-reward` | Evaluates a loot table and distributes rewards. |
| `execute-command`| Runs a command as Console. |
| `set-component-value` | Forcefully updates a component (e.g., set health to 50%). |
| `start-event` | Spawns a *new* instance of another event. |
| `complete-event`| Gracefully forces the global state to `COMPLETED`. |

## Implemented Actions (Config-Driven Runtime)

| Action ID | Category | Description | Parameters |
| :--- | :--- | :--- | :--- |
| `spawn_model` | Platform | Spawns a visual model entity structure | `model`, `height_offset`, `animate` |
| `move_model` | Platform | Moves spawned model to target ground location | `target` |
| `remove_model` | Platform | Removes model entities | None |
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
