# Feature Matrix

## Platform support contract

`Yes` is part of the supported operator contract for that distribution. Folia is supported only on
Minecraft 26.1–26.2; Folia 26.3 is refused before initialization. `Paper only` means that the
Spigot command reports the feature as unavailable; it is not silently emulated. Both artifacts load
the same active YAML definitions and bundled Blockbench assets.

| Capability | Paper / Purpur / Folia artifact | Spigot / CraftBukkit artifact |
| --- | :---: | :---: |
| Definitions, lifecycle, persistence and restart recovery | Yes | Yes |
| Blockbench asset import, models and animations | Yes | Yes |
| Local resource-pack build and configured player delivery | Yes | Yes |
| Event, definition, reward, schedule, status and doctor commands | Yes | Yes |
| Reward-delivery reconciliation commands | Yes | Yes |
| `event start … location …` and named locations | Yes | No; executor or spawn location |
| Asset import/build and model-inspection commands | Yes | No; startup imports assets |
| Inventory administrator GUI and saved GUI overrides | Yes | Paper only |
| Integration and update commands | Yes | Paper only |
| Brigadier suggestions and Adventure-rich messages | Yes | No; Bukkit command UX |

Paper, Purpur, Spigot and CraftBukkit support 26.1–26.3. Folia supports 26.1–26.2 only. New Folia
versions are declared only after their exact real-server workflow passes.

Folia does not implement Bukkit scoreboard creation. A scoreboard action is reported as unsupported
and the event continues without a sidebar; use bossbars for UI that must span Paper, Purpur and Folia.

## Shipped reference presets

Presets are extracted to `events/presets/` and are inactive until copied into `events/`. They are
working configurations that exercise reusable engine primitives; they do not create an event-specific
Java subsystem.

| Preset | Core behaviors illustrated |
| --- | --- |
| Meteor | model, falling animation, timer phases, health, loot and cleanup |
| Airdrop | timed unlock, interaction, rewards and UI |
| Metin | health damage, threshold transitions, mob waves and boss phase |
| Piñata | interaction-driven progress, rewards and presentation |
| Boss Portal | phased portal encounter, mobs, timers and cleanup |

The runtime also supports generic lifecycle, timer, interaction, model, sound, particles, reward,
area and UI actions as documented by the event-definition and authoring guides. A table entry is not a
promise that every preset uses every action.
