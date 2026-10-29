# SpectraEvents — feature truth matrix

Snapshot: 2026-10-09, commit `828b034`. This matrix distinguishes source presence from verified
behavior. Detailed defects use IDs from [`BUG_AUDIT.md`](BUG_AUDIT.md).

## Classification

- **VERIFIED** — confirmed by a proportionate current test or runtime observation.
- **IMPLEMENTED / UNVERIFIED** — source exists, but the product claim lacks sufficient evidence.
- **PARTIAL** — only part of the public contract works.
- **BROKEN** — evidence demonstrates incorrect behavior.
- **STUB / FAKE** — no-op or placeholder reports/looks like a real implementation.
- **DEAD / UNUSED** — not used by the production flow.
- **MISSING** — the required capability does not exist.
- **NOT APPLICABLE** — deliberately outside the approved scope.

## Engine and configuration

| Feature | Public claim | Actual state | Evidence / issue | Decision |
| --- | :---: | --- | --- | --- |
| YAML event engine | Yes | **VERIFIED** | Parser/compiler, unit tests, full Gradle gate | Keep |
| Strict safe YAML | Yes | **VERIFIED** | SafeConstructor, duplicate-key/depth/size limits | Keep |
| File/path diagnostics | Yes | **VERIFIED** | `DefinitionLoader` qualifies compiler diagnostics | Keep |
| Per-file reload isolation | Yes | **VERIFIED** | Invalid source retains its prior valid registry entry | Keep |
| Event schema versioning | Yes | **PARTIAL** | Only schema 1 rejection; no future migration path, SE-P2-013 | Design before schema 2 |
| Bounded scalar overrides | Yes | **VERIFIED** | Compiler validates types/min/max | Keep |
| Admin GUI overrides | Paper | **VERIFIED / PARTIAL UX** | Store and compiler tests; narrow event surfaces, SE-P2-025 | Expand intentionally |
| Command event start | Yes | **VERIFIED** for smoke scope | Uses asynchronous durable start | Keep |
| GUI event start | Yes | **BROKEN** | Synchronous persistence wait, SE-P1-002/SE-P2-026 | Fix |
| Named locations | Paper | **VERIFIED** | Atomic store and command/GUI flows | Add parity strategy |
| Scheduled starts | Yes | **IMPLEMENTED / UNVERIFIED** | Parser/service tests, no real-server schedule run | Add runtime test |
| Schedule examples | Yes | **BROKEN** | Invalid Meteor `health` parameter, SE-P2-012 | Fix |
| Encounter deadlines | Yes | **VERIFIED** at application level | Persisted absolute deadline/recovery tests | Extend runtime |
| Zone reservation | Yes | **VERIFIED** at application level | Overlap/max-active ownership | Keep |
| Area enter/leave enforcement | Yes in docs | **MISSING** | No movement router, SE-P2-006/028 | Implement or remove claim |
| Random-surface spawning | Yes in scope | **MISSING** | No production strategy, SE-P2-005 | Roadmap or implement |
| Multi-timer phases | Parser permits | **VERIFIED** | MultiTimerPhaseRecoveryTest; keyed timer recovery, SE-P1-001 resolved | Keep |
| Manual diagnostics/inspect | Yes | **VERIFIED** for available state | Commands and smoke assertions | Keep |

## Gameplay, UI, and rewards

| Feature | Public claim | Actual state | Evidence / issue | Decision |
| --- | :---: | --- | --- | --- |
| Health/damage primitives | Yes | **VERIFIED** at domain/application level | Unit and config-driven tests | Keep |
| Hit counter | Yes | **VERIFIED** at domain/application level | Threshold and persistence tests | Keep |
| Interaction anti-spam | Expected | **MISSING** | Metin/Piñata accept every interaction, SE-P1-019/022 | Add generic primitive |
| Contribution ranking | Yes | **VERIFIED** at application level | Ranking/podium selection source/tests | Keep |
| Generic leaderboard UI | Yes in scope | **PARTIAL/MISSING** | Contribution exists; general UI does not, SE-P2-005 | Narrow claim/design |
| Bossbars | Yes | **IMPLEMENTED / UNVERIFIED** | Platform managers; no real-client proof | Client test |
| Paper scoreboard | Yes | **IMPLEMENTED / UNVERIFIED** | Manager exists; official YAML does not exercise it | Contract/runtime test |
| Spigot scoreboard update | Yes | **STUB / FAKE** | Returns true without update, SE-P1-007 | Implement |
| Folia scoreboard | Limited | **UNSUPPORTED** | Bukkit scoreboard unavailable | Approved explicit exception needed |
| Personal mailbox claim | Yes | **VERIFIED** at repository/service level | PENDING→DELIVERING→DELIVERED and reconciliation | Keep |
| Automatic participant reward | Yes | **VERIFIED** at application level | Mailbox claim reservation with immediate delivery and failure reconciliation, SE-P0-001/002 resolved | Keep |
| Podium rewards | Yes | **VERIFIED** at application level | Durable mailbox claims with podium announcement, SE-P1-011 resolved | Keep |
| Shared container loot | Yes | **BROKEN** | Full inventory loses items, SE-P0-003/005 | P0 fix |
| Public ground loot | Yes | **BROKEN** | Full inventory loses items, SE-P0-004/005 | P0 fix |
| Exactly-once external delivery | No | **NOT APPLICABLE** | Crash ambiguity needs `DELIVERING` reconciliation | Document honestly |

