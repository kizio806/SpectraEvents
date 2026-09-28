# Command Contract

The public root commands are `/spectraevents` and `/se`. `/event` is not an alias. Commands use
suggestions for registered definitions, saved locations and active instance IDs. A missing value
returns a short message with a correct example instead of a stack trace or raw parser error.

## Everyday administration

- `/spectraevents help` — shows the useful command groups.
- `/spectraevents status` — shows active instances and registered definitions.
- `/spectraevents admin` — opens the Paper operator panel for a player.
- `/spectraevents doctor` — runs diagnostics.
- `/spectraevents integrations` — shows integration status.
- `/spectraevents update check` — checks for an available plugin update.

## Events and settings

- `/spectraevents event list`
- `/spectraevents event start <event-id>` — starts at the executor's position.
- `/spectraevents event start <event-id> location <name>`
- `/spectraevents event inspect <instance-id>`
- `/spectraevents event cancel <instance-id>`
- `/spectraevents event config <event-id> show`
- `/spectraevents event config <event-id> set <yaml-declared-parameter> <value>`

There are no global difficulty profiles. A parameter value resolves in this order: YAML default,
saved server override, schedule override, then a one-off start override. Only scalar parameters
explicitly declared as GUI-editable by YAML can be changed through commands or inventory GUI.

## Player rewards

- `/spectraevents rewards list`
- `/spectraevents rewards claim <claim-id>`

Rewards are durable mailbox claims. An offline player or a full inventory retains the claim until a
later successful delivery.

## Locations

- `/spectraevents location set <name>` — player only; saves the current position.
- `/spectraevents location list`
- `/spectraevents location remove <name>`

## Definitions and assets

- `/spectraevents definition <list|reload|validate>`
- `/spectraevents model <list|info>`
- `/spectraevents assets <list|info|import|validate|build>`

Asset and model commands are operator tools. They are intentionally kept out of the short help path
used during normal event operation. Manual model spawning and animation playback are not public
commands; event definitions own runtime visuals and animation lifecycle.
