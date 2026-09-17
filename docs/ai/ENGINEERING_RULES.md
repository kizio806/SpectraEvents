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