## Assets and resource packs

| Feature | Public claim | Actual state | Evidence / issue | Decision |
| --- | :---: | --- | --- | --- |
| Blockbench `.bbmodel` parsing | Yes | **VERIFIED** structurally | All five files parse and compile | Keep |
| Cuboid model compilation | Yes | **VERIFIED** structurally | Current assets contain no unsupported meshes | Keep |
| Model rendering | Yes | **IMPLEMENTED / UNVERIFIED** | Paper/Spigot renderers, no client proof | Client contract |
| Animation compilation/runtime | Yes | **IMPLEMENTED / UNVERIFIED** | JVM tests only | Client/runtime contract |
| Fresh-install asset availability | Yes | **BROKEN** | Four events have no install bundle, SE-P0-006 | P0 bundle work |
| Metin visual | Yes | **BROKEN** | Transparent 1×1 texture, SE-P0-007 | Replace |
| Boss Portal visual | Yes | **BROKEN** | Transparent 1×1 texture, SE-P0-007 | Replace |
| Airdrop/Meteor/Piñata visual quality | Yes | **UNVERIFIED** | Structure inspected; no Blockbench/client review | Professional review |
| Texture performance | Yes implicitly | **BROKEN/UNVERIFIED** | Six 4096² textures, SE-P1-028 | Optimize and measure |
| Hitbox/scale/pivot quality | Yes | **UNVERIFIED** | Default interaction shapes; no client proof, SE-P1-030 | Client review |
| Local pack generation | Yes | **VERIFIED** | Three profile ZIPs built with `--rerun-tasks` | Keep |
| Configured player delivery | Yes | **IMPLEMENTED / UNVERIFIED** | URL/Modrinth adapters and state listener | Real-client tests |
| Required-pack policy | Yes | **BROKEN** | Bootstrap failure can disable delivery, SE-P1-003 | Fail closed |
| Client acceptance evidence | Required | **MISSING** | Evidence file is disabled/empty, SE-P0-010 | P0 evidence |
| Blockbench exporter | Yes | **IMPLEMENTED / UNVERIFIED** | Documented authoring tool, not runtime code | Keep and validate manually |

## Official events

| Event | Definition/model present | Product state | Primary evidence / issues | Decision |
| --- | :---: | --- | --- | --- |
| Airdrop | Yes | **BROKEN** | Install path, loot loss, timing, no E2E; SE-P0-003/006, SE-P1-013/014 | Keep + rework first |
| Meteor | Yes | **BROKEN** | Infeasible defaults, loot loss, no gameplay proof; SE-P1-015/016/017 | Rework second |
| Metin | Yes | **BROKEN** | Invisible visual, no GUI parameters, spam, podium; SE-P0-007, SE-P1-018–021 | Rework |
| Piñata | Yes | **BROKEN** | Spam, one-click eligibility, duplicate claim, presentation-only frenzy; SE-P1-022–024 | Rework |
| Boss Portal | Yes | **BROKEN** | Invisible visual, fake enrage, narrow config, rewards; SE-P1-025–027 | Rework |
| Vault | Docs only | **MISSING / OUT OF SCOPE** | No YAML/model/runtime/test, SE-P2-010 | Label roadmap |

## Persistence, recovery, and performance

| Feature | Public claim | Actual state | Evidence / issue | Decision |
| --- | :---: | --- | --- | --- |
| SQLite schema migration | Yes | **VERIFIED** for existing versions | Sequential schema migrations and tests | Keep |
| Single-writer persistence | Yes | **VERIFIED** for functional correctness | WAL/executor and concurrency tests | Keep |
| SQLite performance | Yes implicitly | **UNVERIFIED** | “Benchmark” has no timing, SE-P1-032 | Measure |
| Runtime checkpoints | Yes | **VERIFIED** at application level | Durable state/checkpoint service tests | Extend server tests |
| Metin restart recovery | Yes | **VERIFIED** for start/restart/cancel | 14-row runtime workflow | Extend through victory |
| All-event recovery | Yes | **UNVERIFIED** | Only Metin smoke, SE-P0-008 | P0 matrix |
| Natural-success cleanup | Yes | **UNVERIFIED** | No real-server complete-event journey | Add scenarios |
| Failure cleanup | Yes | **UNVERIFIED** | No real-server failure journey | Add scenarios |
| Cancel cleanup | Yes | **VERIFIED** for Metin smoke | Post-cancel inspect | Extend all events |
| Performance envelope | Yes implicitly | **MISSING** | No MSPT/CPU/heap/client/SQLite budgets, SE-P1-031 | Establish before RC |
| Long-running stability | Expected | **MISSING** | No soak/task/entity leak evidence | Add soak gate |

