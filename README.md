# SpectraEvents

[![CI](https://github.com/kizio806/SpectraEvents/actions/workflows/ci.yml/badge.svg)](https://github.com/kizio806/SpectraEvents/actions/workflows/ci.yml)
[![CodeQL](https://github.com/kizio806/SpectraEvents/actions/workflows/codeql.yml/badge.svg)](https://github.com/kizio806/SpectraEvents/actions/workflows/codeql.yml)
[![License: MPL 2.0](https://img.shields.io/badge/License-MPL_2.0-blue.svg)](https://opensource.org/licenses/MPL-2.0)

SpectraEvents is a data-driven event engine for modern Minecraft servers. Administrators compose
YAML definitions from phases, triggers, conditions and actions; the engine owns instance lifecycle,
recovery, assets and reward-mailbox persistence.

## Status

This is pre-release software (`0.1.1-beta.1`). Configuration and command contracts may still change
within the beta line. A successful local build is not a release claim: publication requires every
declared real-server matrix row to pass. Folia is supported on Minecraft 26.1 and 26.2 only; Folia
26.3 is refused at startup and instructs the operator to install a newer SpectraEvents release.

Reward claims are committed before an external inventory mutation. This prevents automatic duplicate
delivery, but a process failure between those two operations can leave a claim in `DELIVERING`.
An administrator must explicitly reconcile that state with the rewards command; the engine never
silently reissues it.

## Distributions

| File | Use on | Minecraft range |
| --- | --- | --- |
| `SpectraEvents-<version>-paper.jar` | Paper, Purpur | 26.1–26.3 |
| `SpectraEvents-<version>-paper.jar` | Folia | 26.1–26.2 |
| `SpectraEvents-<version>-spigot.jar` | Spigot, CraftBukkit | 26.1–26.3 |

Use Java 25. Install exactly one matching JAR in `plugins/`. The Paper-family artifact provides the
Paper/Folia scheduling and GUI features; do not substitute it for the Spigot artifact or vice versa.
See the [platform feature matrix](docs/product/feature-matrix.md) for deliberate differences.

## First installation

1. Start the server once with the matching artifact.
2. SpectraEvents creates `config.yml`, its SQLite database, `events/`, `events/presets/`, locales and
   `assets/source/` below `plugins/SpectraEvents/`.
3. Copy a reference definition from `events/presets/` into `events/` and give it a unique `id` if you
   are making a variant. Presets are not automatically activated.
4. Run `/spectraevents definition validate`, fix every `file:path` diagnostic, then run
   `/spectraevents definition reload`.
5. Run `/spectraevents doctor` and start the loaded definition with
   `/spectraevents event start <id>`.

The shipped reference definitions are Meteor, Airdrop, Metin, Piñata and Boss Portal. They are
examples of the generic engine, not separate hard-coded subsystems.

Release CI builds three resource-pack ZIPs and publishes them to a separate Modrinth resource-pack
project. Player delivery is disabled until an administrator configures that project ID and real-client
acceptance evidence exists; the server then selects the matching pack automatically. See
[resource-pack configuration](docs/config/resource-pack.md).

## Operator workflow

| Need | Command or file |
| --- | --- |
| List / validate / reload definitions | `/spectraevents definition list\|validate\|reload` |
| Start, inspect or stop an event | `/spectraevents event start\|inspect\|stop` |
| Check health and diagnostics | `/spectraevents status`, `/spectraevents doctor` |
| Reload schedules | Edit `schedules.yml`, then `/spectraevents schedule reload` |
| Inspect or claim rewards | `/spectraevents rewards list\|claim <id>` |
| Resolve interrupted reward delivery | `/spectraevents rewards reconcile list`, then explicit `mark-delivered` or `return-pending` |

Schedules use an explicit timezone and may carry scalar `parameters` overrides. A schedule override
applies only to that scheduled instance. Paper's saved GUI override is a separate manual-start feature;
it is not a cross-platform global override layer.

More detail:

- [Installation](docs/guides/installation.md)
- [Local visual model test](docs/guides/local-model-test.md)
- [Create your first event](docs/guides/create-your-first-event.md)
- [Administrator workflow](docs/product/admin-workflow.md)
- [Commands](docs/product/commands.md) and [permissions](docs/product/permissions.md)
- [Schedules](docs/config/schedules.md)
- [Blockbench authoring](docs/authoring/blockbench.md) and [asset pipeline](docs/authoring/asset-pipeline.md)

## Development

Platform-neutral modules target Java 21; platform modules compile against Java 25. Dependency flow is
strictly `platform -> application -> core`; Minecraft APIs stay inside platform adapters.

```bash
./gradlew spotlessApply
./gradlew clean check build
```

The complete contributor contract is in [CONTRIBUTING.md](CONTRIBUTING.md). The project deliberately
does not expose a public addon API without a demonstrated addon use case, and it does not currently
offer a PostgreSQL storage contract.

## License

SpectraEvents is licensed under [MPL-2.0](LICENSE).
