# SpectraEvents — confirmed remediation backlog

Snapshot: 2026-10-09, branch `fix/modrinth-artifact-metadata`, commit `828b034`.

This document is the canonical backlog of defects, product gaps, false claims, and missing release
evidence confirmed by the production-readiness audit. It records the state before remediation. A
green build alone does not close an item.

The owner-approved product contract is:

- ship a reusable engine and five professional built-in events using that same engine;
- treat Airdrop as the first vertical slice, but keep the full release blocked until Airdrop,
  Meteor, Metin, Piñata, and Boss Portal all satisfy their Definition of Done;
- pursue feature parity across Paper, Purpur, Spigot, and supported Folia versions, with no silent
  no-ops or unverified compatibility claims;
- route all personal and podium rewards through durable mailbox claims; public/shared loot may use
  a separate mechanism only if concurrency, disconnects, and full inventories are safe;
- replace prototype assets with professional Blockbench models, normally using optimized 512/1024
  textures and real-client acceptance evidence;
- finish the 26.1–26.3 line first while keeping the architecture ready for later Java 21 and 1.21.x
  distributions without creating speculative empty modules.

## Status and closure rules

Current release verdict: **BLOCKED**.

- `P0`: correctness or release-integrity blocker. Resolve before product expansion.
- `P1`: required for the five-event professional release.
- `P2`: maintainability, documentation, operator UX, or evidence debt.
- `P3`: approved future compatibility and expansion work.
- `CONFIRMED`: source or current runtime/build evidence demonstrates the issue.
- `MISSING EVIDENCE`: an implementation exists, but the required production claim is not proven.

An item may be marked complete only when its acceptance criteria and listed verification pass. Do
not close items by weakening a test, changing documentation to hide required behavior, or replacing
a professional asset with a placeholder.

## P0 — release blockers

### SE-P0-001 — Participant rewards can be delivered twice

- **State:** CONFIRMED.
- **Evidence:** `InternalActionExecutor.java:97-137` saves a claim as `PENDING`, calls
  `deliverRewardOrDrop`, and calls `markDeliveredAsync` without first reserving it through
  `beginDeliveryAsync`. `SQLiteEventInstanceRepository.java:911-946` permits `DELIVERED` only from
  `DELIVERING`.
- **Impact:** Piñata and Boss Portal may give an item immediately while leaving the same claim
  available for `/spectraevents rewards claim`.
- **Required fix:** use one durable reservation/delivery state machine for automatic and manual
  delivery; do not acknowledge a claim that was never reserved.
- **Acceptance:** regression test proves one logical delivery across immediate success, retry,
  disconnect, restart, concurrent claim attempts, and acknowledgement failure.

### SE-P0-002 — Participant reward failures are hidden

- **State:** CONFIRMED.
- **Evidence:** the automatic delivery chain ends with `.handle((ignored, failure) -> true)` in
  `InternalActionExecutor.java:134`.
- **Impact:** failed acknowledgement or delivery is reported to the event engine as success, hiding
  corrupt claim state and allowing the event to complete.
- **Required fix:** preserve an explicit retained/ambiguous/failed result and route fatal failures
  through the lifecycle/reconciliation policy.
- **Acceptance:** tests assert persisted status and operator-visible diagnostics for every failed
  stage; no exception or `false` acknowledgement becomes unconditional success.

### SE-P0-003 — Paper shared-container loot is lost on full inventory

- **State:** CONFIRMED.
- **Evidence:** `PaperSharedLootListener.java:45-62` durably removes a slot and then ignores the
  leftovers returned by `PlayerInventory.addItem`.
- **Impact:** Airdrop loot can disappear permanently.
- **Required fix:** define and implement an atomic result: successful insertion, safe world drop, or
  durable restoration of the slot.
- **Acceptance:** full-inventory and disconnect-race server tests prove no duplication and no loss.

### SE-P0-004 — Paper public ground loot is lost on full inventory

- **State:** CONFIRMED.
- **Evidence:** `PaperSharedLootListener.java:121-141` durably consumes the ground-loot slot, ignores
  inventory leftovers, and removes the ground entity.
- **Impact:** Meteor victory loot can disappear permanently.
- **Required fix:** apply the same safe delivery contract as SE-P0-003.
- **Acceptance:** pickup with zero free slots retains or safely drops every item and removes the
  persistent slot only after the chosen outcome is secured.

### SE-P0-005 — Spigot shared/public loot has the same loss paths

- **State:** CONFIRMED.
- **Evidence:** `SpigotSharedLootListener.java:45-61` and `:119-134` mirror the Paper logic and ignore
  inventory leftovers.
- **Impact:** platform parity reproduces the Airdrop and Meteor data-loss defect on Spigot and
  CraftBukkit.
- **Required fix:** implement a shared application-level outcome contract and thin platform-specific
  inventory/drop operations instead of two independent fixes.
- **Acceptance:** the same adapter contract suite passes for Paper and Spigot families.

### SE-P0-006 — Fresh installation does not provide usable assets for four presets

