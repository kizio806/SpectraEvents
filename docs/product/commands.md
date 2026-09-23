# Command Contract

The only public root command is `/spectraevents`. `/event` is not an alias. Commands use Brigadier
suggestions for registered definitions, profiles, saved locations and active instance IDs. A missing
value returns a short message with a correct example instead of a stack trace or raw parser error.

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
- `/spectraevents event start <event-id> profile <easy|normal|hard>`
- `/spectraevents event start <event-id> location <name>`
- `/spectraevents event start <event-id> profile <easy|normal|hard> location <name>`
- `/spectraevents event start <event-id> location <name> profile <easy|normal|hard>`
- `/spectraevents event inspect <instance-id>`
- `/spectraevents event stop <instance-id>`
- `/spectraevents event cancel <instance-id>`
- `/spectraevents event config <event-id> show`
- `/spectraevents event config <event-id> profile <easy|normal|hard>`
- `/spectraevents event config <event-id> set <health|damage|hits|duration|lock-duration> <value>`

Profile values are applied after definition defaults. Saved configuration values are persistent
overrides applied after the selected profile. A one-off profile passed to `event start` does not
replace the saved profile.

## Locations

- `/spectraevents location set <name>` — player only; saves the current position.
- `/spectraevents location list`
- `/spectraevents location remove <name>`

## Definitions and assets

- `/spectraevents definition <list|reload|validate>`
- `/spectraevents model <list|info|validate|spawn|remove>`
- `/spectraevents animation <list|info|play|pause|resume|seek|stop>`
- `/spectraevents assets <list|info|import|validate|build|refresh|clean>`

Asset, model and animation commands are operator tools. They are intentionally kept out of the
short help path used during normal event operation.
