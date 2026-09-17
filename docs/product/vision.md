# Product Vision

## What SpectraEvents Is
SpectraEvents is a **professional, general-purpose data-driven 3D event engine** for modern Paper servers.

Administrators currently rely on disparate plugins to run different events (e.g., a standalone `MeteorPlugin`, a separate `AirdropPlugin`, another `PinataPlugin`). SpectraEvents eliminates this fragmentation by allowing administrators to construct any of these—and many more (e.g., Metin, Vault, Boss Portal, Seasonal Events)—using a single, unified configuration language.

The engine provides a robust set of shared, reusable primitives:
- Models and Animations
- Phases and Transitions
- Triggers, Conditions, and Actions
- Components (Health, Hit Counters, Timers, Interactions)
- Loot and Rewards
- Leaderboards
- Event Areas
- Persistence and Recovery

These primitives are independent of any specific event. By orchestrating them via data-driven definitions, an administrator can author a "Meteor" event or an "Airdrop" event without writing custom Java plugins for each behavior.

## Core Advantage
By sharing primitives, any improvement to the `HealthComponent` benefits every event that uses health. Any optimization to `ModelRenderer` benefits all visual events. The architecture shifts from building "a meteor" to building "an engine capable of expressing a meteor."

## Non-Goals
SpectraEvents is strictly bounded. It is **NOT**:
- A full custom mob engine (like MythicMobs).
- A complete region protection replacement (like WorldGuard).
- A comprehensive model editor or standalone rendering engine (like ModelEngine).
- A full-scale economy plugin or quest engine.
- A Turing-complete generic scripting language for all server automation.

SpectraEvents focuses purely on orchestrating dynamic, spatial, phase-driven events.
