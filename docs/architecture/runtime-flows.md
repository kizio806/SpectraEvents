# Runtime Flows

These sequences demonstrate how events are orchestrated at runtime, illustrating the decoupling between platform, application, and core layers.

## 1. General Event Lifecycle

A generic flow of how an event transitions phases based on an external trigger or condition.

```text
Command / Trigger (e.g. Timer Expired)
  → Application Layer (EventOrchestrationService.transitionPhase)
      → Core Domain (EventInstance.transitionPhase)
          → Validate Transition via PhaseDefinition
          → Emit EventPhaseChanged
  → Platform Side Effect (e.g. PaperModelRenderer updates model)
```

## 2. Dev Meteor (Vertical Slice)

The current implementation uses a vertical slice approach to validate the engine architecture on a real server.

```text
Admin Command `/spectra dev meteor start`
  → DevMeteorCoordinator (Platform)
      → OrchestrationService.startDevMeteor (Application)
          → Creates EventInstance with MeteorFixture (Core)
      → PaperModelRenderer spawns Model (Magma block + Blackstone) at Y+20
      → PaperEventTaskScheduler schedules Impact phase
  ...
Timer Expired (Platform)
  → DevMeteorCoordinator next(impact)
      → OrchestrationService.transitionPhase
      → PaperModelRenderer teleports Model to ground
      → Spawns explosion particles & sounds
  ...
Player Left-Clicks Hitbox (Interaction)
  → PlatformInteractionRouter intercepts Bukkit event
      → Resolves EventInstanceId via PDC
      → Delegates to DevMeteorCoordinator
          → Decrements Health primitive
          → If depleted, transitions to destroyed phase.
```

## 3. Future Airdrop Event

The following demonstrates how a future Airdrop event will reuse the exact same proven primitives, without copying Meteor's coordinator logic.

```text
Scheduled Event Start (Trigger)
  → OrchestrationService starts Airdrop Event (Application)
      → Creates EventInstance based on YAML Airdrop Definition (Core)
  → PaperModelRenderer spawns Parachute and Crate model (Platform)
      → startTransformAnimation executes descent
  ...
Player Right-Clicks Crate (Interaction)
  → PlatformInteractionRouter intercepts Bukkit event
      → Resolves EventInstanceId via PDC
      → Delegates to Action Engine (Future Component)
          → Validates Claim Condition
          → Distributes Loot Reward
          → Transitions Event to Cleanup phase
```
