# Phases (Schema v1)

Phases describe local lifecycle state for an `EventInstance`. Only one phase is active at a time.

## Structure

```yaml
phases:
  waiting:
    onEnter:
      - type: broadcast
        parameters:
          message: "Event is waiting."
    transitions:
      - trigger:
          type: manual
        target: active
  active:
```

## Supported phase fields

| Field | Type | Description |
| :--- | :--- | :--- |
| `onEnter` | list | Actions executed when entering the phase (compiled but not executed yet) |
| `transitions` | list | Outgoing transition rules for this phase |

Fields such as `components`, `on-exit`, and `on-enter` from design drafts are not parsed in v1.
Use `onEnter` (camelCase) in authoring files.

## Phase map keys

Phase ids are the YAML keys under `phases`. They must match `initial-phase` and all transition
`target` values referenced from other phases.
