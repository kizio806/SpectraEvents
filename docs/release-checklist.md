# SpectraEvents Release Checklist

Use this checklist prior to creating a new official release tag and GitHub Release.

## Pre-Release Verification

- [ ] **Working Tree**: `git status` shows a clean working tree.
- [ ] **Version Single Source of Truth**: `gradle.properties` updated with target release version (e.g. `0.1.0-beta.1`).
- [ ] **Changelog**: `CHANGELOG.md` updated with release highlights under `## [X.Y.Z] - YYYY-MM-DD`.
- [ ] **Quality Gate**: `./gradlew spotlessApply && ./gradlew clean check build` succeeds with zero errors.
- [ ] **Artifact Verification**:
  - Distributable JAR generated under `distributions/paper/v26_2/build/libs/`.
  - Artifact filename formatted as `SpectraEvents-<version>-paper-26.2.jar`.
  - `jar tf <artifact>` verified: `plugin.yml` expanded, main FQCN updated, zero `dev/spectraevents` classes present.
- [ ] **SHA-256 Checksum**: Generated via `sha256sum SpectraEvents-<version>-paper-26.2.jar`.
- [ ] **Runtime Paper Smoke**: Tested on clean Paper 26.2 server instance (`Meteor`, `Airdrop`, `Metin` events start, run, and clean up).
- [ ] **Restart Recovery Smoke**: Verified active event state recovery and SQLite DB integrity across server restart.
- [ ] **Update Checker Verification**: Update checker endpoint confirms version comparison logic (SemVer prerelease).

## Tagging & Release Execution (Manual Step - Not Automated)

- [ ] Commit version bump & changelog: `git commit -m "release: vX.Y.Z"`
- [ ] Tag release: `git tag vX.Y.Z`
- [ ] Push tag to GitHub: `git push origin vX.Y.Z`
- [ ] Create GitHub Release with attached distribution JAR and `.sha256` checksum file.
