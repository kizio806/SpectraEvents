# Command Contract

SpectraEvents uses a predictable, Brigadier-friendly command tree. The primary root command is `/spectra`.

## Command Tree

### General
- `/spectra help`
  - **Purpose**: Display available commands and basic usage.
- `/spectra version`
  - **Purpose**: Show the current plugin version and loaded engine modules.
- `/spectra reload`
  - **Purpose**: Reload all configurations, event definitions, and language files safely.

### Events (Instances)
- `/spectra event list`
  - **Purpose**: List all currently active Event Instances.
- `/spectra event start <definition>`
  - **Purpose**: Manually spawn a new instance of the specified definition.
- `/spectra event stop <instance>`
  - **Purpose**: Gracefully transition an instance to the `COMPLETED` state.
- `/spectra event cancel <instance>`
  - **Purpose**: Forcefully transition an instance to the `CANCELLED` state, bypassing standard completion logic.
- `/spectra event info <instance>`
  - **Purpose**: Display the current lifecycle status, phase, location, and key component states (e.g., health) of the instance.
- `/spectra event teleport <instance>`
  - **Purpose**: Teleport the executing player to the instance.

### Definitions
- `/spectra definition list`
  - **Purpose**: List all loaded Event Definitions and their current versions.
- `/spectra definition info <definition>`
  - **Purpose**: Show metadata about a specific definition (validity, phases, triggers).
- `/spectra definition validate <definition>`
  - **Purpose**: Perform a strict validation check on the YAML configuration.
- `/spectra definition reload <definition>`
  - **Purpose**: Hot-reload a single event definition without affecting others.
- `/spectra definition reload-all`
  - **Purpose**: Reload all definitions.

### Visuals
- `/spectra model list`
  - **Purpose**: List loaded models.
- `/spectra model preview <model>`
  - **Purpose**: Spawn a dummy model at the player's location for previewing.
- `/spectra animation list`
  - **Purpose**: List loaded animations.
- `/spectra animation preview <animation>`
  - **Purpose**: Play an animation on a currently previewed model.

### Rewards
- `/spectra reward test <table>`
  - **Purpose**: Simulate a drop from a specified loot table and output the result to chat.

### Debug
- `/spectra debug event <instance>`
  - **Purpose**: Enable verbose logging for a specific instance in the console.
- `/spectra debug hitboxes [instance]`
  - **Purpose**: Toggle visual hitboxes (using glowing entities or particles) for interaction components.
- `/spectra debug models [instance]`
  - **Purpose**: Expose internal display entity structure.
- `/spectra debug performance`
  - **Purpose**: Output engine performance metrics (tick duration, active components).

## Syntax and Extensibility
All commands must fail gracefully. Missing arguments should provide Brigadier tab-completion. If an instance ID is required, tab-completion must list active IDs.
