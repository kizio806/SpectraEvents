# Architecture

SpectraEvents follows ports and adapters. Dependencies flow from Paper and infrastructure into the
application layer and then into the platform-neutral core. The core never imports Bukkit, Paper, NMS,
or CraftBukkit.

## Module boundaries

| Module | Responsibility | Java | Paper API |
| --- | --- | ---: | --- |
| `spectraevents-core` | Domain model and invariants | 21 | Forbidden |
| `spectraevents-application` | Use cases and ports | 21 | Forbidden |
| `adapters/*` | Platform-neutral infrastructure | 21 | Forbidden |
| `platforms/paper/common` | Paper/Purpur/Folia adapter and bootstrap | 25 | Compile only |
| `platforms/spigot/common` | Spigot/CraftBukkit adapter and bootstrap | 25 | Forbidden |
| `distributions/paper` | Shaded Paper-family plugin | 25 | Server-provided |
| `distributions/spigot` | Shaded Spigot-family plugin | 25 | Forbidden |

Both platform families compile against the oldest supported `26.1` API. Paper/Purpur and
Spigot/CraftBukkit claims are gated by real-server workflows for `26.1`, `26.2`, and `26.3`; Folia
is gated separately for `26.1` and `26.2` and fails closed on unverified newer versions.

The public addon API is intentionally absent in beta; see ADR 0005.

## Testing

JUnit tests verify behavior, ArchUnit and the `verifyPlatformBoundaries` task enforce isolation, and
JaCoCo produces XML and HTML coverage reports. The `integrationTest` convention is available for real
infrastructure tests. `scripts/runtime-smoke/runtime_workflow.py` is the real-server release gate.

See `docs/architecture/adr/` for durable decisions and `docs/ai/` for operational engineering rules.
