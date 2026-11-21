# Versioning and Release Management Standard

## Purpose
This rule establishes the authoritative versioning architecture, multi-platform artifact packaging invariants, Git tagging rules, and platform publishing requirements for **SpectraEvents**. All AI agents and engineers working on this repository must comply with these standards.

---

## 1. Authoritative Versioning Policy
All project versions follow [Semantic Versioning 2.0.0](https://semver.org/) with Minecraft compatibility build metadata:

```text
MAJOR.MINOR.PATCH+mc<MINECRAFT_RANGE>
```

Example for active line:
```text
1.0.0+mc26.x
```

### Version Semantics
- **MAJOR (`X.0.0`)**: Breaking changes affecting server operators, event definition YAML schemas, public APIs, or database storage layouts.
- **MINOR (`x.Y.0`)**: Backward-compatible new functionality, event actions, conditions, triggers, templates, or integrations.
- **PATCH (`x.y.Z`)**: Backward-compatible bug fixes, performance optimizations, and security patches.
- **Pre-releases**: Pre-release identifiers attach before metadata (e.g. `1.0.0-rc.1+mc26.x`).

*Note:* Minecraft version target changes do **NOT** modify `MAJOR.MINOR.PATCH`. Compatibility is expressed exclusively in `+mc<RANGE>`.

---

## 2. Global Base Version Synchronization Rule
**All active support branches must share the exact same base project version (`MAJOR.MINOR.PATCH`).**

- Never increment base version independently on a single branch.
- Upgrades are applied across all active support branches in lockstep.

---

## 3. Production Artifact Packaging Standard
Every release produces **EXACTLY TWO distribution JARs**:
1. **Paper/Purpur/Folia**: `spectra-events-<BASE_VERSION>-<MINECRAFT_RANGE>-paper-purpur-folia.jar`
2. **Spigot/Bukkit**: `spectra-events-<BASE_VERSION>-<MINECRAFT_RANGE>-spigot-bukkit.jar`

Examples:
- `spectra-events-1.0.0-mc26.x-paper-purpur-folia.jar`
- `spectra-events-1.0.0-mc26.x-spigot-bukkit.jar`

---

## 4. Git Tags & Release Conventions
- **Tag Format**: `v<BASE_VERSION>+mc<MINECRAFT_RANGE>` (e.g. `v1.0.0+mc26.x`).
- **Release Title**: `v<BASE_VERSION> — Minecraft <MINECRAFT_RANGE>` (e.g. `v1.0.0 — Minecraft 26.x`).
- **Release Trigger**: Annotated Git tags matching `v*+mc*` or manual `workflow_dispatch`.

---

## 5. Platform Publishing Conventions (Modrinth & CurseForge)
- **Paper Version ID**: `<BASE_VERSION>+mc<MINECRAFT_RANGE>.paper` (e.g. `1.0.0+mc26.x.paper`)
- **Spigot Version ID**: `<BASE_VERSION>+mc<MINECRAFT_RANGE>.spigot` (e.g. `1.0.0+mc26.x.spigot`)
- Never bundle both platforms into a single hybrid JAR.
- Never publish Folia as a separate version entry (it is natively supported by the Paper distribution).

---

## 6. Pre-Release Verification Command
Before tagging or pushing releases, run:
```bash
./scripts/release/release.sh --dry-run
```
All pre-release gates, source hygiene, test suites, and distribution packaging must pass with zero errors.
