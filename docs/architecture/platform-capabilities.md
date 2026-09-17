# Platform Capabilities

This document outlines the capability matrix for the SpectraEvents engine across supported platforms.
Because the engine is platform-neutral, its features dynamically adjust based on the underlying runtime capabilities.

## Capability Matrix

| Capability | Paper | Purpur | Folia | Spigot | Bukkit | Sponge |
| --- | --- | --- | --- | --- | --- | --- |
| **DISPLAY_ENTITIES** | DIRECT | SAME ADAPTER | SAME ADAPTER | DIRECT | FALLBACK | UNSUPPORTED |
| **INTERACTION_ENTITIES**| DIRECT | SAME ADAPTER | SAME ADAPTER | DIRECT | FALLBACK | UNSUPPORTED |
| **REGION_SCHEDULING** | DIRECT | SAME ADAPTER | DIRECT | UNSUPPORTED | UNSUPPORTED | UNSUPPORTED |
| **ADMIN_GUI** | DIRECT | SAME ADAPTER | SAME ADAPTER | DIRECT | FALLBACK | UNSUPPORTED |
| **ADVENTURE_NATIVE** | DIRECT | SAME ADAPTER | SAME ADAPTER | SEPARATE ADAPTER| FALLBACK | DIRECT |
| **CUSTOM_ITEMS** | DIRECT | SAME ADAPTER | SAME ADAPTER | DIRECT | FALLBACK | UNSUPPORTED |

## Legend
- **DIRECT**: The capability is natively supported and mapped directly by the platform adapter.
- **SAME ADAPTER**: The platform runs the Paper adapter natively.
- **SEPARATE ADAPTER**: The platform supports the feature but requires a separate implementation (e.g. Spigot requiring an Adventure shim).
- **FALLBACK**: The platform simulates the feature with reduced fidelity or older APIs.
- **UNSUPPORTED**: The platform lacks the capability entirely (events requiring it will be marked unavailable).
- **NOT YET IMPLEMENTED**: The capability might exist in the API but is not yet mapped by the engine.

## Implementation Details

### Paper Family (Paper, Purpur, Folia)
Fully supports all capabilities. Folia utilizes Region Scheduling to maintain safety. Purpur utilizes the Paper artifact directly without any modifications.

### Bukkit Family (Spigot, CraftBukkit)
Spigot fully supports basic Bukkit abstractions. Adventure requires shading/platform integration since Spigot doesn't bundle it natively. Display Entities exist in Spigot 1.19.4+ but may lack some asynchronous API conveniences present in Paper, requiring manual task scheduling.

### Sponge
Sponge API 12 handles entities differently. Capabilities like `DISPLAY_ENTITIES` and `INTERACTION_ENTITIES` are currently `UNSUPPORTED` in the minimal adapter implementation, meaning events like `meteor` will gracefully report themselves as unavailable rather than crashing.
