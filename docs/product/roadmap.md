# SpectraEvents Roadmap

This roadmap is aligned with [`PROJECT_MASTER_PLAN.md`](../../PROJECT_MASTER_PLAN.md), which is the detailed execution plan and status ledger. A feature is not considered complete merely because source code exists: the relevant tests, real-server workflow, documentation, and full quality gate must also pass.

## Completed foundation

- [x] Multi-module Gradle architecture with `platform → application → core` dependency direction.
- [x] Platform-neutral core and application modules targeting Java 21.
- [x] Paper-family and Spigot-family distributions targeting Java 25.
- [x] Event lifecycle, phases, triggers, conditions, actions, and immutable compiled definitions.
- [x] YAML-driven Meteor, Airdrop, and Metin vertical slices.
- [x] Native Display/Interaction model runtime with hierarchy, transforms, PDC ownership, rollback, and cleanup.
- [x] Timeline and animation runtime with keyframes, easing, playback control, cues, and Display interpolation.
- [x] SQLite persistence, single-writer execution, restart recovery, and claim acceptance semantics.
- [x] Optional integrations, operator diagnostics, command decomposition, and administrative GUI foundations.
- [x] Separate Paper and Spigot artifacts with explicit Paper/Purpur/Folia and Spigot/CraftBukkit scope.
- [x] Architecture, platform-boundary, static-analysis, formatting, and artifact verification gates.

## M0 — quality and change-set baseline

**Status: complete for all currently available runtime rows.**

- [x] Restore strict dependency verification with SHA-256 metadata.
- [x] Run `./gradlew clean check build` successfully on the current checkout.
- [x] Verify platform boundaries and both distribution artifacts.
- [x] Inventory the large pre-existing worktree and classify it into coherent packages.
- [x] Create eight safe local checkpoints for the mixed worktree without staging unrelated user changes or pushing them.
- [x] Re-run the available Paper, Purpur, Folia, Spigot, and CraftBukkit rows of the real-server matrix after the current change set was assigned to release checkpoints.

The current full build is green, but publication remains separate from compilation. The declared Folia 26.3 row is unavailable until an official upstream server build exists.

## M1 — authoring contract for custom events

**Goal:** an administrator can copy a reference definition, attach a model and animations, validate it, reload it, and start it without Java code.

- [ ] Stabilize the YAML schema for models, animations, phases, triggers, conditions, actions, rewards, recovery, and cleanup.
- [ ] Produce precise validation errors with file path and YAML field path.
- [ ] Document the workflow: copy → edit → validate → reload → start → inspect → diagnose.
- [ ] Guarantee that running instances keep their original definition snapshot after reload.
- [ ] Add complete custom-event examples for interaction, health, timers, rewards, and mob waves.

## M2 — Blockbench and resource-pack pipeline

**Goal:** `Blockbench → import → validated asset → resource-pack ZIP → event` works with real files and a real client.

- [ ] Define one supported Blockbench input format and asset directory contract.
- [ ] Import model hierarchy, pivots, textures, named animations, and supported render properties.
- [ ] Enforce hostile-input limits, safe paths, ZIP-slip protection, file-count limits, and size limits.
- [ ] Generate and inspect a real resource-pack ZIP, manifest, hashes, and item-model mappings.
- [ ] Test custom item predicates and model assembly in a real client.
- [ ] Test player delivery, rejection, reconnect, and network failure.
- [ ] Keep Modrinth publishing disabled until the local ZIP and delivery workflow is verified.

## M3 — reference events as product examples

**Goal:** built-in events are ordinary definitions using the same capabilities available to administrators.

- [ ] Bring Meteor, Airdrop, and Metin through the complete M1/M2 acceptance workflow.
- [ ] Add Pinata as a hit-counter/interactions reference.
- [ ] Add one event that exercises mob waves or a boss portal.
- [ ] Keep event-specific Java coordinators out of the design; missing behavior must first be evaluated as a shared primitive.
- [ ] Give every official event assets, YAML, documentation, tests, cleanup, and recovery verification.

## M4 — production compatibility and release

**Goal:** server operators know exactly which artifact and version combination is supported.

- [x] Complete the real-server matrix for every declared Paper, Purpur, Folia, Spigot, and CraftBukkit row that is available: 14/14 PASS. Folia 26.3 remains unavailable upstream.
- [ ] Keep publication blocked for required rows that have not passed.
- [ ] Verify upgrade, restart, recovery, cleanup, checksums, manifests, and release notes.
- [ ] Document SQLite backup/recovery and manual reconciliation of accepted-but-undelivered external effects.

## M5 — extensibility and GUI

**Goal:** developers can extend the engine without coupling addons to unstable internals.

- [ ] Establish a real addon use case before creating a public API.
- [ ] Version and test custom `Action`, `Trigger`, `Condition`, and integration extension points.
- [ ] Publish compatibility and deprecation rules.
- [ ] Build the inventory GUI editor only as a frontend for the canonical YAML source of truth.

## Deliberate non-goals

SpectraEvents will not become a custom mob engine, region-protection replacement, economy system, quest engine, or unrestricted scripting runtime. Sponge is not supported. NMS, global singleton architecture, speculative dependencies, and silent update/restart behavior remain out of scope.

## Current next action

Start M1 by stabilizing the author-facing event definition contract; do not begin another large feature before the contract and verification evidence are synchronized.
