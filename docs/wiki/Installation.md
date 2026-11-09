# Installation & Quick Start

This guide walks you through setting up SpectraEvents 1.0.0 on your Minecraft server.

---

## 📋 Prerequisites

- **Java 25 Runtime:** Current server platform distributions require Java 25 (`java -version`).
- **Supported Server:** Paper, Purpur, Folia, Spigot, or CraftBukkit running **Minecraft 26.1–26.3**.
- **(Optional) Integrations:** Vault (economy), PlaceholderAPI (placeholders), LuckPerms (permissions), or custom item plugins (Oraxen, ItemsAdder, Nexo).

---

## 📦 Choosing the Right Distribution JAR

SpectraEvents follows the **Golden Packaging Invariant** — every release builds exactly two production JARs:

| Server Software | Distribution JAR | Description |
| :--- | :--- | :--- |
| **Paper**, **Purpur**, **Folia** | `SpectraEvents-1.0.0-paper.jar` | Full feature set, native Adventure MiniMessage, asynchronous chunk loading, Folia `RegionTaskScheduler`, and Admin GUI. |
| **Spigot**, **CraftBukkit** | `SpectraEvents-1.0.0-spigot.jar` | Spigot task scheduling, Spigot PDC persistence keys, Bukkit Admin GUI, and modular administration subcommands. |

> ⚠️ **Important:** Place **only one** matching JAR in your server's `plugins/` directory. Never run both simultaneously.

---

## 🚀 Step-by-Step Installation

1. **Stop the server cleanly** before adding new plugins.
2. **Download** the matching JAR from [GitHub Releases](https://github.com/kizio806/SpectraEvents/releases) or [Modrinth](https://modrinth.com/plugin/spectraevents).
3. **Copy the JAR** into your server's `plugins/` folder.
4. **Start the server.** SpectraEvents will initialize its SQLite database in WAL mode and create default directories:
   ```text
   plugins/SpectraEvents/
   ├── config.yml
   ├── schedules.yml
   ├── events/
   └── models/
   ```
5. **Run system diagnostics** to verify complete environment health:
   ```bash
   /spectraevents doctor
   ```
6. **Install a production template** (e.g. Metin Stone):
   ```bash
   /spectraevents template install metin
   ```
7. **Validate the compiled definition:**
   ```bash
   /spectraevents definition validate
   ```
8. **Start your first live event:**
   ```bash
   /spectraevents event start metin
   ```

---

## 🎨 Client Resource Pack Setup

SpectraEvents 3D models run entirely on vanilla Display and Interaction entities without client mods. Client-side textures are delivered through the official companion pack:

- **Automatic Delivery:** SpectraEvents can automatically prompt joining players with the official version-matched resource pack hosted on Modrinth CDN (`spectraevents-assets`). Configure this in `config.yml`.
- **Custom Assets:** Server operators hosting private Blockbench models can host their own ZIP packages on any HTTP(S) server and configure the URL/hash in `config.yml`.
