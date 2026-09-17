# Cross-Event Analysis

This document compares the structural requirements and generic primitive utilization between two fully functional vertical slices: **Dev Meteor** and **Dev Airdrop**. By comparing these two distinct events, we validate the robustness of the core event engine.

## Primitive Reuse Comparison

| Primitive            | Meteor | Airdrop | Metin | Stability Level    |
| -------------------- | ------ | ------- | ----- | ------------------ |
| Lifecycle            | yes    | yes     | yes   | PROVEN GENERIC     |
| Phases               | yes    | yes     | yes   | PROVEN GENERIC     |
| Temporal             | yes    | yes     | yes   | PROVEN GENERIC     |
| Model                | yes    | yes     | yes   | PROVEN GENERIC     |
| Interaction          | yes    | yes     | yes   | PROVEN GENERIC     |
| Cleanup              | yes    | yes     | yes   | PROVEN GENERIC     |
| Health               | yes    | no      | yes   | PROVEN GENERIC     |
| HealthThresholds     | no     | no      | yes   | NOT YET STABLE     |
| DamageContribution   | no     | no      | yes   | DEV-ONLY / UNSTABLE|
| Claim                | no     | yes     | no    | EVENT-SPECIFIC     |
| Entity death routing | no     | no      | yes   | PROVEN GENERIC     |
| PDC ownership        | yes    | yes     | yes   | PROVEN GENERIC     |
| Visual side effects  | yes    | yes     | yes   | EVENT-SPECIFIC     |
| Reward delivery      | yes    | yes     | no    | NOT YET STABLE     |

## New generic abstractions proven by second event
1. **Platform Region Scheduler Interface**: During the implementation of Airdrop, it became clear that executing Bukkit logic (particles, sound, entities) natively directly on the generic `GlobalRegionScheduler` provided by `EventTaskScheduler` violated Folia's thread safety contract. The `PlatformRegionTaskScheduler` was introduced for the platform delegates to natively hook into Region-based threads for specific world mutations, separating domain scheduling from execution scheduling.
2. **Atomicity in Interactivity (Claiming)**: While Meteor utilized a cumulative interaction model (Health depletion), Airdrop utilized a strict **first-wins** atomic model (Claiming). The `EventInteractionDelegate` router proved capable of supporting both disparate state models cleanly. The `Claim` concept is a prime candidate for a generic primitive extraction in a future milestone.
3. **Thresholds and Leaderboard primitives (Metin)**: The Metin vertical slice necessitated the implementation of `HealthThresholds` to automatically trigger state phase transitions when HP dropped below certain percentages, as well as `DamageContribution` to track an active leaderboard of players. These primitives were created agnostically in `core`, demonstrating the ability to rapidly add functional generic components without coupling them to specific event models. Furthermore, Metin necessitated a `PlatformEntityDeathRouter` for delegating spawned Boss entity death events, proving the routing pattern scales seamlessly.
