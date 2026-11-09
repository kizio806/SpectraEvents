# SpectraEvents — AI Agent Guidelines & Architecture Manual

This document defines the mandatory operating standards, behavioral guidelines, and development lifecycle for AI agents and human engineers working on the **SpectraEvents** codebase.

Future AI agents working in this repository must operate as **experienced senior developers and principal software architects**: questioning assumptions, verifying facts against test execution, avoiding shortcuts, and upholding strict technical excellence.

---

## 1. Core Engineering Philosophy & Behavioral Rules

### Mindset & Responsibilities
- **Question Existing Decisions:** Never assume existing code or structure is correct merely because it is already committed. In this repository, historical implementations duplicated ~1,600 lines between Paper and Spigot action adapters and left unneeded `@SuppressWarnings` annotations (now resolved and permanently blocked by `validateStructure` and `validateSourceHygiene`). Always verify what the build system compiles and tests.
- **Search for Better Solutions:** Investigate clean abstractions rather than blindly repeating existing patterns. For example, platform-independent logic belongs exclusively in `core` and `application`, while `:platforms:bukkit` provides shared presentation and adapter primitives across Paper and Spigot.
- **Prefer Maintainable & Simple Solutions:** Do not introduce global singletons, service locators, NMS, reflection hacks, or giant manager classes when clean, testable hexagonal architectures suffice.
- **Explain Trade-offs Explicitly:** When proposing changes, outline the pros, cons, and performance implications (e.g., pure Java in `core` and `application` targeting Java 21 baseline, with platform adapters targeting Java 25 on Paper/Spigot).
- **Avoid Shortcuts:** Never bypass `./gradlew clean check build`, never disable quality gates, never suppress compiler warnings or SpotBugs checks, and never leave uncommitted scratch files.
- **Mandatory 100% Green Verification Before Commit & Push:** Never commit changes, push commits, or request PR merges without ensuring 100% green verification. Every quality gate — local unit & integration tests, Checkstyle, SpotBugs, PMD, JaCoCo instruction coverage (≥85% domain modules, ≥80% patch coverage on Codecov), release dry-runs, GitHub Actions CI workflows, CodeQL analysis (0 open alerts), and SonarCloud Quality Gate (Rating A, 0 issues) — must pass cleanly without a single failure or warning.

### Critical "Do NOT" Rules
- **Do NOT blindly copy current architecture:** Maintain the strict dependency direction: `platform -> application -> core`.
- **Do NOT leak Minecraft APIs into domain modules:** `spectraevents-core` and `spectraevents-application` must never import Bukkit, Paper, Spigot, or NMS packages.
- **Do NOT add unnecessary complexity:** SpectraEvents is a data-driven engine, not a collection of hardcoded event scripts. Prefer reusable components, actions, triggers, conditions, and phases over bespoke event-specific code.
- **Do NOT assume one global Minecraft main thread:** Preserve future region scheduling compatibility (Folia compatibility invariants).
- **Do NOT suppress warnings:** `@SuppressWarnings` and warning-silencing annotations of any kind are strictly forbidden across all modules. Resolve deprecations, type mismatches, and linter warnings at the source.
- **Do NOT add trivial or redundant comments:** Meaningful explanatory comments are permitted when clarifying non-obvious algorithms or contracts, but redundant comments that merely restate code, `TODO`/`FIXME`/`XXX` tags, and wildcard imports are prohibited.

---

## 2. Before Coding: Discovery & Planning

Before touching any code, an AI agent must complete four distinct analysis steps:

### Step 1: Understand the Architecture
1. **Module Separation:**
   - `:spectraevents-core`: Pure Java 21 library. Platform-neutral and Minecraft-free. Contains core domain models (`EventDefinition`, `PhaseDefinition`, `ActionDefinition`, `ConditionDefinition`, `TriggerDefinition`), 3D math (`Quaternion`, `EulerRotation`, `ModelTransform`, `Vector3`), and aggregate records (`RewardClaim`, `HitCounter`).
   - `:spectraevents-application`: Application orchestration and use cases (Java 21). Contains lifecycle management (`EventLifecycleManager`, `EventExecutionEngine`, `EventRuntimeState`), model/animation compilation and runtime services, schedule loaders, and update checking.
   - `:adapters:*`: Platform-neutral port adapters (`storage-sqlite`, `update-http`, `assets-blockbench`, `assets-delivery`).
   - `:platforms:bukkit`: Shared Bukkit/Spigot adapter layer (Java 25). Provides shared presentation logic (`AbstractBukkitActionAdapter`), PDC ownership keys (`BukkitPdcKeys`), shared container listeners (`BukkitSharedLootListener`), and custom item providers.
   - `:platforms:paper:common`: Paper-specific adapters (Java 25). Provides Folia region scheduler integration, MiniMessage templating, modern BossBar/Scoreboard management, and Brigadier command trees.
   - `:platforms:spigot:common`: Spigot/CraftBukkit adapters (Java 25). Provides legacy particle/sound dispatching, BungeeCord chat components, and modular subcommands.
   - `:distributions:*`: Shaded release packaging for Paper and Spigot server distributions.
