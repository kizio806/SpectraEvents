package io.github.kizio806.spectraevents.application.service;

import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.config.registry.RegisteredEventDefinition;
import io.github.kizio806.spectraevents.application.dev.fixture.MeteorFixture;
import io.github.kizio806.spectraevents.application.dev.fixture.WalkingSkeletonFixture;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinition;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.execution.TransitionRule;
import io.github.kizio806.spectraevents.core.event.phase.PhaseDefinition;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleTransition;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

/** Orchestrates event instance lifecycle operations for the application. */
public final class EventOrchestrationService {
  private static final Logger LOGGER = Logger.getLogger(EventOrchestrationService.class.getName());
  private static final String MANUAL_TRIGGER_TYPE = "manual";

  private final EventInstanceRepository repository;
  private final EventDefinitionRegistry definitionRegistry;
  private final io.github.kizio806.spectraevents.application.execution.EventExecutionEngine
      executionEngine;

  public EventOrchestrationService(
      EventInstanceRepository repository,
      EventDefinitionRegistry definitionRegistry,
      io.github.kizio806.spectraevents.application.execution.EventExecutionEngine executionEngine) {
    this.repository = Objects.requireNonNull(repository, "repository");
    this.definitionRegistry = Objects.requireNonNull(definitionRegistry, "definitionRegistry");
    this.executionEngine = executionEngine;
  }

  public EventOrchestrationService(
      EventInstanceRepository repository, EventDefinitionRegistry definitionRegistry) {
    this(repository, definitionRegistry, null);
  }

  public io.github.kizio806.spectraevents.application.execution.EventExecutionEngine
      executionEngine() {
    return executionEngine;
  }

  /**
   * Starts a new walking skeleton event instance.
   *
   * @return the newly started instance
   */
  public EventInstance startWalkingSkeleton() {
    EventInstanceId id = EventInstanceId.generate();
    EventInstance created = EventInstance.create(id, WalkingSkeletonFixture.DEFINITION_ID);
    EventLifecycleTransition transition = created.start(WalkingSkeletonFixture.getDefinition());

    EventInstance running = transition.eventInstance();
    repository.save(running);

    LOGGER.info(
        "Started event instance "
            + id
            + " using walking_skeleton in phase "
            + running.currentPhase().orElseThrow());
    return running;
  }

  /**
   * Starts a new dev_meteor event instance.
   *
   * @return the newly started instance
   */
  public EventInstance startDevMeteor() {
    EventInstanceId id = EventInstanceId.generate();
    EventInstance created = EventInstance.create(id, MeteorFixture.DEFINITION_ID);
    EventLifecycleTransition transition = created.start(MeteorFixture.getDefinition());

    EventInstance running = transition.eventInstance();
    repository.save(running);

    LOGGER.info(
        "Started event instance "
            + id
            + " using dev_meteor in phase "
            + running.currentPhase().orElseThrow());
    return running;
  }

  /**
   * Starts a new event instance from a registered YAML-backed definition.
   *
   * @param definitionId stable definition identifier
   * @return the newly started instance
   * @throws IllegalArgumentException if the definition is not registered
   */
  public EventInstance startDefinition(String definitionId) {
    return startDefinition(definitionId, null);
  }

  /**
   * Starts a new event instance from a registered definition with platform location context.
   *
   * @param definitionId stable definition identifier
   * @param platformLocationReference platform location reference (e.g., Bukkit Location)
   * @return the newly started instance
   */
  public EventInstance startDefinition(String definitionId, Object platformLocationReference) {
    if (executionEngine != null) {
      return executionEngine.startEvent(definitionId, platformLocationReference);
    }
    EventDefinition definition = requireRegisteredDefinition(definitionId);

    EventInstanceId id = EventInstanceId.generate();
    EventInstance created = EventInstance.create(id, definition.id());
    EventLifecycleTransition transition = created.start(definition);

    EventInstance running = transition.eventInstance();
    repository.save(running);

    LOGGER.info(
        "Started event instance "
            + id
            + " using "
            + definitionId
            + " in phase "
            + running.currentPhase().orElseThrow());
    return running;
  }

