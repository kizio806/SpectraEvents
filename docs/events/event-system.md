# Event System Overview

This document clarifies the strict separation between the Engine's **Global Lifecycle** and the **Event-Specific Phases**.

## EventDefinition vs EventInstance

- **EventDefinition**: The immutable blueprint loaded from YAML (e.g., `meteor.yml`). It defines phases, components, and transitions. It has no physical presence in the world.
- **EventInstance**: A runtime object created from a Definition. It possesses a location, a unique ID, and runtime state (current health, active participants).

## Global Lifecycle

The SpectraEvents Runtime Kernel strictly manages the global lifecycle of an *Instance*. This lifecycle is fixed and non-configurable:

- **CREATED**: The instance object exists in memory but has not yet ticked in the world.
- **RUNNING**: The instance is actively ticking and executing its defined phases.
- **COMPLETED**: The instance has reached its natural, successful conclusion.
- **CANCELLED**: The instance was forcefully stopped (e.g., by an admin command or a fatal error).
- **FAILED**: The instance failed to execute properly (e.g., invalid spawn location, missing world).

## Event-Specific Phases

Phases are defined within the `EventDefinition` and only exist while the global lifecycle is `RUNNING`.

For example, a Meteor definition might have these phases:
- `FALLING`
- `IMPACT`
- `LOCKED`
- `ACTIVE`
- `DESTROYED`

These phases are completely arbitrary and specific to the `meteor.yml`. The Engine Kernel does not know what `FALLING` means; it only executes the components and transitions associated with the current phase.

## Participants

Players who interact with the event (deal damage, click, deliver items) are tracked as **Participants**. Their contributions are aggregated throughout the phases and evaluated during the reward phase.

## Configuration Snapshot Behavior

When an instance is created, it references a specific version of its `EventDefinition`. If the administrator runs `/spectra reload` and modifies the definition, the running instance **must not** abruptly change its behavior. It should retain a snapshot or pin to the older version to ensure runtime stability.
