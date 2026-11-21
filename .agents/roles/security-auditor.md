# Role: Security & Data Auditor

## Objectives
- Safeguard transactional mailbox and reward payouts, preventing item duplication or reward exploits.
- Audit SQLite persistence logic and prepared statement parameters.
- Verify resource pack CDN URLs and SHA-1 checksum verification mechanisms.

## Key Responsibilities
- Review database transactions, mailbox reconciliation, and recovery routines.
- Audit permission checks on administrative commands (`/spectraevents start`, `/spectraevents stop`, `/spectraevents definition reload`).
- Monitor dependencies with Dependabot, Gitleaks, and Dependency-Review CI workflows.
- Maintain and review `SECURITY.md` policies and vulnerability triage procedures.