- **State:** CONFIRMED.
- **Evidence:** `FileSystemDefinitionLoader.java:54-78` extracts five YAML files only to inactive
  `events/presets`; `DataDirectoryLayout.java:142-151` does not create `assets/source`;
  `AssetPipelineService.java:56-60` silently returns when that directory is absent. Only
  `templates/metin.spectra.zip` is installable.
- **Impact:** copying Airdrop, Meteor, Piñata, or Boss Portal into `events/` yields a definition whose
  model is unavailable. The documented first-use journey is broken.
- **Required fix:** distribute each official event as an installable, validated bundle containing
  its definition and assets, or implement an equally explicit atomic installer.
- **Acceptance:** an empty server can install, validate, start, complete, restart, and remove every
  official event without copying files from the source repository.

### SE-P0-007 — Metin and Boss Portal render with transparent placeholder textures

- **State:** CONFIRMED.
- **Evidence:** decoded textures embedded in `metin_stone.bbmodel` and `boss_portal.bbmodel` are 1×1
  and fully transparent. Current structural tests only require a non-empty texture collection.
- **Impact:** the two advertised 3D events can compile successfully while being invisible to the
  player.
- **Required fix:** replace both assets with professional models/textures and add opacity/content
  validation without treating that validation as a substitute for client review.
- **Acceptance:** real-client evidence shows correct model, UV, hitbox, animations, pack reload, and
  cleanup on every supported platform family.

### SE-P0-008 — No official event has complete real-server gameplay evidence

- **State:** MISSING EVIDENCE.
- **Evidence:** `runtime_workflow.py:414-468` installs only Metin, starts it, restarts the server, and
  cancels it. It never reaches victory, failure, rewards, waves, interaction completion, or cleanup
  after natural termination; it never runs the other four events.
- **Impact:** the 14-row green matrix proves boot/recovery/cancel compatibility, not five working
  events.
- **Required fix:** add deterministic end-to-end workflows for success, failure, cancel, recovery,
  rewards, and cleanup for all five events and all declared platform/version rows.
- **Acceptance:** every required row publishes machine-readable evidence for every official event;
  any missing row blocks release.

### SE-P0-009 — Release blocker input contradicts the confirmed product state

- **State:** CONFIRMED.
- **Evidence:** `config/release/release-blockers.json` contains an empty `blockers` array despite the
  confirmed reward, installation, asset, and gameplay blockers above.
- **Impact:** `verify_release_gate.py` can authorize publication while known P0 defects remain.
- **Required fix:** extend the release gate from runtime-availability rows to tracked product
  blockers with stable IDs and evidence requirements.
- **Acceptance:** each open P0 makes the release workflow fail closed; removing an entry requires a
  passing closure check linked to the issue ID.

### SE-P0-010 — Real-client resource-pack acceptance is absent

- **State:** MISSING EVIDENCE.
- **Evidence:** `config/release/resource-pack-client-evidence.json` declares `delivery: disabled` and
  `packs: []`; the verifier accepts that state. Pack ZIP creation is tested, not Minecraft-client
  loading or visual correctness.
- **Impact:** invisible, rejected, oversized, or incorrectly versioned packs can be published by a
  green release workflow.
- **Required fix:** require client evidence for every pack profile used by the five-event release.
- **Acceptance:** tested client/version/profile/SHA-1 records show successful download, load,
  rendering, animation, reconnect, and rejection/failure UX.

### SE-P0-011 — Published beta is not aligned with the verified product state

- **State:** CONFIRMED release-management issue.
- **Evidence:** `v0.1.1-beta.2` is publicly published while SE-P0-001 through SE-P0-010 remain open.
- **Impact:** users can install a build that passes CI but can duplicate rewards, lose loot, or show
  invisible/missing models.
- **Required fix:** choose an explicit response before further promotion: prominently mark the beta
  as unsafe/experimental, publish a corrective hotfix after P0 closure, or withdraw affected files.
  No external mutation is authorized by this document.
- **Acceptance:** public metadata, release notes, known issues, and repository release status agree.

## P1 — product correctness and five-event quality

### SE-P1-001 — Multiple phase timers cannot be recovered correctly

- **Evidence:** `PhaseExecutionController.java:160-201` schedules every `timer_elapsed` rule but
  overwrites one `timerDeadlineMillis`; `:234-253` restores every timer using that single deadline.
- **Required fix:** reject more than one timer per phase at compile time or persist a keyed deadline
  per timer rule.
- **Acceptance:** restart tests cover two timers with different durations and deterministic winners.

### SE-P1-002 — Paper GUI starts events synchronously on a platform thread

- **Evidence:** `AdminGuiController.java:330-357` calls `engine.startEvent`; the contract in
  `EventLifecycleManager.java:59-62` requires adapters to use `startEventAsync` so SQLite flushes do
  not stall a server or region thread.
- **Required fix:** use the asynchronous start path and return the result on the correct scheduler.
- **Acceptance:** injected slow persistence does not block the player region/global server thread.

