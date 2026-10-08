# Phases (Schema v1)

Phases describe local lifecycle state for an `EventInstance`. Only one phase is active at a time.

## Structure

```yaml
phases:
  waiting:
    on-enter:
      - type: broadcast_message
        message: "Event is waiting."
    transitions:
      - trigger:
          type: manual
        target: active
  active: {}
```

## Supported phase fields

| Field | Type | Description |
| :--- | :--- | :--- |
| `on-enter` | list | Actions executed when entering the phase |
| `transitions` | list | Outgoing transition rules for this phase |

Fields such as `components` and `on-exit` from design drafts are not parsed in v1.
`onEnter` and `on_enter` are accepted compatibility aliases, but `on-enter` is canonical.

## Phase map keys

Phase ids are the YAML keys under `phases`. They must match `initial-phase` and all transition
`target` values referenced from other phases.
