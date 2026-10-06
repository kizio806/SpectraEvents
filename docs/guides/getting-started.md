# Getting Started with SpectraEvents

Welcome to **SpectraEvents**, a data-driven 3D event engine for modern Minecraft servers. See the
[platform support contract](../product/feature-matrix.md#platform-support-contract) for deliberate
Paper-family and Spigot-family differences.

## Key Concepts

- **Event Definition**: A YAML configuration file describing an event's phases, models, triggers, and actions.
- **Event Instance**: A running runtime instance of an event definition.
- **Phase**: A distinct state in an event's lifecycle (e.g., `falling`, `locked`, `open`, `completed`).
- **Triggers**: Events or conditions that cause phase transitions (e.g., `timer_elapsed`, `interaction`, `health_threshold_crossed`, `entity_death`).
- **Actions**: Tasks executed when entering a phase or on trigger (e.g., `spawn_model`, `apply_damage`, `try_claim`, `give_item`, `spawn_boss`).

## Basic Commands

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/spectraevents status` | `spectraevents.status` | Views runtime status & active stats |
| `/spectraevents doctor` | `spectraevents.doctor` | Performs installation health checks |
| `/spectraevents admin` | `spectraevents.gui` | Opens the interactive Admin GUI |
| `/spectraevents definition list` | `spectraevents.definition.list` | Lists loaded YAML event definitions |
| `/spectraevents definition validate` | `spectraevents.definition.validate` | Validates YAML event definitions |
| `/spectraevents definition reload` | `spectraevents.definition.reload` | Reloads definitions from disk |
| `/spectraevents event start <id>` | `spectraevents.event.start` | Starts an event instance by definition ID |
| `/spectraevents event list` | `spectraevents.event.list` | Lists stored event instances |
| `/spectraevents event cancel <instance>` | `spectraevents.event.cancel` | Cancels a running instance |
| `/spectraevents update check` | `spectraevents.update.check` | Checks for plugin updates (Paper only) |
| `/spectraevents rewards reconcile list` | `spectraevents.rewards.reconcile` | Lists crash-ambiguous reward deliveries for an explicit operator decision |

## Shipped presets

The following YAML files are extracted to `plugins/SpectraEvents/events/presets/`. Copy a file into
`plugins/SpectraEvents/events/`, validate it and reload definitions before it can run; a first start
never silently activates an event.

1. **Meteor** (`meteor`): A falling spatial meteor model that locks on landing and can be destroyed by player interaction.
2. **Airdrop** (`airdrop`): A falling supply crate that locks, unlocks after a timer, and rewards the first player to claim it.
3. **Metin** (`metin`): A boss stone with health thresholds that enrages at 60% health, spawns a defender boss at 25% health, and explodes on boss death.
4. **Piñata** (`pinata`): An interaction-driven model where every accepted click counts equally toward a shared target.
5. **Boss Portal** (`boss_portal`): A timed portal model that spawns a tracked guardian and completes when its death is routed back to the event.
