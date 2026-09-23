# Meteor Event Specification

`meteor.yml` is a bundled, executable reference for an event that drops a fiery celestial rock from the sky,
incorporating altitude-based spawning, phased gameplay, and ranked damage leaderboards.

## Gameplay Summary

| Phase | What happens | Transition |
|---|---|---|
| `falling` | Meteor spawns high up and falls (animated descent) | `timer_elapsed` (10s fall duration) |
| `locked` | Meteor is embedded in ground but too hot to mine | `timer_elapsed` (15s cooling) |
| `active` | Meteor cools down; players can mine/damage it | `health_depleted` |
| `completed` | Core shatters, drops loot, HUD cleared | Terminal |

## Visual Model

The `meteor_core` model ([`models/meteor.yml`](../authoring/models.md)) is built from 12 parts to simulate a real burning rock:

| Group | Parts | Description |
|---|---|---|
| Core | `core` | Central deepslate/magma block structure |
| Outer Crust | `shell_n/s/e/w`, `shell_top/bottom` | Jagged rocky armor plates wrapping the core |
| Debris | `debris_1/2/3/4` | Small floating fragments orbiting the meteor |
| Marker | `impact_crater` | Visual scorch mark placed on the ground |

## Animations

| Animation | Loop | Trigger | Description |
|---|---|---|---|
| `fall` | ONCE | Phase `falling` on-enter | Meteor plunges from Y+40 down to ground level (Z-axis rotation) |
| `pulse` | LOOP | Phase `locked` on-enter | Magma core glows and throbs; crust shifts slightly |
| `cool` | LOOP | Phase `active` on-enter | Core stops pulsing; debris orbits slowly |
| `break` | ONCE | Phase `completed` on-enter | Shell plates explode outward; core shrinks and vanishes |

## Shared Contracts Used

| Concern | Contract |
|---|---|
| Visual asset | `models/meteor.yml` — 12-part model |
| Timers | `timer_elapsed` for both fall duration and cooling period |
| Health | `damage_entity` with 500 HP |
| HUD | Bossbar updates dynamically with `%health%/%max_health%` |
| Rewards | `drop_loot` based on damage participation |
| Recovery | Position and health state persist across server restarts |

## Operator Workflow

1. Stand in a wide open area (or allow automatic surface spawning).
2. Start the event: `/spectraevents event start meteor`
3. The meteor spawns high in the air and descends rapidly with the `fall` animation.
4. Players wait 15 seconds during `locked` phase for it to cool.
5. Players damage the meteor in `active` phase until broken.
6. Verify model breaks apart via `break` animation and loot drops.
