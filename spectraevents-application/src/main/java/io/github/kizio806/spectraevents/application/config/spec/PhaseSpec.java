package io.github.kizio806.spectraevents.application.config.spec;

import java.util.List;
import java.util.Set;

/** Raw authoring DTO for a phase configuration. */
public record PhaseSpec(
    Set<String> allowedTransitions, List<TransitionSpec> transitions, List<ActionSpec> onEnter) {}