## Platforms and integrations

| Feature | Public claim | Actual state | Evidence / issue | Decision |
| --- | :---: | --- | --- | --- |
| Paper 26.1–26.3 | Supported | **VERIFIED** for limited smoke | Real boot/restart/cancel rows | Full five-event rows required |
| Purpur 26.1–26.3 | Supported | **VERIFIED** for limited smoke | Real boot/restart/cancel rows | Full five-event rows required |
| Spigot 26.1–26.3 | Supported | **PARTIAL** | Limited smoke, no adapter contract suite | Close parity gaps |
| CraftBukkit 26.1–26.3 | Supported | **PARTIAL** | Limited smoke only | Close parity gaps |
| Folia 26.1–26.2 | Supported | **PARTIAL/VERIFIED smoke** | Correct server rows; feature paths not complete | Full scheduler/gameplay evidence |
| Folia 26.3 | Not supported | **UNSUPPORTED** | Startup guard refuses before mutation | Keep until exact verification |
| Spigot admin GUI parity | Claimed inconsistently | **MISSING** | SE-P2-008/SE-P3-006 | Implement equivalent UX |
| LuckPerms | Yes | **IMPLEMENTED / UNVERIFIED** | Resolver exists; no real integration test | Add contract/runtime test |
| WorldGuard | Yes | **IMPLEMENTED / UNVERIFIED** | Region resolver exists; no real integration test | Add contract/runtime test |
| Vault economy | Yes | **IMPLEMENTED / UNVERIFIED** | Deposit/withdraw resolver | Add contract/runtime test |
| PlaceholderAPI | Yes | **PARTIAL** | Only `spectra_active_count`, SE-P2-029 | Narrow or version API |
| MiniPlaceholders | Yes | **IMPLEMENTED / UNVERIFIED** | Small JVM test only | Runtime test |
| Nexo/Oraxen/ItemsAdder | Yes | **IMPLEMENTED / UNVERIFIED** | Exceptions suppressed, SE-P1-010 | Contract tests/diagnostics |
| Integration status | Yes | **PARTIAL** | Detection can outlive failed init, SE-P1-009 | Model real state |

## Localization, documentation, and release

| Feature | Public claim | Actual state | Evidence / issue | Decision |
| --- | :---: | --- | --- | --- |
| Six locale key sets | Yes | **VERIFIED** structurally | 352 keys per catalog | Keep CI check |
| Translation quality | Yes | **UNVERIFIED/PARTIAL** | No human review, SE-P2-017 | Native review |
| All visible paths localized | Yes | **BROKEN/PARTIAL** | GUI/template/podium literals, SE-P2-014–016 | Fix |
| Installation docs | Yes | **BROKEN** | Missing asset journey, SE-P2-001 | Rewrite after installer |
| Event docs | Yes | **BROKEN/PARTIAL** | Stale Boss/Pinata and broad false claims | Reconcile |
| Internal Markdown links | Yes implicitly | **BROKEN** | Missing master-plan target, SE-P2-011 | CI link gate |
| Gradle quality gate | Yes | **VERIFIED** | Current clean check/build passed | Keep |
| Architecture boundaries | Yes | **VERIFIED** | Executable boundary task passed | Keep |
| Dependency/security scans | Yes | **VERIFIED** in release workflow scope | OSV/CodeQL workflows | Keep; not product proof |
| Distribution artifacts | Yes | **VERIFIED** | Paper and Spigot JARs built | Keep |
| Runtime matrix | Yes | **PARTIAL** | Availability smoke only, SE-P0-008 | Expand evidence |
| Product release blockers | Yes | **PARTIAL, now fail-closed** | Aggregate blocker added; schema redesign planned | Build evidence model |
| Resource-pack evidence gate | Yes | **PARTIAL** | Disabled delivery passes, SE-P2-023 | Require for full release |
| Release readiness | Yes | **BLOCKED** | Open P0 set in `RELEASE_BLOCKERS.md` | No release |

## Dead or misleading surfaces

| Surface | State | Evidence / issue | Decision |
| --- | --- | --- | --- |
| Event-specific interaction delegates | **DEAD / UNUSED** | No production registrations, SE-P2-020 | Remove or justify |
| Runtime smoke helper functions | **DEAD / UNUSED** | Defined but not called, SE-P2-019 | Use or remove |
| Spigot scoreboard update | **STUB / FAKE** | SE-P1-007 | Replace with real behavior |
| “All integrations enabled” status | **MISLEADING** | Presence is not successful initialization, SE-P1-009 | Redesign status |
| Vault as current event | **MISLEADING** | Docs only, SE-P2-010 | Label roadmap |

This matrix must be updated in the same change that closes or materially changes any linked audit
item. Unsupported or unverified cells must never be converted to `VERIFIED` solely because source
code or a unit test exists.
