# Terminology

To avoid naming chaos, the following terms hold strict definitions within the SpectraEvents domain.

- **Event Definition**: The immutable, declarative blueprint of an event (e.g., `meteor.yml`). It describes what an event *could* be. It is not active.
- **Event Instance**: A specific, running occurrence of an Event Definition in the world. It holds state (location, current health, active phase).
- **Event Lifecycle**: The global state of an Event Instance (`CREATED`, `RUNNING`, `COMPLETED`, `CANCELLED`, `FAILED`). This is independent of the definition's phases.
- **Phase**: A user-defined state within an Event Definition (e.g., `FALLING`, `LOCKED`, `ACTIVE`). An instance can only be in one phase at a time.
- **Transition**: The rules dictating how an instance moves from one Phase to another, triggered by specific events.
- **Trigger**: An event that initiates an action or a transition (e.g., `timer-expired`, `health-zero`).
- **Condition**: A prerequisite that must be true for a Trigger to fire or an Action to execute (e.g., `minimum-players: 5`).
- **Action**: A one-shot operation executed by the engine (e.g., `spawn-model`, `give-reward`, `broadcast`).
- **Component**: A persistent behavior attached to an instance during a specific phase (e.g., `HealthComponent`, `TimerComponent`).
- **Participant**: A player who has meaningfully interacted with an Event Instance and is eligible for tracking/rewards.
- **Event Area**: A spatial boundary surrounding an instance that tracks membership and enforces rules (e.g., no PvP).
- **Model**: A visual representation built from one or more native Minecraft display entities.
- **Model Part**: An individual node within a Model, possessing local transforms (position, rotation, scale).
- **Animation**: A collection of tracks dictating how Model Parts move over time.
- **Animation Track**: A sequence of keyframes applied to a specific property (e.g., rotation) of a Model Part.
- **Keyframe**: A defined state of a property at a specific timestamp in an animation.
- **Reward**: A tangible benefit given to a participant (item, command execution, currency).
- **Loot Table**: A weighted collection of potential rewards.
- **Reward Distribution**: The strategy dictating who gets what (e.g., `top-3`, `all-participants`, `last-hit`).
- **Spawn Strategy**: The logic determining where a new Event Instance appears (e.g., `random-surface`).
- **Event Snapshot**: A serialized representation of a running Event Instance used for persistence and recovery.
- **Provider**: An internal abstraction for fetching external data (e.g., `CurrencyProvider`).
- **Adapter**: Code translating between the core domain and a specific platform (e.g., `Paper26Adapter`).
- **Integration**: Code connecting SpectraEvents to a third-party plugin (e.g., `WorldGuardIntegration`).
