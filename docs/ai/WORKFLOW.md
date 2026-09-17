# SpectraEvents Engineering Workflow

For every substantial change:

```text
understand
↓
inspect
↓
plan
↓
implement
↓
test
↓
inspect diff
↓
run full quality gate
↓
update durable documentation when necessary
```

Before editing, read the relevant documentation, production code, and tests, then inspect `git
status`. After editing, run targeted tests, run architecture checks when boundaries changed, execute
`./gradlew clean check build`, inspect the final diff, and verify that unrelated files did not change.

Compilation alone is not completion. For a distribution change, inspect the final JAR and run a Paper
smoke test when the environment permits it.
