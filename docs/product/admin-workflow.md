# Admin Workflow

This document describes the expected end-to-end experience for a server administrator interacting with SpectraEvents.

## The Journey

1. **Install Plugin**
   - The administrator drops the `spectraevents.jar` into the `plugins/` folder and starts the server.

2. **Generate Defaults**
   - On first boot, the engine generates an `examples/` directory containing commented reference configs (e.g., `meteor.yml`, `airdrop.yml`).

3. **Create Event Definition**
   - The admin copies `meteor.yml` into `definitions/`, names it `my-custom-meteor.yml`, and modifies the values (changing the health, model, or loot).
   - This configuration file serves as the **Primary Source of Truth**.

4. **Validation**
   - The admin runs `/event definition validate my-custom-meteor`.
   - The engine parses the YAML and reports any semantic errors (e.g., missing referenced models, cyclical phases) as `ERROR`, `WARNING`, or `INFO`.

5. **Preview / Test**
   - The admin uses `/event model preview <model-id>` to spawn the defined visual components locally and ensure they look correct.

6. **Reload Definition**
   - The admin runs `/event definition reload my-custom-meteor` (or `/event reload`).
   - The engine compiles the YAML into an immutable `EventDefinition` in memory.
   - Any currently running `EventInstance` created from an older version of this definition retains its original configuration snapshot (or version reference) to prevent runtime corruption.

7. **Manual Start**
   - The admin executes `/event event start my-custom-meteor`.
   - The engine selects a valid spawn location (based on the strategy) and creates a new `EventInstance`.

8. **Runtime Inspection**
   - The admin tracks the event using `/event event info <instance-id>` to see its current Phase, Location, and Health.
   - They can teleport to the instance via `/event event teleport <instance-id>`.

9. **Debug**
   - If interactions aren't working, the admin runs `/event debug hitboxes <instance-id>` to visualize the physical interaction boundaries.

10. **Production Scheduling**
    - The admin uses an external scheduler or a future SpectraEvents cron feature to run the event automatically.

## Future GUI
While a GUI editor (inventory-based) may be added in a Post-V1 release, it will act strictly as a frontend to generate or modify the underlying YAML files. The configuration files remain the canonical source of truth.
