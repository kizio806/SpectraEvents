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
     at startup. On Paper, `/event assets build` can run the same process manually.
   - Use the imported model ID and animation names in the regular YAML `spawn_model` and
     `play_animation` actions. The generated ZIP must be hosted externally before it can be delivered
     to players.

4. **Create Event Definition**
   - The admin copies `events/meteor.yml` to `events/my-custom-meteor.yml` and gives it a unique `id` before editing health, model, or loot.
   - This configuration file serves as the **Primary Source of Truth**.

5. **Validation**
   - The admin runs `/event definition validate`.
   - The engine parses the YAML and reports any semantic errors (e.g., missing referenced models, cyclical phases) as `ERROR`, `WARNING`, or `INFO`.

6. **Preview / Test**
   - Paper operators may use the available model/debug commands to inspect native visual components. Spigot reports unsupported Paper-only admin features explicitly.

7. **Reload Definition**
   - The admin runs `/event definition reload`.
   - The engine compiles the YAML into an immutable `EventDefinition` in memory.
   - Any currently running `EventInstance` created from an older version of this definition retains its original configuration snapshot (or version reference) to prevent runtime corruption.

8. **Manual Start**
   - The admin executes `/event event start my-custom-meteor`.
   - The engine selects a valid spawn location (based on the strategy) and creates a new `EventInstance`.

9. **Runtime Inspection**
   - The admin tracks state, pending tasks and resource counts with `/event event inspect <instance-id>`.

10. **Debug**
   - The admin runs `/event doctor` and `/event event inspect <instance-id>` before collecting logs.

11. **Production Scheduling**
    - The admin uses an external scheduler or a future SpectraEvents cron feature to run the event automatically.

## Future GUI
While a GUI editor (inventory-based) may be added in a Post-V1 release, it will act strictly as a frontend to generate or modify the underlying YAML files. The configuration files remain the canonical source of truth.
