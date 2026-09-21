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

**Status: complete.** The schema-v1 contract, source-aware diagnostics, three reference examples,
authoring guide, `play_animation` workflow, and running-instance snapshots are complete.

## M2 — Blockbench and resource-pack pipeline

**Goal:** `Blockbench → import → validated asset → resource-pack ZIP → event` works with real files and a real client.

- [x] Define and test the signed Generic Model bundle, safe importer, bounded hostile-input handling,
  deterministic ZIP, model/animation registration, and opt-in HTTPS/SHA-1 delivery contract.
- [ ] Test custom item predicates, model assembly, pivots, hierarchy, and animation in a real client.
- [ ] Test player delivery, rejection, reconnect, and failed download with a real player.
- [x] Keep Modrinth publishing disabled until the local ZIP and delivery workflow is verified.

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

Run the two real-client M2 acceptance workflows and record the server/client versions and outcomes in
the master plan. Do not mark M2 complete from JVM tests alone.
