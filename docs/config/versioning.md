# Configuration Versioning

Every configuration file must declare its schema version.

```yaml
schema-version: 1
```

## Strategy
As SpectraEvents evolves, the structure of the YAML files may change. The `schema-version` allows the engine to:
1. **Identify** which parser to use.
2. **Migrate** old configurations in memory (or optionally on disk).
3. **Warn** administrators of deprecations.

## Backward Compatibility
V1 will exclusively support `schema-version: 1`. Future major engine updates (V2) may introduce `schema-version: 2`.
The engine will aim to support parsing older versions for at least one major release cycle.

## Unsupported Schemas
If a configuration specifies a version from the distant future (e.g., downgrading the plugin jar but keeping new configs) or the distant past (dropped support), the validation engine will throw an `ERROR` and refuse to load the definition.