  /**
   * Transitions an event instance to its next available phase.
   *
   * @param instanceId string representation of the instance UUID
   * @return the transitioned instance
   * @throws IllegalArgumentException if the ID is invalid or instance is not found
   */
  public EventInstance transitionPhase(String instanceId) {
    EventInstance instance = getExistingInstance(instanceId);
    EventDefinition definition = resolveDefinition(instance.definitionId());

    PhaseId currentPhase =
        instance
            .currentPhase()
            .orElseThrow(() -> new IllegalStateException("Event has no current phase."));

    PhaseId targetPhase = resolveNextPhase(definition, currentPhase);

    EventLifecycleTransition transition = instance.transitionPhase(definition, targetPhase);

    EventInstance nextInstance = transition.eventInstance();
    repository.save(nextInstance);

    LOGGER.info("Event " + instance.id() + ": " + currentPhase + " -> " + targetPhase);
    return nextInstance;
  }

  /**
   * Completes a running event instance.
   *
   * @param instanceId string representation of the instance UUID
   * @return the completed instance
   */
  public EventInstance completeEvent(String instanceId) {
    EventInstance instance = getExistingInstance(instanceId);
    EventLifecycleTransition transition = instance.complete();

    EventInstance completed = transition.eventInstance();
    repository.save(completed);

    LOGGER.info("Completed event " + instance.id());
    return completed;
  }

  /**
   * Cancels a running event instance.
   *
   * @param instanceId string representation of the instance UUID
   * @return the cancelled instance
   */
  public EventInstance cancelEvent(String instanceId) {
    EventInstance instance = getExistingInstance(instanceId);
    EventLifecycleTransition transition = instance.cancel();

    EventInstance cancelled = transition.eventInstance();
    repository.save(cancelled);

    LOGGER.info("Cancelled event " + instance.id());
    return cancelled;
  }

  /**
   * Queries the current status of an event instance.
   *
   * @param instanceId string representation of the instance UUID
   * @return the instance
   */
  public EventInstance getEventInfo(String instanceId) {
    return getExistingInstance(instanceId);
  }

  private EventDefinition requireRegisteredDefinition(String definitionId) {
    if (definitionId == null || definitionId.isBlank()) {
      throw new IllegalArgumentException("Definition ID cannot be blank.");
    }

    EventDefinitionId id = new EventDefinitionId(definitionId);
    return definitionRegistry
        .get(id)
        .map(RegisteredEventDefinition::definition)
        .orElseThrow(
            () -> new IllegalArgumentException("Event definition not found: " + definitionId));
  }

  private EventDefinition resolveDefinition(EventDefinitionId definitionId) {
    return definitionRegistry
        .get(definitionId)
        .map(RegisteredEventDefinition::definition)
        .orElseGet(() -> resolveFixtureDefinition(definitionId));
  }

  private EventDefinition resolveFixtureDefinition(EventDefinitionId definitionId) {
    if (definitionId.equals(WalkingSkeletonFixture.DEFINITION_ID)) {
      return WalkingSkeletonFixture.getDefinition();
    }
    if (definitionId.equals(MeteorFixture.DEFINITION_ID)) {
      return MeteorFixture.getDefinition();
    }
    throw new IllegalStateException("Unsupported event definition: " + definitionId.value());
  }

  private PhaseId resolveNextPhase(EventDefinition definition, PhaseId currentPhase) {
    PhaseDefinition phaseDef =
        definition
            .phase(currentPhase)
            .orElseThrow(
                () -> new IllegalStateException("Current phase is unknown to definition."));

    for (TransitionRule rule : phaseDef.rules()) {
      if (MANUAL_TRIGGER_TYPE.equals(rule.trigger().type()) && rule.targetPhase().isPresent()) {
        PhaseId target = rule.targetPhase().get();
        if (phaseDef.allowsTransitionTo(target)) {
          return target;
        }
      }
    }

    return phaseDef.allowedTransitions().stream()
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No further transitions available."));
  }

  private EventInstance getExistingInstance(String instanceId) {
    UUID uuid;
    try {
      uuid = UUID.fromString(instanceId);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid instance ID format.", e);
    }

    return repository
        .findById(new EventInstanceId(uuid))
        .orElseThrow(() -> new IllegalArgumentException("Event instance not found: " + instanceId));
  }
}
