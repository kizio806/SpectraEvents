# 🌌 SpectraEvents

[![CI Status](https://github.com/kizio806/SpectraEvents/actions/workflows/ci.yml/badge.svg)](https://github.com/kizio806/SpectraEvents/actions/workflows/ci.yml)
[![SonarCloud Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=kizio806_SpectraEvents&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=kizio806_SpectraEvents)
[![Codecov Coverage](https://codecov.io/gh/kizio806/SpectraEvents/graph/badge.svg)](https://codecov.io/gh/kizio806/SpectraEvents)
[![GitHub Release](https://img.shields.io/github/v/release/kizio806/SpectraEvents?sort=semver&logo=github)](https://github.com/kizio806/SpectraEvents/releases)
[![Modrinth Assets](https://img.shields.io/badge/Modrinth-SpectraEvents%20Assets-00AF5C?logo=modrinth)](https://modrinth.com/resourcepack/spectraevents-assets)
[![License: MPL 2.0](https://img.shields.io/badge/License-MPL_2.0-blue.svg)](LICENSE)
[![Platform: Paper](https://img.shields.io/badge/Platform-Paper%2026.2-blue?logo=paper)](https://papermc.io/)
[![Platform: Purpur](https://img.shields.io/badge/Platform-Purpur%2026.2-purple)](https://purpurmc.org/)
[![Platform: Folia](https://img.shields.io/badge/Platform-Folia%2026.1--26.2-cyan)](https://papermc.io/software/folia)
[![Platform: Spigot](https://img.shields.io/badge/Platform-Spigot%2026.2-orange)](https://www.spigotmc.org/)

**SpectraEvents** is a high-performance, data-driven 3D event engine for modern Minecraft servers. Rather than hardcoding static mini-games, SpectraEvents empowers server operators and creators to author dynamic, multiplayer world encounters entirely through declarative YAML definitions, smooth Blockbench 3D display entities, and deterministic phase state machines.

---

## 📖 Table of Contents

- [Features](#features)
- [Architectural Highlights](#architectural-highlights)
- [Supported Platforms & Distributions](#supported-platforms--distributions)
- [Installation & Quick Start](#installation--quick-start)
- [Official Reference Presets](#official-reference-presets)
- [Operator Commands & Permissions](#operator-commands--permissions)
- [Asset Delivery & Resource Pack](#asset-delivery--resource-pack)
- [Building from Source](#building-from-source)
- [Documentation & Wiki](#documentation--wiki)
- [License](#license)

---

## ✨ Features

- 🪐 **Data-Driven Event Architecture:** Compose complete world events using modular YAML definitions—phases, triggers, conditions, actions, and limits—without writing a single line of Java code.
- 🧊 **Real-Time 3D Blockbench Engine:** Render, translate, and animate complex 3D models using vanilla Minecraft display entities and interaction hitboxes. Zero client-side mods required.
- ⚡ **Multi-Platform & Regional Threading:** Engineered for modern Paper 26.2, Purpur 26.2, Spigot 26.2, and regional multithreading on Folia 26.1–26.2. Free of global singletons, service locators, NMS, or reflection hacks.
- 💾 **Transactional Mailbox & Crash Recovery:** Fully persistent SQLite runtime state. Offline player rewards are stored in transactional mailboxes; active event instances seamlessly reconcile and recover upon server restarts.
- 🕒 **Timezone-Calibrated Schedules:** Schedule recurring world events with timezone precision, Cron expressions, and parameter overrides (`schedules.yml`).
- 📦 **Automated Modrinth Asset CDN:** Official resource-pack bundles are version-matched and delivered securely via Modrinth CDN with SHA-1 checksum validation, or hosted independently on custom server URLs.
- 🛡️ **Operator Diagnostic Doctor:** Comprehensive health diagnostics (`/spectraevents doctor`, `/spectraevents status`, `/spectraevents definition validate`) with actionable error codes and file path diagnostics.

---

## 🏛️ Architectural Highlights

SpectraEvents follows strict **Hexagonal Architecture (Ports and Adapters)**:

- **`spectraevents-core`:** Pure platform-neutral domain model (event lifecycle state machine, components, phases, triggers, conditions, rewards). Has zero dependencies on Bukkit, Paper, Spigot, or NMS.
- **`spectraevents-application`:** Use cases, configuration parsers, YAML compiler, validation diagnostics, scheduling orchestrators, and port definitions.
- **`adapters/`:** Pluggable infrastructure adapters (SQLite storage, HTTP update checkers, Blockbench asset parsing, resource-pack delivery).
- **`platforms/`:** Platform adapters bridging Paper and Spigot server runtimes with the domain layer.
- **`distributions/`:** Packaged deployment artifacts targeting specific server families (`paper` and `spigot`).

Dependency flow is strictly unidirectional: `platform -> application -> core`.

---

## 📦 Supported Platforms & Distributions

SpectraEvents targets **Java 25** for server execution (platform-neutral code targets Java 21). We produce two distinct distribution artifacts:

| Distribution JAR | Target Server Families | Minecraft Compatibility | Notes |
| :--- | :--- | :---: | :--- |
| **`SpectraEvents-<version>-paper.jar`** | **Paper**, **Purpur** | **26.1 – 26.3** | Full feature set, asynchronous chunk loading, native Adventure MiniMessage, admin GUI controller. |
| **`SpectraEvents-<version>-paper.jar`** | **Folia** | **26.1 – 26.2** | Regional multithreading compatible. *Note: Folia 26.3 is cleanly refused at startup due to upstream API removals.* |
| **`SpectraEvents-<version>-spigot.jar`** | **Spigot**, **CraftBukkit** | **26.1 – 26.3** | Dedicated Spigot task scheduling, Spigot PDC keys, and fallback platform adapters. |

> [!IMPORTANT]
> Install **exactly one** matching JAR in your server's `plugins/` directory. Do not substitute the Paper artifact for Spigot or vice-versa.

---

## 📥 Installation & Quick Start

### 1. Prerequisites
- Minecraft server running **Paper 26.2**, **Purpur 26.2**, **Folia 26.2**, or **Spigot 26.2**.
- **Java 25 runtime** (`java -version`).
- *(Optional)* [PlaceholderAPI](https://placeholderapi.com/) for leaderboard and event status placeholders.

### 2. First Installation
1. Download the latest distribution JAR (`SpectraEvents-<version>-paper.jar` or `SpectraEvents-<version>-spigot.jar`) from [GitHub Releases](https://github.com/kizio806/SpectraEvents/releases).
2. Place the JAR into your server's `plugins/` folder.
3. Start the server. SpectraEvents will create:
   ```text
   plugins/SpectraEvents/
   ├── config.yml              # Primary server configuration
   ├── schedules.yml           # Automated cron event schedules
   ├── spectraevents.db        # SQLite persistence database
   ├── events/                 # Active event definitions
   │   └── presets/            # Shipped reference templates (Meteor, Metin, etc.)
   ├── assets/                 # Custom Blockbench models and bundles
   └── locales/                # Language localization files
   ```
4. Copy a reference definition from `events/presets/` into `events/` and give it a unique `id` if creating a variant.
5. Validate the definition and reload:
   ```bash
   /spectraevents definition validate
   /spectraevents definition reload
   ```
6. Run the diagnostic doctor and launch your first event:
   ```bash
   /spectraevents doctor
   /spectraevents event start <definition-id>
   ```

---

## 🎮 Official Reference Presets

SpectraEvents ships with five production-ready reference presets located in `events/presets/`:

| Preset | Description | Key Mechanics Demonstrated |
| :--- | :--- | :--- |
| **Meteor** | A blazing celestial rock crashes from the sky into the world. | Falling animation, impact particle shockwave, multi-stage damage thresholds, area loot drops. |
| **Airdrop** | A supply crate parachutes slowly from cloud level down to the terrain. | Slow-fall display entity physics, smoke beacon flare, interactive loot chest with auto-despawn timer. |
| **Metin** | A cursed stone spawns hostile waves of aggressive mobs as its health depletes. | Health-gated phase transitions, mob spawning actions, bossbar countdown, top-damager reward distribution. |
| **Piñata** | A suspended festive piñata bursts into celebratory fireworks and bouncing loot. | Hit-count triggers, sound action chains, physical item drops scattered within a radial zone. |
| **Boss Portal** | An ominous rift opens, summoning an elite encounter with custom phase shifts. | Boss summon actions, dynamic area restriction boundaries, phase transition conditions, podium awards. |

> [!NOTE]
> Presets are generic event definitions showcasing the engine's capabilities—not hardcoded plugin subsystems. You can customize, copy, or replace them entirely.

---

## ⌨️ Operator Commands & Permissions

All commands use the base command `/spectraevents` (aliases: `/se`, `/events`). Permissions are nested under `spectraevents.*`:

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/spectraevents status` | `spectraevents.status` | Displays engine status, active instances, and runtime metrics. |
| `/spectraevents doctor` | `spectraevents.admin` | Performs complete system diagnostics and environment checks. |
| `/spectraevents event list` | `spectraevents.event.list` | Lists all active and running event instances across worlds. |
| `/spectraevents event start <id>` | `spectraevents.event.start` | Spawns a new instance of the specified event definition. |
| `/spectraevents event stop <uuid>` | `spectraevents.event.stop` | Immediately cancels and despawns an active event instance. |
| `/spectraevents event inspect <uuid>` | `spectraevents.event.inspect` | Displays detailed runtime state, current phase, and participants. |
| `/spectraevents definition list` | `spectraevents.definition.list` | Lists all compiled and loaded event definitions. |
| `/spectraevents definition validate` | `spectraevents.definition.validate` | Validates YAML definitions and reports compiler diagnostics. |
| `/spectraevents definition reload` | `spectraevents.definition.reload` | Hot-reloads definition files from disk without restarting. |
| `/spectraevents schedule list\|reload` | `spectraevents.schedule` | Inspects active cron schedules or reloads `schedules.yml`. |
| `/spectraevents rewards list\|claim` | `spectraevents.rewards` | Inspects or claims pending rewards from the personal mailbox. |
| `/spectraevents rewards reconcile` | `spectraevents.admin` | Reconciles interrupted or pending mailbox claims. |
| `/spectraevents admin` | `spectraevents.admin.gui` | Opens the interactive administrative GUI (Paper only). |

---

## 🎨 Asset Delivery & Resource Pack

SpectraEvents supports delivering custom 3D models and textures through client resource packs:

- **Modrinth Asset CDN:** Official resource packs are published to Modrinth project [`spectraevents-assets`](https://modrinth.com/resourcepack/spectraevents-assets) (ID: `Rg1nw8IW`).
- **Private Server Hosting:** Operators hosting custom Blockbench models can configure direct HTTP URLs (`ManualUrlResourcePackSource`).
- **Default Delivery:** In accordance with [ADR 0007](docs/architecture/adr/0007-defer-default-resource-pack-delivery.md), automatic resource-pack prompt delivery is **disabled by default** (`enabled: false`) until configured by an administrator.

For complete setup instructions, see the [Resource Pack Guide](docs/config/resource-pack.md).

---

## 🛠️ Building from Source

### Prerequisites
- **JDK 25** installed and set as `JAVA_HOME`.
- **Git**

### Build & Verification Commands
```bash
# Clone the repository
git clone https://github.com/kizio806/SpectraEvents.git
cd SpectraEvents

# Format source code
./gradlew spotlessApply

# Run test suites, architecture contracts, and static analysis
./gradlew check

# Build release distribution JARs
./gradlew build
```

Compiled JARs will be located in:
- `distributions/paper/build/libs/SpectraEvents-<version>-paper.jar`
- `distributions/spigot/build/libs/SpectraEvents-<version>-spigot.jar`

---

## 📚 Documentation & Wiki

Explore our comprehensive documentation suite:

### Guides & Operations
- [Installation Guide](docs/guides/installation.md)
- [Getting Started & First Event](docs/guides/getting-started.md)
- [Operator & Admin Workflow](docs/product/admin-workflow.md)
- [Commands Reference](docs/product/commands.md) & [Permissions Reference](docs/product/permissions.md)
- [Release and Recovery Procedures](docs/operations/release-and-recovery.md)

### Configuration & Event Authoring
- [Event Definition Format](docs/config/event-definition.md)
- [Phases & Transitions](docs/config/phases.md)
- [Triggers](docs/config/triggers.md) & [Conditions](docs/config/conditions.md)
- [Actions Reference](docs/config/actions.md)
- [Rewards & Mailbox Persistence](docs/config/rewards.md)
- [Automated Schedules](docs/config/schedules.md)
- [Blockbench Authoring Guide](docs/authoring/blockbench.md) & [Asset Pipeline](docs/authoring/asset-pipeline.md)

### Architecture & Standards
- [Target Architecture Overview](docs/architecture/target-architecture.md)
- [Platform Compatibility Matrix](docs/architecture/platform-compatibility.md)
- [Architectural Decision Records (ADRs)](docs/architecture/adr/0001-hexagonal-core.md)
- [Documentation Style Guide](docs/ai/DOCUMENTATION_STYLE.md)
- [GitHub Wiki](wiki/Home.md)

---

## 📜 License

SpectraEvents is licensed under the [Mozilla Public License 2.0 (MPL-2.0)](LICENSE).