### SE-P1-003 — Required resource-pack configuration fails open

- **Evidence:** `PaperResourcePackDeliveryBootstrap.java:82-107` and the Spigot equivalent log
  resolution/configuration failures and return a completed future even when delivery is required.
- **Required fix:** distinguish optional fail-soft delivery from `required: true`, which must make
  event readiness unavailable or fail startup according to an explicit operator contract.
- **Acceptance:** invalid URL, SHA-1, Modrinth ID, network failure, decline, and failed download have
  tested deterministic outcomes on both platform families.

### SE-P1-004 — Missing asset source is treated as a successful no-op

- **Evidence:** `AssetPipelineService.java:56-60` returns normally when the source directory is
  absent.
- **Required fix:** return an explicit build result/capability state and make definitions referencing
  unavailable models fail readiness validation.
- **Acceptance:** `/doctor`, startup, template installation, and definition validation report the
  exact missing model/source and cannot call the event ready.

### SE-P1-005 — Startup performs blocking asset work

- **Evidence:** Paper calls `buildAssetsAsync().join()` in `PaperBootstrap.java:137-138`; Spigot
  calls synchronous `buildAssets()` in `SpigotBootstrap.java:103-104`.
- **Required fix:** establish a bounded startup/import lifecycle with progress/readiness state and no
  unbounded disk work on tick/region threads.
- **Acceptance:** measured startup with maximum supported bundle size stays within an approved budget
  and commands cannot start an event before its assets are ready.

### SE-P1-006 — Platform action defaults can hide missing implementations

- **Evidence:** `PlatformActionPort` provides successful/default behavior for operations that may be
  platform-significant; fake integration tests rely on recording/no-op ports.
- **Required fix:** make mandatory capabilities explicit and fail definition validation when an
  action is unsupported; reserve defaults for genuinely optional behavior.
- **Acceptance:** shared contract tests fail when a supported action is unimplemented or a no-op.

### SE-P1-007 — Spigot scoreboard update is a fake success

- **Evidence:** `SpigotActionAdapter.java:874-880` returns
  `CompletableFuture.completedFuture(true)` without updating a scoreboard.
- **Required fix:** implement equivalent behavior or reject the capability/definition explicitly.
- **Acceptance:** a platform contract test observes the updated lines; no unsupported path reports
  success.

### SE-P1-008 — Platform adapter parity is not tested

- **Evidence:** Paper contains a small test set; Spigot has no corresponding adapter behavior tests.
  The runtime smoke exercises only Metin start/restart/cancel.
- **Required fix:** define one capability/behavior contract suite reused by Paper and Spigot, plus
  Folia scheduling-specific tests.
- **Acceptance:** every feature promised as equal has the same black-box assertions on each family;
  documented exceptions are explicit and owner-approved.

### SE-P1-009 — Integration registry may report failed integrations as enabled

- **Evidence:** `PaperIntegrationManager.java:19-43` records plugin presence. ServiceLoader
  initialization in `PaperBootstrap.java:205-223` catches failure without updating that state.
- **Required fix:** model `DETECTED`, `INITIALIZED`, `FAILED`, and capability details separately.
- **Acceptance:** `/doctor` and GUI report the actual resolver/provider initialization result and a
  failing integration cannot be advertised as enabled.

### SE-P1-010 — Custom item integrations suppress actionable failures

- **Evidence:** Nexo, Oraxen, and ItemsAdder providers catch `NoClassDefFoundError | Exception` and
  return `null` without an operator diagnostic.
- **Required fix:** distinguish missing item, unavailable API, invalid namespace, and integration
  failure; rate-limit but preserve diagnostics.
- **Acceptance:** contract tests cover installed/missing/broken providers and the event receives an
  explicit result instead of silently losing a configured reward.

### SE-P1-011 — Podium rewards bypass the durable mailbox

- **Evidence:** `PaperActionAdapter.java:650-724` and the Spigot equivalent select online players and
  insert/drop rewards directly.
- **Impact:** offline winners are excluded; delivery is not recoverable and uses a different
  guarantee from personal claims.
- **Required fix:** create deterministic durable podium claims, preserving rank and rolled reward
  snapshot.
- **Acceptance:** offline, reconnect, restart, full inventory, and concurrent claim tests pass.

### SE-P1-012 — Reward documentation overstates a universal mailbox guarantee

- **Evidence:** `docs/product/commands.md:45-46` says rewards are durable mailbox claims, while podium
  and shared/public loot use different paths and automatic participant claims are broken.
- **Required fix:** document exact guarantees per reward type after the unified design is complete.
- **Acceptance:** documentation is generated or tested against the supported reward contracts.

### SE-P1-013 — Airdrop default timing is unsuitable as a ready-to-run experience

- **Evidence:** `airdrop.yml:43-69` defaults to 15 minutes announcement, 5 minutes lock, 15 minutes
  first-loot window, and 5 minutes empty display.
- **Required fix:** provide validated operational profiles such as demo/small-SMP/network and expose
  intended timing controls clearly.
