# Support Policy

Thank you for using **SpectraEvents**. This document provides guidelines on how to get assistance, report bugs, ask configuration questions, and engage with the maintainers.

---

## Getting Help

Before opening an issue, please consult the project documentation:
- [Installation Guide](docs/installation.md) — Server requirements, distribution JAR selection (Paper vs Spigot), and initial setup.
- [Compatibility Matrix](docs/compatibility-matrix.md) — Supported Minecraft versions (26.1–26.3), server platforms (Paper, Purpur, Folia, Spigot), and Java runtime requirements (Java 25).
- [Architecture Assessment](docs/architecture.md) — Hexagonal modularity, 3D entity engine, SQLite persistence, and Folia scheduler integration.
- [Troubleshooting & FAQ](docs/troubleshooting.md) — Resource-pack delivery troubleshooting, entity collision hitboxes, command diagnostics (`/spectraevents doctor`), and performance tuning.
- [Wiki Documentation](docs/wiki/Home.md) — Complete user manual, event authoring tutorials, preset templates, and command references.

---

## Communication Channels

### 1. GitHub Issues
GitHub Issues are reserved for:
- **Bug Reports:** Event phase transition failures, display entity desync, SQLite persistence exceptions, or command execution errors.
- **Feature Requests:** New condition types, reward actions, Blockbench animation improvements, or platform hooks.
- **Compatibility Reports:** Verifications or issues encountered on specific server builds or forks.

Please use the appropriate [Issue Template](.github/ISSUE_TEMPLATE) when submitting tickets.

### 2. Security Vulnerabilities
If you identify a security issue, item duplication exploit, or critical vulnerability, please review our [Security Policy](SECURITY.md) and submit a report privately.

### 3. Community Discussions
For event YAML showcases, custom Blockbench model shares, or general help, visit our [GitHub Discussions](https://github.com/kizio806/SpectraEvents/discussions).

---

## Supported Server Platforms

Active support and verification are guaranteed for:
- **Paper / Purpur 26.1–26.3:** Supported via `SpectraEvents-<version>-paper.jar` (Brigadier commands, asynchronous chunk loading, native MiniMessage).
- **Folia 26.1–26.3:** Supported via `SpectraEvents-<version>-paper.jar` (Regional multi-threaded scheduling via `RegionTaskScheduler`).
- **Spigot / CraftBukkit 26.1–26.3:** Supported via `SpectraEvents-<version>-spigot.jar` (Bukkit scheduler, standard commands).
