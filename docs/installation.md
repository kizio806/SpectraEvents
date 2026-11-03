# Installation Guide

SpectraEvents is a data-driven 3D event engine for modern Paper and Spigot Minecraft servers.

---

## ☕ System Requirements

- **Server Software:** Paper, Purpur, Folia, Spigot, or CraftBukkit (versions 26.1–26.3).
- **Java Runtime:** **Java 25** (strictly required for server plugin distributions).
- **Client Requirements:** Vanilla Minecraft clients (1.19.4+) supporting native Display and Interaction entities.

---

## 📥 Artifact Selection

Download the matching artifact for your server software from [GitHub Releases](https://github.com/kizio806/SpectraEvents/releases):

| Server Software | Artifact to Download |
|---|---|
| **Paper / Purpur / Folia** | `SpectraEvents-<version>-paper.jar` |
| **Spigot / CraftBukkit** | `SpectraEvents-<version>-spigot.jar` |

> ⚠️ **Important:** Do not place both JARs on the same server. Install exactly one JAR matching your server platform.

---

## 🚀 Step-by-Step Installation

1. Stop your Minecraft server cleanly.
2. Place the downloaded `SpectraEvents-<version>-<platform>.jar` into your server's `plugins/` directory.
3. Start the server and check the console logs for the SpectraEvents startup banner.
4. Verify the installation by running in console or in-game:
   ```text
   /spectraevents status
   /spectraevents doctor
   ```
5. *(Optional)* Install default official event templates:
   ```text
   /spectraevents template list
   /spectraevents template install metin
   /spectraevents template install airdrop
   /spectraevents template install meteor
   ```
6. Verify event definitions:
   ```text
   /spectraevents definition list
   /spectraevents definition validate
   ```

---

## 🔌 Optional Integrations

SpectraEvents automatically detects and integrates with the following plugins when present:
- **LuckPerms:** Fine-grained permissions checking and hierarchy.
- **MiniPlaceholders:** Rich MiniMessage placeholder expansion.
- **Vault:** Economy and currency rewards.
- **Custom Item Plugins:** Oraxen, ItemsAdder, and Nexo custom items in event loot and rewards.
