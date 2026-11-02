# Code Style and Development Guidelines

## Architectural Baseline

SpectraEvents is engineered as a modular, data-driven 3D event engine adhering to strict clean architecture principles:

- domain models and 3D math live entirely in pure Java in `:spectraevents-core` (Java 21)
- application use cases and model runtime orchestration live in `:spectraevents-application` (Java 21)
- shared Bukkit presentation and tracking logic live in `:platforms:bukkit` (Java 25)
- platform-specific modules (`:platforms:paper:common` and `:platforms:spigot:common`) are thin adapters bridging server APIs to application ports

## Package Naming

- root package: `io.github.kizio806.spectraevents`
- maintainer identifier: `kizio`
- keep module packages aligned with their project path

## Module Boundaries

- `spectraevents-core` owns domain entities, 3D math, transitions, actions, triggers, and rewards
- `spectraevents-application` owns event lifecycle, compilation, runtime services, and schedule execution
- `core` and `application` must never import Bukkit, Paper, Spigot, or NMS packages
- `platforms:bukkit` unifies shared Presentation and PDC logic between Paper and Spigot
- do not duplicate shared behavior across platform modules

## Naming Rules

- use direct names over clever names
- booleans should read as predicates such as `isEnabled`, `isShuttingDown`, `hasPendingClaims`
- keep YAML definition types, parameter keys, and domain actions aligned

## File And Repo Naming

- repository docs should expose consistent documentation across projects:
  `CHANGELOG.md`, `CONTRIBUTING.md`, `SECURITY.md`, `docs/architecture.md`, `docs/compatibility-matrix.md`, `docs/development.md`, `docs/installation.md`, `docs/modrinth.md`, `docs/releases.md`, `docs/troubleshooting.md`, `docs/wiki/*`
- docs should describe what the repository actually targets
- release automation should publish the verified artifacts described by the README and docs

## Hard Quality Gates

- `./gradlew clean check build` is mandatory for CI and release workflows
- Spotless formatting is enforced via `spotlessCheck` (and auto-formatted via `spotlessApply`)
- Java sources allow meaningful explanatory comments; trivial or redundant comments are forbidden
- Java sources must not contain `TODO`, `FIXME`, or `XXX` markers (enforced by `validateSourceHygiene`)
- root `src/` directory is prohibited; all sources must reside in subprojects (enforced by `validateStructure`)
- wildcard imports are forbidden (enforced by `validateSourceHygiene`)
- `@SuppressWarnings` annotations are strictly forbidden across all modules; warnings must be resolved by fixing root causes rather than silencing them (enforced by `validateSourceHygiene`)
- compilation is enforced with `-Xlint:all -Werror`
- minimum 85% instruction coverage is enforced on `core` and `application` (enforced by `jacocoTestCoverageVerification`)
