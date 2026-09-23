# Metin Event Specification

`metin.yml` is a bundled, executable reference for a combat-oriented event where players must
destroy a corrupted, self-healing crystal monolith while fending off waves of monster spawns.

## Gameplay Summary

| Phase | What happens | Transition |
|---|---|---|
| `spawn` | Crystal crashes into the ground | `timer_elapsed` (3s crash animation) |
| `active` | Crystal can be damaged; waves of monsters spawn at health thresholds | `health_depleted` |
| `completed` | Crystal shatters, loot drops | Terminal |

## Visual Model

The `metin_stone` model ([`models/metin.yml`](../authoring/models.md)) is built from 14 parts:

| Group | Parts | Description |
|---|---|---|
| Core Structure | `base_rock`, `core_crystal` | The main obsidian base and inner glowing purpur pillar |
| Spikes | `spike_n/s/e/w` | Sharp amethyst shards protruding outward |
| Runes | `rune_ring_1/2/3` | Magical glowing glyphs orbiting the crystal |
| Corruption | `void_cloud_1/2/3`, `tentacle_1/2` | Dark ethereal matter clinging to the rock |

## Animations

| Animation | Loop | Trigger | Description |
|---|---|---|---|
| `spawn` | ONCE | Phase `spawn` on-enter | Crystal slams into the ground from slightly above, spikes extend outward |
| `pulse` | LOOP | Phase `active` on-enter | Core throbs, runes orbit, tentacles writhe |
| `hit` | ONCE | `interaction` (left-click) | Core flashes bright red, base shudders briefly |
| `shatter` | ONCE | Phase `completed` on-enter | All spikes break off, core implodes, runes scatter and fade |

## Shared Contracts Used

| Concern | Contract |
|---|---|
| Visual asset | `models/metin.yml` — 14-part model |
| Timers | `timer_elapsed` for the 3-second spawn intro |
| Health | `damage_entity` with 1000 HP |
| Damage Feedback | `interaction` triggers `hit` animation and updates the bossbar instantly |
| Mob Waves | `health_threshold_crossed` at 75%, 50%, and 25% HP spawns protecting mobs |
| Recovery | Health and current state persist across server restarts |

## Operator Workflow

1. Start the event: `/spectraevents event start metin`
2. The crystal spawns and settles into the ground for 3 seconds.
3. The crystal becomes `active` and players begin attacking it.
4. Verify that when health reaches 75%, 50%, and 25%, a wave of monsters (`spawn_wave`) spawns to defend the crystal.
5. Destroy the crystal to see it shatter and drop its loot.
