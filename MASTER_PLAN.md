# SpectraEvents Master Plan

Authoritative execution ledger for product readiness, defect remediation, and release verification.

## Source of Truth

The canonical technical audit, evidence-backed defect findings, and verification gates are maintained in:
- [Current release blockers](docs/audit/RELEASE_BLOCKERS.md) (`config/release/release-blockers.json`)
- [Bug and Defect Audit](docs/audit/BUG_AUDIT.md)
- [Architecture & Boundaries](docs/ai/ARCHITECTURE.md)
- [Project State](docs/ai/PROJECT_STATE.md)

## Remediation & Verification Ledger

### P0 Release Blockers
- **SE-P0-001**: Participant reward double-delivery prevention via optimistic locking and durable claims. *(Verified)*
- **SE-P0-002**: Automatic reward failures correctly reported as non-success (`RETAINED` yields false) to prevent premature phase completion. *(Fixed & Tested)*
- **SE-P0-003 / SE-P0-004**: Paper container and ground loot conservation via durable reservation and remainder retention. *(Verified)*
- **SE-P0-005**: Spigot shared and public loot parity with Paper container and ground loot preservation. *(Verified)*
- **SE-P0-006**: Official bundled templates (Airdrop, Boss Portal, Meteor, Metin, Piñata) packaged and installable from clean server. *(Verified)*
- **SE-P0-007**: Embedded Blockbench textures verified against canonical high-resolution sources. *(Verified)*
- **SE-P0-008**: Official events runtime journey and lifecycle verification. *(In progress)*
- **SE-P0-009**: Release blockers validation with stable IDs and closure evidence in `verify_release_gate.py`. *(Verified)*
- **SE-P0-010**: Real-client resource-pack acceptance verification gate. *(Verified)*
- **SE-P0-011**: Public release surfaces and beta metadata synchronization. *(Verified)*

### Additional Findings Remediated
- **FIND-001 / FIND-006**: `spawn_boss` action compiled and executed across Paper and Spigot platforms with instance PDC tagging.
- **FIND-002**: `entity_death` trigger routing isolated from wave mob deaths in Paper and Spigot routers.
- **FIND-003**: Asynchronous fatal action handler (`setFatalActionHandler`) implemented in Spigot adapter.
- **FIND-004**: Multi-event podium announcement support with fallback to `event-podium` key across all 6 locales.
- **FIND-005**: 100% key parity maintained across all six locale files (en-US, de-DE, es-ES, fr-FR, pl-PL, pt-BR).
