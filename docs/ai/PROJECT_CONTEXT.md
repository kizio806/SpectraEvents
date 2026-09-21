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

Meteor, Airdrop, and Metin are bundled definitions that exercise the generic architecture; none of
them is a separate hardcoded event subsystem.

Future events may include Meteor, Airdrop, Metin, Pinata, Crystal, Vault, Boss Portal, Dragon Egg,
Seasonal Event, Pirate Treasure, and UFO. Most behavior should be composed from common engine
primitives rather than implemented as isolated event-specific systems.

The current milestone completes the Blockbench asset workflow: signed Generic Model import, local
resource-pack generation, and opt-in administrator-hosted delivery. Modrinth publishing and the public
addon API remain deliberately unavailable. M2 cannot close until real-client rendering and player
delivery are observed and recorded.
