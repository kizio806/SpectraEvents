package io.github.kizio806.spectraevents.platform.bukkit.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.execution.EventLocation;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleState;
import io.github.kizio806.spectraevents.core.gameplay.health.Health;
import io.github.kizio806.spectraevents.core.gameplay.hits.HitCounter;
import org.junit.jupiter.api.Test;

class EventDisplayPlaceholdersTest {

  @Test
  void shouldResolveAllPlaceholdersCorrectly() {
    EventInstanceId instanceId = EventInstanceId.generate();
    EventInstance instance =
        EventInstance.reconstitute(
            instanceId,
            new EventDefinitionId("meteor_shower"),
            EventLifecycleState.RUNNING,
            new PhaseId("phase_2"));

    EventRuntimeState state = new EventRuntimeState(instanceId);
    state.setHealth(new Health(75, 100));
    state.setHitCounter(new HitCounter(5, 10));
    state.setPlatformLocation(new EventLocation("world_nether", 10.4, 64.0, -20.6, 90f, 0f));
    state.setTimerDeadlineMillis(System.currentTimeMillis() + 60_000L);

    String template =
        "Event %event% in phase %phase% at %location%: HP %health%/%max_health%"
            + " (%health_percent%%), Hits %hits%/%max_hits%, Time %time_remaining%s";
    String resolved = EventDisplayPlaceholders.resolve(template, instance, state);

    assertTrue(resolved.contains("Event meteor_shower in phase phase_2"));
    assertTrue(resolved.contains("at world_nether 10, 64, -21"));
    assertTrue(resolved.contains("HP 75/100 (75%)"));
    assertTrue(resolved.contains("Hits 5/10"));
    assertTrue(resolved.matches(".*Time (59|60)s.*"));
  }

  @Test
  void shouldFallbackWhenValuesMissing() {
    EventInstanceId instanceId = EventInstanceId.generate();
    EventInstance instance =
        EventInstance.create(instanceId, new EventDefinitionId("simple_event"));

    EventRuntimeState state = new EventRuntimeState(instanceId);
    String template = "%event% %phase% %location% %hits%/%max_hits% %time_remaining%";
    String resolved = EventDisplayPlaceholders.resolve(template, instance, state);

    assertEquals("simple_event active unknown 0/0 0", resolved);
  }
}
