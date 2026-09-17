# Platform Versioning Strategy

This document outlines the versioning and compatibility strategy for SpectraEvents across different Minecraft server implementations.

## Strategy: Compatibility Band First

SpectraEvents favors a **Compatibility Band** approach over per-minor-version module duplication:
- **One JAR per Platform Family**: Serves all compatible Minecraft versions within a band without duplicate code or reflection hacks.
- **Version-Specific Binding**: Created ONLY when upstream APIs break in an incompatible way that cannot be abstracted cleanly.

## Terminology
- **Platform Family**: The overarching server software ecosystem (`paper`, `spigot`).
- **Compatibility Band**: Range of supported game versions served by a single artifact (e.g. `26.1 – 26.3`).
- **Compile Baseline**: The oldest API version against which the platform module is compiled (`26.1`).

## Version Mapping

| Platform Family | Target Artifact | Compatibility Band | Loaders | Baseline API | Strategy |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Paper Family** | `SpectraEvents-<ver>-paper.jar` | `26.1` – `26.3` | `paper`, `purpur`, `folia` | Paper `26.1` | Compatibility Band (Single JAR) |
| **Spigot Family** | `SpectraEvents-<ver>-spigot.jar` | `26.1` – `26.3` | `spigot`, `bukkit` | Spigot `26.1` | Compatibility Band (Single JAR) |

## Workflow for New Minecraft Versions (e.g. `26.4`)

When a new minor or patch version of Minecraft is released:
1. **DO NOTHING to Module Structure by Default**: Do NOT duplicate modules or create `v26_4`.
2. **Add Candidate Version**: Add `26.4` to candidate metadata in `gradle.properties` / `compatibility.versions.toml`.
3. **Build & Static Verification**: Compile against current baseline (`26.1`).
4. **Runtime Smoke Verification**: Boot Paper/Spigot runtime smoke tests on `26.4`.
5. **If Tests Pass**: Update supported metadata list. The SAME public JAR serves `26.4`.
6. **If API Incompatibility Discovered**:
   - Determine if a narrow adapter or capability query solves it.
   - Only if unavoidable due to breaking API changes, introduce a version-specific module (e.g. `paper/v26_4`).
