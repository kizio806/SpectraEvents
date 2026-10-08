# Meteor raid

`meteor.yml` is the reference large-raid definition. It uses the shared encounter zone,
tracked-wave, global HUD and durable public-loot primitives; it is not implemented by an
event-specific coordinator.

## Flow

| Phase | Behaviour | Exit |
|---|---|---|
| `announced` | Three-minute global warning and HUD. | Warning timer. |
| `falling` | The model descends for 15 seconds. | Impact. |
| `impact_lock` | Impact effects and a 45-second protected cooling period. | Cooling timer. |
| `assault_one` | Melee damage can reduce the core from 100% to 75%. | `fracture_guard`. |
| `fracture_guard` | Core is protected until its bounded first wave dies. | Clear within two minutes. |
| `assault_two` | Core can be reduced from 75% to 50%. | `eruption_guard`. |
| `eruption_guard` | Second bounded wave protects the core. | Clear within two minutes. |
| `assault_three` | Core can be reduced from 50% to 25%. | `cataclysm`. |
| `cataclysm` | Meteor Warden and the final bounded wave must die. | Clear within three minutes. |
| `final_core` | The remaining 25% can be destroyed. | `victory`. |

Each uncleared wave, and the absolute 20-minute encounter deadline, causes `failed`: no loot is
released and every event-owned model, wave and HUD resource is cleaned up.

## Combat and rewards

Only direct player melee hits on the model hitbox count. The platform passes Bukkit's final
damage value into the platform-neutral `combat_damage` trigger; the YAML-configured cap and
per-player cooldown are then applied. Percentage gates clamp a hit at the boundary, so an
overpowered weapon cannot skip a wave.

Victory rolls the YAML loot pool once, persists the resulting slots, then releases tagged item
entities around the core. The items are public immediately. Their tags and snapshot prevent a
recovery replay from rerolling or spawning a second copy; normal Minecraft item despawn remains
five minutes.

## Operator configuration

The definition exposes only bounded scalar parameters to command/GUI overrides: maximum HP,
damage cap, hit cooldown, timings, zone radius and deadline. Waves, messages, models, animations
and loot remain YAML-owned. A Meteor reserves a non-overlapping 160-block zone by default and
counts toward the maximum of three large encounters per world.
