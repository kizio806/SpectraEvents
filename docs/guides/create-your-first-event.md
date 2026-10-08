# Create Your First Event

This guide walks through the complete author loop: copy a verified event, change its model and
behavior, validate it, reload it, start it, and diagnose the running instance.

## Where definitions live

On first startup, SpectraEvents creates:

```text
plugins/SpectraEvents/events/          # active definitions; initially empty
plugins/SpectraEvents/events/presets/  # shipped reference definitions
```

No definition is activated automatically. This makes a new server's first live event an explicit
administrator decision.

## Copy a verified event

Start with one of the shipped v1 examples:

| Example | Demonstrates |
| :--- | :--- |
| `meteor.yml` | model spawn, named falling animation, timer phases, health, mobs, loot, cleanup |
| `airdrop.yml` | timed unlock, interaction claim, rewards, bossbar/scoreboard, cleanup |
| `metin.yml` | health damage, threshold transitions, mob waves, boss phase, cleanup |

Copy the file from `plugins/SpectraEvents/events/presets/` to `plugins/SpectraEvents/events/` under a
new filename and change its `id`.
Create or edit the referenced Blockbench source under `plugins/SpectraEvents/assets/source/` so the
model ID and named animations match.

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
- The `manual` trigger is advanced with `/spectraevents event trigger <instance> manual`.

## Operator commands

| Command | Purpose |
| :--- | :--- |
| `/spectraevents definition list` | Lists loaded definition IDs |
| `/spectraevents definition validate` | Parses files without changing running definitions and prints diagnostics |
| `/spectraevents definition reload` | Atomically registers valid files; invalid files keep their previous version |
| `/spectraevents event start <id>` | Starts a new instance from a loaded definition |
| `/spectraevents event inspect <instance>` | Shows state, persisted runtime state, tasks, and resources |
| `/spectraevents doctor` | Shows diagnostics and integration status |

Use `/spectraevents event trigger <instance> manual` to advance a matching manual transition.

## Smoke test workflow

1. Start the Paper server with SpectraEvents enabled.
2. Copy `meteor.yml`, `airdrop.yml`, or `metin.yml` to a new file and change its `id`.
3. Run `/spectraevents definition validate`; fix every `file:path` diagnostic before continuing.
4. Run `/spectraevents definition reload` and verify the result reports the new definition as loaded.
5. Run `/spectraevents event start <id>` and copy the returned instance UUID.
6. Run `/spectraevents event inspect <instance>` and verify the expected initial phase, tasks, and resources.
7. Trigger the next phase using the Paper or Spigot command above, or wait for its timer.
8. Complete/cancel the event and inspect it again to confirm cleanup.

## Config-Driven Meteor Event Example

SpectraEvents ships the Meteor reference configuration at `events/presets/meteor.yml`. Copy it to
`events/meteor.yml` before loading it:

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
        model: meteor_core
        height-offset: 20
      - type: play_animation
        model: meteor_core
        animation: fall
        loop: ONCE
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

1. Run `/spectraevents definition validate` to ensure definitions compile.
2. Run `/spectraevents event start meteor`.
3. Watch the automated falling -> impact -> locked -> active lifecycle.
4. Interact (right-click) with the active meteor to deal damage. When health reaches 0, the meteor completes cleanly.
