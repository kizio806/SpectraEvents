# Permissions Contract

SpectraEvents follows a structured permission namespace (`spectraevents.*`). Permissions are granular but sensible wildcards are designed for admin ease.

## Permission Nodes

### Global
- `spectraevents.admin`: Grants access to all `/spectra commands` and bypasses event-area restrictions.
- `spectraevents.*`: Alias for `spectraevents.admin`.

### Commands
- `spectraevents.command.help`: Allow `/spectra help`. (Usually default true).
- `spectraevents.command.version`: Allow `/spectra version`.

### Events
- `spectraevents.event.*`: Grants all event instance controls.
- `spectraevents.event.list`: Allow listing active events.
- `spectraevents.event.start`: Allow starting events.
- `spectraevents.event.stop`: Allow gracefully stopping events.
- `spectraevents.event.cancel`: Allow forcefully cancelling events.
- `spectraevents.event.info`: Allow checking event status.
- `spectraevents.event.teleport`: Allow teleporting to events.

### Definitions
- `spectraevents.definition.*`: Grants all definition management controls.
- `spectraevents.definition.list`: Allow listing definitions.
- `spectraevents.definition.info`: Allow viewing definition info.
- `spectraevents.definition.validate`: Allow running validation checks.
- `spectraevents.definition.reload`: Allow reloading configurations.

### Visuals
- `spectraevents.model.preview`: Allow previewing 3D models.
- `spectraevents.animation.preview`: Allow previewing animations.

### Debug & Testing
- `spectraevents.reward.test`: Allow simulating loot tables.
- `spectraevents.debug`: Grants access to all debugging tools (hitboxes, performance metrics, verbose logging).
