# Event Authoring Documentation

SpectraEvents allows creating complex, dynamic 3D events using pure YAML authoring without writing any Java code.

## Architecture

```text
YAML File (plugins/SpectraEvents/events/*.yml)
  ↓ EventSpecYamlParser
EventSpec (Authoring Model)
  ↓ EventDefinitionCompiler
EventDefinition (Validated Compiled Model)
  ↓ Registered in EventDefinitionRegistry
EventExecutionEngine (Generic State Machine Engine)
  ↓ Platform Action Adapters
Native Paper 3D Display Entities & Gameplay
```

## Creating Your First Event

Create a file named `my_event.yml` inside `plugins/SpectraEvents/events/`:

```yaml
id: my_event
schema-version: "1"
initial-phase: spawning

phases:
  spawning:
    on-enter:
      - type: broadcast_message
        message: "<gold>A special event has spawned!"
      - type: spawn_model
        model: dev_cube
    transitions:
      - trigger:
          type: timer_elapsed
          duration: 10s
        target: active

  active:
    transitions:
      - trigger:
          type: interaction
        actions:
          - type: try_claim
          - type: give_item
            material: minecraft:diamond
            amount: 5
          - type: broadcast_message
            message: "<green>Event claimed!"
        target: completed

  completed:
    on-enter:
      - type: remove_model
      - type: complete_event
```

Validate your event with `/spectra definition validate` and test it with `/spectra event start my_event`!
