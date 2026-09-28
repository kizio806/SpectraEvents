# 6. Mandatory Modrinth Resource-Pack Delivery

Date: 2026-09-28

## Status

Accepted (Supercedes docs/architecture/modrinth-delivery.md)

## Context

SpectraEvents relies on a custom resource pack (ZIP) generated at runtime to display imported 3D models and animations. Previously, we deferred publishing and enforcing the delivery of this pack. Players could join without the pack, falling back to basic YAML models, which compromised the core feature of the plugin and led to a fragmented experience.

## Decision

We have decided to make resource-pack delivery **mandatory** for all players on both Paper and Spigot platform families.
- The administrator must configure a pinned Modrinth version ID (or URL/SHA-1) in `resource-pack.yml`.
- Startup is actively aborted (`IllegalStateException`) if resource-pack delivery is disabled in the configuration.
- The legacy YAML fallback logic has been entirely removed from the bootstrapping process.
- Players who decline, fail to download, or time out during the resource-pack prompt will be disconnected with a localized message.

## Consequences

- **Consistent visual experience:** All players will see identical models, particles, and animations.
- **Simpler architecture:** We no longer maintain or test dual-rendering pathways (native vs. fallback YAML models).
- **Stricter deployment:** Server administrators must maintain an active resource-pack hosting mechanism (like Modrinth) for the plugin to start.