- **Acceptance:** playtests confirm the selected defaults; docs state total expected duration.

### SE-P1-014 — Airdrop has no complete contention/recovery test

- **Evidence:** `AirdropClaimRaceTest` is application-level; no real-server test covers two players,
  a full inventory, disconnect, restart, first-loot transition, and empty cleanup together.
- **Required fix:** add deterministic server scenarios on every supported platform family.
- **Acceptance:** all loot conservation invariants and every Airdrop terminal path pass.

### SE-P1-015 — Meteor defaults are mathematically infeasible for a typical server

- **Evidence:** `meteor.yml:25-95` defaults to 1,000,000 HP, 50 maximum damage per player per second,
  20-minute deadline, 3-minute announcement, 45-second impact lock, and three guarded waves. Twenty
  perfect players need 1,000 seconds for damage alone, exceeding the remaining time before waves.
- **Required fix:** define population-aware profiles/scaling and validate total encounter budgets.
- **Acceptance:** deterministic simulations and real-server tests prove completion/failure balance
  for owner-approved small, medium, and large populations.

### SE-P1-016 — Meteor victory loot inherits the public-loot loss defect

- **Evidence:** `meteor.yml:359-391` initializes/releases ground loot through SE-P0-004/005.
- **Required fix:** close the shared/public loot contract before considering Meteor ready.
- **Acceptance:** no-loss/no-duplication tests pass for every declared platform.

### SE-P1-017 — Meteor has no full gameplay, wave, or failure verification

- **State:** MISSING EVIDENCE.
- **Required fix:** execute every health gate, owned-wave death, timeout, victory, reward, restart,
  and cleanup path on real servers.
- **Acceptance:** the release matrix carries a completed Meteor scenario, not merely boot evidence.

### SE-P1-018 — Metin has a placeholder visual and no operator parameters

- **Evidence:** SE-P0-007 and `metin.yml`, which declares no `parameters` section.
- **Impact:** health, damage, waves, contribution threshold, timings, and rewards require editing a
  large definition manually.
- **Required fix:** replace the asset and expose a bounded, useful configuration surface without
  hardcoding Metin behavior.
- **Acceptance:** basic setup is possible through documented YAML/GUI controls in minutes and remains
  the same generic engine contract available to custom events.

### SE-P1-019 — Metin interaction damage has no anti-spam policy

- **Evidence:** `metin.yml:76-87` applies 10 damage for every accepted interaction with no cooldown.
- **Required fix:** add a reusable configurable interaction cooldown/rate-limit primitive or use a
  generic damage trigger with equivalent safeguards.
- **Acceptance:** automated high-rate input cannot bypass the configured contribution/damage budget.

### SE-P1-020 — Metin podium excludes offline winners

- **Evidence:** `PaperActionAdapter.java:659-705` builds eligibility from currently online players
  inside the event zone and skips a winner if `Bukkit.getPlayer` returns null.
- **Required fix:** persist eligibility/contribution outcome and create mailbox claims independent of
  current connection state.
- **Acceptance:** the same winners and claims result whether a qualifying player stays online,
  disconnects before victory, or reconnects after restart.

### SE-P1-021 — Metin runtime smoke never proves victory or podium

- **State:** MISSING EVIDENCE.
- **Required fix:** extend the runtime scenario through all phases, waves, ranking, claims, and natural
  cleanup.
- **Acceptance:** real-server logs/state assert the complete path on all declared rows.

### SE-P1-022 — Piñata can be completed by interaction spam

- **Evidence:** `pinata.yml:85-144` increments the hit counter for every interaction with no global or
  per-player cooldown.
- **Required fix:** add reusable rate limiting and an owner-approved participation model.
- **Acceptance:** spam from one player cannot violate the intended duration/participation rules.

### SE-P1-023 — One Piñata click qualifies for the full participant reward

- **Evidence:** `InternalActionExecutor.java:161-165` adds the actor to a set on any increment;
  `pinata.yml:146-163` rewards every recorded participant without a minimum contribution.
- **Required fix:** support a configurable eligibility threshold or explicitly approve equal reward
  for one click and document the anti-abuse implications.
- **Acceptance:** eligibility tests cover one click, threshold contribution, disconnect, and repeat
  interaction.

### SE-P1-024 — Piñata frenzy is presentation-only

- **Evidence:** the frenzy phase changes particles, bossbar, and message but retains the same
  interaction/increment loop.
- **Required fix:** either give frenzy a real, tested mechanical distinction or describe it honestly
  as a presentation phase.
- **Acceptance:** YAML, documentation, and observed gameplay use the same definition.

### SE-P1-025 — Boss Portal enrage is presentation-only

- **Evidence:** `boss-portal.yml:128-146` changes particles, bossbar, and message but does not alter
  the boss, damage, AI, skills, or timing mechanics.
- **Required fix:** implement a reusable generic mechanic that produces a measurable enrage, or
  rename the phase to a final warning.
- **Acceptance:** a server test observes the declared gameplay change, and documentation states the
  exact effect.

