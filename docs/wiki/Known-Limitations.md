# Known Limitations & Platform Constraints

The current beta is subject to several architectural and operational boundaries that developers and server operators should understand. These constraints are tracked toward the 1.0.0 production milestone.

---

## 1. Multi-Platform & Server Engine Boundaries

### Folia Region Scheduling Invariants
- **Region-Bound Task Delegation:** In Folia environments, entity state, Display entity transforms, and player inventories are bound to individual region tick threads. Cross-region operations must never be executed synchronously on arbitrary threads. All model position updates, particle spawns, and rewards dispatches must route through `RegionTaskScheduler`.
- **Folia Version Compatibility Gate:** The Paper distribution enforces strict version gating via `FoliaCompatibilityGuard`. Supported lines currently encompass Folia 26.1 through 26.3. Any unsupported future Minecraft line is actively refused during startup to protect persistent world and event state from corruption.

### Spigot / CraftBukkit Execution Realities
- **Adventure Text Serialization:** Spigot does not natively expose Adventure components on its `Player` or `CommandSender` interfaces. The Spigot platform adapter bridges text components via reflection and legacy serialization (`GuiText.legacy`), preserving color codes, hex gradients, and formatting across platforms.
- **Single-Thread Tick Bottlenecks:** Because Spigot operates on a single global main thread, database persistence must always execute asynchronously via `SingleWriterPersistenceExecutor` to prevent tick stalling.

---

## 2. 3D Display & Interaction Entity Constraints

- **Client-Side Interpolation Smoothing:** Vanilla Minecraft Display entities interpolate position and rotation updates across ticks using `interpolation_duration`. When teleporting models across large distances, client-side visual stretching may occur if teleport flags are omitted.
- **Interaction Hitboxes:** Interaction entities provide the clickable collision volume for complex 3D models. Nested interaction hitboxes must not overlap in ways that occlude click-event dispatching for distinct subcomponents.

---

## 3. Resource Pack Delivery & Mailbox Claims

- **Fail-Closed Pack Policy:** When a server enforces required resource pack delivery, any failure (`DECLINED`, `INVALID_URL`, `FAILED_RELOAD`) triggers a fail-closed response to prevent missing texture errors or invisible models.
- **Three-State Claim Durability:** Reward claims follow strict atomic transitions: `PENDING -> DELIVERING -> DELIVERED`. Claims must survive unexpected server halts without loss or duplication.

---

## 4. Release Blocker Tracking

The executable blocker register is tracked in:
[`config/release/release-blockers.json`](https://github.com/kizio806/SpectraEvents/blob/main/config/release/release-blockers.json).

Missing empirical test evidence is treated as an active release blocker, never as implicit success.
