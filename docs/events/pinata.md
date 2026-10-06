# Piñata Event

The Piñata is a community event: every player who right-clicks it contributes one hit toward a
shared goal. When the hit counter reaches the target, the piñata shatters and loot scatters
across the area — rewarding the crowd, not just a single claimant.

## Gameplay Summary

| Phase | What happens | Duration |
|---|---|---|
| `idle` | Model spawns, sway animation starts, 2 s prep pause | 2 s |
| `active` | Players right-click; hit counter increments on each interaction | Until `hits_reached` |
| `broken` | Break animation fires, loot scatters in a 6-block radius | Instant → `completed` |

## Visual Model

The `pinata` model ([`assets/source/pinata.bbmodel`](../authoring/models.md)) is imported from Blockbench:

| Part | Type | Role |
|---|---|---|
| `hook` | item_display (iron_bars) | Suspension point; the whole model rotates from here |
| `rope` | block_display (quartz_pillar) | Thin cord between hook and body |
| `body` | item_display (magenta_wool) | Main piñata blob — primary animation target |
| `top_dome` | item_display (pink_wool) | Rounded head dome |
| `tail` | item_display (white_wool) | Decorative puff |
| `leg_1–4` | block_display (coloured concrete) | Hanging tassels in yellow, red, green, blue |
| `label` | text_display | Floating name tag |

## Animations

| Animation | Loop | Trigger | Description |
|---|---|---|---|
| `idle` | LOOP | Phase `idle` on-enter | Pendulum swing from the hook pivot; legs lag behind (secondary motion) |
| `hit` | ONCE | Each `interaction` | 350 ms squash-and-stretch with leg kick-out |
| `break` | ONCE | `hits_reached` → phase `broken` | Explosive body expansion + legs scatter in 4 directions |

## Shared Contracts Used

| Concern | Contract |
|---|---|
| Visual asset | `assets/source/pinata.bbmodel` — imported hierarchy with interaction hitbox |
| State | `initialize_hit_counter` creates a durable SQLite-backed counter |
| Input | Platform router emits generic `interaction`; YAML calls `increment_hit_counter` |
| Completion | Engine emits `hits_reached` exactly once when counter == max |
| HUD | Bossbar (NOTCHED_20) and scoreboard track `%hits%/%max_hits%` live |
| Restart | Hit count persists across restarts; the loop animation has `recovery: RESUME` |
| Rewards | `drop_loot` with radius 6.0 — wide enough for all nearby players |

## Configuration

Configure via `/spectraevents event config pinata` or the Admin GUI → Configuration screen:

| Setting | easy | normal | hard | Description |
|---|---|---|---|---|
| `hits` | 25 | 50 | 100 | Total right-clicks needed to break the piñata |

No `health` or `damage` settings — Piñata uses the hit counter exclusively.

## Operator Workflow

1. Place yourself at the intended hang point and run:
   ```
   /spectraevents event start pinata
   ```
2. The piñata appears 4 blocks above your feet.
3. Invite players to right-click the model. The bossbar fills as hits accumulate.
4. At `hits` count the piñata shatters and loot drops.
5. Verify bossbar, scoreboard, and model are removed after completion.

To customise: copy `events/presets/pinata.yml` into `events/`, change the `id`, adjust `max:` on `initialize_hit_counter`,
change the loot table under `drop_loot`, and reload with `/spectraevents definition reload`.