### SE-P1-026 — Boss Portal exposes only one operator parameter

- **Evidence:** only `gathering-duration` is declared GUI-editable; boss type/health, waves, rewards,
  zone, and enrage are not basic controls.
- **Required fix:** define a bounded configuration surface suitable for server owners while keeping
  advanced YAML authoring available.
- **Acceptance:** an administrator can configure the approved gameplay profile without modifying
  Java or reverse-engineering the full phase graph.

### SE-P1-027 — Boss Portal has no real boss/wave/reward recovery evidence

- **State:** MISSING EVIDENCE.
- **Required fix:** test restart in gathering, both waves, boss, enrage, and victory; assert entity
  ownership, no duplicate spawns, durable claims, and cleanup.
- **Acceptance:** all supported rows pass the full scenario.

### SE-P1-028 — Oversized textures have no client-performance justification

- **Evidence:** Airdrop, Meteor, and Piñata each embed two 4096×4096 RGBA textures. Six decoded
  textures are roughly 384 MiB before mipmap overhead.
- **Required fix:** replace them with optimized 512/1024 assets unless a measured exception is
  approved.
- **Acceptance:** per-model client FPS, load time, memory/VRAM, and visual comparison evidence meet
  the approved budget.

### SE-P1-029 — Model tests accept transparent or pathological textures

- **Evidence:** `BundledReferenceAssetsTest.java:42-60` checks only that texture/node/animation
  collections are non-empty.
- **Required fix:** validate dimensions, decoded byte budget, opacity/content, UV bounds, model
  limits, referenced animations, and hitbox plausibility.
- **Acceptance:** the current placeholders fail the new tests, professional replacements pass, and
  real-client review remains mandatory.

### SE-P1-030 — Hitboxes, pivots, scale, and visibility are not client-verified

- **State:** MISSING EVIDENCE.
- **Required fix:** define a visual acceptance checklist and capture repeatable screenshots/video or
  an approved manual evidence record per model/profile.
- **Acceptance:** every event demonstrates correct interaction volume, world placement, render
  distance, animation pose reset, and cleanup.

### SE-P1-031 — No production performance envelope exists

- **Evidence:** no real-server measurements for MSPT, CPU, heap, entities, tasks, packets, SQLite
  latency, client FPS, or pack load time.
- **Required fix:** implement baseline, one-event, five-event, high-interaction, restart, and soak
  scenarios on documented hardware.
- **Acceptance:** owner-approved budgets are recorded and enforced or attached as release evidence.

### SE-P1-032 — The SQLite “benchmark” is only a correctness test

- **Evidence:** `SQLiteConcurrencyBenchmarkTest.java:48-103` runs 1,000 writes at several thread
  counts but asserts only that no write failed; it records no duration or percentiles.
- **Required fix:** rename it or add a separate reproducible benchmark for throughput and p50/p95/p99
  latency under representative state/reward/checkpoint workloads.
- **Acceptance:** release performance claims cite measured results rather than the unit test name.

### SE-P1-033 — Five-event Definition of Done is absent from executable release gates

- **State:** CONFIRMED process gap.
- **Required fix:** encode the owner-approved requirement that all five events, both artifact
  families, all supported versions, client profiles, locales, rewards, recovery, and cleanup must
  pass before full release.
- **Acceptance:** an omitted event/platform/profile makes `RELEASE READY` impossible.

## P2 — operator UX, documentation, localization, tests, and maintainability

### SE-P2-001 — README first-install instructions are false

- **Current status:** FIXED 2026-10-11 — README now labels the beta release-blocked and routes
  installation guidance to the versioned Wiki without claiming a usable clean-install bundle.
- **Evidence:** `README.md:35-48` says the plugin creates `assets/source` and implies copying a preset
  is sufficient; SE-P0-006 proves otherwise.
- **Fix/acceptance:** rewrite after the bundle installer is real and validate the documented journey
  on an empty server.

### SE-P2-002 — Project state falsely marks all five events complete

- **Current status:** FIXED 2026-10-11 — project state is now a concise blocker snapshot.
- **Evidence:** `docs/ai/PROJECT_STATE.md:113-114` claims all five definitions have bossbars,
  scoreboards, titles, HUD, thresholds, waves, loot, cleanup, and timeouts. The YAML files do not all
  contain those behaviors and none is product-ready.
- **Fix/acceptance:** replace with a concise truthful milestone snapshot linked to executable
  evidence.

### SE-P2-003 — Boss Portal documentation describes a different contract

- **Current status:** FIXED 2026-10-11 — the stale per-event duplicate was removed; the Wiki states
  that Boss Portal is incomplete and does not promise the old contract.
- **Evidence:** `docs/events/boss-portal.md:44-58` promises a scoreboard and old reward/phase behavior
  absent from current YAML.
- **Fix/acceptance:** regenerate/rewrite from the accepted gameplay and test every command/example.

### SE-P2-004 — Piñata documentation describes stale actions and rewards

