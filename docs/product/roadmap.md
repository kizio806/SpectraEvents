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

The current full build is green, but publication remains separate from compilation. The Folia
compatibility band deliberately ends at Minecraft 26.2; a Folia 26.3 startup is refused until its
upstream server exists and passes the real-server workflow.

## M1 — authoring contract for custom events

**Goal:** an administrator can copy a reference definition, attach a model and animations, validate it, reload it, and start it without Java code.

**Status: complete.** The schema-v1 contract, source-aware diagnostics, three reference examples,
authoring guide, `play_animation` workflow, and running-instance snapshots are complete.

## M2 — Blockbench and resource-pack pipeline

**Goal:** `Blockbench → import → validated asset → resource-pack ZIP → event` works with real files and a real client.

**Status: complete.** The implementation and its real-client/delivery acceptance were confirmed by
the operator; exact client and server versions were not recorded.

- [x] Define and test the signed Generic Model bundle, safe importer, bounded hostile-input handling,
  deterministic ZIP, model/animation registration, and opt-in HTTPS/SHA-1 delivery contract.
- [x] Operator-confirm the generated pack's custom item mapping, model assembly, pivots, hierarchy,
  and animation in a real client.
- [x] Operator-confirm real-player delivery after M2 implementation.
- [x] Keep Modrinth publishing disabled until the local ZIP and delivery workflow is verified.

## M3 — reference events as product examples

**Goal:** built-in events are ordinary definitions using the same capabilities available to administrators.

**Status: complete.** M2 acceptance was confirmed by the operator; M3 added the shared durable hit
counter, Piñata, and Boss Portal without adding any event-specific coordinator.

- [x] Bring Meteor, Airdrop, and Metin through the complete M1/M2 acceptance workflow.
- [x] Add Piñata as a hit-counter/interactions reference.
- [x] Add Boss Portal as a timed model, boss-spawn, and entity-death reference.
- [x] Keep event-specific Java coordinators out of the design; the only missing behavior was first
  added as the shared `HitCounter` primitive.
- [x] Give every official event assets, YAML, documentation, focused tests, cleanup, and shared
  recovery verification.

## M4 — production compatibility and release

**Goal:** server operators know exactly which artifact and version combination is supported.

**Status: complete.** The published matrix is Paper/Purpur and Spigot/CraftBukkit for Minecraft
26.1–26.3, plus Folia for 26.1–26.2. All 14 declared real-server rows passed; representative Paper
26.1/26.3, Folia 26.2, and Spigot 26.3 were rerun after the M4 workflow changes. Folia 26.3 is
fail-closed at plugin startup until an upstream server exists and the row passes. Release eligibility,
both artifacts/checksums, recovery procedures, `/spectraevents doctor`/inspect guidance, and manual reward
reconciliation are documented and executable.

## M5 — extensibility and GUI

**Goal:** developers can extend the engine without coupling addons to unstable internals.

- [ ] Establish a real addon use case before creating a public API.
- [ ] Version and test custom `Action`, `Trigger`, `Condition`, and integration extension points.
- [ ] Publish compatibility and deprecation rules.
- [ ] Build the inventory GUI editor only as a frontend for the canonical YAML source of truth.

## Deliberate non-goals

SpectraEvents will not become a custom mob engine, region-protection replacement, economy system, quest engine, or unrestricted scripting runtime. Sponge is not supported. NMS, global singleton architecture, speculative dependencies, and silent update/restart behavior remain out of scope.

## Current next action

Begin M5 only after a concrete external-addon use case exists. Do not expose a public API merely to
make an extension point available.
