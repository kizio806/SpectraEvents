## Summary

Describe the problem and the implemented change.

## Architecture

- [ ] Module placement and dependency direction were reviewed (`platform -> application -> core`).
- [ ] Core, application, and adapters remain free of Bukkit/Paper/Spigot/NMS types.
- [ ] Platform parity between Paper and Spigot was verified when applicable.

## Verification

- [ ] Targeted tests pass.
- [ ] `./gradlew clean check build` passes.
- [ ] `./scripts/verification/validate-structure.sh` and `./scripts/verification/validate-source-hygiene.sh` pass.
- [ ] Exactly two production JARs are produced (Paper and Spigot).

## Documentation & Hygiene

- [ ] `CHANGELOG.md` updated for user-visible changes.
- [ ] Documentation under `docs/` updated when warranted.
- [ ] Zero `@SuppressWarnings`, `TODO`, `FIXME`, or wildcard imports.
