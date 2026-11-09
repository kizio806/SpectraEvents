# Shipped Official Event Presets

SpectraEvents ships with five production-ready, data-driven event presets. Each preset is fully authored in declarative YAML and demonstrates distinct mechanics, phase state machines, 3D Blockbench models, and reward distribution patterns.

---

## 🎮 Shipped Event Presets

| Event Preset | In-Game Command | Theme & Key Mechanics |
| :--- | :--- | :--- |
| **Meteor** | `/se template install meteor` | **Celestial Impact Raid:** A flaming meteor descends from the sky, detonating upon contact. Spawns hostile waves of aggressive elemental mobs as health depletes. Radial loot scatter and top-damager rewards. |
| **Airdrop** | `/se template install airdrop` | **Supply Crate Drop:** A supply crate parachutes slowly from cloud level down to the terrain. Emits smoke beacon flares, unlocks an interactive contested loot container, and despawns after a countdown timer. |
| **Metin Stone** | `/se template install metin` | **Multi-Phase Structure Raid:** An ancient cursed stone summons defensive waves of monsters as players chip away at its durability. Features dynamic BossBar health tracking, phase shifts, and podium rewards. |
| **Piñata** | `/se template install pinata` | **Festive Hit-Counter Event:** A suspended festive piñata bursts into celebratory fireworks, sound action chains, and bouncing loot showers upon reaching target hit counts. Anti-spam hit validation included. |
| **Boss Portal** | `/se template install boss-portal` | **Dimensional Elite Encounter:** An ominous dimensional rift opens, summoning an elite boss with enrage mechanics, area boundaries, phase transitions, and contribution-weighted podium awards. |

---

## 🛠️ Managing Presets

You can manage pre-packaged event definitions directly via in-game operator commands:

```bash
# List all available bundled presets
/spectraevents template list

# Install a preset into your active events directory
/spectraevents template install <airdrop|boss-portal|meteor|metin|pinata>

# Update an installed preset to the latest version bundled with the plugin
/spectraevents template update <preset-id>

# Remove an installed template definition
/spectraevents template remove <preset-id>
```

---

## ✏️ Customizing Presets

All installed presets are placed in:
```text
plugins/SpectraEvents/events/<id>.yml
```
You can freely customize phases, timers, mob spawns, loot tables, and visual effects in these files. After modifying a file, reload definitions without restarting:
```bash
/spectraevents definition validate
/spectraevents definition reload
```
