# Platform Versioning Strategy

This document outlines the versioning and compatibility strategy for SpectraEvents across different Minecraft server implementations.

## Terminology
- **Platform Family**: The overarching server software family (e.g., Paper, Spigot, Sponge).
- **Minecraft Version**: The underlying Mojang Minecraft release (e.g., 1.20.4, 1.21.3).
- **Adapter Version**: The SpectraEvents adapter module version (e.g., `v26_2`).
- **Distribution**: The final compiled JAR artifact intended for server administrators.

## Version Mapping

| Platform Family | Target Minecraft | API Version | Adapter Module | Support Level |
|-----------------|------------------|-------------|----------------|---------------|
| **Paper**       | 1.20.4 / 1.21.3  | 1.20.4-R0.1 | `v26_2`        | **FULL**      |
| **Purpur**      | 1.20.4 / 1.21.3  | 1.20.4-R0.1 | `v26_2` (via Paper) | **FULL** |
| **Folia**       | 1.20.4 / 1.21.3  | 1.20.4-R0.1 | `v26_2` (via Paper) | **FULL** |
| **Spigot**      | 1.21.3           | 1.21.3-R0.1 | `v26_2`        | **FULL**      |
| **Sponge**      | 1.20.4 / 1.21.x  | 12.0.0      | `v26_2`        | **FULL**      |

## Workflow for New Minecraft Versions
When a new major Minecraft version is released (e.g., 1.22 / Adapter `v27_1`):
1. **Analyze API Changes**: Review Bukkit/Paper/Sponge changelogs for breaking API changes, especially around Display Entities, persistence, and scheduling.
2. **Create New Adapter Module**: If breaking changes exist, create `platforms/paper/v27_1`, `platforms/spigot/v27_1`, etc. Do not mutate `v26_2` if it breaks backwards compatibility.
3. **Update Distributions**: Map the new adapter to the appropriate distribution module in `settings.gradle.kts`.
4. **Integration Testing**: Verify against real runtime servers using the scripts in `scripts/runtime-smoke/`.
