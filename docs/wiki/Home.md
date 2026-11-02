# SpectraEvents Wiki

SpectraEvents is a data-driven 3D event engine for Minecraft servers. Event behavior is defined in
YAML from reusable phases, triggers, conditions, actions, assets, persistence, and reward
primitives. Official events use the same engine contract as administrator-authored events.

> **Current status:** `0.1.1-beta.2` is experimental and not release-ready. The repository release
> gate is intentionally blocked while confirmed reward, loot, asset, installation, runtime, and
> real-client acceptance items remain open. Do not deploy this beta on an economy-sensitive
> production server without reviewing the known limitations.

## Start here

- [[Installation]] — prerequisites, artifact choice, and first boot.
- [[Compatibility]] — exact server and Java support.
- [[Commands-and-Permissions]] — operator and player command surface.
- [[Official-Events]] — honest readiness state for the five event bundles.
- [[Event-Authoring]] — YAML workflow and the supported schema-v1 model.
- [[Assets-and-Resource-Packs]] — Blockbench pipeline and delivery boundary.
- [[Operations-and-Recovery]] — backups, upgrades, reward reconciliation, and rollback.
- [[Known-Limitations]] — blockers that must not be mistaken for finished behavior.

Source, issues, license, and release artifacts live in the
[GitHub repository](https://github.com/kizio806/SpectraEvents). The Wiki is user-facing; architecture,
audit evidence, contributor rules, and release gates remain versioned with the source code.