- **Current status:** FIXED 2026-10-11 — the stale duplicate was removed and Wiki readiness is
  aligned with the current hit-counter/participant-claim implementation.
- **Evidence:** `docs/events/pinata.md:45-68` promises scoreboard and `drop_loot`, while current YAML
  uses bossbar plus participant claims.
- **Fix/acceptance:** align with the approved eligibility/reward design and real runtime evidence.

### SE-P2-005 — Scope claims missing leaderboard, spawn, and area behavior

- **Current status:** FIXED 2026-10-11 — the speculative scope, component, spawn, and leaderboard
  pages were removed; the zone page now documents only implemented reservation behavior.
- **Evidence:** `docs/product/scope.md:16-18` claims generic leaderboards, random-surface spawn, and
  area entry/exit enforcement. Source contains contribution ranking/zone reservation, not those full
  product flows.
- **Fix/acceptance:** either implement and test the approved features or move them to a clearly
  labelled roadmap.

### SE-P2-006 — Area documentation invents a movement event system

- **Current status:** FIXED 2026-10-11 — `docs/config/areas.md` explicitly excludes player tracking
  and protection rules from schema v1.
- **Evidence:** `docs/config/areas.md` claims asynchronous movement monitoring and
  `player-enter-area`/`player-leave-area` triggers; no corresponding production router exists.
- **Fix/acceptance:** remove the false contract or implement the generic, scheduler-safe feature with
  tests on Paper, Spigot, and Folia.

### SE-P2-007 — Reward documentation mixes proposals with implemented behavior

- **Current status:** FIXED 2026-10-11 — the page now maps only compiler/runtime actions and states
  the external-delivery ambiguity.
- **Evidence:** `docs/config/rewards.md` presents weighted tables, top-N, last-hit, personal loot,
  currency, and other modes without consistently marking absent behavior as proposed.
- **Fix/acceptance:** publish only implemented contracts; isolate future designs in roadmap/ADR form.

### SE-P2-008 — Platform capability documentation falsely claims Spigot admin GUI parity

- **Current status:** FIXED 2026-10-11 — the matrix now marks Spigot GUI and custom-item providers
  absent and keeps runtime certification separate from source implementation.
- **Evidence:** `docs/architecture/platform-capabilities.md:13` marks Spigot admin GUI `DIRECT`, while
  the Spigot artifact has no GUI and `docs/product/feature-matrix.md:19` calls it Paper-only.
- **Fix/acceptance:** implement owner-required equivalent UX or record the temporary deviation as an
  explicit release blocker.

### SE-P2-009 — Feature matrix overstates bundled-asset usability

- **Current status:** FIXED 2026-10-11 — the duplicate matrix was removed; README and Wiki describe
  official definitions as release-blocked development presets.
- **Evidence:** `docs/product/feature-matrix.md:8,31-44` says both artifacts load the same bundled
  assets and calls presets working configurations; fresh-install extraction disproves usability.
- **Fix/acceptance:** align after SE-P0-006 and five-event runtime verification.

### SE-P2-010 — Vault documentation looks like a shipped event specification

- **Current status:** FIXED 2026-10-11 — the orphaned guide was removed and Wiki explicitly says
  Vault is outside the five-event release.
- **Evidence:** `docs/events/vault.md` exists without YAML, model, runtime, or tests.
- **Fix/acceptance:** mark it explicitly `ROADMAP / NOT IMPLEMENTED`; Vault remains outside the
  approved five-event scope until a separate decision.

### SE-P2-011 — Product roadmap contains a broken link

- **Current status:** FIXED 2026-10-11 — the duplicate roadmap was removed; `MASTER_PLAN.md` is the
  sole execution ledger and `docs/README.md` records that ownership.
- **Evidence:** `docs/product/roadmap.md:3` points to missing `../../PROJECT_MASTER_PLAN.md`.
- **Fix/acceptance:** replace it with the approved canonical `MASTER_PLAN.md` location and run a
  repository Markdown link checker in CI.

### SE-P2-012 — Default schedule example uses an invalid Meteor parameter

- **Evidence:** `DataDirectoryLayout.java:47-58` shows `health: 250`; Meteor declares
  `maximum-health`.
- **Fix/acceptance:** use a real parameter and add a test that parses/compiles every shipped config
  example.

### SE-P2-013 — Event-schema migration is missing

- **Evidence:** `EventSpecYamlParser` accepts only schema version `1`; no migration path or upgrade
  diagnostics exist for future schema changes.
- **Fix/acceptance:** define a versioning/migration contract before schema 2, including backups,
  dry-run diagnostics, and tests preserving operator data.

### SE-P2-014 — GUI success text bypasses localization

- **Evidence:** `AdminGuiController.java:358-360` sends hardcoded English `Started ...`.
- **Fix/acceptance:** route every player/operator-visible GUI message through the locale catalog and
  enforce it with source/static tests where practical.

### SE-P2-015 — Template commands bypass localization

- **Evidence:** `TemplateCommandHandler.java:56-105` contains hardcoded English list/install/error
  messages; Spigot mirrors this behavior.
