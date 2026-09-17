# Components

Components are stateful mechanics attached to an event during a specific phase. While the core engine identifies these concepts, they will be implemented as discrete Java classes in subsequent milestones.

## Candidate Primitives Identified

### `TimerComponent`
- **Candidate Primitive**: Yes (Timer).
- **Evidence**: Required by Meteor (locked phase), Airdrop, Vault, Boss Portal.
- **Owns**: Countdown logic, formatting time for placeholders.
- **Must NOT own**: Transitions. It only emits `timer-expired` triggers.

### `HealthComponent`
- **Candidate Primitive**: Yes (Health).
- **Evidence**: Meteor, Metin.
- **Owns**: Max health, current health, damage calculation, damage leaderboards.
- **Must NOT own**: Visual bossbars or particle effects for damage.

### `HitCounterComponent`
- **Candidate Primitive**: Yes (Hit Counter).
- **Evidence**: Pinata.
- **Owns**: Number of discrete interactions recorded, cooldown between hits.
- **Must NOT own**: "Damage" scaling. Every hit is exactly 1 count.

### `InteractionComponent`
- **Candidate Primitive**: Yes (Interaction).
- **Evidence**: Meteor, Airdrop, Pinata, Vault, Boss Portal.
- **Owns**: Spawning an invisible Interaction entity, intercepting Bukkit interact/damage events, mapping them to domain triggers (`player-interact`).

### `BossBarComponent`
- **Candidate Primitive**: Yes (Bossbar).
- **Evidence**: Meteor, Metin, Boss Portal.
- **Owns**: Creating and updating a server-side bossbar, managing player visibility (who can see it).

### `HologramComponent`
- **Candidate Primitive**: Yes (Hologram).
- **Evidence**: All events.
- **Owns**: Spawning and updating TextDisplay entities.

### `LeaderboardComponent`
- **Candidate Primitive**: Yes (Leaderboard).
- **Evidence**: Meteor, Metin, Pinata, Boss Portal.
- **Owns**: Aggregating player scores (damage, hits, items delivered), sorting, and providing top N lists for reward distribution.

### `LootComponent` (or RewardDistributionComponent)
- **Candidate Primitive**: Yes (Loot).
- **Evidence**: All events.
- **Owns**: Referencing loot tables, distributing items to eligible participants based on rules.

### `AreaComponent`
- **Candidate Primitive**: Yes (Event Area).
- **Evidence**: All events.
- **Owns**: Defining a bounding box or sphere, tracking players inside, enforcing rules (PvP blocking).

### `MobWaveComponent`
- **Candidate Primitive**: Yes (Mob Waves).
- **Evidence**: Metin, Boss Portal.
- **Owns**: Spawning vanilla or custom mobs, tracking their living status, emitting `wave-cleared` or `mob-killed` triggers.
