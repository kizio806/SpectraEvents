# Airdrop Event Specification

`airdrop.yml` is a bundled, executable reference for a highly visible supply drop event that creates a
point of interest on the map, forcing players to converge and fight for control before looting.

## Gameplay Summary

| Phase | What happens | Transition |
|---|---|---|
| `descent` | Airdrop crate spawns in the sky and descends on a parachute | `timer_elapsed` (15s parachute drop) |
| `landed` | Crate hits the ground, parachute detaches, crate opens | `timer_elapsed` (5s unlatch delay) |
| `looted` | Loot drops around the crate, visual beacon disabled | `timer_elapsed` (30s cleanup delay) |
| `completed` | Crate model removed | Terminal |

## Visual Model

The `airdrop_crate` model ([`models/airdrop.yml`](../authoring/models.md)) is built from 12 parts:

| Group | Parts | Description |
|---|---|---|
| Crate | `base`, `lid` | The main container box |
| Parachute | `chute_canopy`, `chute_lines_1/2/3/4` | The deployed parachute holding the crate |
| Flare | `flare_base`, `flare_smoke` | Red signal flare attached to the crate |
| Straps | `strap_front`, `strap_back` | Cargo straps securing the lid |

## Animations

| Animation | Loop | Trigger | Description |
|---|---|---|---|
| `descent` | LOOP | Phase `descent` on-enter | Parachute inflates, crate sways gently as it falls down the Y-axis |
| `impact` | ONCE | Phase `landed` on-enter | Parachute collapses and fades out; crate bounces slightly on impact |
| `open` | ONCE | Phase `looted` on-enter | Straps snap off, lid swings open 120 degrees; flare extinguishes |

## Shared Contracts Used

| Concern | Contract |
|---|---|
| Visual asset | `models/airdrop.yml` — 12-part model |
| Timers | `timer_elapsed` controls the entire event flow autonomously |
| HUD | Dynamic scoreboard updates showing current phase |
| Rewards | `drop_loot` triggers exactly when the lid opens |
| Cleanup | `remove_model` triggers 30 seconds after opening |

## Operator Workflow

1. Move to a surface location with clear sky access.
2. Run `/spectraevents event start airdrop`
3. A parachute drops from 30 blocks above your position.
4. Players rush to the landing zone as the crate falls for 15 seconds.
5. Upon landing, the parachute detaches and 5 seconds later the crate springs open.
6. Loot scatters on the ground and 30 seconds later the empty crate disappears.
