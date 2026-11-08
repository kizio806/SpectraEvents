# Compatibility

Current distributions target Java 25. Platform-neutral core and application modules target Java 21;
that does not make the current server JARs Java-21 compatible.

SpectraEvents compiles exactly two production distribution JARs:

| Distribution Artifact | Target Server Platforms | Declared Minecraft Band | Feature & Scheduling Support |
| --- | --- | :---: | --- |
| `SpectraEvents-<version>-paper.jar` | Paper, Purpur, Folia | 26.1–26.3 | Full feature set, Adventure MiniMessage, Folia regional multithreading (`RegionTaskScheduler`), and Admin GUI. |
| `SpectraEvents-<version>-spigot.jar` | Spigot, CraftBukkit | 26.1–26.3 | Dedicated Spigot task scheduling, Spigot PDC persistence, Bukkit Admin GUI, and modular administration subcommands. |

All 15 server runtime combinations (Paper, Purpur, Folia, Spigot, CraftBukkit across 26.1, 26.2, 26.3) pass automated lifecycle, recovery, reward durability, and cleanup qualification.
