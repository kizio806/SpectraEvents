# ADR 0009: Exclude Folia 26.3 from the 0.1.1 beta compatibility matrix

## Status

Accepted on 2026-10-04.

## Context

SpectraEvents supports Folia 26.1 and 26.2 through the Paper-family artifact. The official Folia
catalog has no 26.3 runtime, so the former declared Folia 26.3 release row could not be executed.
Keeping it declared would indefinitely block a beta that otherwise has verified Paper, Purpur, Spigot
and CraftBukkit support for 26.1–26.3.

## Decision

The 0.1.1 beta declares Folia 26.1–26.2 only. The Paper artifact detects Folia 26.3 and newer before
initialization, disables itself without mutating event data, and tells the operator to download a
newer SpectraEvents release when support returns. Release CI runs Folia rows only for 26.1 and 26.2.

Three distinct resource-pack ZIPs remain published for Minecraft 26.1, 26.2 and 26.3 because client
resource-pack profiles are version-specific. They are published automatically to the separate
Modrinth resource-pack project before the plugin version is published.

## Consequences

- Paper/Purpur and Spigot/CraftBukkit remain supported on 26.1–26.3.
- Folia 26.3 is not a supported use of this beta and cannot silently start.
- A future Folia 26.3 entry requires an available official runtime and a passing lifecycle, cleanup
  and restart-recovery workflow before documentation, metadata and the release matrix are changed.
