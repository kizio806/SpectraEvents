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
| **Folia** | `SpectraEvents-<version>-paper.jar` | **26.1 – 26.3** | Paper 26.2 | **Java 25** | **Supported** |

---

## 2. Java Runtime Requirements

- **Platform Server Runtime:** **Java 25** is strictly required to execute the plugin distributions on Paper and Spigot servers.
- **Platform-Neutral Baseline:** Core domain (`spectraevents-core`) and application orchestration (`spectraevents-application`) compile against **Java 21** bytecode level.

---

## 3. Folia Compatibility Note

Folia builds for Minecraft 26.1–26.3 are supported using `RegionTaskScheduler` for region-threaded execution. SpectraEvents automatically detects Folia at bootstrap and delegates all tick-bound operations through the region scheduler, avoiding global main-thread assumptions.
