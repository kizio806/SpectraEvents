# Installation Guide for SpectraEvents Beta

This guide covers the Paper-family and Spigot-family distributions of **SpectraEvents**.

## System Requirements

- **Server Platform**: Paper, Purpur, Spigot, or CraftBukkit across Minecraft 26.1–26.3; Folia on
  Minecraft 26.1–26.2 only
- **Java**: Java 25
- **Plugin Dependencies**: None required (all integrations are optional)

## Installation Steps

1. **Download the Plugin JAR**:
   Download `SpectraEvents-0.1.1-beta.1-paper.jar` for Paper, Purpur or Folia 26.1–26.2, or
   `SpectraEvents-0.1.1-beta.1-spigot.jar` for Spigot/CraftBukkit. Folia 26.3 is refused before
   initialization and instructs the operator to install a newer SpectraEvents version.

2. **Place in Server Plugins Folder**:
   Copy the downloaded `.jar` file to your server's `plugins/` directory.

3. **Start the Server**:
   Start the server with the matching distribution. On first startup, SpectraEvents will automatically create:
   - `plugins/SpectraEvents/config.yml` (global plugin configuration; `locale: en-US` by default)
   - `plugins/SpectraEvents/data/spectraevents.db` (SQLite persistence database)
   - `plugins/SpectraEvents/events/` as the directory of active definitions, initially empty.
   - `plugins/SpectraEvents/events/presets/` containing the reference Meteor, Airdrop, Metin, Piñata,
     and Boss Portal definitions. They are authoring examples, never active definitions.
   - `plugins/SpectraEvents/templates/metin.spectra.zip`, the installable Metin template. A startup
     never silently activates an event or copies a template into the active directory.
   - `plugins/SpectraEvents/events/overrides/` for bounded GUI and command overrides, without
     rewriting the source definition.
   - `plugins/SpectraEvents/locales/` for operator translations. English is always the fallback for
     missing keys.
   - `plugins/SpectraEvents/assets/source/`, initially empty; installed templates place their
     declared model sources here after their manifest and YAML have passed validation.

4. **Verify Installation**:
   Run `/spectraevents template install metin`, then `/spectraevents validate`. The installer is
   atomic and refuses to overwrite server-owned definitions or assets. Start the reference event
   with `/spectraevents event start metin`. For a custom event, copy a preset into `events/`, give
   it a distinct `id`, validate, and reload it. Finally run `/spectraevents doctor` in server
   console or as an OP in-game and correct every diagnostic it reports.

5. **Open Admin Panel**:
   On the Paper-family artifact, an in-game administrator can run `/spectraevents admin` to view
   the interactive GUI panel. The Spigot-family artifact provides the supported command workflow
   instead; see the [platform feature matrix](../product/feature-matrix.md#platform-support-contract).