2. **Execution Flow:**
   - Events are loaded from YAML definitions (`plugins/SpectraEvents/events/*.yml`) and compiled into immutable `EventDefinition` objects.
   - Execution proceeds through discrete phases governed by transition rules, evaluated periodically or upon interaction triggers.
   - 3D models are rendered via native Display and Interaction entities, tracked by UUID in runtime state, with transforms interpolated along keyframed timelines.
   - Database mutations (state snapshots, participant awards) execute through a dedicated single-writer thread pool in SQLite WAL mode.

### Step 2: Inspect Existing Patterns & Caveats
- **Single-Writer SQLite Persistence:** All database write operations must execute asynchronously via `SingleWriterPersistenceExecutor`. Never perform blocking database writes on server tick threads.
- **Durable Mailbox Claims:** Reward claims follow strict three-state lifecycle transitions: `PENDING -> DELIVERING -> DELIVERED`. Claims must survive unexpected server terminations without loss or duplication.
- **Folia Support:** Folia 26.1–26.3 is supported via `RegionTaskScheduler` with region-bound task delegation.

### Step 3: Check Documentation & Repository Guidelines
- Read `docs/architecture.md`, `docs/development.md`, and `docs/releases.md`.
- Read wiki articles in `docs/wiki/`.
- Review `.agents/rules/` for detailed coding rules.

### Step 4: Create a Written Execution Plan
- Specify exact files to create or modify.
- Outline how backward compatibility with existing event YAML definitions will be maintained.
- Identify the verification commands that will validate the changes.

---

## 3. During Coding: Standards & Conventions

### 1. Source Hygiene (Hard Gate)
- **Comment Hygiene:** Meaningful, high-value comments explaining complex algorithms or non-obvious contracts are permitted. Redundant, trivial comments that merely narrate self-explanatory code are strictly discouraged.
- **Zero Markers:** No `TODO`, `FIXME`, or `XXX` tags in Java files.
- **Zero Wildcard Imports:** `import java.util.*;` is strictly prohibited. Use explicit single-class imports.
- **Zero Warning Suppressions:** `@SuppressWarnings` is strictly forbidden. Resolve warnings and deprecations using modern non-deprecated APIs. Enforced by `validateSourceHygiene`.

### 2. Code Style & Clean Architecture
- **Formatting:** 2 spaces for Java indentation (Google Java Style via Spotless), UTF-8 encoding, Unix line endings (`\n`), no trailing whitespace.
- **Compiler Flags:** Compiles with `-Xlint:all` and `-Werror`. Do not introduce compiler warnings.
- **Boolean Predicates:** Name boolean methods and variables descriptively: `isEnabled`, `isShuttingDown`, `hasPendingClaims`.
- **Modularity:**
  - Never import Bukkit or Paper classes in `core`, `application`, or `adapters/*`.
  - Always keep platform adapters in `:platforms:*`.
- **Coverage Threshold:** Maintain at least 85% instruction coverage on `core` and `application` modules. Enforced by `jacocoTestCoverageVerification`.

### 3. Change Safety & Packaging Contract
- **Golden Packaging Invariant:** Every release builds **EXACTLY TWO production JARs** built via `./gradlew build`:
  1. **EXACTLY ONE Paper JAR** (`SpectraEvents-<version>-paper.jar`)
  2. **EXACTLY ONE Spigot JAR** (`SpectraEvents-<version>-spigot.jar`)
- **Platform Publication Parity:** Modrinth and release assets always publish matching Paper and Spigot distributions.

---

## 4. Before Finishing: Verification & Review

Before concluding any work, an AI agent must run the complete verification gauntlet:

```bash
# 1. Format code according to Spotless
./gradlew spotlessApply

# 2. Run unit and integration tests across all modules
./gradlew test

# 3. Validate structural layout and source hygiene
./scripts/verification/validate-structure.sh
./scripts/verification/validate-source-hygiene.sh

# 4. Verify distribution artifact packaging
./gradlew build

# 5. Execute full release readiness dry-run
./scripts/release/release.sh --dry-run --allow-existing-tag

# 6. Execute full quality gate
./gradlew clean check build
```

### Self-Review Checklist
- [ ] Run `git status` to ensure no untracked scratch files or unintended changes exist.
- [ ] Run `git diff` to review every changed line against project standards.
- [ ] Ensure `scripts/verification/validate-source-hygiene.sh` passes cleanly (0 `@SuppressWarnings`, no `TODO`/`FIXME`).
- [ ] Ensure `scripts/verification/validate-structure.sh` passes cleanly (canonical docs, no orphaned sources).
- [ ] Execute `./gradlew clean check build` and confirm 100% GREEN (zero test failures, zero lint warnings, ≥85% coverage).
- [ ] For every pull request, watch `gh pr checks <PR> --watch` to verify that 100% of CI, CodeQL, SonarCloud, and Codecov checks turn GREEN before merging.
- [ ] Update `CHANGELOG.md` under the appropriate version heading if changes are user-visible.

---

## 5. Directory & Customization Architecture

Customizations and operational instructions are organized as follows:
- **`AGENTS.md` / `.agents/AGENTS.md`**: Main instruction and architecture guide (this document).
- **`.agents/rules/`**: Specialized rules detailing standards, DOs, DON'Ts, and verification procedures.
- **`.agents/skills/`**: Procedures for auditing, quality gate validation, asset pipelines, and platform adaptation.
- **`.agents/workflows/`**: Process lifecycles for New Features, Bug Fixes, and Releases.
