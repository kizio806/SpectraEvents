# SpectraEvents Architecture

## Invariants

```text
Paper / infrastructure
          ↓
     Application
          ↓
        Core
```

Core must not depend on Bukkit, Paper, Minecraft internals, or any platform adapter. Application and
public API are also platform-neutral. Dependencies point inward; infrastructure implements ports
owned by inner layers.

## Modules

- `spectraevents-api`: intentionally small public contracts for future addon developers.
- `spectraevents-core`: pure domain code and engine invariants; Java 21, no platform types.
- `spectraevents-application`: use cases, orchestration, and infrastructure ports; Java 21.
- `platforms/paper-common`: stable Paper adapter code reusable across supported Paper lines.
- `platforms/paper-26_2`: Paper 26.2 and Java 25 bootstrap and version-specific behavior.
- `distributions/paper-26_2`: shaded, deployable Paper 26.2 plugin and local server task.

The intended version-support shape is:

```text
core
 ├── Paper 26.2 adapter
 └── future Paper 1.21 adapter
```

Compatibility belongs in versioned adapters, not in hundreds of scattered runtime version checks.
`paper-common` contains only behavior proven stable across the versions that use it.
