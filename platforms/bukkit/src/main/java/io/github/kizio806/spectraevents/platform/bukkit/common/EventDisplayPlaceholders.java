package io.github.kizio806.spectraevents.platform.bukkit.common;

import io.github.kizio806.spectraevents.application.execution.EventLocation;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;

/** Resolves safe runtime placeholders for Bukkit/Paper/Spigot displays and templates. */
public final class EventDisplayPlaceholders {
  private EventDisplayPlaceholders() {}

  public static String resolve(String template, EventInstance instance, EventRuntimeState state) {
    if (template == null) {
      return "";
    }
    int hp = state != null ? state.currentHealth() : 0;
    int maxHp = state != null ? Math.max(1, state.maxHealth()) : 1;
    int healthPercent = (int) (((double) hp / maxHp) * 100);
    int hits = state != null && state.hitCounter() != null ? state.hitCounter().current() : 0;
    int maxHits = state != null && state.hitCounter() != null ? state.hitCounter().maximum() : 0;
    long deadline = state != null ? state.timerDeadlineMillis() : 0L;
    long remainingSeconds =
        deadline == 0L ? 0L : Math.max(0L, (deadline - System.currentTimeMillis()) / 1_000L);
    String location =
        state != null
            ? state
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
                .orElse("unknown")
            : "unknown";
    String phase =
        instance != null ? instance.currentPhase().map(p -> p.value()).orElse("active") : "active";
    String event = instance != null ? instance.definitionId().value() : "unknown";

    return template
        .replace("%health%", String.valueOf(hp))
        .replace("%max_health%", String.valueOf(maxHp))
        .replace("%health_percent%", String.valueOf(healthPercent))
        .replace("%hits%", String.valueOf(hits))
        .replace("%max_hits%", String.valueOf(maxHits))
        .replace("%time_remaining%", String.valueOf(remainingSeconds))
        .replace("%location%", location)
        .replace("%phase%", phase)
        .replace("%event%", event);
  }
}
