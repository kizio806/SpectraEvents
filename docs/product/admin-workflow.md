# Admin Workflow

This document describes the expected end-to-end experience for a server administrator interacting with SpectraEvents.

## The Journey

1. **Install Plugin**
   - Install `SpectraEvents-<version>-paper.jar` on Paper/Purpur/Folia or `SpectraEvents-<version>-spigot.jar` on Spigot/CraftBukkit.
   - Do not mix the two artifacts. The generated resource pack is local by default; player delivery is
     opt-in and requires the explicit HTTPS/SHA-1 configuration.

2. **Generate Defaults**
   - On first boot, the engine generates `events/` and `models/` directories containing paired
     reference definitions (`meteor.yml`, `airdrop.yml`, `metin.yml`, `pinata.yml`, and
     `boss-portal.yml`).

3. **Import Optional Blockbench Asset**
   - Export a Generic Model `.spectra.zip` into `assets/source/`; the server validates and builds it
     at startup. On Paper, `/spectraevents assets build` can run the same process manually.
   - Use the imported model ID and animation names in the regular YAML `spawn_model` and
     `play_animation` actions. The generated ZIP must be hosted externally before it can be delivered
     to players.

4. **Create Event Definition**
   - The admin copies `events/meteor.yml` to `events/my-custom-meteor.yml` and gives it a unique `id` before editing health, model, or loot.
   - This configuration file serves as the **Primary Source of Truth**.

5. **Validation**
   - The admin runs `/spectraevents definition validate`.
   - The engine parses the YAML and reports any semantic errors (e.g., missing referenced models, cyclical phases) as `ERROR`, `WARNING`, or `INFO`.

6. **Test Runtime**
   - Start the definition at a controlled location and inspect its lifecycle with `/spectraevents event inspect <instance-id>`. Visual models and animations are owned by the event definition, not by ad-hoc player commands.

7. **Reload Definition**
   - The admin runs `/spectraevents definition reload`.
   - The engine compiles the YAML into an immutable `EventDefinition` in memory.
   - Any currently running `EventInstance` created from an older version of this definition retains its original configuration snapshot (or version reference) to prevent runtime corruption.

8. **Manual Start**
   - The admin executes `/spectraevents event start my-custom-meteor`.
   - The engine selects a valid spawn location (based on the strategy) and creates a new `EventInstance`.

9. **Runtime Inspection**
   - The admin tracks state, pending tasks and resource counts with `/spectraevents event inspect <instance-id>`.

10. **Debug**
   - The admin runs `/spectraevents doctor` and `/spectraevents event inspect <instance-id>` before collecting logs.

11. **Production Scheduling**
    - The admin uses an external scheduler or a future SpectraEvents cron feature to run the event automatically.

## Admin GUI

The Admin GUI is an in-game inventory panel opened with `/spectraevents admin` (requires
`spectraevents.gui`). All GUI actions are equivalent to the corresponding command: the YAML
definitions and `config.yml` remain the canonical source of truth.

### Navigation overview

- **Main Menu** — entry point. Tiles navigate to Dashboard, Active Events, Definitions,
  Integrations, Updates, Configuration, and Locations.
- **Dashboard** — shows active event count, loaded definitions, storage status, and enabled
  integrations. The Refresh tile reloads the values.
- **Active Events** — paginated list of running instances. Click an instance to open its detail
  view where you can inspect state or open the cancel confirmation.
- **Definitions** — paginated list of registered definitions. Click a definition to open its detail
  view, which shows phases and offers a **Start** button that spawns the event at your current
  position using saved scalar overrides.
- **Configuration** — paginated list of definitions with saved operator overrides. Click a
  definition to open its detail screen:
  - There is no difficulty-profile selector. Each YAML-declared, GUI-editable scalar parameter
    has an override tile; left-click increases and right-click decreases it within its declared
    range and step. Changes persist to `config.yml`.
  - For an exact value, use `/spectraevents event config <id> set <parameter> <value>`.
- **Locations** — paginated list of named event locations. The **Save Current Position** tile
  (slot 45) records your standing location. Click a saved location name to open the removal
  confirmation. Named locations can be used with `/spectraevents event start <id> location <name>`.
- **Integrations** — shows which optional integrations (LuckPerms, WorldGuard, Vault,
  PlaceholderAPI, MiniPlaceholders, Nexo, Oraxen, ItemsAdder) are active.
- **Updates** — shows the current version and any available update.
