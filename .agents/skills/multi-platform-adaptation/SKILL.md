---
name: multi-platform-adaptation
description: Maintain and verify platform adapter parity between Paper/Purpur/Folia and Spigot/CraftBukkit distributions.
---

# Multi-Platform Adaptation Skill

## When to use it
Use this skill when implementing new platform actions, particle/sound dispatchers, GUI menus, or server listeners across Paper and Spigot.

## Required steps
1. **Identify Shared Logic:** Place shared Presentation or Bukkit event logic in `:platforms:bukkit` (`AbstractBukkitActionAdapter`, `BukkitPdcKeys`).
2. **Implement Platform Divergence:**
   - Paper: Use `RegionTaskScheduler`, MiniMessage, and native Adventure components.
   - Spigot: Use standard Bukkit scheduler, ChatColor, and BungeeCord components.
3. **Verify Both Distributions:**
   ```bash
   ./gradlew :platforms:bukkit:check :platforms:paper:common:check :platforms:spigot:common:check
   ./gradlew build
   ```
