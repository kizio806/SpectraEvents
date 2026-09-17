# Target Architecture

SpectraEvents is a general-purpose, data-driven 3D event engine designed for modern Paper servers and multi-platform expansion (Spigot, Folia, Purpur, Sponge). It operates on a strict hexagonal architecture ensuring that domain logic remains entirely agnostic of the underlying server implementation.

## Architectural Areas

### 1. API (`spectraevents-api`)
**Responsibility**: Defines minimal, stable contracts for external integrations and addon developers.
**Dependency Direction**: None (Java standard library only).

### 2. Core (`spectraevents-core`)
**Responsibility**: Houses pure domain logic, engine invariants, primitives, event lifecycle rules, phase transitions, visual 3D models, damage contribution tracking, and execution logic.
**Dependency Direction**: Depends only on `spectraevents-api`. Zero platform dependencies.

### 3. Application (`spectraevents-application`)
**Responsibility**: Contains use cases, orchestration services, YAML specification parsers/compilers, execution engine, and infrastructure ports. Bundles default event resources.
**Dependency Direction**: Depends on `spectraevents-core` and `spectraevents-api`. Zero platform dependencies.

### 4. Infrastructure Adapters (`adapters/storage-sqlite`, `adapters/update-http`)
**Responsibility**: Platform-neutral infrastructure adapters implementing application ports (SQLite instance repository, HTTP update provider).
**Dependency Direction**: Depends on `spectraevents-application`. Zero Bukkit/Minecraft dependencies.

### 5. Platform Adapters (`platforms/paper/common`, `platforms/paper/v26_2`, etc.)
**Responsibility**: Adapts abstract application ports to specific server implementations. Translates server events, schedulers, rendering, PDC metadata, and command/GUI frameworks into domain calls.
**Dependency Direction**: Depends on `spectraevents-application` and specific platform APIs.

### 6. Distribution (`distributions/paper/v26_2`, etc.)
**Responsibility**: The final assembly module that packages the platform plugin entrypoint, platform-neutral adapters, application, core, and API into a shadow jar artifact.
**Dependency Direction**: Depends on target platform implementation and adapters.
