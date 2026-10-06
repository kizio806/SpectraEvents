# Event Definition Format (Schema v1)

Every event configuration file defines an immutable `EventDefinition` after parse, validate, and
compile.

## Supported root fields

| Field | Required | Description |
| :--- | :--- | :--- |
| `schema-version` | yes | Must be `1` |
| `id` | yes | Stable event identifier |
| `initial-phase` | yes | Phase key where instances start |
| `phases` | yes | Map of phase id → phase body |

Additional root fields such as `display` or `spawn` are rejected by the v1 parser with a precise
unknown-field diagnostic. They may be added only by a future schema version with an explicit
migration rule.

## Canonical authoring rules

Use the kebab-case names shown in this document. The v1 parser also accepts `onEnter`,
`on_enter`, `target-phase`, `targetPhase`, and `target_phase` for compatibility, but new files
should use `on-enter` and `target`.

Action, trigger, and condition parameters are written directly next to `type`:

```yaml
actions:
  - type: play_animation
    model: meteor_core
    animation: fall
```

The nested `parameters:` map is accepted for compatibility, but direct parameters are the stable
v1 style. Unknown structural fields are errors; action-specific values are validated when the
action is executed.

## Minimal example

```yaml
schema-version: 1
id: example
initial-phase: waiting
phases:
  waiting:
    transitions:
      - trigger:
          type: manual
        target: active
  active:
    transitions:
      - trigger:
          type: manual
        target: completed
  completed:
```

## Runtime behavior

The engine never reads raw YAML during gameplay. Files are parsed at startup (or on
`/spectraevents definition validate`), compiled into immutable objects, and registered by `id`.
Running instances capture the compiled definition snapshot at start. Reloading a file changes only
future instances; an already running instance continues with its original phases, transitions,
conditions, and actions. When an instance ends, its snapshot is released.

## Complete v1 contract

| Area | v1 contract |
| :--- | :--- |
| Model | `spawn_model`, `move_model`, `remove_model`; Blockbench `.bbmodel` and `.spectra.zip` sources are loaded from `plugins/SpectraEvents/assets/source/`. |
| Animations | Named Blockbench animations are started with `play_animation` after `spawn_model`; playback is stopped before cleanup. |
| Phases | `phases` is a map. A phase may contain `on-enter` and `transitions`. |
| Triggers | A transition has `trigger.type`; supported runtime triggers include `manual`, `timer_elapsed`, `interaction`, `health_depleted`, `health_threshold_crossed`, `hits_reached`, and `entity_death`. |
| Conditions | A transition may contain a list of conditions. Core v1 evaluates `is_locked` and `not_locked`; integrations may register additional resolvers. |
| Actions | A transition may contain `actions`; a phase may contain `on-enter` actions. See [Actions](actions.md) for the Paper/Spigot catalog. |
| Rewards | Rewards are ordinary actions such as `give_item` and `drop_loot`; reward claims are guarded by the event runtime. |
| Recovery | Event phase/state and timers are persisted by the repository. Platform resources are reconciled on restart; animation definitions declare `recovery` policy. |
| Cleanup | Terminal, failed, cancelled, and restarted instances invoke platform cleanup. Authors should include `remove_model`, `remove_bossbar`, and `remove_scoreboard` when those resources are part of the event. |

There are intentionally no root-level `recovery` or `cleanup` maps in schema v1. Those behaviors
are engine lifecycle guarantees, while per-animation recovery belongs in model animation YAML.
Future schema versions may add explicit policies only with a migration rule.

## Author workflow

1. Copy one of the verified examples: `meteor`, `airdrop`, `metin`, `pinata`, or `boss-portal`.
2. Change the event `id`, model IDs, messages, and gameplay parameters.
3. Run `/spectraevents definition validate` and fix every diagnostic using its `file:path` location.
4. Run `/spectraevents definition reload` to register the valid version.
5. Start it with `/spectraevents event start <id>`.
6. Inspect it with `/spectraevents event inspect <instance>` or `/spectraevents doctor`.
7. Cancel or complete the event and confirm that resources are cleaned up.
