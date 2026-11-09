# Architecture & Operational Invariants

This document outlines the architectural boundaries, threading models, and platform guarantees enforced by SpectraEvents 1.0.0. Understanding these invariants helps operators and developers configure large-scale production deployments safely.

---

## 1. Multi-Platform & Threading Invariants

### Folia Regional Threading (`RegionTaskScheduler`)
- **Region-Bound Operations:** On Folia servers, entity states, Display entity transforms, and player inventories are bound to individual regional tick threads. SpectraEvents routes all tick-bound updates through `RegionTaskScheduler`. Cross-region operations are never dispatched synchronously.
- **Version Compatibility Gate:** The Paper distribution enforces strict runtime compatibility gating via `FoliaCompatibilityGuard`. Supported lines currently encompass Folia 26.1 through 26.3.

### Spigot / CraftBukkit Execution
- **Adventure Text Bridging:** Spigot does not natively expose Adventure components. The Spigot platform adapter bridges text components via reflection and legacy serialization (`GuiText.legacy`), preserving color codes, hex gradients, and formatting.
- **Asynchronous Persistence:** To prevent main-thread tick stalling on Spigot's single thread, all database mutations execute asynchronously via `SingleWriterPersistenceExecutor`.

---

## 2. 3D Display & Interaction Entity Invariants

- **Vanilla Display Entities:** All models render natively using vanilla `item_display` and `block_display` entities. No custom client-side mods or Forge/Fabric loaders are required.
- **Interpolation Smoothing:** Display entities interpolate position and rotation updates across ticks using `interpolation_duration`. When translating or rotating model bones, quaternion slerp produces smooth motion without client jitter.
- **Hitbox Mapping:** Clickable collision volumes are powered by native `interaction` entities bound to model bones, providing precise interaction tracking during animations.

---

## 3. Durability & Transaction Invariants

- **Single-Writer SQLite WAL:** All event instance states and player awards persist to SQLite running in Write-Ahead Logging (WAL) mode with memory caching.
- **Three-State Claim Durability:** Player mailbox claims follow strict atomic transitions:
  ```text
  PENDING  ──►  DELIVERING  ──►  DELIVERED
  ```
  If a server crashes while a reward is in `DELIVERING` status, SpectraEvents marks the state for operator inspection rather than blindly reissuing or discarding the reward, preventing item duplication exploits.
- **Fail-Closed Resource Pack Enforcement:** When required resource pack delivery is enabled, client rejections (`DECLINED`, `INVALID_URL`, `FAILED_RELOAD`) are handled deterministically according to operator configuration to prevent invisible models or broken visuals.
