package dev.spectraevents.application.config.spec;

import java.util.Map;

/** Raw authoring DTO for a condition definition. */
public record ConditionSpec(String type, Map<String, Object> parameters) {}
