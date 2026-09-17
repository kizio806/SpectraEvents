package io.github.kizio806.spectraevents.application.integration;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Central registry for tracking optional plugin integrations. */
public final class IntegrationRegistry {
  private final Map<String, IntegrationInfo> integrations = new LinkedHashMap<>();

  public record IntegrationInfo(String name, IntegrationState state, String details) {
    public IntegrationInfo {
      Objects.requireNonNull(name, "name");
      Objects.requireNonNull(state, "state");
    }
  }

  public void register(String name, IntegrationState state, String details) {
    integrations.put(name.toLowerCase(), new IntegrationInfo(name, state, details));
  }

  public Optional<IntegrationInfo> get(String name) {
    return Optional.ofNullable(integrations.get(name.toLowerCase()));
  }

  public Map<String, IntegrationInfo> getAll() {
    return Collections.unmodifiableMap(integrations);
  }

  public long enabledCount() {
    return integrations.values().stream()
        .filter(i -> i.state() == IntegrationState.ENABLED)
        .count();
  }
}
