package io.github.kizio806.spectraevents.platform.paper.config;

import io.github.kizio806.spectraevents.application.execution.EventLocation;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/** Durable Paper-owned operator overrides and named event locations. */
public final class PaperEventSettingsStore {
  public static final String EASY = "easy";
  public static final String NORMAL = "normal";
  public static final String HARD = "hard";
  private static final java.util.Set<String> PROFILES = java.util.Set.of(EASY, NORMAL, HARD);
  private static final java.util.Set<String> PARAMETERS =
      java.util.Set.of("health", "damage", "hits", "duration", "lock-duration");

  private final File file;
  private final YamlConfiguration yaml;

  public PaperEventSettingsStore(JavaPlugin plugin) {
    Objects.requireNonNull(plugin, "plugin");
    this.file = new File(plugin.getDataFolder(), "event-settings.yml");
    this.yaml = YamlConfiguration.loadConfiguration(file);
  }

  public Map<String, Object> settingsFor(String definitionId, String profile) {
    validateDefinitionId(definitionId);
    String normalizedProfile = normalizeProfile(profile);
    Map<String, Object> settings = new LinkedHashMap<>();
    settings.putAll(builtInProfile(definitionId, normalizedProfile));
    copySection("profiles." + normalizedProfile + ".defaults", settings);
    copySection("profiles." + normalizedProfile + ".events." + definitionId, settings);
    copySection("events." + definitionId, settings);
    return Map.copyOf(settings);
  }

  public void setProfile(String definitionId, String profile) {
    validateDefinitionId(definitionId);
    yaml.set("events." + definitionId + ".profile", normalizeProfile(profile));
    save();
  }

  public String profileFor(String definitionId) {
    validateDefinitionId(definitionId);
    return normalizeProfile(yaml.getString("events." + definitionId + ".profile", NORMAL));
  }

  public void setParameter(String definitionId, String parameter, String value) {
    validateDefinitionId(definitionId);
    String normalizedParameter = normalizeParameter(parameter);
    Object parsed = parseParameter(normalizedParameter, value);
    yaml.set("events." + definitionId + "." + normalizedParameter, parsed);
    save();
  }

  public void saveLocation(String name, EventLocation location) {
    String normalizedName = normalizeLocationName(name);
    yaml.set("locations." + normalizedName, location.serialize());
    save();
  }

  public Optional<EventLocation> location(String name) {
    return EventLocation.deserialize(yaml.getString("locations." + normalizeLocationName(name)));
  }

  public Map<String, EventLocation> locations() {
    ConfigurationSection section = yaml.getConfigurationSection("locations");
    if (section == null) {
      return Map.of();
    }
    Map<String, EventLocation> locations = new LinkedHashMap<>();
    for (String key : section.getKeys(false)) {
      EventLocation.deserialize(section.getString(key))
          .ifPresent(location -> locations.put(key, location));
    }
    return Map.copyOf(locations);
  }

  public boolean removeLocation(String name) {
    String normalizedName = normalizeLocationName(name);
    String path = "locations." + normalizedName;
    if (!yaml.contains(path)) {
      return false;
    }
    yaml.set(path, null);
    save();
    return true;
  }

  private void copySection(String path, Map<String, Object> target) {
    ConfigurationSection section = yaml.getConfigurationSection(path);
    if (section == null) {
      return;
    }
    for (String key : PARAMETERS) {
      if (section.contains(key)) {
        target.put(key, section.get(key));
      }
    }
  }

  /**
   * Provides usable production defaults even before an operator creates event-settings.yml. YAML
   * profile sections intentionally override these values.
   */
  private Map<String, Object> builtInProfile(String definitionId, String profile) {
    Map<String, Object> values = new LinkedHashMap<>();
    switch (definitionId) {
      case "meteor" -> {
        values.put("health", profileValue(profile, 300, 500, 900));
        values.put("damage", profileValue(profile, 15, 10, 7));
        values.put("lock-duration", profileDuration(profile, "8s", "12s", "18s"));
      }
      case "airdrop" -> values.put("lock-duration", profileDuration(profile, "20s", "35s", "50s"));
      case "metin" -> {
        values.put("health", profileValue(profile, 600, 1_000, 1_600));
        values.put("damage", profileValue(profile, 15, 10, 7));
      }
      case "pinata" -> values.put("hits", profileValue(profile, 12, 20, 35));
      case "boss_portal" -> values.put("duration", profileDuration(profile, "90s", "120s", "180s"));
      default -> {
        // Definitions supplied by operators begin with their own YAML values.
      }
    }
    return values;
  }

  private int profileValue(String profile, int easy, int normal, int hard) {
    return switch (profile) {
      case EASY -> easy;
      case HARD -> hard;
      default -> normal;
    };
  }

  private String profileDuration(String profile, String easy, String normal, String hard) {
    return switch (profile) {
      case EASY -> easy;
      case HARD -> hard;
      default -> normal;
    };
  }

  private Object parseParameter(String parameter, String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Value is required.");
    }
    if ("duration".equals(parameter) || "lock-duration".equals(parameter)) {
      if (!value.matches("[1-9][0-9]{0,5}(ms|s|m|h)")) {
        throw new IllegalArgumentException("Duration must look like 30s, 10m, or 1h.");
      }
      return value;
    }
    try {
      int parsed = Integer.parseInt(value);
      if (parsed < 1 || parsed > 1_000_000) {
        throw new IllegalArgumentException("Value must be between 1 and 1000000.");
      }
      return parsed;
    } catch (NumberFormatException exception) {
      throw new IllegalArgumentException("Value must be a whole number.");
    }
  }

  private String normalizeProfile(String profile) {
    String normalized = Objects.requireNonNull(profile, "profile").toLowerCase(Locale.ROOT);
    if (!PROFILES.contains(normalized)) {
      throw new IllegalArgumentException("Profile must be easy, normal, or hard.");
    }
    return normalized;
  }

  private String normalizeParameter(String parameter) {
    String normalized = Objects.requireNonNull(parameter, "parameter").toLowerCase(Locale.ROOT);
    if (!PARAMETERS.contains(normalized)) {
      throw new IllegalArgumentException(
          "Setting must be health, damage, hits, duration, or lock-duration.");
    }
    return normalized;
  }

  private String normalizeLocationName(String name) {
    String normalized = Objects.requireNonNull(name, "name").toLowerCase(Locale.ROOT);
    if (!normalized.matches("[a-z0-9_-]{1,32}")) {
      throw new IllegalArgumentException(
          "Location name must use 1-32 lowercase letters, numbers, _ or -.");
    }
    return normalized;
  }

  private void validateDefinitionId(String definitionId) {
    if (definitionId == null || !definitionId.matches("[a-z0-9_-]{1,64}")) {
      throw new IllegalArgumentException("Event ID is invalid.");
    }
  }

  private void save() {
    try {
      yaml.save(file);
    } catch (IOException exception) {
      throw new IllegalStateException("Could not save event settings.", exception);
    }
  }
}
