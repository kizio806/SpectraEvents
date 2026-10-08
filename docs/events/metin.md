# Metin Event Specification

`metin.yml` is a bundled, executable reference for a combat-oriented raid where players must
destroy a corrupted, self-healing crystal monolith while fending off waves of monster spawns.

## Gameplay Summary

| Phase | What happens | Transition |
|---|---|---|
| `manifestation` | Crystal manifests, locked for 30 seconds. | `timer_elapsed` (30s) |
| `dominance` | Crystal is vulnerable (100–75% HP). | `health_threshold_crossed` (75%) |
| `fracture` | Crystal becomes locked, spawns a wave of guards. | `wave_cleared` or `timer_elapsed` (90s) |
| `desperation` | Crystal is vulnerable (75–25% HP), spawns guards at 50%. | `health_threshold_crossed` (25%) |
| `final_assault` | Crystal becomes locked, spawns a Metin Defender boss. | `wave_cleared` |
| `victory` | Boss defeated, crystal shatters, top-3 podium rewarded. | Terminal |

## Visual Model

The `metin_stone` model ([`assets/source/metin_stone.bbmodel`](../authoring/models.md)) is imported from Blockbench:

| Group | Parts | Description |
|---|---|---|
| Core Structure | `base_rock`, `core_crystal` | The main obsidian base and inner glowing purpur pillar |
| Spikes | `spike_n/s/e/w` | Sharp amethyst shards protruding outward |
| Runes | `rune_ring_1/2/3` | Magical glowing glyphs orbiting the crystal |
| Corruption | `void_cloud_1/2/3`, `tentacle_1/2` | Dark ethereal matter clinging to the rock |

## Animations

| Animation | Loop | Trigger | Description |
|---|---|---|---|
| `idle` | LOOP | Phases `manifestation`, `dominance`, `fracture`, `desperation`, `final_assault` | Core throbs, runes orbit |
| `hit` | ONCE | `interaction` (left-click) | Core flashes bright red, base shudders briefly |
| `destroy` | ONCE | Phase `victory` on-enter | All spikes break off, core implodes, runes scatter and fade |

## Shared Contracts Used

| Concern | Contract |
|---|---|
| Visual asset | `assets/source/metin_stone.bbmodel` — imported model |
| Health | `initialize_health` with 1000 HP |
| Damage Feedback | `interaction` triggers damage and updates the bossbar |
| Mob Waves | `health_threshold_crossed` at 75%, 50%, and 25% HP spawns protecting mobs |
| Ranking | `award_podium` directly drops loot for the top-3 damage contributors |
| Recovery | Health and current state persist across server restarts |

## Operator Workflow

1. Start the event: `/spectraevents event start metin`
2. The crystal manifests and is locked for 30 seconds.
3. The crystal enters `dominance` and players begin attacking it.
4. Verify that when health reaches 75% and 25%, the crystal locks and waves of monsters (`spawn_wave`) spawn to defend it.
5. Defeat the final Metin Defender boss to see the crystal shatter and drop its loot.
