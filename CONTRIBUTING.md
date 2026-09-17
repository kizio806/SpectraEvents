# Contributing to SpectraEvents

Thank you for contributing to SpectraEvents! Please follow these guidelines when submitting patches or feature pull requests.

## Development Environment

- **JDK Version**: Java 25 required for Paper platform compilation (`platforms/paper/v26_2`). Platform-neutral modules (`core`, `application`, `api`) target Java 21.
- **Build System**: Gradle 8+ with Kotlin DSL wrapper (`./gradlew`).

## Architectural Invariants

- **Dependency Flow**: Strictly `platform -> application -> core`.
- **Core & Application Isolation**: Core domain, application services, and public API must **never** import Bukkit, Paper, Spigot, NMS, or CraftBukkit classes.
- **Platform Code**: Minecraft API code belongs strictly inside platform modules (`platforms/paper/...`).
- **No Global Singletons**: Avoid global singletons, service locators, NMS hacks, or giant manager classes.

## Quality Gate

Before submitting a Pull Request, ensure all checks pass cleanly:

```bash
./gradlew spotlessApply
./gradlew clean check build
```

All new domain behaviors must be accompanied by unit or architecture tests.
