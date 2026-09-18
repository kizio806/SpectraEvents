# SpectraEvents Built-in Event Specifications & Mechanics

SpectraEvents provides three fully data-driven baseline 3D events: **Meteor**, **Metin**, and **Airdrop**.

All events are 100% config-driven via YAML definitions located in `plugins/SpectraEvents/events/`.

---

## 1. Meteor Event (`meteor.yml`)

The **Meteor Event** is a high-impact cosmic event featuring a falling 3D meteor model, multi-phase HP combat, hostile minion waves, dynamic BossBars, Scoreboards, and loot rewards.

### Mechanics & Flow

```text
Phase 1: Falling (3s)
  ├─ Spawns 3D Meteor Model (height offset 25 blocks)
  ├─ Displays Cosmic BossBar: "Meteor Descending..."
  └─ Plays flame particles & falling sound

Phase 2: Impact (1s)
  ├─ Moves 3D Meteor Model to ground level
  ├─ Spawns massive explosion particles & dragon growl sound
  └─ Sets initial Health to 500 HP

Phase 3: Active Combat (100% -> 75% HP)
  ├─ Players attack meteor by left-clicking
  ├─ Dynamic BossBar updates live HP % and remaining health
  └─ Side-bar Scoreboard displays event status & top attackers

Phase 4: Enraged Phase (75% -> 25% HP)
  ├─ Triggered at 75% HP threshold
  ├─ Spawns Magma Minion Mob Wave (5 Magma Cubes / Fire Mobs)
  ├─ Broadcasts chat alert: "<gold>The Meteor is glowing red-hot! Minions summoned!"
  └─ Increases particle intensity & plays fiery sounds

Phase 5: Critical Instability (< 25% HP)
  ├─ Triggered at 25% HP threshold
  ├─ Spawns Meteor Defender Boss with custom armor & sword
  ├─ BossBar changes color to PURPLE with notched segment style
  └─ Heavy explosion emitter particles

Phase 6: Destroyed & Loot Drop (0 HP)
  ├─ Removes 3D Meteor Model
  ├─ Drops Loot Table (Diamonds, Netherite Scraps, Fire Charges, Gold, Experience)
  ├─ Broadcasts victory message to server
  └─ Removes BossBar & Scoreboard
```

---

## 2. Metin Event (`metin.yml`)

The **Metin Event** is an intense structure-bound combat event inspired by classic MMORPG stone crushing mechanics. It features 1000 HP, multiple minion waves, invulnerability phases, and rare crystal loot.

### Mechanics & Flow

```text
Phase 1: Spawning (2s)
  ├─ Spawns 3D Metin Stone Model on ground
  ├─ Initializes 1000 HP
  └─ Creates Green Segmented BossBar: "Metin Stone - 1000/1000 HP"

Phase 2: Phase One Combat (100% -> 75% HP)
  ├─ Players attack Metin stone with melee left-clicks
  └─ Damage updates BossBar & Scoreboard in real time

Phase 3: Shadow Minions Wave (75% -> 40% HP)
  ├─ Triggered at 75% HP threshold
  ├─ Spawns Shadow Minion Wave (4 Wither Skeletons / Zombies)
  ├─ Plays dragon growl sound & smoke particle blast
  └─ Chat alert: "<dark_purple>Shadow Minions have emerged to defend the Metin!"

Phase 4: Fiery Wave & Enraged State (40% -> 15% HP)
  ├─ Triggered at 40% HP threshold
  ├─ Spawns Fiery Skeleton Wave (6 Piglins / Skeletons)
  ├─ BossBar changes color to RED
  └─ Chat alert: "<red>The Metin Stone has entered an enraged state!"

Phase 5: Metin Overlord Boss Invulnerability (15% -> 0% HP)
  ├─ Spawns Metin Overlord Boss
  ├─ Metin stone becomes invulnerable until Boss is slain
  └─ Message on click: "<red>Metin is invulnerable! Defeat the Metin Overlord!"

Phase 6: Defeated & Crystal Rewards
  ├─ Triggers explosion effects & removes 3D Metin Model
  ├─ Drops Rare Loot Table (Emeralds, Enchanted Golden Apples, Diamonds, Rare Gear)
  ├─ Removes BossBar & Scoreboard
  └─ Completes event instance
```

---

## 3. Airdrop Event (`airdrop.yml`)

The **Airdrop Event** is a tactical supply crate delivery event. An Airdrop supply crate falls from the sky, lands on the ground, and enters a locked countdown phase before unlocking into a claimable loot container.

### Mechanics & Flow

```text
Phase 1: Descending (3s)
  ├─ Spawns 3D Airdrop Crate Model in sky (height offset 20 blocks)
  ├─ Parachute / cloud particle trail
  └─ Broadcasts server alert with coordinates

Phase 2: Locked Crate Landing (30s Unlock Timer)
  ├─ Crate lands at ground level
  ├─ Plays wood placement sound & cloud impact particles
  ├─ Starts 30-second unlock countdown
  ├─ Displays Yellow Progress BossBar: "Airdrop Unlocking in X seconds..."
  └─ Clicking crate displays: "<yellow>Airdrop is currently locked. Unlocks soon!"

Phase 3: Unlocked Crate
  ├─ 30s timer expires
  ├─ Plays chest open sound & villager happy particle burst
  ├─ BossBar changes to GREEN: "Airdrop Unlocked! Right-Click to Claim!"
  └─ Broadcasts chat alert: "<green>Airdrop is now unlocked and claimable!"

Phase 4: Claimed & Reward Distribution
  ├─ First player to interact claims the Airdrop
  ├─ Gives Loot Table (Diamonds, Golden Apples, Iron Ingot stack, Custom Items)
  ├─ Plays level-up sound & broadcasts claimer name to server
  ├─ Removes 3D Airdrop Model
  └─ Cleanly removes BossBar & Scoreboard
```

---

## Engine Actions Reference

| Action Type | Description | Key Parameters |
| :--- | :--- | :--- |
| `show_bossbar` | Displays dynamic BossBar to players | `title`, `color`, `style`, `radius` |
| `update_bossbar` | Updates active BossBar title or progress | `title`, `color`, `style`, `progress` |
| `remove_bossbar` | Removes event BossBar | — |
| `show_scoreboard` | Displays event sidebar Scoreboard | `title`, `lines` |
| `remove_scoreboard` | Removes event Scoreboard | — |
| `drop_loot` | Spawns items / loot table at event location | `items` (material, amount, chance) |
| `spawn_mobs` | Spawns wave of custom mobs | `entity_type`, `count`, `name`, `offset-x` |
