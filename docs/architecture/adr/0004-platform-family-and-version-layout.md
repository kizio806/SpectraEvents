# 0004. Platform Family and Version Layout

* Status: Accepted
* Date: 2026-09-17

## Context

SpectraEvents initially placed platform adapters under flat directory names (`platforms/paper-common`, `platforms/paper-26_2`). As multi-platform support (Paper, Purpur, Folia, Spigot/Bukkit, Sponge) was added to the roadmap, flat naming degraded architectural clarity, mixed version-agnostic code with version-specific bindings, and coupled infrastructure (storage/update checking) directly to Bukkit/Paper APIs.

## Decision

1. **Hierarchy over Flat Directories**: Standardize module layout to `platforms/<family>/<version-or-common>` (e.g. `platforms/paper/common`, `platforms/paper/v26_2`) and `distributions/<family>/<version>` (e.g. `distributions/paper/v26_2`).
2. **Infrastructure Decoupling**: Move non-Minecraft storage (`adapters/storage-sqlite`) and update checking (`adapters/update-http`) out of platform modules into generic, platform-neutral `adapters/` modules.
3. **Platform Family Taxonomy**: Treat Purpur and Folia as Paper platform family implementations using the Paper distribution rather than creating redundant adapter modules.
4. **Concrete Implementation Naming**: Prefix concrete platform classes with their family name (e.g. `PaperActionAdapter`, `PaperInteractionRouter`, `PaperBootstrap`).
5. **Composition Root Separation**: Each platform family maintains a single version-agnostic `Bootstrap` class (e.g. `PaperBootstrap`) so version-specific plugin entrypoints remain thin wrappers (~25 lines).

## Consequences

* Adding new platform families (Sponge, Bukkit) or Paper version bindings requires zero changes to core domain or application logic.
* Quality gate verification cleanly isolated per platform family and infrastructure adapter.
* Supersedes flat module layout decisions from ADR 0002.
