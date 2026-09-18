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

## Asset Contribution Rules

When contributing official visual assets (models, animations, textures) to SpectraEvents:
- **Edit Source Assets**: Always edit the Blockbench `.bbmodel` or `.spectra.zip` sources.
- **Do Not Edit Generated JSON**: Never hand-edit the generated resource-pack JSON output (e.g. `pack.mcmeta` or model definitions). Your changes will be overwritten by the automated pipeline.
- **Stable IDs**: Keep model hierarchy node IDs stable. They are used for targeting in animations and interaction logic.
- **Descriptive Names**: Use descriptive group names in your hierarchy (e.g. `right_arm` instead of `group2`).
- **Git Policy**:
  - **Commit**: Canonical source assets, `.bbmodel`, `.spectra.zip`, textures, exporter scripts, and test fixtures.
  - **Do NOT Commit**: Runtime caches, temporary build outputs, temporary resource pack ZIPs, or local server output.

## Quality Gate

Before submitting a Pull Request, ensure all checks pass cleanly:

```bash
./gradlew spotlessApply
./gradlew clean check build
```

All new domain behaviors must be accompanied by unit or architecture tests.
