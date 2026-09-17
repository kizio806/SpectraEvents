# Platform Versioning & Addition Guide

## 1. Directory Layout Guidelines

All multi-platform adapters and distributions follow strict hierarchical grouping:

```text
platforms/
  ├── paper/
  │   ├── common/           # Shared version-agnostic Paper logic (GUI, commands, renderers)
  │   └── v26_2/            # Paper 26.2 NMS/API version bindings & plugin entrypoint
  ├── bukkit/               # (Future) Spigot/Bukkit common adapters & versions
  └── sponge/               # (Future) Sponge common adapters & versions

adapters/
  ├── storage-sqlite/       # Platform-neutral SQLite repository adapter
  └── update-http/          # Platform-neutral HTTP update adapter

distributions/
  └── paper/
      └── v26_2/            # Final Paper 26.2 shadowed plugin jar
```

---

## 2. Step-by-Step Guide for Adding a New Platform Family (e.g. Sponge)

To add a new platform family (e.g., `sponge`):

1. **Create Module Hierarchy**:
   - `platforms/sponge/common`: Shared Sponge services, event listeners, and action handlers.
   - `platforms/sponge/v10`: Sponge API v10 version bindings.
   - `distributions/sponge/v10`: Final distribution shadowJar.

2. **Implement Application Ports**:
   - Implement `PlatformActionPort` in `platforms/sponge/common/action/SpongeActionAdapter.java`.
   - Implement `EventTaskScheduler` in `platforms/sponge/common/scheduler/SpongeEventTaskScheduler.java`.
   - Implement `PlatformLifecyclePort` in `platforms/sponge/common/lifecycle/SpongeLifecycleReporter.java`.

3. **Wire Composition Root**:
   - Create `SpongeBootstrap.java` in `platforms/sponge/common` to orchestrate core domain application startup without changing domain code.
   - Core (`spectraevents-core`) and Application (`spectraevents-application`) require zero edits.

4. **Register in Gradle**:
   - Include new modules in `settings.gradle.kts`.
   - Ensure `distributions/sponge/v10` depends on `platforms:sponge:v10`, `adapters:storage-sqlite`, and `adapters:update-http`.

---

## 3. Step-by-Step Guide for Adding a New Version to an Existing Family (e.g. Paper v27_0)

To add a new version binding to the `paper` family:

1. Create directory `platforms/paper/v27_0`.
2. Add `build.gradle.kts` referencing `:platforms:paper:common`.
3. Implement `dev.spectraevents.platform.paper.v27_0.SpectraEventsPlugin` delegating to `PaperBootstrap.enable(this)`.
4. Create distribution `distributions/paper/v27_0` referencing `:platforms:paper:v27_0`.
5. Update `settings.gradle.kts`.
