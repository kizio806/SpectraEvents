# SpectraEvents Roadmap

This roadmap outlines the technical progression from the minimal kernel to a fully realized general-purpose event engine.

1. **Product/Event Specification (Current)**
   - Define contracts, terminology, and reference event blueprints.
2. **Phase Model**
   - Integrate the phase model and phase transitions with the existing Event Runtime Kernel.
3. **Paper Walking Skeleton**
   - Ensure the architecture correctly spans core, application, and platform layers on Paper 26.2.
4. **First Meteor Lifecycle**
   - Implement the first vertical slice demonstrating a full instance lifecycle (without models/loot).
5. **Native Display Rendering**
   - Implement `ModelComponent` parsing and rendering using native Display Entities.
6. **Interaction**
   - Implement `InteractionComponent` for physical clicking/hitting.
7. **Timer + Health**
   - Implement `TimerComponent` and `HealthComponent`.
8. **Basic Animation**
   - Implement rudimentary linear track animations for models.
9. **Simple Reward**
   - Connect a basic loot table and distribution mechanic.
10. **Extract Proven Generic Primitives**
    - Refactor any specific code from the vertical slice into truly generic Phase/Trigger/Action modules.
11. **Configuration Compiler**
    - Build the robust YAML parser and validator.
12. **Persistence / Recovery**
    - Implement serialization of Event Snapshots for server restart recovery.
13. **Second Materially Different Event**
    - Implement the Airdrop or Pinata event to validate engine generality.
14. **Validate Engine Generality**
    - Ensure both Meteor and Airdrop run concurrently on the same unified abstractions.
15. **Integrations**
    - Support WorldGuard, PlaceholderAPI, and popular custom item plugins.
16. **Public API**
    - Finalize and expose `spectraevents-api` for external triggers, conditions, and actions.
17. **GUI/Editor (Post-V1)**
    - Implement the in-game configuration editor.
