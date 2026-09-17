# Reward Specification

The Reward system governs what players receive when an event succeeds.

## Reward Types
- `vanilla-item`: Standard Minecraft item stacks.
- `custom-item`: Placeholders for integrations (e.g., Oraxen, ItemsAdder).
- `command`: Executes a command (e.g., `eco give %player% 1000`).
- `experience`: Grants vanilla XP.
- `currency-provider`: A direct hook into Vault or similar plugins.

## Loot Tables
Rewards are grouped into weighted loot tables.
```yaml
loot-tables:
  premium_crate:
    rolls: 3
    entries:
      - type: vanilla-item
        material: DIAMOND
        amount: 5
        weight: 10
      - type: command
        command: "crate give %player% legendary 1"
        weight: 1
```

## Distribution Modes
When the `give-reward` action is fired, the engine needs to know *who* gets the reward.
- `all-participants`: Everyone who interacted gets a roll.
- `top-1`: Only the #1 player on the leaderboard.
- `top-N`: The top N players.
- `last-hit`: Only the player who dealt the final blow.
- `random-participant`: A randomly selected eligible player.
- `personal-loot`: Each player gets their own instanced drops (e.g., Airdrop).
- `world-drop`: Items are physically spawned on the ground at the event location.

## Anti-Dupe & Eligibility
- Players must meet a minimum participation threshold (e.g., >1% damage) to be eligible for `all-participants` or `random-participant` rewards.
- The engine guarantees a single reward distribution event cannot be fired twice for the same instance.
