# SpectraEvents Engineering Rules

- No NMS or CraftBukkit internals unless an explicit ADR proves public APIs insufficient.
- No global singleton architecture or service locator.
- No giant manager dumping grounds.
- No speculative abstractions or dependencies added for later.
- No business logic in Bukkit listeners.
- No blocking database or network I/O on server execution threads.
- No global tick loop without a demonstrated requirement; prefer event-driven execution.
- Do not assume a single global Minecraft main thread. Design Paper interaction with future region
  scheduling and Folia compatibility in mind, but do not claim Folia support before testing it.
- Use Adventure and MiniMessage for text when text behavior is introduced.
- Use Persistent Data Container for persistent identification of owned Minecraft objects.
- Prefer public Paper APIs. Do not use reflection hacks.
- Parse, validate, and compile configuration into immutable definitions before domain execution.
- Do not hide warnings with broad suppressions or weaken quality gates.

Before creating feature-specific behavior, ask whether it should instead be represented by a
`Component`, `Action`, `Trigger`, `Condition`, or `Phase`. Prefer a reusable `HealthComponent` over
separate `MeteorHealthManager`, `PinataHealthManager`, and `MetinHealthManager` systems.

## Comments & Documentation

- Comments explain WHY, constraints, invariants, security or compatibility decisions. They must not narrate obvious code.
- Do not add comments that simply restate the following line of code.
- Do not leave comments describing what was changed, fixed, refactored, or implemented during the current task. Git history belongs in Git.
- Do not add comments merely to make code look documented.
- Use Javadoc for public contracts and non-obvious extension points, not mechanically for every getter, setter, constructor, or obvious method.
- Prefer descriptive names and small functions over explanatory comments.
- Keep concise comments for non-obvious security decisions where removing the comment could cause a future maintainer to reintroduce a vulnerability.
- Comments may document unusual Minecraft/Paper/Spigot compatibility constraints when they are not obvious from types or tests.
- Do not add vague TODO comments. A TODO must describe a concrete unresolved action and reason.

## Logging

- Production logs must be concise, actionable, and use appropriate levels.
- **INFO**: important lifecycle summaries and administrator-relevant state.
- **WARN**: recoverable problems requiring attention.
- **ERROR**: failures requiring investigation.
- **DEBUG**: high-volume internal diagnostics.
- Do not log per entity, per keyframe, per cube, or per cache lookup at INFO.
- Never log access tokens, authorization headers, passwords, or sensitive URLs.

## Commands & Public Naming

- Product name: `SpectraEvents`
- Root command: `/event`
- Permission namespace: `spectraevents.*`

## Update Policy

- SpectraEvents may automatically check for updates.
- Never implement silent in-place JAR replacement, automatic hot reload, or automatic server restart.
- Any future automatic download must be explicit/configurable and stage updates for a controlled restart.

## Asset Authoring Policy

- Always edit canonical source assets (e.g. Blockbench `.bbmodel`, `.spectra.zip`), never the generated resource-pack JSON output.
- Custom servers must host their own custom assets (`ManualUrlResourcePackSource`).
- The official Modrinth CDN project (ID: `Rg1nw8IW`, slug: `spectraevents-assets`) is strictly for maintaining the official baseline packs.
