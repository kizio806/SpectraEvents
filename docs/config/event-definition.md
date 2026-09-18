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

Additional root fields such as `display` or `spawn` may appear in design examples but are ignored
by the v1 parser until future schema versions add support.

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
`/event dev definition validate`), compiled into immutable objects, and registered by `id`.
Running instances reference the compiled definition snapshot.
