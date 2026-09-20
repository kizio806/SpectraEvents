# SpectraEvents Architecture

## Invariants

```text
Paper / infrastructure
          ↓
     Application
          ↓
        Core
```

Core must not depend on Bukkit, Paper, Minecraft internals, or any platform adapter. Application is
also platform-neutral. Dependencies point inward; infrastructure implements ports owned by inner
layers. There is no public addon API in the current beta; ADR 0005 records why it was deferred.

## Modules

- `spectraevents-core`: pure domain code and engine invariants; Java 21, no platform types.
- `spectraevents-application`: use cases, orchestration, and infrastructure ports; Java 21.
- `adapters/*`: platform-neutral storage, update, and currently disabled asset adapters.
- `platforms/paper/common`: Paper/Purpur/Folia platform implementation, compiled against Paper 26.1.
- `platforms/spigot/common`: Spigot/CraftBukkit platform implementation, compiled against Spigot 26.1.
- `distributions/paper`: shaded Paper-family plugin.
- `distributions/spigot`: shaded Spigot-family plugin.

The intended version-support shape is:

```text
core <- application <- platform family <- distribution
```

Both artifacts use the oldest supported API as their compile baseline. A release claim is valid only
after the command-driven runtime and restart workflow passes for every declared server/version row.
If a platform API is unavailable, the adapter must report that capability as unsupported rather than
silently ignore the action.
