# Airdrop PvP event

`airdrop.yml` is the public, durable supply-crate reference. It creates a time-bounded point of
conflict; the plugin leaves PvP and team policy to the server's own rules and region system.

## Flow

| Phase | Behaviour | Exit |
|---|---|---|
| `announced` | Global warning and HUD before the landing. | Configured announcement timer (15 minutes by default). |
| `falling` | The crate model descends to the reserved landing zone. | Landing sequence completes. |
| `locked` | The physical crate is visible but cannot be opened. | Configured lock expires (five minutes by default). |
| `open` | The pre-rolled public inventory can be opened; no item has been taken yet. | First atomic slot withdrawal, or the first-loot deadline. |
| `looted` | Everyone may immediately take remaining slots. | The final slot is removed. |
| `empty_display` | The empty crate remains as a visible objective. | Configured display timer expires (five minutes by default). |
| `evacuated` | Nobody took an item before the deadline. | Terminal cleanup without rewards. |

The first-loot window is 15 minutes by default. Airdrop's 45-minute encounter deadline covers
the normal timeline and protects recovery from an indefinitely running definition.

## Loot and recovery

The YAML pool is rolled exactly once before the crate opens. The resulting slot snapshot, its
empty state and the first-loot state are persisted. Each slot withdrawal is atomic, so concurrent
players can take different slots while only one can win a race for the same slot. Restart recovery
restores the phase, countdown, zone and remaining slots without rerolling loot.

## Operator configuration

Only the announcement, lock, first-loot and empty-display durations are GUI-editable scalar
parameters. Loot, model, animations and messages remain YAML-owned. The Airdrop uses the shared
zone contract (128-block radius by default) and counts toward the maximum of three large events
per world.
