# SpectraEvents Release Checklist

Use this checklist prior to creating a new official release tag and GitHub Release.

## Pre-Release Verification

- [ ] **Working Tree**: `git status` shows a clean working tree.
- [ ] **Version Single Source of Truth**: `gradle.properties` updated with target release version (e.g. `0.1.0-beta.1`).
- [ ] **Changelog**: `CHANGELOG.md` updated with release highlights under `## [X.Y.Z] - YYYY-MM-DD`.
- [ ] **Quality Gate**: `./gradlew clean check build` succeeds with zero errors; do not rewrite the
  worktree merely to make a check pass.
- [ ] **Artifact Verification**:
  - Both JARs exist under `distributions/paper/build/libs/` and `distributions/spigot/build/libs/`.
  - Filenames are `SpectraEvents-<version>-paper.jar` and `SpectraEvents-<version>-spigot.jar`.
  - Run `jar tf` on both artifacts: each has the intended `plugin.yml`, main FQCN, SQLite driver,
    and no classes from the other platform family.
- [ ] **Checksums**: Generate and verify both checksums:
  `sha256sum SpectraEvents-*.jar > SHA256SUMS.txt && sha256sum -c SHA256SUMS.txt`.
- [ ] **Publication eligibility**: `python3 scripts/release/verify_release_gate.py` passes. A newly
  declared server row is a release blocker until it has a passing real-server workflow. Use
  `--allow-blocked` only for a non-release CI visibility check.
- [ ] **Real-server matrix**: Run `scripts/runtime-smoke/runtime_workflow.py` for every available
  Paper, Purpur, Folia, Spigot, and CraftBukkit row. The workflow validates the Blockbench asset
  import, Meteor, Airdrop, Metin, Piñata, Boss Portal, cleanup, and restart recovery.
- [ ] **Upgrade and rollback**: Follow `docs/operations/release-and-recovery.md` on a copy of a real
  plugin data directory, including SQLite backup, restore, `/spectraevents doctor`, and a disposable event.
- [ ] **External effects**: Review each accepted `try_claim` near a crash and record a manual
  reconciliation decision before compensating a player.

## Tagging & Release Execution (Manual Step - Not Automated)

- [ ] Commit version bump & changelog: `git commit -m "release: vX.Y.Z"`
- [ ] Tag release: `git tag vX.Y.Z`
- [ ] Push tag to GitHub: `git push origin vX.Y.Z`
- [ ] Create GitHub Release with attached distribution JAR and `.sha256` checksum file.
