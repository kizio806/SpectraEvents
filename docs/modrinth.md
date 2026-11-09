# Modrinth Project & Publishing Guide

This document contains the official description markdown and release metadata for SpectraEvents on [Modrinth](https://modrinth.com).

---

## 📋 Modrinth Project Description

*(Copy the markdown below into the Modrinth project description editor)*

```markdown
# 🌌 SpectraEvents

[![Platform: Paper](https://img.shields.io/badge/Platform-Paper%2026.1--26.3-blue?logo=paper)](https://papermc.io/)
[![Platform: Purpur](https://img.shields.io/badge/Platform-Purpur%2026.1--26.3-purple)](https://purpurmc.org/)
[![Platform: Folia](https://img.shields.io/badge/Platform-Folia%2026.1--26.3-cyan)](https://papermc.io/software/folia)
[![Platform: Spigot](https://img.shields.io/badge/Platform-Spigot%2026.1--26.3-orange)](https://www.spigotmc.org/)
[![Java: 25](https://img.shields.io/badge/Java-25-red?logo=openjdk)](https://openjdk.org/)
[![Storage: SQLite WAL](https://img.shields.io/badge/Storage-SQLite%20WAL-green?logo=sqlite)](https://www.sqlite.org/)
[![No Client Mods](https://img.shields.io/badge/Clients-Vanilla%20Only-brightgreen)](https://modrinth.com)

**SpectraEvents** is a next-generation, high-performance **3D event engine** for modern Minecraft servers. Rather than hardcoding static mini-games, SpectraEvents empowers server operators and creators to author dynamic, cinematic, multiplayer world encounters entirely through declarative YAML definitions, smooth Blockbench 3D display entities, and deterministic phase state machines.

**Zero client-side mods required!** All 3D models and animations run using native vanilla Display and Interaction entities (`item_display`, `block_display`, `interaction`).

---

## ✨ Key Features

- 🪐 **100% Data-Driven Architecture:** Define full multi-phase events, raids, boss fights, loot drops, and piñatas entirely in YAML—phases, triggers, conditions, actions, and limits—without writing a single line of Java.
- 🧊 **Real-Time 3D Blockbench Engine:** Render, translate, and animate complex multi-bone Blockbench models directly in-game. Powered by smooth keyframed bone animations, quaternion rotations (slerp), and vanilla display entities.
- ⚡ **Multi-Platform & Regional Threading:** Dedicated distribution JARs for **Paper**, **Purpur**, **Spigot**, **CraftBukkit**, and native regional multithreading on **Folia** (`RegionTaskScheduler`). Zero global singletons, service locators, NMS, or reflection hacks.
- 📬 **Crash-Proof Transactional Mailbox:** State persistence backed by SQLite WAL mode. Offline player rewards are stored in durable, three-state mailboxes (`PENDING -> DELIVERING -> DELIVERED`) that seamlessly reconcile and survive unexpected server restarts.
- ⏰ **Timezone-Calibrated Schedules:** Schedule recurring world events with timezone precision, Cron expressions, and parameter overrides (`schedules.yml`).
- 📦 **Automated Modrinth Asset CDN:** Official resource-pack bundles are version-matched and delivered securely via the Modrinth CDN (`spectraevents-assets`), or hosted independently on private server URLs.
- 🛠️ **In-Game Administration & Live Diagnostics:** Built-in operator tools (`/spectraevents doctor`, `/spectraevents status`, definition validator, and live template installer).

---

## 🎮 Shipped Reference Presets

SpectraEvents comes pre-packaged with production-ready reference presets that you can install in seconds:

| Preset | Description | Key Mechanics Demonstrated |
| :--- | :--- | :--- |
| **Meteor** | A blazing celestial rock crashes from the sky into the world. | Falling animation, impact shockwave, multi-stage damage thresholds, radial loot drops. |
| **Airdrop** | A supply crate parachutes slowly from cloud level down to the terrain. | Slow-fall display entity physics, smoke beacon flare, interactive loot chest with auto-despawn timer. |
| **Metin Stone** | A cursed stone spawns hostile waves of aggressive mobs as health depletes. | Health-gated phase transitions, mob spawning actions, bossbar countdown, top-damager rewards. |
| **Piñata** | A suspended festive piñata bursts into celebratory fireworks and bouncing loot. | Hit-count triggers, sound action chains, physical item drops scattered within a radial zone. |
| **Boss Portal** | An ominous rift opens, summoning an elite encounter with custom phase shifts. | Boss summon actions, dynamic area restriction boundaries, phase transition conditions, podium awards. |

---

## 📦 Which JAR Should I Download?

SpectraEvents strictly follows the **Golden Packaging Invariant**—every release produces **EXACTLY TWO production JARs**:

| Distribution JAR | Target Server Platforms | Minecraft Versions | Features & Architecture |
| :--- | :--- | :---: | :--- |
| **`SpectraEvents-<version>-paper.jar`** | **Paper**, **Purpur**, **Folia** | **26.1 – 26.3** | Full feature set, asynchronous chunk loading, native Adventure MiniMessage, Folia regional multithreading via `RegionTaskScheduler`, Admin GUI. |
| **`SpectraEvents-<version>-spigot.jar`** | **Spigot**, **CraftBukkit** | **26.1 – 26.3** | Dedicated Spigot task scheduling, Spigot PDC persistence keys, Bukkit Admin GUI, and modular administration subcommands. |

> ⚠️ **Important:** Install **only one** matching JAR in your server's `plugins/` directory. Do not place both JARs on the same server.

---

## 🚀 Quick Start Guide

### 1. Prerequisites
- Server running **Paper 26.1–26.3**, **Purpur 26.1–26.3**, **Folia 26.1–26.3**, or **Spigot 26.1–26.3**.
- **Java 25 runtime** (`java -version`).
- *(Optional)* [PlaceholderAPI](https://placeholderapi.com/) for leaderboard and event status placeholders.

### 2. Installation
1. Download the matching JAR (`SpectraEvents-<version>-paper.jar` or `SpectraEvents-<version>-spigot.jar`).
2. Place the JAR into your server's `plugins/` folder.
3. Start the server. SpectraEvents will generate all default directories and configurations.
4. Install an official template (e.g. Metin Stone):
   ```bash
   /spectraevents template install metin
   ```
5. Run the diagnostic doctor to verify system health:
   ```bash
   /spectraevents doctor
   ```
6. Start your first event:
   ```bash
   /spectraevents event start metin
   ```

---

## ⌨️ Essential Commands & Permissions

Base command: `/spectraevents` (aliases: `/se`, `/events`). Permissions are nested under `spectraevents.*`:

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/se status` | `spectraevents.status` | Displays engine status, active instances, and runtime metrics. |
| `/se doctor` | `spectraevents.admin` | Performs complete system diagnostics and environment checks. |
| `/se event list` | `spectraevents.event.list` | Lists all active and running event instances across worlds. |
| `/se event start <id>` | `spectraevents.event.start` | Spawns a new instance of the specified event definition. |
| `/se event stop <uuid>` | `spectraevents.event.stop` | Immediately cancels and despawns an active event instance. |
| `/se event inspect <uuid>` | `spectraevents.event.inspect` | Displays detailed runtime state, current phase, and participants. |
| `/se definition validate` | `spectraevents.definition.validate` | Validates YAML definitions and reports compiler diagnostics. |
| `/se definition reload` | `spectraevents.definition.reload` | Hot-reloads definition files from disk without restarting. |
| `/se template list` | `spectraevents.template` | Lists bundled event templates available for instant installation. |
| `/se template install <id>` | `spectraevents.template` | Installs and compiles a bundled event template. |
| `/se schedule list\|reload` | `spectraevents.schedule` | Inspects active cron schedules or reloads `schedules.yml`. |
| `/se rewards list\|claim` | `spectraevents.rewards` | Inspects or claims pending rewards from the personal mailbox. |
| `/se admin` | `spectraevents.admin.gui` | Opens the interactive administrative GUI (Paper only). |

---

## 🎨 Asset Delivery & Resource Pack

SpectraEvents supports delivering custom 3D models and textures through client resource packs:
- **Modrinth Asset CDN:** Official resource packs are published to Modrinth project [`spectraevents-assets`](https://modrinth.com/resourcepack/spectraevents-assets) (ID: `Rg1nw8IW`).
- **Private Server Hosting:** Operators hosting custom Blockbench models can configure direct HTTP URLs in `config.yml`.
- **Default Delivery:** Automatic resource-pack prompt delivery is disabled by default until configured by an administrator.

---

## 🔌 Integrations

SpectraEvents automatically detects and seamlessly integrates with:
- **PlaceholderAPI:** Leaderboard and event status placeholders (`%spectraevents_...%`).
- **LuckPerms:** Fine-grained permissions checking and hierarchy.
- **MiniPlaceholders:** Rich MiniMessage placeholder expansions.
- **Vault:** Economy and currency rewards.
- **Custom Items:** Oraxen, ItemsAdder, and Nexo custom items in event loot and rewards.

---

## 🔗 Useful Links

- 📖 **[GitHub Repository](https://github.com/kizio806/SpectraEvents)**
- 📚 **[Comprehensive Wiki & Guides](https://github.com/kizio806/SpectraEvents/wiki)**
- 🐛 **[Issue Tracker](https://github.com/kizio806/SpectraEvents/issues)**
- 📦 **[Official Resource Pack](https://modrinth.com/resourcepack/spectraevents-assets)**
- 📜 **[License (MPL-2.0)](https://github.com/kizio806/SpectraEvents/blob/main/LICENSE)**
```

---

## 📦 Official Resource Pack CDN Metadata

Official Blockbench model textures are delivered through the Modrinth resource pack CDN project:
- **Project:** [`spectraevents-assets`](https://modrinth.com/resourcepack/spectraevents-assets)
- **Project ID:** `Rg1nw8IW`
- **Supported Versions:** `26.1`, `26.2`, `26.3`
