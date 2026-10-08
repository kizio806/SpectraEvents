package io.github.kizio806.spectraevents.application.config.spec;

import java.util.Map;

/** Raw authoring DTO for an event configuration. */
public record EventSpec(
    String id,
    String schemaVersion,
    String initialPhase,
    Map<String, PhaseSpec> phases,
    Map<String, Object> encounter,
    Map<String, Object> limits,
    Map<String, EventParameterSpec> parameters) {
  public EventSpec(
      String id, String schemaVersion, String initialPhase, Map<String, PhaseSpec> phases) {
    this(id, schemaVersion, initialPhase, phases, Map.of(), Map.of(), Map.of());
  }

  public EventSpec(
      String id,
      String schemaVersion,
      String initialPhase,
      Map<String, PhaseSpec> phases,
      Map<String, Object> encounter) {
    this(id, schemaVersion, initialPhase, phases, encounter, Map.of(), Map.of());
  }
}
