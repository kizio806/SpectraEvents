# SpectraEvents

[![CI](https://github.com/kizio806/SpectraEvents/actions/workflows/ci.yml/badge.svg)](https://github.com/kizio806/SpectraEvents/actions/workflows/ci.yml)
[![CodeQL](https://github.com/kizio806/SpectraEvents/actions/workflows/codeql.yml/badge.svg)](https://github.com/kizio806/SpectraEvents/actions/workflows/codeql.yml)
[![License: MPL 2.0](https://img.shields.io/badge/License-MPL_2.0-blue.svg)](https://opensource.org/licenses/MPL-2.0)

**SpectraEvents Beta Foundation (v0.1.0-beta.1)**

SpectraEvents is in public Beta. The generic event engine features 100% config-driven execution for Meteor, Airdrop, and Metin, atomic claim logic, damage tracking, transactional YAML reloads, SQLite persistence with restart recovery, an interactive Admin Inventory GUI, native permissions, and an update checker. SpectraEvents provides reusable building blocks that can be combined into completely different experiences.

> **One engine. Any event.**

---

## Project Status

> **Public Beta (v0.1.0-beta.1)**

SpectraEvents is currently in public beta (`v0.1.0-beta.1`).

APIs, configuration formats, and internal architecture are pre-release and subject to SemVer beta refinement.

The generic event engine features 100% config-driven execution for `Meteor`, `Airdrop`, and `Metin`, atomic claim logic, damage tracking, transactional YAML reloads, SQLite persistence with restart recovery, an interactive Admin Inventory GUI, native permissions, and an update checker.

## Platform Support & Multi-Platform Release Matrix

| Artifact | Target Platform | Compatible Minecraft Versions | Loaders | Status |
| --- | --- | --- | --- | --- |
| `SpectraEvents-<version>-paper.jar` | Paper, Purpur, Folia | `26.1.1`, `26.1.2`, `26.2`, `26.3` | `paper`, `purpur`, `folia` | **SUPPORTED / RUNTIME VERIFIED** |
| `SpectraEvents-<version>-spigot.jar` | Spigot, Bukkit | `26.1.1`, `26.1.2`, `26.2`, `26.3` | `spigot`, `bukkit` | **SUPPORTED / RUNTIME VERIFIED** |

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

SpectraEvents automatically handles the delivery of 3D models and textures through a seamless Modrinth resource pack integration.

```text
Blockbench
  → SpectraEvents (Asset Compiler)
  → Resource Pack ZIP
  → Modrinth CDN
  → Minecraft Player
```

**Server Owners**: You only need to install the `spectraevents.jar`. The official SpectraEvents resource pack is delivered automatically to your players upon joining. You do not need to host or download the resource pack manually!

If you wish to create your own custom 3D models, see the [Asset Pipeline](docs/authoring/asset-pipeline.md) and [Blockbench Authoring Guide](docs/authoring/blockbench.md).

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
├── spectraevents-api/
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

A future event definition may look similar to:

```yaml
id: meteor

model:
  id: meteor

spawn:
  strategy: random-surface
  world: world

phases:

  falling:
    on-enter:
      - type: spawn-model

      - type: animation
        animation: meteor_fall

    transitions:
      - trigger:
          type: animation-finished

        next: impact

  impact:
    on-enter:
      - type: animation
        animation: meteor_impact

      - type: particle
        preset: massive_explosion

    transitions:
      - trigger:
          type: timer
          duration: 3s

        next: locked

  locked:
    transitions:
      - trigger:
          type: timer
          duration: 5m

        next: active

  active:
    health:
      max: 10000

    leaderboard:
      metric: damage

    transitions:
      - trigger:
          type: health-zero

        next: destroyed

  destroyed:
    on-enter:
      - type: animation
        animation: meteor_break

      - type: reward
        table: meteor_rewards
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
