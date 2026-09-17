package io.github.kizio806.spectraevents.application.execution;

import java.util.Collections;
import java.util.Map;

/** Context passed during trigger evaluation and action execution. */
public record ExecutionContext(Object actor, Map<String, Object> attributes) {
  public static final ExecutionContext EMPTY = new ExecutionContext(null, Collections.emptyMap());

  public ExecutionContext {
    attributes = attributes != null ? Map.copyOf(attributes) : Collections.emptyMap();
  }

  public static ExecutionContext withActor(Object actor) {
    return new ExecutionContext(actor, Collections.emptyMap());
  }

  public static ExecutionContext of(Object actor, Map<String, Object> attributes) {
    return new ExecutionContext(actor, attributes);
  }
}
