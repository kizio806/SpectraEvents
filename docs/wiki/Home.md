# SpectraEvents Wiki

Welcome to the official documentation for **SpectraEvents** — a high-performance, data-driven **3D event engine** for modern Minecraft servers.

Rather than hardcoding rigid mini-games into monolithic plugins, SpectraEvents empowers server operators and creators to author dynamic, cinematic, multiplayer world encounters entirely through declarative YAML definitions, smooth Blockbench 3D display entities, and deterministic phase state machines.

**Zero client-side mods required!** All 3D models and animations run using native vanilla Display and Interaction entities (`item_display`, `block_display`, `interaction`).

---

## 🌟 Current Version: 1.0.0 (Production Stable)

SpectraEvents 1.0.0 is the first official stable release, targeting **Minecraft 26.1–26.3** on Java 25.

- **Universal Engine:** Identical declarative YAML syntax runs on Paper, Purpur, Folia, and Spigot.
- **Folia Native:** Full multi-threaded regional scheduling via `RegionTaskScheduler`.
- **Durable Persistence:** Offline reward claims and crash-proof state snapshots backed by SQLite WAL mode.
- **100% Quality Qualification:** Complete test coverage ($\ge 85\%$), 0 open CodeQL alerts, SonarCloud Rating A.

---

## 🧭 Navigation Hub

- [[Installation]] — Prerequisites, artifact selection, and quick-start guide.
- [[Compatibility]] — Supported server software, version matrix, and Java 25 runtime.
- [[Commands-and-Permissions]] — Operator tools, diagnostics, and player claim commands.
- [[Official-Events]] — Guide to the five bundled production presets (Airdrop, Meteor, Metin, Piñata, Boss Portal).
- [[Event-Authoring]] — Declarative YAML workflow, schema-v1 specification, phases, triggers, conditions, and actions.
- [[Assets-and-Resource-Packs]] — Blockbench 3D model pipeline, `.spectra.zip` bundles, and Modrinth CDN delivery.
- [[Operations-and-Recovery]] — Database backups, crash recovery, mailbox reconciliation, and maintenance.
- [[Known-Limitations]] — Architecture invariants, Folia region boundaries, and operational guidelines.

---

## 🔗 External Links

- [GitHub Repository](https://github.com/kizio806/SpectraEvents)
- [Issue Tracker](https://github.com/kizio806/SpectraEvents/issues)
- [Modrinth Plugin](https://modrinth.com/plugin/spectraevents)
- [Official Resource Pack (SpectraEvents Assets)](https://modrinth.com/resourcepack/spectraevents-assets)
- [License (MPL-2.0)](https://github.com/kizio806/SpectraEvents/blob/main/LICENSE)
