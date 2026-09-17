# Architecture

SpectraEvents follows ports and adapters. Dependencies flow from Paper and infrastructure into the
application layer and then into the platform-neutral core. The core never imports Bukkit, Paper, NMS,
or CraftBukkit.

## Module boundaries

| Module | Responsibility | Java | Paper API |
| --- | --- | ---: | --- |
| `spectraevents-api` | Future public addon contracts | 21 | Forbidden |
| `spectraevents-core` | Domain model and invariants | 21 | Forbidden |
| `spectraevents-application` | Use cases and ports | 21 | Forbidden |
| `platforms/paper-common` | Cross-version Paper adapters | 25 | Compile only |
| `platforms/paper-26_2` | Paper 26.2 bootstrap and adapters | 25 | Compile only |
| `distributions/paper-26_2` | Shaded deployable plugin | 25 | Server-provided |

Future Paper lines receive separate adapters. Version-specific compatibility must not spread through
core as runtime conditionals.

## Testing

JUnit tests verify behavior, ArchUnit and the `verifyPlatformBoundaries` task enforce isolation, and
JaCoCo produces XML and HTML coverage reports. The `integrationTest` convention is available for real
infrastructure tests. `runServer` is the controlled Paper smoke-test entry point.

See `docs/architecture/adr/` for durable decisions and `docs/ai/` for operational engineering rules.
