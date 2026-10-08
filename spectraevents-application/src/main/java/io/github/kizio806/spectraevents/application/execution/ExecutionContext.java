package io.github.kizio806.spectraevents.application.execution;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/** Context passed during trigger evaluation and action execution. */
public record ExecutionContext(
    Object actor, UUID actorId, Map<String, Object> attributes, double combatDamage) {
  public static final ExecutionContext EMPTY =
      new ExecutionContext(null, null, Collections.emptyMap(), Double.NaN);

  public ExecutionContext {
    attributes = attributes != null ? Map.copyOf(attributes) : Collections.emptyMap();
  }

  public ExecutionContext(Object actor, UUID actorId, Map<String, Object> attributes) {
    this(actor, actorId, attributes, Double.NaN);
  }

  public ExecutionContext(Object actor, Map<String, Object> attributes) {
    this(actor, actor instanceof UUID uuid ? uuid : null, attributes);
  }

  public static ExecutionContext withActor(Object actor) {
    return new ExecutionContext(
        actor, actor instanceof UUID uuid ? uuid : null, Collections.emptyMap());
  }

  public static ExecutionContext withActor(Object actor, UUID actorId) {
    return new ExecutionContext(actor, actorId, Collections.emptyMap());
  }

  public static ExecutionContext of(Object actor, Map<String, Object> attributes) {
    return new ExecutionContext(actor, actor instanceof UUID uuid ? uuid : null, attributes);
  }

  /** Carries server-calculated melee damage without exposing platform types to the engine. */
  public static ExecutionContext withCombatDamage(Object actor, UUID actorId, double damage) {
    if (!Double.isFinite(damage) || damage < 0.0d) {
      throw new IllegalArgumentException("combat damage must be finite and non-negative");
    }
    return new ExecutionContext(actor, actorId, Collections.emptyMap(), damage);
  }

  public boolean hasCombatDamage() {
    return Double.isFinite(combatDamage);
  }
}
