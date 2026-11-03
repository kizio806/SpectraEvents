# SpectraEvents — Compatibility Matrix

This document is the **canonical compatibility reference** for the SpectraEvents project across supported server software and Minecraft versions.

Last verified: **2026-11-03** | Verification toolchain: Gradle 9.8.2, OpenJDK 25.

---

## 1. Supported Server Distributions (Golden Packaging Invariant)

SpectraEvents produces **exactly two production JARs** built via `./gradlew build`:

| Distribution Family | Production Artifact | Target Server Platforms | Supported Minecraft Versions | Default Compile Target | Java Baseline | Status |
|---|---|---|---|---|---|---|
| **Paper Family** | `SpectraEvents-<version>-paper.jar` | **Paper**, **Purpur**, **Folia** | **26.1 – 26.3** | Paper 26.2 | **Java 25** | **Active (Primary)** |
| **Spigot Family** | `SpectraEvents-<version>-spigot.jar` | **Spigot**, **CraftBukkit** | **26.1 – 26.3** | Spigot 26.2 | **Java 25** | **Active** |

---

## 2. Complete 15-Combination Server Runtime Matrix & Verification Results

Every declared combination of server runtime and Minecraft version is compiled, probed, tested, and verified in automated CI and release qualification workflows (`scripts/runtime-smoke/runtime_workflow.py`):

| # | Server Platform | Platform Family | Minecraft Version | Production Artifact | Scheduling Model | Verification Status |
|---|---|---|---|---|---|---|
| 1 | **Paper** | Paper | **26.1** | `SpectraEvents-<version>-paper.jar` | Asynchronous task scheduler | **VERIFIED (PASS)** |
| 2 | **Paper** | Paper | **26.2** | `SpectraEvents-<version>-paper.jar` | Asynchronous task scheduler | **VERIFIED (PASS)** |
| 3 | **Paper** | Paper | **26.3** | `SpectraEvents-<version>-paper.jar` | Asynchronous task scheduler | **VERIFIED (PASS)** |
| 4 | **Purpur** | Paper | **26.1** | `SpectraEvents-<version>-paper.jar` | Asynchronous task scheduler | **VERIFIED (PASS)** |
| 5 | **Purpur** | Paper | **26.2** | `SpectraEvents-<version>-paper.jar` | Asynchronous task scheduler | **VERIFIED (PASS)** |
| 6 | **Purpur** | Paper | **26.3** | `SpectraEvents-<version>-paper.jar` | Asynchronous task scheduler | **VERIFIED (PASS)** |
| 7 | **Folia** | Paper | **26.1** | `SpectraEvents-<version>-paper.jar` | Regional multithreading (`RegionTaskScheduler`) | **VERIFIED (PASS)** |
| 8 | **Folia** | Paper | **26.2** | `SpectraEvents-<version>-paper.jar` | Regional multithreading (`RegionTaskScheduler`) | **VERIFIED (PASS)** |
| 9 | **Folia** | Paper | **26.3** | `SpectraEvents-<version>-paper.jar` | Regional multithreading (`RegionTaskScheduler`) | **VERIFIED (PASS)** |
| 10 | **Spigot** | Spigot | **26.1** | `SpectraEvents-<version>-spigot.jar` | Primary server thread scheduler | **VERIFIED (PASS)** |
| 11 | **Spigot** | Spigot | **26.2** | `SpectraEvents-<version>-spigot.jar` | Primary server thread scheduler | **VERIFIED (PASS)** |
| 12 | **Spigot** | Spigot | **26.3** | `SpectraEvents-<version>-spigot.jar` | Primary server thread scheduler | **VERIFIED (PASS)** |
| 13 | **CraftBukkit** | Spigot | **26.1** | `SpectraEvents-<version>-spigot.jar` | Primary server thread scheduler | **VERIFIED (PASS)** |
| 14 | **CraftBukkit** | Spigot | **26.2** | `SpectraEvents-<version>-spigot.jar` | Primary server thread scheduler | **VERIFIED (PASS)** |
| 15 | **CraftBukkit** | Spigot | **26.3** | `SpectraEvents-<version>-spigot.jar` | Primary server thread scheduler | **VERIFIED (PASS)** |

---

## 3. Java Runtime & Bytecode Requirements

- **Platform Server Runtime:** **Java 25** (`java -version`) is strictly required to execute the plugin distributions on Paper, Purpur, Folia, and Spigot servers.
- **Platform-Neutral Baseline:** Core domain (`spectraevents-core`) and application orchestration (`spectraevents-application`) compile against **Java 21** bytecode level with zero Minecraft API imports.

---

## 4. Folia Architecture & Regional Threading

Folia builds for Minecraft 26.1–26.3 are supported natively by the Paper distribution artifact (`SpectraEvents-<version>-paper.jar`):

- **Automatic Bootstrap Detection:** SpectraEvents automatically probes server brand information via `ServerBuildInfo` at enable time and activates `PaperRegionTaskScheduler`.
- **Region Scheduling:** All tick-bound operations (Display entity transforms, interaction hitbox registration, location-based zone triggers) execute through Folia's region scheduler, bound to the chunk coordinates of the active event.
- **Scoreboard Limitation Graceful Fallback:** Folia deliberately leaves standard Bukkit scoreboards unsupported across separate regions. When Folia is detected, SpectraEvents automatically falls back to BossBar HUD alerts for event phase and objective notifications.
- **Zero Global Thread Assumptions:** The engine contains no global singletons or assumptions of a single server main thread, guaranteeing safe execution in concurrent regional tick loops.
