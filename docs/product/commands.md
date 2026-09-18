# Command Contract

SpectraEvents uses a predictable, Brigadier-friendly command tree. The primary root command is `/event`.

## Command Tree

### General
- `/event help`
  - **Purpose**: Display available commands and basic usage.
- `/event version`
  - **Purpose**: Show the current plugin version and loaded engine modules.
- `/event reload`
  - **Purpose**: Reload all configurations, event definitions, and language files safely.

### Events (Instances)
- `/event event list`
  - **Purpose**: List all currently active Event Instances.
- `/event event start <definition>`
  - **Purpose**: Manually spawn a new instance of the specified definition.
- `/event event stop <instance>`
  - **Purpose**: Gracefully transition an instance to the `COMPLETED` state.
- `/event event cancel <instance>`
  - **Purpose**: Forcefully transition an instance to the `CANCELLED` state, bypassing standard completion logic.
- `/event event info <instance>`
  - **Purpose**: Display the current lifecycle status, phase, location, and key component states (e.g., health) of the instance.
- `/event event teleport <instance>`
  - **Purpose**: Teleport the executing player to the instance.

### Definitions
- `/event definition list`
  - **Purpose**: List all loaded Event Definitions and their current versions.
- `/event definition info <definition>`
  - **Purpose**: Show metadata about a specific definition (validity, phases, triggers).
- `/event definition validate <definition>`
  - **Purpose**: Perform a strict validation check on the YAML configuration.
- `/event definition reload <definition>`
  - **Purpose**: Hot-reload a single event definition without affecting others.
- `/event definition reload-all`
  - **Purpose**: Reload all definitions.

### Visuals
- `/event model list`
  - **Purpose**: List loaded models.
- `/event model preview <model>`
  - **Purpose**: Spawn a dummy model at the player's location for previewing.
- `/event animation list`
  - **Purpose**: List loaded animations.
- `/event animation preview <animation>`
  - **Purpose**: Play an animation on a currently previewed model.

### Rewards
- `/event reward test <table>`
  - **Purpose**: Simulate a drop from a specified loot table and output the result to chat.

### Debug
- `/event debug event <instance>`
  - **Purpose**: Enable verbose logging for a specific instance in the console.
- `/event debug hitboxes [instance]`
  - **Purpose**: Toggle visual hitboxes (using glowing entities or particles) for interaction components.
- `/event debug models [instance]`
  - **Purpose**: Expose internal display entity structure.
- `/event debug performance`
  - **Purpose**: Output engine performance metrics (tick duration, active components).

## Syntax and Extensibility
All commands must fail gracefully. Missing arguments should provide Brigadier tab-completion. If an instance ID is required, tab-completion must list active IDs.
