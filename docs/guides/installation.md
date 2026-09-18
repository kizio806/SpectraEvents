# Installation Guide for SpectraEvents Beta

This guide covers setting up **SpectraEvents** on your Minecraft Paper server.

## System Requirements

- **Server Platform**: Paper 26.2+ (Java 25 runtime environment)
- **Plugin Dependencies**: None required (all integrations are optional)

## Installation Steps

1. **Download the Plugin JAR**:
   Download `spectraevents-paper-26_2-0.1.0-beta.1.jar` from GitHub Releases.

2. **Place in Server Plugins Folder**:
   Copy the downloaded `.jar` file to your server's `plugins/` directory.

3. **Start the Server**:
   Start your Paper server. On first startup, SpectraEvents will automatically create:
   - `plugins/SpectraEvents/config.yml` (main plugin configuration)
   - `plugins/SpectraEvents/eventevents.db` (SQLite persistence database)
   - `plugins/SpectraEvents/events/` directory containing executable default definitions (`meteor.yml`, `airdrop.yml`, `metin.yml`).

4. **Verify Installation**:
   Run `/event doctor` in server console or as an OP in-game. You should see:
   `Doctor complete: No critical errors detected.`

5. **Open Admin Panel**:
   In-game as an admin, run `/event admin` to view the interactive GUI panel.
