# SpectraEvents Project Context

## Product

SpectraEvents is a professional, modular, data-driven 3D event engine for modern Paper Minecraft
servers.

- Primary target: latest supported Paper line
- Current initial target: Paper 26.2
- Platform runtime: Java 25
- Platform-neutral baseline: Java 21

The engine concept is:

```text
EventDefinition
      ↓
EventInstance
      ↓
Phase
      ↓
Trigger / Condition
      ↓
Action
```

Meteor is not the architecture. Meteor is the first vertical slice that will prove the architecture.

Future events may include Meteor, Airdrop, Metin, Pinata, Crystal, Vault, Boss Portal, Dragon Egg,
Seasonal Event, Pirate Treasure, and UFO. Most behavior should be composed from common engine
primitives rather than implemented as isolated event-specific systems.

The current milestone is foundation only. It intentionally does not implement event definitions,
models, animation, loot, persistence, integrations, or any event-specific behavior.
