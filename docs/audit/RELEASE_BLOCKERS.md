# SpectraEvents — release blockers

Snapshot: 2026-10-09. Verdict: **RELEASE BLOCKED**.

The schema-v2 executable register in `config/release/release-blockers.json` prevents another
publication while the product-level P0 set is open. All 11 IDs are mandatory; a closed item requires
its declared evidence types, and every evidence record must match the exact candidate commit.
Detailed closure criteria live in [`BUG_AUDIT.md`](BUG_AUDIT.md); this file is the concise release
view.

## Open P0 set

| ID | Blocker | Required proof before closure |
| --- | --- | --- |
| SE-P0-001 | Participant reward can remain claimable after immediate delivery | Automatic/manual claim state-machine regression suite |
| SE-P0-002 | Automatic reward failures are converted to success | Persisted failure/ambiguous-state and diagnostics tests |
| SE-P0-003 | Paper shared-container loot can be lost | Full-inventory, disconnect, and contention server tests |
| SE-P0-004 | Paper ground loot can be lost | No-loss/no-duplication pickup tests |
| SE-P0-005 | Spigot repeats the shared/public loot defects | Shared adapter contract suite plus Spigot runtime |
| SE-P0-006 | Four presets lack a usable fresh-install asset path | Empty-server install-to-completion proof for all five bundles |
| SE-P0-007 | Metin and Boss Portal use invisible textures | Professional replacement assets and real-client evidence |
| SE-P0-008 | No event has a complete real-server journey | All five success/failure/cancel/restart/reward/cleanup workflows |
| SE-P0-009 | Release gate previously had no product blocker input | Executable blocker and closure-evidence validation |
| SE-P0-010 | No real-client resource-pack acceptance exists | Per-profile client load/render/animation evidence |
| SE-P0-011 | Published beta does not disclose confirmed blockers consistently | Owner-approved public response and synchronized metadata |

## Release states

### BLOCKED

Any of the following is enough:

- an open P0;
- an official event without a complete required platform/version journey;
- missing client evidence for a published pack profile;
- known reward loss/duplication or recovery ambiguity outside the documented reconciliation model;
- incompatible public metadata, artifacts, version catalog, or documentation.

### RELEASE CANDIDATE

- all P0 items are closed with linked evidence;
- all five bundles install and validate on a clean server;
- all five pass the selected Paper/Purpur baseline journeys;
- reward invariants, restart recovery, and natural cleanup pass;
- all professional assets pass structural and real-client review;
- documentation and six locale catalogs match the candidate behavior;
- full Gradle, security, artifact, and release gates pass on the exact candidate commit.

### RELEASE READY

- the release-candidate conditions hold;
- every declared Paper, Purpur, Spigot, CraftBukkit, and Folia row passes its complete contract;
- performance and soak budgets pass on recorded reference hardware;
- upgrade/migration and rollback procedures pass;
- published artifacts, checksums, Modrinth metadata, GitHub Release, and resource-pack versions are
  verified after publication.

No local build or aggregate runtime status can substitute for a missing condition above.
