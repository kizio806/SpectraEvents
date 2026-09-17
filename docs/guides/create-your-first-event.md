# Create Your First Event

This guide walks through the minimal YAML-backed event definition shipped with SpectraEvents and
the developer commands used to load and run it.

## Where definitions live

On first startup, Paper creates:

```text
plugins/SpectraEvents/events/
```

If the directory is empty, the plugin writes `example.yml` automatically.

## Minimal working definition

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

Notes:

- `schema-version: 1` is required. Other values are rejected at load time.
- `initial-phase` must reference a key under `phases`.
- Each transition uses `trigger.type` and `target` (not the future design-doc aliases).
- The `manual` trigger is advanced with `/spectra dev event next <instance>`.

## Developer commands

Requires permission `spectra.dev`.

| Command | Purpose |
| :--- | :--- |
| `/spectra dev definition list` | Lists loaded definition IDs |
| `/spectra dev definition info <id>` | Shows source file, initial phase, and phase count |
| `/spectra dev definition validate` | Reloads `events/*.yml` and prints validation diagnostics |
| `/spectra dev definition start <id>` | Starts a new instance from a loaded definition |
| `/spectra dev event next <instance>` | Fires the first `manual` transition in the current phase |

## Smoke test workflow

1. Start the Paper server with SpectraEvents enabled.
2. Confirm `plugins/SpectraEvents/events/example.yml` exists.
3. Run `/spectra dev definition list` and verify `example` appears.
4. Run `/spectra dev definition start example`.
5. Copy the returned instance UUID.
6. Run `/spectra dev event info <instance>` and verify phase `waiting`.
7. Run `/spectra dev event next <instance>` and verify phase `active`.

## Config-Driven Meteor Event Example

SpectraEvents ships with a complete executable configuration for the Meteor event at `events/meteor.yml`:

```yaml
id: meteor
schema-version: "1"
initial-phase: falling

phases:
  falling:
    on-enter:
      - type: initialize_health
        max: 20
      - type: spawn_model
        model: dev_meteor_model
        height-offset: 20
        animate: true
    transitions:
      - trigger:
          type: timer_elapsed
          duration: 3s
        target: impact

  impact:
    on-enter:
      - type: move_model
        target: ground
      - type: play_sound
        sound: minecraft:entity.generic.explode
      - type: spawn_particles
        particle: minecraft:explosion
        count: 10
    transitions:
      - trigger:
          type: timer_elapsed
          duration: 1s
        target: locked

  locked:
    on-enter:
      - type: set_locked
        duration: 10s
    transitions:
      - trigger:
          type: timer_elapsed
          duration: 10s
        target: active
      - trigger:
          type: interaction
        actions:
          - type: send_message
            message: "<red>Meteor is locked."

  active:
    transitions:
      - trigger:
          type: interaction
        actions:
          - type: apply_damage
            amount: 1
      - trigger:
          type: health_depleted
        target: destroyed

  destroyed:
    on-enter:
      - type: remove_model
      - type: complete_event

  completed: {}
```

## Running the Meteor Event

1. Run `/spectra dev definition validate` to ensure definitions compile.
2. Run `/spectra dev definition start meteor` or `/spectra dev meteor start`.
3. Watch the automated falling -> impact -> locked -> active lifecycle.
4. Interact (right-click) with the active meteor to deal damage. When health reaches 0, the meteor completes cleanly.
