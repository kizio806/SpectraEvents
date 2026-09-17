# SpectraEvents Copilot Instructions

Canonical repository instructions are defined in `/AGENTS.md`. Before substantial changes, read
`/AGENTS.md` and its required documents under `/docs/ai/`.

- The project is a modular event engine, not a set of hardcoded events.
- Core, application, and API never import Bukkit or Paper.
- Dependency direction is `platform -> application -> core`.
- Tests accompany meaningful domain behavior.
- Run the documented quality gates before completion.

Do not bypass architecture, testing, or workflow rules defined by the canonical documents.
