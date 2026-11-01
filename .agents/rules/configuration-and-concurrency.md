# Configuration, Concurrency, and Persistence

## Purpose
SpectraEvents guarantees data durability, restart recovery, and server tick stability under multi-threaded server environments.

## Standard
- **Single-Writer SQLite Persistence:** All database mutations (instance state, player awards, mailbox claims) must execute asynchronously via `SingleWriterPersistenceExecutor`.
- **Database Pragmas:** SQLite databases operate in WAL mode (`journal_mode=WAL`, `synchronous=NORMAL`, `busy_timeout=5000`).
- **Durable Claim Lifecycle:** Reward claims transition deterministically:
  `PENDING -> DELIVERING -> DELIVERED`.
  Any claim interrupted by server crash must recover cleanly on restart without double-crediting or lost items.
- **Asynchronous Execution:** Never block Minecraft server tick threads or region threads on I/O, network requests, or database transactions. Guard all `CompletableFuture` handlers with error logging.

## Do:
```java
// Correct: Asynchronous single-writer task
persistenceExecutor.submit(() -> {
  databaseManager.saveInstanceSnapshot(instance, state);
  return null;
});
```

## Don't:
```java
// Bad: Direct JDBC write on server thread
connection.createStatement().executeUpdate("UPDATE ...");
```

## Verification:
```bash
./gradlew :adapters:storage-sqlite:test :adapters:storage-sqlite:check
```
