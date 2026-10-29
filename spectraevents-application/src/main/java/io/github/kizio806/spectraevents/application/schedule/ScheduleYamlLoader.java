package io.github.kizio806.spectraevents.application.schedule;

import io.github.kizio806.spectraevents.application.execution.EventLocation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/** Strict parser for the standalone, versioned {@code schedules.yml} file. */
public final class ScheduleYamlLoader {
  public List<EventSchedule> load(String source, String fileName) {
    ScheduleLoadResult result = loadResilient(source, fileName);
    if (!result.failures().isEmpty()) {
      ScheduleLoadResult.Failure failure = result.failures().getFirst();
      throw new IllegalArgumentException(failure.path() + " " + failure.message());
    }
    return result.schedules();
  }

  /**
   * Loads all independently valid entries. A malformed sibling is reported with {@code file:path}
   * and does not prevent scheduling the remaining definitions.
   */
  public ScheduleLoadResult loadResilient(String source, String fileName) {
    Object loaded;
    try {
      loaded = new Yaml(new SafeConstructor(new LoaderOptions())).load(source);
    } catch (RuntimeException exception) {
      return failed(
          fileName,
          "root",
          exception.getMessage() == null
              ? "invalid YAML"
              : "invalid YAML: " + exception.getMessage());
    }
    if (!(loaded instanceof Map<?, ?> root)) {
      return failed(fileName, "root", "must be a YAML mapping");
    }
    if (!"1".equals(String.valueOf(root.get("schema-version")))) {
      return failed(fileName, "schema-version", "must be 1");
    }
    if (!(root.get("schedules") instanceof List<?> entries)) {
      return failed(fileName, "schedules", "must be a list");
    }
    List<EventSchedule> schedules = new ArrayList<>();
    List<ScheduleLoadResult.Failure> failures = new ArrayList<>();
    for (int index = 0; index < entries.size(); index++) {
      if (!(entries.get(index) instanceof Map<?, ?> entry)) {
        failures.add(
            new ScheduleLoadResult.Failure(
                fileName + ":schedules[" + index + "]", "must be a mapping"));
        continue;
      }
      String path = "schedules[" + index + "]";
      try {
        schedules.add(
            EventSchedule.parse(
                required(entry, "id", fileName, path),
                required(entry, "definition", fileName, path),
                required(entry, "cron", fileName, path),
                required(entry, "timezone", fileName, path),
                new EventLocation(
                    required(entry, "world", fileName, path),
                    number(entry, "x", fileName, path),
                    number(entry, "y", fileName, path),
                    number(entry, "z", fileName, path),
                    0,
                    0),
                parameterOverrides(entry, fileName, path)));
      } catch (IllegalArgumentException exception) {
        failures.add(new ScheduleLoadResult.Failure(fileName + ":" + path, exception.getMessage()));
      }
    }
    return new ScheduleLoadResult(schedules, failures);
  }

  private static ScheduleLoadResult failed(String fileName, String path, String message) {
    return new ScheduleLoadResult(
        List.of(), List.of(new ScheduleLoadResult.Failure(fileName + ":" + path, message)));
  }

  private static String required(Map<?, ?> entry, String key, String file, String path) {
    Object value = entry.get(key);
    if (value == null || String.valueOf(value).isBlank())
      throw invalid(file, path + "." + key, "is required");
    return String.valueOf(value);
  }

  private static double number(Map<?, ?> entry, String key, String file, String path) {
    Object value = entry.get(key);
    if (!(value instanceof Number number)) throw invalid(file, path + "." + key, "must be numeric");
    return number.doubleValue();
  }

  private static Map<String, Object> parameterOverrides(Map<?, ?> entry, String file, String path) {
    Object raw = entry.get("parameters");
    if (raw == null) {
      return Map.of();
    }
    if (!(raw instanceof Map<?, ?> values)) {
      throw invalid(file, path + ".parameters", "must be a mapping");
    }
    java.util.LinkedHashMap<String, Object> result = new java.util.LinkedHashMap<>();
    values.forEach(
        (key, value) -> {
          if (key == null || String.valueOf(key).isBlank()) {
            throw invalid(file, path + ".parameters", "keys must not be blank");
          }
          if (value instanceof Map<?, ?> || value instanceof List<?>) {
            throw invalid(file, path + ".parameters." + key, "must be a scalar value");
          }
          result.put(String.valueOf(key), value);
        });
    return Map.copyOf(result);
  }

  private static IllegalArgumentException invalid(String file, String path, String message) {
    return new IllegalArgumentException(file + ":" + path + " " + message);
  }
}
