# Technical documentation

Public administrator documentation lives in the versioned [`wiki/`](../wiki/Home.md) source. The
GitHub Wiki is a published mirror, not a second independently edited copy.

This directory contains material that belongs with the source code:

| Area | Purpose |
| --- | --- |
| [`ai/`](ai/README.md) | Required project context, engineering rules, workflow, testing, and current state |
| [`architecture/`](architecture/README.md) | Current boundaries, runtime flows, compatibility, and durable ADRs |
| [`authoring/`](authoring/README.md) | Detailed model and asset authoring reference |
| [`config/`](config/README.md) | Exact schema-v1 configuration reference |
| [`operations/`](operations/release-and-recovery.md) | Maintainer release and recovery procedures |
| [`audit/`](audit/BUG_AUDIT.md) | Evidence-backed product truth, gaps, and release blockers |
| [`releases/`](releases/0.1.1-beta.2.md) | Version-specific release notes |

`MASTER_PLAN.md` at the repository root is the single implementation ledger. Avoid adding a second
roadmap, feature matrix, command guide, or event guide. Update the Wiki for user-facing behavior,
the exact technical reference for contracts, and the audit/plan only when their evidence changes.
