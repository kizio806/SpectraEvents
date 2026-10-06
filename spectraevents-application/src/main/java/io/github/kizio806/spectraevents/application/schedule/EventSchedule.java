package io.github.kizio806.spectraevents.application.schedule;

import io.github.kizio806.spectraevents.application.execution.EventLocation;
import java.time.ZoneId;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/** Immutable, validated schedule entry loaded from {@code schedules.yml}. */
public record EventSchedule(
    String id,
    String definitionId,
    CronExpression cron,
    ZoneId zoneId,
    EventLocation location,
    Map<String, Object> parameterOverrides) {
  private static final Pattern ID = Pattern.compile("[a-z][a-z0-9-]{0,63}");

  public EventSchedule {
    if (!ID.matcher(Objects.requireNonNull(id, "id")).matches()) {
      throw new IllegalArgumentException("Schedule ID must use lowercase kebab-case");
    }
    if (!ID.matcher(Objects.requireNonNull(definitionId, "definitionId")).matches()) {
      throw new IllegalArgumentException("Definition ID must use lowercase kebab-case");
    }
    Objects.requireNonNull(cron, "cron");
    Objects.requireNonNull(zoneId, "zoneId");
    Objects.requireNonNull(location, "location");
    parameterOverrides =
        Map.copyOf(Objects.requireNonNull(parameterOverrides, "parameterOverrides"));
  }

  public EventSchedule(
      String id, String definitionId, CronExpression cron, ZoneId zoneId, EventLocation location) {
    this(id, definitionId, cron, zoneId, location, Map.of());
  }

  public static EventSchedule parse(
      String id, String definitionId, String cron, String timeZone, EventLocation location) {
    return new EventSchedule(
        id, definitionId, CronExpression.parse(cron), ZoneId.of(timeZone), location);
  }

  public static EventSchedule parse(
      String id,
      String definitionId,
      String cron,
      String timeZone,
      EventLocation location,
      Map<String, Object> parameterOverrides) {
    return new EventSchedule(
        id,
        definitionId,
        CronExpression.parse(cron),
        ZoneId.of(timeZone),
        location,
        parameterOverrides);
  }
}
