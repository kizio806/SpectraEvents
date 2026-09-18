# Transitions (Schema v1)

Transitions define how an instance may leave its current phase when a trigger fires.

## Structure

```yaml
transitions:
  - trigger:
      type: manual
    target: active
    conditions:
      - type: players-online
        parameters:
          min: 1
    actions:
      - type: broadcast
        parameters:
          message: "Moving to active."
```

## Supported fields

| Field | Required | Description |
| :--- | :--- | :--- |
| `trigger` | yes | Object with `type` and optional `parameters` |
| `target` | recommended | Destination phase id |
| `conditions` | no | List of condition objects |
| `actions` | no | List of action objects |

The parser reads `target` (not design-draft `to:`). Trigger type lives at `trigger.type` (not
`when.trigger`).

## Manual transitions in development

When `trigger.type` is `manual`, `/event dev event next <instance>` selects the first matching
rule in declaration order and moves the instance to `target`.

Conditions and actions are compiled but not evaluated or executed in the current milestone.

## Determinism

Rules in a phase are evaluated in YAML list order. The first matching manual transition wins when
using the developer `next` command.
