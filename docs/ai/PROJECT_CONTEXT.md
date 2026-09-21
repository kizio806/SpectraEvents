# SpectraEvents Project Context

## Product

SpectraEvents is a modular, data-driven 3D event engine for modern Paper-family and Spigot-family
Minecraft servers.

- Supported families: Paper/Purpur/Folia and Spigot/CraftBukkit
- Current compatibility band: Minecraft 26.1, 26.2, and 26.3
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

Meteor, Airdrop, Metin, Pinata, and Boss Portal are bundled definitions that exercise the generic
architecture; none of them is a separate hardcoded event subsystem.

Future events may include Meteor, Airdrop, Metin, Pinata, Crystal, Vault, Boss Portal, Dragon Egg,
Seasonal Event, Pirate Treasure, and UFO. Most behavior should be composed from common engine
primitives rather than implemented as isolated event-specific systems.

M2 and M3 are complete: the signed Generic Model import, local resource-pack generation, and opt-in
administrator-hosted delivery were confirmed by the operator in a real-client workflow; the five
reference events remain ordinary YAML and asset definitions with no event-specific coordinators. M4
production compatibility is complete: Paper/Purpur, Spigot/CraftBukkit 26.1–26.3 and Folia 26.1–26.2
have real-server coverage. The current milestone is M5: admin GUI expansion and documentation
cleanup. No public addon API exists and none is planned without a demonstrated use case.
