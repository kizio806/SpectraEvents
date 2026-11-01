# SpectraEvents — Compatibility Matrix

This document is the **canonical compatibility reference** for the SpectraEvents project across supported server software and Minecraft versions.

Last verified: **2026-11-01** | Verification toolchain: Gradle 9.7.1, OpenJDK 25.

---

## 1. Supported Server Platforms & Distributions

| Platform Family | Production Artifact | Supported Minecraft Versions | Default Compile Target | Java Baseline | Status |
|---|---|---|---|---|---|
| **Paper** | `SpectraEvents-<version>-paper.jar` | **26.1 – 26.3** | Paper 26.2 | **Java 25** | **Active (Primary)** |
| **Purpur** | `SpectraEvents-<version>-paper.jar` | **26.1 – 26.3** | Paper 26.2 | **Java 25** | **Active** |
| **Spigot** | `SpectraEvents-<version>-spigot.jar` | **26.1 – 26.3** | Spigot 26.2 | **Java 25** | **Active** |
| **CraftBukkit** | `SpectraEvents-<version>-spigot.jar` | **26.1 – 26.3** | Spigot 26.2 | **Java 25** | **Active** |
| **Folia (26.1–26.2)** | `SpectraEvents-<version>-paper.jar` | **26.1 – 26.2** | Paper 26.2 | **Java 25** | **Supported** |
| **Folia (26.3)** | `SpectraEvents-<version>-paper.jar` | **26.3** | Paper 26.2 | **Java 25** | **Opt-Out (ADR 0009)** |

---

## 2. Java Runtime Requirements

- **Platform Server Runtime:** **Java 25** is strictly required to execute the plugin distributions on Paper and Spigot servers.
- **Platform-Neutral Baseline:** Core domain (`spectraevents-core`) and application orchestration (`spectraevents-application`) compile against **Java 21** bytecode level.

---

## 3. Folia Compatibility Note (ADR 0009)

In accordance with Architectural Decision Record 0009:
- Folia builds for Minecraft 26.1 and 26.2 are supported using `RegionTaskScheduler` for region-threaded execution.
- Folia 26.3 is explicitly refused before plugin startup at bootstrap. This prevents silent thread-safety violations until Folia 26.3 entity thread boundaries and region schedulers are fully certified.
