package io.github.kizio806.spectraevents.application.config.spec;

import java.util.Map;

/** Raw authoring DTO for an action definition. */
public record ActionSpec(String type, Map<String, Object> parameters) {}