- **Fix/acceptance:** add keys to all six locales and test placeholders/fallback.

### SE-P2-016 — Podium fallback text bypasses localization

- **Evidence:** `PaperActionAdapter.java:729-731` returns `no eligible finalists`; Spigot has the same
  hardcoded label.
- **Fix/acceptance:** locale key in all languages with a test through the actual podium result path.

### SE-P2-017 — Locale structure is complete but language quality is unverified

- **Evidence:** all six catalogs contain 352 keys, but 26–35 values per non-English catalog are
  identical to English; no human review record exists.
- **Fix/acceptance:** native/human review of player and operator paths, placeholder validation,
  terminology glossary, and a CI key/placeholder parity check.

### SE-P2-018 — Runtime “integration” tests overstate platform evidence

- **Evidence:** `ReferenceEventsConfigDrivenIntegrationTest.java:262-299` uses an empty scheduler and
  recording action port and asserts requested actions, not Bukkit/Paper outcomes.
- **Fix/acceptance:** rename appropriately and add actual server tests for effects that depend on the
  platform implementation.

### SE-P2-019 — Runtime smoke contains dead helpers

- **Evidence:** `start_and_cancel_reference_event` and `write_asset_smoke_fixture` are defined in
  `runtime_workflow.py` but never called.
- **Fix/acceptance:** incorporate them into deliberate scenarios or delete them after coverage is
  replaced; dead code checks should prevent recurrence.

### SE-P2-020 — Router delegate extension points are unused

- **Evidence:** `PaperInteractionRouter.registerDelegate` and `PaperEntityDeathRouter.registerDelegate`
  have no production registrations.
- **Fix/acceptance:** remove the unused event-specific extension mechanism if the generic engine is
  canonical, or document/test a demonstrated use case without hardcoding official events.

### SE-P2-021 — Paper and Spigot action adapters are duplicated and drifting

- **Evidence:** both adapters exceed 1,000 lines and already differ in scoreboard, GUI, location,
  scheduling, and reward behavior.
- **Fix/acceptance:** extract only demonstrated platform-neutral orchestration/result contracts;
  retain Minecraft API operations in adapters; shared contract tests must prevent drift.

### SE-P2-022 — Release runtime gate checks availability, not product readiness

- **Evidence:** `verify_release_gate.py` consumes only aggregate runtime-matrix status and a manually
  maintained blocker list; it does not know event/scenario/client evidence.
- **Fix/acceptance:** consume machine-readable per-event/per-platform evidence and open blocker IDs.

### SE-P2-023 — Client-evidence verifier treats disabled delivery as sufficient

- **Evidence:** `verify_resource_pack_client_evidence.py:18-21` returns success for `delivery:
  disabled`, even though release publishes three resource packs.
- **Fix/acceptance:** distinguish development/pre-release mode from the five-event full-release gate,
  where verified evidence is mandatory.

### SE-P2-024 — Published documentation and marketplace metadata are inconsistent

- **Evidence:** README, release notes, repository docs, GitHub Release, and Modrinth descriptions do
  not consistently distinguish implemented engine primitives, experimental presets, and verified
  ready events.
- **Fix/acceptance:** one release metadata source produces truthful platform/version/features/known
  limitations across publication targets, followed by a post-publication verification job.

### SE-P2-025 — Configuration surface is technically valid but product-incomplete

- **Evidence:** Airdrop exposes 4 GUI parameters, Meteor 9, Piñata 3, Boss Portal 1, and Metin 0;
  critical rewards, scaling, cooldowns, and gameplay values require manual YAML edits.
- **Fix/acceptance:** approve a small/basic parameter contract per event and validate that an
  administrator can configure each event without reading Java.

### SE-P2-026 — GUI and command start paths use different execution behavior

- **Evidence:** command start compiles overrides and calls `startEventAsync`; GUI compiles overrides
  and calls synchronous `startEvent`.
- **Fix/acceptance:** one application use case drives both frontends, with identical validation,
  persistence, errors, and scheduling.

### SE-P2-027 — Resource-pack and model readiness are not connected to event readiness

- **Evidence:** definitions can load while their models are absent; delivery may be disabled or
  unavailable while events remain startable.
- **Fix/acceptance:** define capability/readiness checks at install, validate, doctor, schedule, GUI,
  and command start boundaries.

### SE-P2-028 — Event-zone documentation implies enforcement not present in runtime

- **Evidence:** runtime reserves overlapping zones and uses zones for some eligibility checks, but it
  does not generally enforce entry, exit, PvP, block, or participation rules.
- **Fix/acceptance:** name the current primitive accurately and separately implement only approved
  enforcement policies with WorldGuard/Folia-safe tests.

### SE-P2-029 — PlaceholderAPI surface is narrower than integration messaging suggests

- **Evidence:** `PlaceholderAPIIntegration` implements only `spectra_active_count`.
- **Fix/acceptance:** document that exact surface or add a versioned, tested placeholder contract
  driven by real administrator use cases.

