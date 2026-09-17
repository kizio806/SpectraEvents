# Target Package Map

The table below outlines the target packages, their responsibilities, and their current implementation status.

## Status Overview

| Module | Target Package | Responsibilities | Status |
| --- | --- | --- | --- |
| **API** | `dev.spectraevents.api` | Minimal public API interfaces | Stable |
| **Core** | `dev.spectraevents.core.event.runtime` | Event instance state machines & lifecycle | Refactored & Active |
| **Core** | `dev.spectraevents.core.event.lifecycle` | Event lifecycle domain events | Refactored & Active |
| **Core** | `dev.spectraevents.core.visual.model` | 3D visual models and part definitions | Refactored & Active |
| **Core** | `dev.spectraevents.core.gameplay.contribution` | Player damage contribution tracking | Refactored & Active |
| **Application** | `dev.spectraevents.application.config.spec` | Config DTO specs | Refactored & Active |
| **Application** | `dev.spectraevents.application.config.compiled` | Compiled domain event definitions | Refactored & Active |
| **Application** | `dev.spectraevents.application.config.loader` | Yaml & file system definition loaders | Refactored & Active |
| **Adapter: Storage** | `dev.spectraevents.adapter.storage.sqlite` | SQLite database implementation of EventInstanceRepository | Implemented |
| **Adapter: Update** | `dev.spectraevents.adapter.update.http` | HTTP update check provider implementation | Implemented |
| **Platform: Paper Common** | `dev.spectraevents.platform.paper` | Composition Root (`PaperBootstrap`), action adapters, schedulers, command & GUI decomposition | Refactored & Active |
| **Platform: Paper v26_2** | `dev.spectraevents.platform.paper.v26_2` | Thin JavaPlugin entrypoint | Refactored & Active |

## Package Dependency Boundary Rules

1. `dev.spectraevents.core.*` must NEVER import `dev.spectraevents.application.*`, `dev.spectraevents.adapter.*`, `dev.spectraevents.platform.*`, or Bukkit/Minecraft APIs.
2. `dev.spectraevents.application.*` must NEVER import `dev.spectraevents.platform.*` or Bukkit/Minecraft APIs.
3. `dev.spectraevents.adapter.*` must NEVER import `dev.spectraevents.platform.*` or Bukkit/Minecraft APIs.
4. Platform implementations (`dev.spectraevents.platform.paper.*`) depend on `application`, `core`, and platform APIs.
