package dev.spectraevents.application.config.spec;

import java.util.Map;

/** Raw authoring DTO for a trigger definition. */
public record TriggerSpec(String type, Map<String, Object> parameters) {}