### SE-P2-030 — No automated documentation truth/link/example gate exists

- **Evidence:** a broken link and multiple stale command/feature examples survive the full Gradle
  gate.
- **Fix/acceptance:** CI validates internal links, commands/permissions, locale keys, YAML snippets,
  version strings, and declared feature status against machine-readable sources where possible.

## P3 — approved compatibility and future expansion

### SE-P3-001 — Compatibility metadata covers only the 26.x Java 25 line

- **Evidence:** `gradle/compatibility.versions.toml` and `AssetTargetProfile` enumerate only 26.1,
  26.2, and 26.3.
- **Required work:** centralize platform/version/Java/resource-pack/artifact declarations so a future
  line can be added predictably without editing unrelated classes.
- **Acceptance:** a documented dependency-impact exercise demonstrates where a representative Java
  21/1.21.x line would plug in, without publishing or claiming that support.

### SE-P3-002 — No Java 21 platform/distribution toolchain exists

- **Evidence:** current platform/distribution and workflows target Java 25; only neutral core and
  application are constrained to Java 21-compatible boundaries.
- **Required work:** design separate Java-level build lines sharing core/application and isolating API
  differences; do not create empty version modules.
- **Acceptance:** Gradle architecture and naming convention can compile a representative adapter
  fixture with Java 21 while the 26.x line remains Java 25.

### SE-P3-003 — Legacy resource-pack strategy is undefined

- **Evidence:** current profiles emit only 26.1/26.2/26.3 formats and modern item-definition output.
- **Required work:** map 1.21.x pack/item-model format boundaries and define versioned builder
  strategies rather than assuming one ZIP works across the family.
- **Acceptance:** the compatibility design names exact candidate versions, pack formats, artifact
  naming, and required client tests without claiming support prematurely.

### SE-P3-004 — Platform/version test matrix is not generated from one source of truth

- **Evidence:** compatibility data appears in TOML, plugin descriptors, Gradle, workflows, docs,
  release notes, and Modrinth metadata.
- **Required work:** generate or validate all consumers from one compatibility model.
- **Acceptance:** an inconsistent version/loader/Java/profile declaration fails CI before release.

### SE-P3-005 — Folia 26.3 remains unsupported and technically unverified

- **Evidence:** startup intentionally refuses Folia 26.3 and no runtime row exists.
- **Required work:** retain the safe refusal while investigating upstream/API/scheduler constraints;
  add support only after the exact row and full five-event contract pass.
- **Acceptance:** either verified support with evidence or a truthful, tested unsupported status. Do
  not silently remove Folia 26.1–26.2 support.

### SE-P3-006 — Full platform parity requires an explicit capability strategy

- **Evidence:** Paper currently has GUI, named locations, update/integration and model commands that
  Spigot lacks; Folia cannot supply Bukkit scoreboards in the same way.
- **Required work:** for each public feature choose shared implementation, platform-specific adapter,
  equivalent UX, or an owner-approved hard technical exception. No fake success is permitted.
- **Acceptance:** the final platform matrix lists no accidental drift and every supported cell has a
  contract test and real-server evidence.

## Required verification suites before `RELEASE READY`

These are not optional enhancements; they close multiple items above.

1. **Reward invariants:** concurrent claim, retry, disconnect, full inventory, process interruption,
   ambiguous acknowledgement, reconciliation, podium, shared and public loot.
2. **Five event journeys:** clean install, configure, start, every phase, success, failure, cancel,
   rewards, restart in representative phases, cleanup.
3. **Platform contracts:** Paper, Purpur, Spigot, CraftBukkit, Folia 26.1–26.2, plus each declared
   26.x row; Folia 26.3 stays refused until verified.
4. **Client visuals:** pack download/status, all five models, UV/textures, hitboxes, animations,
   reconnect/reload, supported profiles.
5. **Performance:** baseline, one event, five events, high interaction, full waves, restart, and soak;
   record MSPT, CPU, heap, entity/task count, SQLite latency, client FPS/memory, pack load time.
6. **Configuration and docs:** compile every shipped YAML/snippet, link check, command/permission
   parity, locale key/placeholder parity, clean-server walkthrough.
7. **Release:** clean full Gradle gate, dependency/CodeQL scans, complete runtime evidence, no open P0,
   correct artifacts/checksums/metadata, and post-publication verification.

## Explicitly not classified as defects

- Core/application targeting Java 21 while current platform distributions use Java 25 is a useful
  boundary, not a defect.
- Folia 26.3 fail-closed refusal is safer than unverified support; the missing verified row is future
  work, not permission to remove Folia 26.1–26.2.
- Crash ambiguity after an external inventory/economy effect cannot honestly be called exactly-once;
  explicit `DELIVERING` plus operator reconciliation is the intended safety model.
- Vault is outside the approved five-event release scope; only its misleading documentation status
  must be corrected now.
- The Blockbench exporter is part of the authoring workflow and must not be deleted merely because
  Gradle/server runtime does not invoke it.
