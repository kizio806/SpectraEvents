# Leaderboard Specification

Leaderboards aggregate player participation to determine reward eligibility and distribution rankings.

## Metrics
Leaderboards can track different metrics based on the active components:
- `damage`: Sourced from the `HealthComponent`.
- `hits`: Sourced from the `HitCounterComponent`.
- `interactions`: Sourced from the `InteractionComponent`.
- `items-delivered`: Sourced from specific delivery actions (Boss Portal).

## Runtime Aggregation
As players interact, their scores are updated in memory. The leaderboard is sorted on demand (typically when an action requires it, or periodically if a hologram is displaying the top 3).

## Ties
In the event of a tie (e.g., two players have exactly 50 hits), the player who reached the score first is ranked higher.

## Snapshot Behavior
When the event completes, the final leaderboard state is snapshotted and used by the `RewardDistribution` system. Once snapshotted, further interactions (if any occur before cleanup) do not affect rewards.
