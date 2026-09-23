package io.github.kizio806.spectraevents.application.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kizio806.spectraevents.application.config.compiled.ConfiguredActionDefinition;
import io.github.kizio806.spectraevents.application.config.compiled.ConfiguredTriggerDefinition;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinition;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.execution.TransitionRule;
import io.github.kizio806.spectraevents.core.event.phase.PhaseDefinition;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EventDefinitionProfileApplierTest {
  @Test
  void appliesSupportedRuntimeOverridesWithoutMutatingTheSourceDefinition() {
    PhaseId active = new PhaseId("active");
    EventDefinition original =
        new EventDefinition(
            new EventDefinitionId("meteor"),
            active,
            Map.of(
                active,
                new PhaseDefinition(
                    active,
                    Set.of(),
                    List.of(
                        new TransitionRule(
                            new ConfiguredTriggerDefinition(
                                "timer_elapsed", Map.of("duration", "30s")),
                            List.of(),
                            Optional.empty(),
                            List.of())),
                    List.of(
                        new ConfiguredActionDefinition("initialize_health", Map.of("max", 100)),
                        new ConfiguredActionDefinition("apply_damage", Map.of("amount", 10)),
                        new ConfiguredActionDefinition("initialize_hit_counter", Map.of("max", 5)),
                        new ConfiguredActionDefinition("set_locked", Map.of("duration", "5s"))))));

    EventDefinition configured =
        EventDefinitionProfileApplier.apply(
            original,
            Map.of(
                "health", 500,
                "damage", 20,
                "hits", 25,
                "duration", "90s",
                "lock-duration", "15s"));

    PhaseDefinition phase = configured.phase(active).orElseThrow();
    assertEquals(500, phase.onEnterActions().get(0).parameters().get("max"));
    assertEquals(20, phase.onEnterActions().get(1).parameters().get("amount"));
    assertEquals(25, phase.onEnterActions().get(2).parameters().get("max"));
    assertEquals("15s", phase.onEnterActions().get(3).parameters().get("duration"));
    assertEquals("90s", phase.rules().get(0).trigger().parameters().get("duration"));
    assertEquals(
        100, original.phase(active).orElseThrow().onEnterActions().get(0).parameters().get("max"));
  }
}
