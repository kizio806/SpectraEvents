package io.github.kizio806.spectraevents.application.config.spec;

import java.util.List;

/** Raw authoring DTO for a transition rule. */
public record TransitionSpec(
    TriggerSpec trigger,
    List<ConditionSpec> conditions,
    String targetPhase,
    List<ActionSpec> actions) {}
