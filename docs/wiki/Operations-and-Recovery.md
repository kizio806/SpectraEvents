# Operations and Recovery

## Before upgrading

1. Stop the server cleanly.
2. Back up the complete `plugins/SpectraEvents/` directory, including SQLite WAL files only after
   shutdown.
3. Record the installed JAR name and SHA-256.
4. Install exactly one artifact for the server family.
5. Start the server, run `/spectraevents doctor`, validate definitions, and exercise a disposable
   event before reopening production gameplay.

## Reward reconciliation

Claims use `PENDING → DELIVERING → DELIVERED`. A crash after an external inventory/economy mutation
but before durable acknowledgement is ambiguous. Inspect server/inventory/economy evidence, then use
an explicit operator decision. Never automatically retry a `DELIVERING` claim.

## Rollback

Stop the server, restore the complete pre-upgrade data directory and prior matching JAR, then start
once and inspect diagnostics. Do not combine an old database with hand-picked new YAML/assets.

Publication additionally requires the exact candidate commit, full build, artifacts/checksums,
all required server/client evidence, and zero open P0 entries in the executable release gate.
