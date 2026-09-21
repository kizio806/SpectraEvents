# Release, Upgrade, Recovery, and Reward Reconciliation

This guide is the operational source of truth for a SpectraEvents upgrade or release. It complements
the automated checks; it does not authorize publication by itself.

## Supported artifacts and publication gate

Use exactly one artifact per server:

| Server family | Artifact | Verified band |
| --- | --- | --- |
| Paper, Purpur, Folia | `SpectraEvents-<version>-paper.jar` | Minecraft 26.1–26.3; Folia is verified only for 26.1 and 26.2 while its Minecraft 26.3 server build is unavailable |
| Spigot, CraftBukkit | `SpectraEvents-<version>-spigot.jar` | 26.1–26.3 |

`config/release/release-blockers.json` is an executable publication gate. The release workflow runs
`scripts/release/verify_release_gate.py` after the required runtime matrix and refuses publication
while a required row is blocked. Folia for Minecraft 26.3 is not a required row: no upstream server
build exists and the plugin refuses to enable there with an explicit console message. `26.3` is a
Minecraft compatibility target, not a Folia release number.

When an upstream Folia build for Minecraft 26.3 appears, first add it as a required release row and
to the blocker file. Remove that blocker only after the new server has passed the same lifecycle,
cleanup, and restart-recovery workflow as every other declared row.

## Before an upgrade

1. Stop the server cleanly. Do **not** copy a live SQLite database with its WAL files in flight.
2. Copy the whole `plugins/SpectraEvents/` directory to timestamped offline storage. At minimum this
   preserves `spectraevents.db`, `events/`, `models/`, `assets/`, `resource-pack.yml`, and generated
   pack evidence.
3. Record the installed JAR name and SHA-256. For a candidate artifact, compare it with the release
   `SHA256SUMS.txt` using `sha256sum -c SHA256SUMS.txt`.
4. Read `CHANGELOG.md`, then replace only the family-specific JAR. Never place both distribution JARs
   in the same `plugins/` directory.

## Upgrade and rollback workflow

1. Start the server and run `/event doctor`.
2. Run `/event definition validate`, then `/event definition list`; resolve every reported definition
   error before starting an event.
3. Start a disposable event, inspect it with `/event event inspect <instance-id>`, and cancel it.
   A cancelled instance must report `runtimeState=false`, `tasks=0`, and `resources=0`.
4. For a recovery check, start a timer-driven event, stop the server cleanly, start it again, inspect
   the same instance, and cancel it only after its state and resources have been recovered.
5. If the upgrade fails, stop the server, restore the complete backup from step 2, restore the prior
   family-specific JAR, and start it once. Do not mix old/new YAML or database files by hand.

SpectraEvents never replaces its own JAR or restarts the server automatically. A rollback is always an
operator-controlled restore.

## Recovery, cleanup, and monitoring

At startup the engine restores persistent running state, resumes applicable timers, and reconciles
event-owned Display, Interaction, and boss entities. Use these signals:

| Signal | Healthy result | Operator response when unhealthy |
| --- | --- | --- |
| Startup log | `READY platform=...` and a reconciliation summary | Preserve the log, stop the server, back up the data directory, then investigate the first error |
| `/event doctor` | Definitions loaded; active instances and reconciliation are visible | Run `/event definition validate`; resolve missing models/configuration before retrying |
| `/event event inspect <id>` | Running state has runtime state, expected tasks/resources, and an optional recorded claimant | For terminal events, non-zero tasks/resources require log collection and a controlled restart; do not delete entities manually first |
| Cancellation | `CANCELLED`, `runtimeState=false`, `tasks=0`, `resources=0` | Treat residual resources as a recovery incident and retain logs/database backup |

## Accepted-but-undelivered external effects

`try_claim` durably accepts a claim before an external Minecraft/economy side effect can be made
transactional. This prevents duplicate acceptance but cannot prove delivery if the server crashes in
between. The claim is therefore **at-most-once in process, not exactly-once delivery**.

When a crash occurs around a reward:

1. Do not rerun the event or grant a second automated reward.
2. Save the instance ID, definition ID, and `claim=` value shown by `/event event inspect <id>` or
   `/event doctor`.
3. Compare that evidence with the server/economy/inventory logs for the same player and time window.
4. If delivery cannot be confirmed, grant one manual compensating reward, record the action in the
   incident ticket, and retain the database backup. If delivery is confirmed, do nothing further.

The operator, not the plugin, owns the final decision because external inventory/economy effects do
not share an atomic transaction with SQLite.
