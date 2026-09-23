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

## 2. Configured event execution

Every bundled event uses the same definition-driven execution path. There are no event-specific
coordinators or developer-only commands.

```text
Admin command `/spectraevents event start <definition>`
  → EventExecutionEngine starts a compiled definition snapshot
  → Paper action adapter spawns the model, HUD and scheduled actions
  → State is persisted for recovery
  ...
Player interaction or timer trigger
  → PlatformInteractionRouter intercepts Bukkit event
      → Resolves EventInstanceId via PDC
      → EventExecutionEngine evaluates the configured transition
      → Paper action adapter updates the model, HUD, rewards and cleanup
```
