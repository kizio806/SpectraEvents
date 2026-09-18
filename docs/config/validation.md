# Validation (Schema v1)

Configuration errors are collected as structured diagnostics during parse and compile. They never
surface as raw stack traces to operators.

## Diagnostic shape

Each diagnostic contains:

- `severity` — `ERROR`, `WARNING`, or `INFO`
- `code` — stable identifier (for example `SE-YAML-003`)
- `path` — logical location in the file (for example `phases.waiting.transitions[0].trigger`)
- `message` — human-readable explanation

## Common YAML parser codes

| Code | Meaning |
| :--- | :--- |
| `SE-YAML-001` | Malformed YAML syntax |
| `SE-YAML-002` | Root value is not a map |
| `SE-YAML-003` | Missing `schema-version` |
| `SE-YAML-004` | Unsupported `schema-version` |
| `SE-YAML-005` | Phase value is not an object |
| `SE-YAML-006` | Transition missing `trigger` |
| `SE-YAML-007` | Trigger missing `type` |
| `SE-YAML-008` | Condition missing `type` |
| `SE-YAML-009` | Action missing `type` |

## Compiler codes

| Code | Meaning |
| :--- | :--- |
| `SE-DEF-001` | Blank event `id` |
| `SE-DEF-002` | Blank `initial-phase` |
| `SE-DEF-003` | `initial-phase` not found in `phases` |
| `SE-DEF-004` | No phases defined |
| `SE-DEF-005` | Transition target phase missing |
| `SE-DEF-006` | Transition rule missing trigger |
| `SE-DEF-007` | Transition `target` phase missing |

## Registry codes

| Code | Meaning |
| :--- | :--- |
| `SE-REG-001` | Duplicate event `id` across loaded files |

## Load isolation

`DefinitionLoader` skips files that fail parse, compile, or registration. Other files in the same
batch still load successfully.

Use `/event dev definition validate` on a running Paper server to reload and inspect diagnostics.
