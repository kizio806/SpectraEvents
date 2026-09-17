# Triggers (Schema v1)

Triggers describe why a transition or action should be considered.

## Authoring shape

```yaml
trigger:
  type: manual
  parameters: {}
```

| Field | Required | Description |
| :--- | :--- | :--- |
| `type` | yes | Trigger identifier string |
| `parameters` | no | String-keyed map of trigger-specific values |

## Implemented trigger types

| Type | Meaning | Parameters |
| :--- | :--- | :--- |
| `manual` | Admin/command initiated | None |
| `timer_elapsed` | Fires when a phase timer completes | `duration` (e.g. `3s`, `10s`, `500ms`) |
| `interaction` | Player right-click on event model | None |
| `health_depleted` | Fired when event Health reaches 0 | None |

## Firing and Runtime Behavior

Triggers are evaluated deterministically against transition rules in the active phase definition.
When a phase with a `timer_elapsed` transition rule is entered, the runtime automatically schedules a task and evaluates the rule upon delay expiration.
`interaction` triggers are dispatched by platform interaction routers.
`health_depleted` is emitted automatically when an interaction damages event Health to 0.
