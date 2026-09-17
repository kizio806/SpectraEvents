# Getting Started with SpectraEvents

Welcome to **SpectraEvents**, a data-driven 3D event engine for Paper servers!

## Key Concepts

- **Event Definition**: A YAML configuration file describing an event's phases, models, triggers, and actions.
- **Event Instance**: A running runtime instance of an event definition.
- **Phase**: A distinct state in an event's lifecycle (e.g., `falling`, `locked`, `open`, `completed`).
- **Triggers**: Events or conditions that cause phase transitions (e.g., `timer_elapsed`, `interaction`, `health_threshold_crossed`, `entity_death`).
- **Actions**: Tasks executed when entering a phase or on trigger (e.g., `spawn_model`, `apply_damage`, `try_claim`, `give_item`, `spawn_boss`).

## Basic Commands

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/spectra status` | `spectraevents.status` | Views runtime status & active stats |
| `/spectra doctor` | `spectraevents.doctor` | Performs installation health checks |
| `/spectra admin` | `spectraevents.gui` | Opens the interactive Admin GUI |
| `/spectra definition list` | `spectraevents.definition.list` | Lists loaded YAML event definitions |
| `/spectra definition validate` | `spectraevents.definition.validate` | Validates YAML event definitions |
| `/spectra definition reload-all` | `spectraevents.definition.reload` | Reloads all definitions from disk |
| `/spectra event start <id>` | `spectraevents.event.start` | Starts an event instance by definition ID |
| `/spectra event list` | `spectraevents.event.list` | Lists all active running instances |
| `/spectra event cancel <instance>` | `spectraevents.event.cancel` | Cancels a running instance |
| `/spectra update check` | `spectraevents.update.check` | Checks for plugin updates |

## Default Included Events

1. **Meteor** (`meteor`): A falling spatial meteor model that locks on landing and can be destroyed by player interaction.
2. **Airdrop** (`airdrop`): A falling supply crate that locks, unlocks after a timer, and rewards the first player to claim it.
3. **Metin** (`metin`): A boss stone with health thresholds that enrages at 60% health, spawns a defender boss at 25% health, and explodes on boss death.
