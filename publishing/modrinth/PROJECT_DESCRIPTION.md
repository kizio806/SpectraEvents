# SpectraEvents

SpectraEvents is a data-driven 3D event engine for Minecraft servers. Administrators compose YAML
definitions from reusable phases, triggers, conditions, actions, models, animations, persistence,
rewards, and platform integrations instead of installing a separate hardcoded plugin for every
event.

## Highlights

- YAML-defined event lifecycle with source-aware validation.
- Native model and animation pipeline built around validated Blockbench projects.
- SQLite checkpoints, restart recovery, cleanup, and explicit reward reconciliation.
- Separate Paper-family and Spigot-family artifacts.
- Paper, Purpur, Folia, Spigot, and CraftBukkit compatibility tracked by exact tested rows.
- Six locale catalogs and operator diagnostics.

## Current beta status

`0.1.1-beta.2` is experimental and **not production-ready**. The next release is blocked until the
repository closes the recorded reward, loot, official-bundle, asset, complete gameplay, and
real-client evidence requirements. Review the
[known limitations](https://github.com/kizio806/SpectraEvents/wiki/Known-Limitations) before testing.

The intended official set is Airdrop, Meteor, Metin, Piñata, and Boss Portal. They use the same
engine primitives available to custom definitions; they are not five hardcoded systems. Their
presence in source does not mean each event is release-ready.

## Compatibility

Current server artifacts require Java 25. The declared beta band is Paper/Purpur and
Spigot/CraftBukkit 26.1–26.3, plus Folia 26.1–26.2. Folia 26.3 is refused before initialization until
an exact runtime passes the complete compatibility suite.

## Links

- [Wiki](https://github.com/kizio806/SpectraEvents/wiki)
- [Source code](https://github.com/kizio806/SpectraEvents)
- [Issue tracker](https://github.com/kizio806/SpectraEvents/issues)
- [License: MPL-2.0](https://github.com/kizio806/SpectraEvents/blob/main/LICENSE)
