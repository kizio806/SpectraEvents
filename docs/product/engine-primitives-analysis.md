# Engine Primitives Analysis

After specifying a diverse set of reference events (Meteor, Airdrop, Metin, Pinata, Vault, Boss Portal), we can definitively isolate the generic engine primitives required.

## 1. Timer
- **Used by**: Meteor, Airdrop, Pinata, Vault
- **Type**: Component (`TimerComponent`) / Trigger (`timer-expired`)
- **Layer**: Core domain (logic), Application (scheduling)
- **Status**: Universal requirement. Needs to track remaining time and emit triggers.

## 2. Health
- **Used by**: Meteor, Metin
- **Type**: Component (`HealthComponent`) / Trigger (`health-zero`, `health-threshold`)
- **Layer**: Core domain
- **Status**: Required for combat-oriented events.

## 3. Hit Counter
- **Used by**: Pinata, Boss Portal (as delivery tracker)
- **Type**: Component (`HitCounterComponent`) / Trigger (`hits-reached`)
- **Layer**: Core domain
- **Status**: Required for non-combat discrete interactions.

## 4. Interaction
- **Used by**: Meteor, Airdrop, Metin, Pinata, Vault, Boss Portal (all)
- **Type**: Component (`InteractionComponent`) / Trigger (`player-interact`)
- **Layer**: Platform (Bukkit entity mapping) -> Core (Domain trigger)
- **Status**: Universal. The platform adapter must translate Bukkit damage/interact events into domain logic.

## 5. Leaderboard
- **Used by**: Meteor, Metin, Pinata, Boss Portal
- **Type**: Component (`LeaderboardComponent`)
- **Layer**: Core domain
- **Status**: Essential for ranked reward distributions.

## 6. Loot & Rewards
- **Used by**: All events
- **Type**: Action (`give-reward`) / Abstraction (`RewardDistribution`)
- **Layer**: Core (evaluation) -> Platform (giving items)
- **Status**: Universal.

## 7. Event Area
- **Used by**: Meteor, Airdrop, Metin, Pinata, Vault, Boss Portal (all)
- **Type**: Component (`AreaComponent`) / Trigger (`player-enter-area`, `player-leave-area`)
- **Layer**: Platform (movement tracking, event cancellation) -> Core
- **Status**: Universal for spatial presence and temporary rule enforcement.

## 8. Mob Waves
- **Used by**: Metin, Boss Portal
- **Type**: Component (`MobWaveComponent`) / Action (`spawn-mob`) / Trigger (`wave-cleared`)
- **Layer**: Core (tracking logic) -> Platform (entity spawning)
- **Status**: Required for PvE events.

## Conclusion
The analysis confirms that the proposed architecture is viable. None of the reference events require a hardcoded "MeteorManager" or "VaultManager". By implementing the above 8 primitives, the engine can express all of them purely through YAML configuration and Phase Transitions.
