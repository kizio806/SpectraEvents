# Target Package Map

The table below outlines the target packages, their responsibilities, and their current implementation status.

## Status Overview

| Module | Target Package | Responsibilities | Status |
| --- | --- | --- | --- |
| **Core** | `io.github.kizio806.spectraevents.core.event.runtime` | Event instance state machines & lifecycle | Refactored & Active |
| **Core** | `io.github.kizio806.spectraevents.core.event.lifecycle` | Event lifecycle domain events | Refactored & Active |
| **Core** | `io.github.kizio806.spectraevents.core.visual.model` | 3D visual models and part definitions | Refactored & Active |
| **Core** | `io.github.kizio806.spectraevents.core.gameplay.contribution` | Player damage contribution tracking | Refactored & Active |
| **Application** | `io.github.kizio806.spectraevents.application.config.spec` | Config DTO specs | Refactored & Active |
| **Application** | `io.github.kizio806.spectraevents.application.config.compiled` | Compiled domain event definitions | Refactored & Active |
| **Application** | `io.github.kizio806.spectraevents.application.config.loader` | Yaml & file system definition loaders | Refactored & Active |
| **Adapter: Storage** | `io.github.kizio806.spectraevents.adapter.storage.sqlite` | SQLite database implementation of EventInstanceRepository | Implemented |
| **Adapter: Update** | `io.github.kizio806.spectraevents.adapter.update.http` | HTTP update check provider implementation | Implemented |
| **Platform: Paper Common** | `io.github.kizio806.spectraevents.platform.paper` | Composition Root (`PaperBootstrap`), action adapters, schedulers, command & GUI decomposition | Refactored & Active |
| **Platform: Spigot Common** | `io.github.kizio806.spectraevents.platform.spigot` | Composition root, Bukkit scheduler/actions, commands, interactions, cleanup, and recovery | Active |
| **Distribution: Paper** | `distributions/paper` | Paper/Purpur/Folia shaded JAR | Active |
| **Distribution: Spigot** | `distributions/spigot` | Spigot/CraftBukkit shaded JAR | Active |

## Package Dependency Boundary Rules

1. `io.github.kizio806.spectraevents.core.*` must NEVER import `io.github.kizio806.spectraevents.application.*`, `io.github.kizio806.spectraevents.adapter.*`, `io.github.kizio806.spectraevents.platform.*`, or Bukkit/Minecraft APIs.
2. `io.github.kizio806.spectraevents.application.*` must NEVER import `io.github.kizio806.spectraevents.platform.*` or Bukkit/Minecraft APIs.
3. `io.github.kizio806.spectraevents.adapter.*` must NEVER import `io.github.kizio806.spectraevents.platform.*` or Bukkit/Minecraft APIs.
4. Platform implementations (`io.github.kizio806.spectraevents.platform.paper.*` and `io.github.kizio806.spectraevents.platform.spigot.*`) depend on `application`, `core`, and only their family API.
