# Target Architecture

SpectraEvents is a general-purpose, data-driven 3D event engine designed for modern Paper and Spigot ecosystem servers (Paper, Purpur, Folia, Spigot, Bukkit). It operates on a strict hexagonal architecture ensuring that domain logic remains entirely agnostic of the underlying server implementation.

## Architectural Areas

### 1. Core (`spectraevents-core`)
**Responsibility**: Houses pure domain logic, engine invariants, primitives, event lifecycle rules, phase transitions, visual 3D models, damage contribution tracking, and execution logic.
**Dependency Direction**: Java standard library only. Zero platform dependencies.

### 2. Application (`spectraevents-application`)
**Responsibility**: Contains use cases, orchestration services, YAML specification parsers/compilers, execution engine, and infrastructure ports. Bundles default event resources.
**Dependency Direction**: Depends on `spectraevents-core`. Zero platform dependencies.

### 3. Infrastructure Adapters (`adapters/storage-sqlite`, `adapters/update-http`)
**Responsibility**: Platform-neutral infrastructure adapters implementing application ports (SQLite instance repository, HTTP update provider).
**Dependency Direction**: Depends on `spectraevents-application`. Zero Bukkit/Minecraft dependencies.

### 4. Platform Adapters (`platforms/paper/common`, `platforms/spigot/common`)
**Responsibility**: Adapts abstract application ports to specific server implementations. Translates server events, schedulers, rendering, PDC metadata, and command/GUI frameworks into domain calls.
**Dependency Direction**: Depends on `spectraevents-application` and specific platform APIs.

### 5. Distribution (`distributions/paper`, `distributions/spigot`)
**Responsibility**: Final assembly modules that package each family-specific plugin entrypoint, platform-neutral adapters, application, and core into two distinct shadow JAR artifacts.
**Dependency Direction**: Depends on target platform implementation and adapters.
