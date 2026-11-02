# Event Authoring

Definitions live in `plugins/SpectraEvents/events/*.yml` and declare `schema-version: "1"`.
SpectraEvents parses and validates YAML before compiling an immutable runtime definition. Invalid
files report source-aware diagnostics and must not partially replace the last valid definition.

## Minimal structure

```yaml
id: example
schema-version: "1"
initial-phase: announced

phases:
  announced:
    on-enter:
      - type: broadcast_message
        message: "<gold>An event is starting"
    transitions:
      - trigger:
          type: timer_elapsed
          duration: 30s
        target: active
  active:
    transitions:
      - trigger:
          type: manual
        target: finished
  finished:
    on-enter:
      - type: complete_event
```

Use `/spectraevents definition validate`, then `/spectraevents definition reload`, and inspect the
started instance. Do not assume a documented idea is implemented: the current source compiler and
validation output are authoritative for action, condition, trigger, component, and parameter names.

Reusable primitives belong in core/application. Event-specific Java coordinators, Bukkit types in
the neutral modules, reflection/NMS shortcuts, or successful no-op platform actions are not accepted
extension mechanisms.
