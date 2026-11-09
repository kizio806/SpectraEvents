# Compatibility Matrix

SpectraEvents compiles exactly two production distribution JARs targeting modern Minecraft server software.

---

## 📦 Distribution Artifact Matrix

| Distribution Artifact | Target Server Platforms | Declared Minecraft Band | Feature & Scheduling Support |
| :--- | :--- | :---: | :--- |
| **`SpectraEvents-1.0.0-paper.jar`** | **Paper**, **Purpur**, **Folia** | **26.1–26.3** | Full feature set, native Adventure MiniMessage, asynchronous chunk loading, Folia regional multithreading (`RegionTaskScheduler`), and Admin GUI. |
| **`SpectraEvents-1.0.0-spigot.jar`** | **Spigot**, **CraftBukkit** | **26.1–26.3** | Dedicated Spigot task scheduling, Spigot PDC persistence keys, Bukkit Admin GUI, and modular administration subcommands. |

All 15 server runtime combinations (Paper, Purpur, Folia, Spigot, CraftBukkit across 26.1, 26.2, 26.3) pass automated lifecycle, recovery, reward durability, and cleanup qualification.

---

## ☕ Java Runtime Requirement

- **Server Runtime:** Requires **Java 25** (`java -version`).
- **Core Baseline:** Core and application domain modules compile against pure Java 21 baseline for portability.

---

## 🔌 Supported Plugin Integrations

SpectraEvents automatically detects and seamlessly bridges with:

- **PlaceholderAPI:** Leaderboard and event status placeholders (`%spectraevents_...%`).
- **LuckPerms:** Fine-grained permissions checking and hierarchy.
- **MiniPlaceholders:** Rich MiniMessage placeholder expansions.
- **Vault:** Economy and currency rewards.
- **Custom Item Providers:** Oraxen, ItemsAdder, and Nexo custom items in event loot tables and reward claims.

---

## 🔄 Versioning & Minecraft Backports

SpectraEvents follows Semantic Versioning (`MAJOR.MINOR.PATCH`). A single release build covers an entire Minecraft generation band (`26.1–26.3`).

For details on support branches, long-term backports, and release lifecycles, see the [Versioning Strategy](https://github.com/kizio806/SpectraEvents/blob/main/docs/versioning.md).
