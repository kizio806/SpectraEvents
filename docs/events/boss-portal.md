# Boss Portal Event

`boss-portal.yml` is a bundled, executable reference for a timed visual event that releases a
tracked boss. It uses the existing generic timer, model animation, boss spawn, entity-death routing,
loot, cleanup, and recovery contracts — no `BossPortalManager` is needed.

## Gameplay Summary

| Phase | What happens | Transition |
|---|---|---|
| `opening` | Portal assembles (pillars rise, arch drops, gate materialises) | `timer_elapsed` after 10 s |
| `active` | Boss spawns immediately; players fight the Portal Guardian | `entity_death` when boss dies |
| `completed` | Loot drops, portal closes, HUD removed | Terminal |

## Visual Model

The `boss_portal` model ([`models/boss-portal.yml`](../authoring/models.md)) is built from 16 parts:

| Group | Parts | Description |
|---|---|---|
| Pillars | `pillar_n/s/e/w` | End stone bricks columns at ±1.8 blocks |
| Arch | `arch_top`, `arch_e`, `arch_w` | Purpur frame connecting the pillars |
| Gate | `gate` | Thin blackstone plane filling the arch interior |
| Eyes | `eye_n/s/e/w` | End portal frame blocks atop each pillar |
| Halos | `ring_inner`, `ring_outer` | Counter-rotating glass rings |
| Label | `label` | Floating text display |

## Animations

| Animation | Loop | Trigger | Description |
|---|---|---|---|
| `opening` | ONCE | Phase `opening` on-enter | Pillars rise from underground; arch descends; gate fades in; rings spin up |
| `active` | LOOP | Phase `active` on-enter | Inner ring orbits clockwise, outer counter-clockwise; gate pulses |
| `close` | ONCE | Phase `completed` on-enter | All parts implode toward centre simultaneously |

## Shared Contracts Used

| Concern | Contract |
|---|---|
| Visual asset | `models/boss-portal.yml` — 16-part hierarchical model |
| Timer | `timer_elapsed` moves `opening → active` after 10 s |
| Boss spawn | `spawn_boss` tags entity with PDC linking it to this event instance |
| Boss kill | `entity_death` routes the kill back automatically via PDC tag |
| HUD | Bossbar (RED, NOTCHED_6) + scoreboard in `active` phase |
| Rewards | `drop_loot` with radius 3.0 drops at the portal centre |
| Cleanup | `remove_bossbar`, `remove_scoreboard`, `remove_model`, `complete_event` in `completed` |
| Recovery | Phase, timer, and boss entity ownership persist across restarts |

## Operator Workflow

1. Stand at the intended portal location and run:
   ```
   /spectraevents event start boss_portal
   ```
2. The portal assembles over 2 s; players have 10 s total to gather.
3. The Portal Guardian spawns as soon as the `active` phase starts.
4. Players defeat the boss; loot drops and the portal collapses.
5. Verify bossbar, scoreboard, and model are removed after completion.

To customise: copy `events/boss-portal.yml`, change the `id`, adjust the boss `entity_type`,
the `duration` on `timer_elapsed`, and the loot table under `drop_loot`.
Then reload with `/spectraevents definition reload`.
