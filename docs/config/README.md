# Configuration Specification

This directory specifies the design and philosophy of the SpectraEvents configuration language (YAML).

## Core Philosophy
1. **YAML -> Parse -> Validate -> Compile -> Immutable Runtime Definition**
2. The running engine **NEVER** reads raw YAML. All configurations are compiled into immutable domain objects during the load phase.
3. Errors are caught early during validation, preventing runtime crashes.

## Getting started

- [Create Your First Event](../guides/create-your-first-event.md)

## Index
- [Event Definition Format](event-definition.md)
- [Phases](phases.md)
- [Transitions](transitions.md)
- [Triggers](triggers.md)
- [Conditions](conditions.md)
- [Actions](actions.md)
- [Components](components.md)
- [Spawn Strategies](spawn.md)
- [Models](models.md)
- [Animations](animations.md)
- [Interactions](interactions.md)
- [Areas](areas.md)
- [Rewards](rewards.md)
- [Leaderboards](leaderboards.md)
- [Persistence](persistence.md)
- [Placeholders](placeholders.md)
- [Validation](validation.md)
- [Versioning](versioning.md)
