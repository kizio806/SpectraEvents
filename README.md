# SpectraEvents

[![CI](https://github.com/kizio806/SpectraEvents/actions/workflows/ci.yml/badge.svg)](https://github.com/kizio806/SpectraEvents/actions/workflows/ci.yml)
[![CodeQL](https://github.com/kizio806/SpectraEvents/actions/workflows/codeql.yml/badge.svg)](https://github.com/kizio806/SpectraEvents/actions/workflows/codeql.yml)
[![License: MPL 2.0](https://img.shields.io/badge/License-MPL_2.0-blue.svg)](https://opensource.org/licenses/MPL-2.0)

**SpectraEvents Beta Foundation (v0.1.0-beta.2)**

SpectraEvents is a data-driven event engine for modern Minecraft servers. YAML definitions compose phases, triggers, conditions and actions; the bundled Meteor, Airdrop and Metin definitions exercise the same generic runtime. The beta includes SQLite-backed lifecycle recovery, native Display/Interaction entities, operator diagnostics, and separate Paper-family and Spigot-family distributions.

> **One engine. Any event.**

---

## Project Status

> **Public Beta (v0.1.0-beta.2)**

SpectraEvents is currently in public beta (`v0.1.0-beta.2`).

APIs, configuration formats, and internal architecture are pre-release and subject to SemVer beta refinement.

Reward claims are durably accepted before delivery and are at-most-once in-process and across a committed restart. A process or host crash between claim commit and the external Minecraft inventory mutation can leave an accepted-but-undelivered reward; `/event doctor` exposes claims for operator reconciliation. The project does not claim impossible exactly-once delivery of an external side effect.

## Platform Support & Multi-Platform Release Matrix

| Artifact | Target Platform | Compatible Minecraft Versions | Loaders | Status |
| --- | --- | --- | --- | --- |
| `SpectraEvents-<version>-paper.jar` | Paper, Purpur | `26.1`, `26.2`, `26.3` | `paper`, `purpur` | Local runtime workflow passed |
| `SpectraEvents-<version>-paper.jar` | Folia | `26.1`, `26.2` | `folia` | Local runtime workflow passed; upstream Folia 26.3 is not published |
| `SpectraEvents-<version>-spigot.jar` | Spigot, CraftBukkit | `26.1`, `26.2`, `26.3` | `spigot`, `bukkit` | Local runtime workflow passed |

Download the Paper JAR for Paper, Purpur or Folia. Download the Spigot JAR for Spigot/CraftBukkit.

**Do not install the Paper JAR on Spigot. Do not install the Spigot JAR on Paper, Purpur or Folia if you need Paper/Folia behavior.**

Every published release must pass its declared runtime workflow. The intended combined Modrinth Paper-family entry remains blocked until an official Folia 26.3 runtime exists and passes; the release workflow deliberately fails that missing row. A source checkout or untagged build is not described as fully release verified merely because it compiles.

Folia does not implement Bukkit scoreboard creation. A scoreboard action therefore logs a clear `unsupported` warning and the event continues without a sidebar; use bossbars for UI shared across Paper, Purpur, and Folia.

---

## Vision

Traditional Minecraft event plugins usually implement one specific mechanic.

SpectraEvents takes a different approach.

An event is composed from reusable systems:

```text
EventDefinition
       ↓
 EventInstance
       ↓
     Phase
       ↓
Trigger / Condition
       ↓
     Action
```

This allows completely different events to use the same runtime.

Examples:

```text
☄ Meteor
🎁 Airdrop
🪨 Metin
🪅 Piñata
💎 Crystal
🏦 Vault
🐉 Dragon Egg
🛸 UFO
🏴‍☠️ Pirate Treasure
👹 Boss Portal
🎃 Halloween Event
🎄 Christmas Event
```

The event type should be configuration and assets — not another hardcoded Java implementation.

---

## Planned Features

### Event Engine

* Data-driven event definitions
* Multiple simultaneous event instances
* Configurable phases
* Phase transitions
* Triggers and conditions
* Reusable actions
* Reusable event components
* Event lifecycle management
* Automatic cleanup
* Crash and restart recovery

### 3D Models & Assets

The runtime renders server-authored YAML models with native Display and Interaction entities. It also
imports one signed Blockbench Generic Model bundle format, registers its model and named animations
for ordinary event actions, and builds a deterministic local resource-pack ZIP. Optional player
delivery accepts only an administrator-hosted HTTPS ZIP with an explicit SHA-1; Modrinth publishing is
disabled.

The importer and ZIP structure have automated verification, but the final M2 requirement — observing
custom-model-data mapping and animation in a real Minecraft client — is still open. See the
[Blockbench Authoring Guide](docs/authoring/blockbench.md) and
[Asset Pipeline](docs/authoring/asset-pipeline.md) for the current supported contract.

### Planned Features

#### 3D Models

* Native Minecraft Display Entities
* `ItemDisplay`
* `BlockDisplay`
* Multipart models
* `Interaction` entities for hitboxes
* Custom model transforms
* Position, rotation and scale control
* Per-part interactions
* Distance-based visibility and optimization

### Animation Engine

* Timeline-based animations
* Keyframes
* Translation
* Rotation
* Scaling
* Multiple animation tracks
* Easing functions
* Client-side display interpolation where possible
* Animation events and callbacks

Example:

```text
METEOR

Spawn
  ↓
Fall
  ↓
Rotate
  ↓
Impact
  ↓
Explosion
  ↓
Locked
  ↓
Active
  ↓
Destroyed
```

### Event Components

Planned reusable components include:

```text
ModelComponent
HealthComponent
TimerComponent
InteractionComponent
BossBarComponent
HologramComponent
LeaderboardComponent
LootComponent
AreaComponent
SpawnComponent
MobWaveComponent
ParticleComponent
SoundComponent
```

Components are intended to be reusable across completely different event definitions.

### Interactions

Events may react to:

* Player interaction
* Player attacks
* Projectiles
* Timers
* Health changes
* Hit counters
* Mob kills
* Cleared waves
* Entering or leaving event areas
* Delivered items
* Animation completion
* Custom addon triggers

### Rewards

Planned reward system:

* Vanilla items
* Custom items
* Commands
* Experience
* Currency
* Weighted loot tables
* Personal loot
* World drops
* Top-player rewards
* Participation rewards
* Random participant rewards
* Last-hit rewards

### Leaderboards

Events may track statistics such as:

* Damage dealt
* Hits
* Kills
* Items collected
* Time spent inside an objective
* Interactions
* Custom metrics

### Event Areas

Optional event regions may control mechanics such as:

* PvP
* Building
* Block breaking
* Flight
* Elytra
* Teleportation
* Commands
* Ender pearls

Advanced region support may be provided through integrations such as WorldGuard.

---

## Architecture

SpectraEvents is designed around a modular architecture with strict separation between the event domain and the Minecraft platform.

```text
┌─────────────────────────────────────┐
│              Paper                  │
│ Commands / Listeners / Rendering    │
└─────────────────┬───────────────────┘
                  │
┌─────────────────▼───────────────────┐
│          Application Layer          │
│ Event / Reward / Animation Services │
└─────────────────┬───────────────────┘
                  │
┌─────────────────▼───────────────────┐
│             Domain Core             │
│ Events / Phases / Actions / Triggers│
└─────────────────┬───────────────────┘
                  │
                 Ports
                  │
       ┌──────────┼──────────┐
       │          │          │
       ▼          ▼          ▼
     Paper     Storage   Integrations
```

The core module should not depend directly on Bukkit or Paper.

Minecraft-specific behavior belongs in platform adapters.

---

## Planned Modules

```text
spectraevents/
│
├── spectraevents-core/
├── spectraevents-paper/
├── spectraevents-storage/
├── spectraevents-pack/
│
├── spectraevents-integrations/
│   └── (Planned integrations)
│
├── spectraevents-testkit/
│
└── spectraevents-plugin/
```

The distributed server plugin will still be provided as a normal `.jar`.

---

## Example Event

A minimal definition accepted by the current strict schema looks like:

```yaml
schema-version: "1"
id: example
initial-phase: waiting
phases:
  waiting:
    on-enter:
      - type: broadcast_message
        message: "<yellow>Event started.</yellow>"
    transitions:
      - trigger:
          type: timer_elapsed
          duration: 5s
        target: active
  active:
    transitions:
      - trigger:
          type: manual
        target: completed
  completed:
    on-enter:
      - type: complete_event
```

---

## Technology

SpectraEvents is planned around the modern Paper ecosystem.

Core technologies include:

* Java
* Gradle
* Paper
* Adventure
* MiniMessage
* Brigadier
* Persistent Data Container
* Display Entities
* SQLite
* PostgreSQL support for larger networks
* Blockbench for model creation
* Minecraft resource packs

The architecture is also intended to remain compatible with region-based scheduling models such as Folia.

---

## Resource Packs

SpectraEvents will support native custom event assets.

The planned pipeline is:

```text
Blockbench
    ↓
Minecraft model assets
    ↓
SpectraEvents resource pack
    ↓
ItemDisplay / BlockDisplay
    ↓
Animation Engine
```

The core plugin should not require Oraxen, Nexo, ItemsAdder or ModelEngine.

Integrations with third-party asset systems may be provided as optional adapters.

---

## Persistence

Active event state is intended to survive server restarts.

Persistent state may include:

* Active event instances
* Current phase
* Remaining timers
* Event health
* Event location
* Participant statistics
* Reward claims
* Cooldowns
* Event history

SQLite is planned as the default zero-configuration storage provider.

Network installations may use an external database such as PostgreSQL.

---

## Performance Principles

SpectraEvents is intended for real production servers.

The project will prioritize:

* Event-driven execution
* No unnecessary global tick loops
* Client-side Display Entity interpolation
* Controlled particle budgets
* Distance-based rendering
* Batched persistence
* Asynchronous database operations
* Region-aware scheduling
* Efficient participant tracking
* Strict entity cleanup
* Minimal work while events are idle

---

## Developer API

A public API is planned after the core runtime becomes stable.

The API is expected to support:

* Starting and stopping events
* Querying active events
* Registering actions
* Registering triggers
* Registering custom components
* Listening to lifecycle events
* Providing custom items
* Providing models
* Building third-party addons

Example concept:

```java
SpectraEventsAPI api = SpectraEventsProvider.get();

api.events().start("meteor");
```

---

## Initial Milestone

The first vertical slice will implement one complete event:

### Meteor Event

```text
Spawn
 ↓
3D model
 ↓
Falling animation
 ↓
Impact animation
 ↓
Particles + sound
 ↓
Locked phase
 ↓
Countdown
 ↓
Active phase
 ↓
Health system
 ↓
Damage leaderboard
 ↓
Destruction animation
 ↓
Loot
 ↓
Cleanup
```

Once this event works correctly, its mechanics will be extracted into reusable engine components.

The goal is not to build a special `MeteorEvent`.

The goal is to prove that a meteor can be built entirely from SpectraEvents primitives.

---

## Non-Goals

SpectraEvents is not intended to become:

* A replacement for Minecraft itself
* A complete custom mob engine
* A full skeletal animation engine
* A WorldGuard replacement
* A generic database framework
* A hard dependency on one custom-item plugin

The project should remain focused on one problem:

**building sophisticated Minecraft server events from reusable components.**

---

## Contributing

SpectraEvents is currently in early development.

Contribution guidelines will be added once the core architecture and coding standards are established.

Before the first public development release, major architectural changes should be discussed before implementation.

---

## License

SpectraEvents is licensed under the **Mozilla Public License 2.0**.

See `LICENSE` for details.

---

## Authors & Maintainers

* **kizio806** (Author & Lead Maintainer)
