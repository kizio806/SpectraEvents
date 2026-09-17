package io.github.kizio806.spectraevents.application.config.spec;

import java.util.Map;

/** Raw authoring DTO for an event configuration. */
public record EventSpec(
    String id, String schemaVersion, String initialPhase, Map<String, PhaseSpec> phases) {}
