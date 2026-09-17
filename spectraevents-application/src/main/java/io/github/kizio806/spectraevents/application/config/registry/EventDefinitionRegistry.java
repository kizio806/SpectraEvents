package io.github.kizio806.spectraevents.application.config.registry;

import io.github.kizio806.spectraevents.core.event.definition.EventDefinition;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Stores compiled event definitions keyed by their stable IDs. */
public final class EventDefinitionRegistry {
  private final Map<EventDefinitionId, RegisteredEventDefinition> definitions =
      new LinkedHashMap<>();

  /**
   * Registers a compiled definition.
   *
   * @param definition compiled event definition
   * @param sourceFile authoring source path or name
   * @throws DuplicateEventDefinitionException if the ID is already registered
   */
  public void register(EventDefinition definition, String sourceFile) {
    Objects.requireNonNull(definition, "definition");
    Objects.requireNonNull(sourceFile, "sourceFile");

    EventDefinitionId id = definition.id();
    RegisteredEventDefinition existing = definitions.get(id);
    if (existing != null) {
      throw new DuplicateEventDefinitionException(id, existing.sourceFile(), sourceFile);
    }

    definitions.put(id, new RegisteredEventDefinition(definition, sourceFile));
  }

  /**
   * Registers or updates a compiled definition in the registry.
   *
   * @param definition compiled event definition
   * @param sourceFile authoring source path or name
   */
  public void registerOrUpdate(EventDefinition definition, String sourceFile) {
    Objects.requireNonNull(definition, "definition");
    Objects.requireNonNull(sourceFile, "sourceFile");
    definitions.put(definition.id(), new RegisteredEventDefinition(definition, sourceFile));
  }

  public Optional<RegisteredEventDefinition> get(EventDefinitionId id) {
    Objects.requireNonNull(id, "id");
    return Optional.ofNullable(definitions.get(id));
  }

  public Collection<RegisteredEventDefinition> getAll() {
    return List.copyOf(definitions.values());
  }

  /** Removes all registered definitions. Used when reloading configuration. */
  public void clear() {
    definitions.clear();
  }

  public boolean isEmpty() {
    return definitions.isEmpty();
  }
}
