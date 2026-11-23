## Description
<!-- Provide a concise summary of the changes and motivation for this pull request. -->

## Type of Change
- [ ] Bug fix (non-breaking change fixing an issue)
- [ ] New feature (non-breaking change adding functionality)
- [ ] Breaking change (fix or feature changing existing behavior)
- [ ] Documentation update
- [ ] Refactoring / tooling enhancement

## Architectural Compliance
- [ ] **Multi-Runtime Parity:** Changes tested or compatible across Paper, Folia, and Spigot distributions.
- [ ] **Asset Pipeline Integrity:** 3D model definitions, animations, and resource packs validate without schema errors.
- [ ] **Strict Typing:** No raw warnings or suppressions introduced.
- [ ] **Supply Chain Security:** No compiled `.jar` or binary artifacts committed.
- [ ] **Actions Pinning:** Any new workflow actions are pinned to full commit SHAs.

## Verification Checklist
- [ ] `./gradlew check` passes locally
- [ ] `./gradlew test` passes across runtime matrix
- [ ] All commits follow Conventional Commits standard
