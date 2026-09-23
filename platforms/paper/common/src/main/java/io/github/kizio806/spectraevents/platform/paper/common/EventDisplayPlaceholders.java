package io.github.kizio806.spectraevents.platform.paper.common;

import io.github.kizio806.spectraevents.application.execution.EventLocation;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;

/** Resolves the same safe runtime placeholders for Paper boss bars and scoreboards. */
public final class EventDisplayPlaceholders {
  private EventDisplayPlaceholders() {}

  public static String resolve(String template, EventInstance instance, EventRuntimeState state) {
    int hp = state.currentHealth();
    int maxHp = Math.max(1, state.maxHealth());
    int healthPercent = (int) (((double) hp / maxHp) * 100);
    int hits = state.hitCounter().map(counter -> counter.current()).orElse(0);
    int maxHits = state.hitCounter().map(counter -> counter.maximum()).orElse(0);
    long deadline = state.timerDeadlineMillis();
    long remainingSeconds =
        deadline == 0L ? 0L : Math.max(0L, (deadline - System.currentTimeMillis()) / 1_000L);
    String location =
        state
            .platformLocation()
            .filter(EventLocation.class::isInstance)
            .map(EventLocation.class::cast)
            .map(
                anchor ->
                    anchor.world()
                        + " "
                        + Math.round(anchor.x())
                        + ", "
                        + Math.round(anchor.y())
                        + ", "
                        + Math.round(anchor.z()))
            .orElse("unknown");
    return template
        .replace("%health%", String.valueOf(hp))
        .replace("%max_health%", String.valueOf(maxHp))
        .replace("%health_percent%", String.valueOf(healthPercent))
        .replace("%hits%", String.valueOf(hits))
        .replace("%max_hits%", String.valueOf(maxHits))
        .replace("%time_remaining%", String.valueOf(remainingSeconds))
        .replace("%location%", location)
        .replace("%phase%", instance.currentPhase().map(phase -> phase.value()).orElse("active"))
        .replace("%event%", instance.definitionId().value());
  }
}
