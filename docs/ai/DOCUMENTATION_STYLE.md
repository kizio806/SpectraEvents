# SpectraEvents Documentation Style Guide

This style guide establishes the canonical editorial, structural, terminology, and visual standards for all documentation across the **SpectraEvents** repository, including `docs/`, `wiki/`, and AI instructions under `docs/ai/`.

Its goal is to ensure that SpectraEvents maintains the same caliber of clarity, precision, and authoritativeness demonstrated in reference production repositories (such as `jump-delay-fix`).

---

## 1. Editorial Voice & Philosophy

- **Authoritative & Technically Accurate:** Explain systems exactly as they are implemented in code. Never document speculative plans or wishful features as existing capabilities.
- **Honest About Limitations:** Document boundaries, edge cases, and known limitations explicitly. If a platform line is unsupported (such as Folia 26.3+), document the exact technical reason why.
- **Concise & Direct:** Respect the reader's time. Avoid corporate jargon, filler paragraphs, or marketing fluff.
- **Operator & Developer First:** Structure content around concrete workflows: installing, configuring, authoring events, troubleshooting, building from source, and extending architecture.

---

## 2. Terminology Glossary

Use these terms consistently across code comments, commit messages, documentation, and wiki pages:

| Term | Definition | Incorrect Synonyms |
| :--- | :--- | :--- |
| **Engine** | The underlying SpectraEvents framework orchestrating lifecycles, state machines, and adapters. | "Plugin", "Mod" |
| **Event Definition** | The compiled, immutable declarative template (loaded from YAML) describing an event's structure, phases, triggers, actions, and limits. | "Config", "Script" |
| **Event Instance** | An active, executing occurrence of an event in a Minecraft world with its own runtime state, phase, entities, and participants. | "Event run", "Spawned event" |
| **Encounter** | The physical gameplay space and bounds of an active event instance (e.g. radius, bossbar, contributor tracking). | "Battle area", "Arena" |
| **Component** | A reusable behavioral building block attached to an instance (e.g. `HealthComponent`, `LootComponent`, `BossbarComponent`). | "Manager", "Handler" |
| **Phase** | A discrete lifecycle stage in an event definition with specific triggers, actions, and valid transitions. | "State", "Step", "Wave" |
| **Action** | An executable domain operation dispatched during transitions or triggers (e.g. `SpawnModelAction`, `GiveItemAction`). | "Command", "Effect" |
| **Trigger** | A condition-tested stimulus that prompts a transition or action (e.g. `DamageTrigger`, `TimerTrigger`). | "Listener", "Hook" |
| **Spectra Bundle** | A packaged asset bundle (`.spectra.zip` or directory) containing 3D models, textures, animations, and metadata. | "Custom model pack" |
| **Mailbox** | The transactional, persistent storage mechanism holding unclaimed rewards for offline or inventory-full players. | "Reward chest", "Drop queue" |

---

## 3. Markdown Formatting Standards

### Headings
- Use `#` for page title only (one per document).
- Use `##` for top-level major sections.
- Use `###` for subsections.
- Do not skip heading levels (e.g. do not jump from `##` to `####`).
- Headings should use Title Case or Sentence Case consistently within a document.

### GitHub Alerts
Use standard GitHub markdown alerts to emphasize critical details:
- `> [!NOTE]` — For contextual background, defaults, or implementation details.
- `> [!TIP]` — For performance optimizations, best practices, or workflow shortcuts.
- `> [!IMPORTANT]` — For essential operational requirements and prerequisites.
- `> [!WARNING]` — For breaking changes, deprecation warnings, or dangerous configurations.
- `> [!CAUTION]` — For actions that could cause data corruption, desynchronization, or server instability.

Do not nest alerts or stack identical alerts consecutively.

### Tables
- Format Markdown tables with clean alignment and explicit column headers.
- Include column justification colons (`:---`, `:---:`, `---:`) where appropriate.
- Every column must have a meaningful header.

### Code Blocks & Syntax Highlighting
Always specify language tags on fenced code blocks:
- `bash` for shell commands and script snippets.
- `java` for Java code examples.
- `yaml` for event definitions and configuration examples.
- `json` for JSON metadata or schemas.
- `properties` for `.properties` files.
- `mermaid` for architectural sequence and flowchart diagrams.

---

## 4. Hyperlinks & Navigation

- **In-Repository Links:** All links within markdown files must be relative paths with `.md` extensions (e.g. `[Architecture](../architecture/target-architecture.md)`).
- **Zero Broken Links:** All relative links must resolve to existing files. Use `python3 scripts/verification/verify_markdown_links.py` to validate before committing.
- **Section Anchors:** Ensure lowercase hyphenated GitHub anchor links match real headings (e.g. `#table-of-contents`).
- **AI / Chat Agent Protocol:** When communicating in chat or IDE artifacts, use clickable `file:///` URIs with line ranges as required by workspace instructions.

---

## 5. Badge Standards

Repository badges at the top of `README.md` must adhere to:
1. Flat or flat-square visual style with consistent shields.io / vendor integrations.
2. Direct links to the underlying service or report (CI workflow run, SonarCloud project, Codecov report, GitHub Releases).
3. Canonical badge sequence:
   - Build & Verification (`CI Status`)
   - Quality & Security Gate (`SonarCloud Quality Gate`, `Codecov Coverage`)
   - Release Status (`GitHub Release`, `Modrinth Assets`)
   - Legal & Standards (`License`)
   - Supported Runtime Platforms (`Paper 26.2`, `Purpur 26.2`, `Folia 26.2`, `Spigot 26.2`)

---

## 6. Document Maintenance Rules

1. When code changes invalidate documentation, update the canonical document immediately in the same pull request.
2. If an architectural decision changes, author an ADR in `docs/architecture/adr/` that supersedes the prior ADR; do not rewrite historical ADRs.
3. Synchronize `wiki/` documentation whenever matching `docs/` articles are updated.
4. Keep `docs/ai/PROJECT_STATE.md` current when milestones, blockers, or direction change.
