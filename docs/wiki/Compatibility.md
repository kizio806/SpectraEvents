# Compatibility

Current distributions target Java 25. Platform-neutral core and application modules target Java 21;
that does not make the current server JARs Java-21 compatible.

| Server | Artifact | Declared Minecraft band | Current proof |
| --- | --- | --- | --- |
| Paper | `SpectraEvents-<version>-paper.jar` | 26.1–26.3 | Boot/restart/cancel smoke only |
| Purpur | `SpectraEvents-<version>-paper.jar` | 26.1–26.3 | Boot/restart/cancel smoke only |
| Folia | `SpectraEvents-<version>-paper.jar` | 26.1–26.2 | Limited smoke; full feature parity pending |
| Spigot | `SpectraEvents-<version>-spigot.jar` | 26.1–26.3 | Limited smoke; parity gaps pending |
| CraftBukkit | `SpectraEvents-<version>-spigot.jar` | 26.1–26.3 | Limited smoke; parity gaps pending |

Folia 26.3 is deliberately refused before plugin initialization. It will not be advertised until an
exact official runtime passes the complete event, recovery, scheduler, and capability matrix.

“Declared” is not the same as release-ready. A supported release row must pass all five complete
event journeys, recovery, rewards, cleanup, and the exact platform capability contract.
