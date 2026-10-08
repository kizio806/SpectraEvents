# Permissions Contract

SpectraEvents follows a structured permission namespace (`spectraevents.*`). Permissions are granular but sensible wildcards are designed for admin ease.

## Permission Nodes

### Global
- `spectraevents.admin`: Grants all declared administration features.

### Events
- `spectraevents.event.*`: Grants all event instance controls.
- `spectraevents.event.list`: Allow listing active events.
- `spectraevents.event.start`: Allow starting events.
- `spectraevents.event.cancel`: Allow forcefully cancelling events.
- `spectraevents.event.trigger`: Allow manually advancing a configured diagnostic trigger.
- `spectraevents.event.inspect`: Allow inspecting event runtime state.

### Definitions
- `spectraevents.definition.*`: Grants all definition management controls.
- `spectraevents.definition.list`: Allow listing definitions.
- `spectraevents.definition.validate`: Allow running validation checks.
- `spectraevents.definition.reload`: Allow reloading configurations.

### Paper-only administration
- `spectraevents.admin.assets` with `build`, `import`, `info`, `list`, and `validate` children.
- `spectraevents.admin.model` with `info` and `list` children.
- `spectraevents.gui`, `spectraevents.integrations`, and `spectraevents.update.*`.

### Other declared nodes
- `spectraevents.status`, `spectraevents.doctor`, `spectraevents.schedule`.
- `spectraevents.rewards.claim`: allows a player to claim their own durable rewards; default `true`.
- `spectraevents.rewards.reconcile`: allows an administrator to inspect and explicitly resolve
  crash-ambiguous `DELIVERING` claims. It must not be granted to ordinary players.
