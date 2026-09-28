package io.github.kizio806.spectraevents.platform.paper.config;

import io.github.kizio806.spectraevents.application.execution.EventLocation;
import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/** Durable Paper-owned scalar overrides and named event locations. */
public final class PaperEventSettingsStore {
  private final File file;
  private final YamlConfiguration yaml;

  public PaperEventSettingsStore(JavaPlugin plugin) {
    Objects.requireNonNull(plugin, "plugin");
    archiveLegacySettingsFile(plugin.getDataFolder().toPath());
    this.file = new File(plugin.getDataFolder(), "config.yml");
    this.yaml = YamlConfiguration.loadConfiguration(file);
    initializeSchema();
  }

  /** Returns only explicitly saved overrides; YAML owns all defaults. */
  public Map<String, Object> overridesFor(String definitionId) {
    validateDefinitionId(definitionId);
    Map<String, Object> settings = new LinkedHashMap<>();
    copySection("overrides." + definitionId, settings);
    return Map.copyOf(settings);
  }

  public void setParameter(String definitionId, String parameter, String value) {
    validateDefinitionId(definitionId);
    String normalizedParameter = normalizeParameter(parameter);
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Value is required.");
    }
    yaml.set("overrides." + definitionId + "." + normalizedParameter, value.trim());
    save();
  }

  public void removeParameter(String definitionId, String parameter) {
    validateDefinitionId(definitionId);
    yaml.set("overrides." + definitionId + "." + normalizeParameter(parameter), null);
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
    for (String key : section.getKeys(false)) {
      if (section.isConfigurationSection(key)) {
        continue;
      }
      target.put(key, section.get(key));
    }
  }

  private String normalizeParameter(String parameter) {
    String normalized = Objects.requireNonNull(parameter, "parameter").toLowerCase(Locale.ROOT);
    if (!normalized.matches("[a-z][a-z0-9-]{0,63}")) {
      throw new IllegalArgumentException("Setting name is invalid.");
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
      Path target = file.toPath();
      Files.createDirectories(target.getParent());
      Path temporary = Files.createTempFile(target.getParent(), "config", ".yml.tmp");
      try {
        yaml.save(temporary.toFile());
        moveAtomically(temporary, target);
      } finally {
        Files.deleteIfExists(temporary);
      }
    } catch (IOException exception) {
      throw new IllegalStateException("Could not save SpectraEvents configuration.", exception);
    }
  }

  private void initializeSchema() {
    if (yaml.contains("schema-version")) {
      return;
    }
    yaml.set("schema-version", 1);
    if (!yaml.isConfigurationSection("overrides")) {
      yaml.createSection("overrides");
    }
    if (!yaml.isConfigurationSection("locations")) {
      yaml.createSection("locations");
    }
    save();
  }

  private void archiveLegacySettingsFile(Path dataDirectory) {
    Path legacy = dataDirectory.resolve("event-settings.yml");
    if (!Files.isRegularFile(legacy)) {
      return;
    }
    try {
      String timestamp =
          ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
      Path backup =
          dataDirectory.resolve("backups").resolve(timestamp).resolve("event-settings.yml");
      Files.createDirectories(backup.getParent());
      moveAtomically(legacy, backup);
    } catch (IOException exception) {
      throw new IllegalStateException("Could not preserve legacy event-settings.yml", exception);
    }
  }

  private void moveAtomically(Path source, Path target) throws IOException {
    try {
      Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException exception) {
      Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
    }
  }
}
