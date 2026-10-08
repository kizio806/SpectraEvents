# Command Contract

The public root commands are `/spectraevents` and `/se`. `/event` is not an alias. Commands use
suggestions for registered definitions, saved locations and active instance IDs. A missing value
returns a short message with a correct example instead of a stack trace or raw parser error.

Unless marked **Paper only**, the commands below are available on both distribution families. See
the [platform support contract](feature-matrix.md#platform-support-contract) for intentional
differences.

## Everyday administration

- `/spectraevents help` — shows the useful command groups.
- `/spectraevents status` — shows active instances and registered definitions.
- `/spectraevents admin` — opens the Paper operator panel for a player. **Paper only.**
- `/spectraevents doctor` — runs diagnostics.
- `/spectraevents integrations` — shows integration status. **Paper only.**
- `/spectraevents update check` — checks for an available plugin update. **Paper only.**

## Events and settings

- `/spectraevents event list`
- `/spectraevents event start <event-id>` — starts at the executor's position.
- `/spectraevents event start <event-id> location <name>` — **Paper only.**
- `/spectraevents event inspect <instance-id>`
- `/spectraevents event cancel <instance-id>`
- `/spectraevents event trigger <instance-id> <trigger>`
- `/spectraevents event config <event-id> show` — **Paper only.**
- `/spectraevents event config <event-id> set <yaml-declared-parameter> <value>` — **Paper only.**

There are no global difficulty profiles. Paper manual starts resolve YAML defaults followed by the
saved Paper override (`events/overrides/<id>.yml`). A schedule can supply its own scalar
`parameters` mapping, which is validated through the same event compiler. Spigot does not expose
the Paper GUI override store.

## Player rewards

- `/spectraevents rewards list`
- `/spectraevents rewards claim <claim-id>`
- `/spectraevents rewards reconcile list` — administrator-only list of crash-ambiguous claims.
- `/spectraevents rewards reconcile mark-delivered <claim-id>` — records a verified delivery.
- `/spectraevents rewards reconcile return-pending <claim-id>` — requeues only after an operator
  has established that the external inventory mutation did not happen.

Rewards are durable mailbox claims. An offline player or a full inventory retains the claim until a
later successful delivery.

## Locations — Paper only

- `/spectraevents location set <name>` — player only; saves the current position.
- `/spectraevents location list`
- `/spectraevents location remove <name>`

## Definitions and assets

- `/spectraevents template list`
- `/spectraevents template install metin` — atomically installs the bundled Metin bundle, builds
  its declared assets, and reloads the definition. It never overwrites an active definition or
  source asset.
- `/spectraevents validate` — validates active YAML, duplicate IDs, and model/animation references
  without replacing the live registry. `/spectraevents definition validate` is the equivalent
  nested form.
- `/spectraevents definition <list|reload|validate>`
- `/spectraevents model <list|info>` — **Paper only.**
- `/spectraevents assets <list|info|import|validate|build>` — **Paper only.** Spigot imports
  bundled and administrator-provided assets at startup but does not expose manual asset commands.

Asset and model commands are operator tools. They are intentionally kept out of the short help path
used during normal event operation. Manual model spawning and animation playback are not public
commands; event definitions own runtime visuals and animation lifecycle.
