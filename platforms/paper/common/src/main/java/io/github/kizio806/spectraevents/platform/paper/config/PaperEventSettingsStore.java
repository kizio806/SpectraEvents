package io.github.kizio806.spectraevents.platform.paper.config;

import io.github.kizio806.spectraevents.application.config.DataDirectoryLayout;
import io.github.kizio806.spectraevents.application.execution.EventLocation;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Persists global settings, named locations and GUI overrides in separate operator-facing files.
 */
public final class PaperEventSettingsStore {
  private final Path configFile;
  private final Path locationsFile;
  private final Path overridesDirectory;
  private final YamlConfiguration config;
  private final YamlConfiguration locations;

  public PaperEventSettingsStore(JavaPlugin plugin) {
    this(Objects.requireNonNull(plugin, "plugin").getDataFolder().toPath());
  }

  PaperEventSettingsStore(Path dataDirectory) {
    try {
      DataDirectoryLayout layout = DataDirectoryLayout.prepare(dataDirectory);
      this.configFile = layout.configFile();
      this.locationsFile = layout.locationsFile();
      this.overridesDirectory = layout.eventOverridesDirectory();
      this.config = YamlConfiguration.loadConfiguration(configFile.toFile());
      this.locations = YamlConfiguration.loadConfiguration(locationsFile.toFile());
      initializeGlobalConfiguration();
    } catch (IOException exception) {
      throw new IllegalStateException("Could not prepare SpectraEvents settings.", exception);
    }
  }

  public String locale() {
    return config.getString("locale", "en-US");
  }

  /** Returns only explicit overrides; the event definition owns the default values. */
  public Map<String, Object> overridesFor(String definitionId) {
    validateDefinitionId(definitionId);
    ConfigurationSection parameters =
        overrideConfiguration(definitionId).getConfigurationSection("parameters");
    if (parameters == null) {
      return Map.of();
    }
    Map<String, Object> result = new LinkedHashMap<>();
    for (String key : parameters.getKeys(false)) {
      if (!parameters.isConfigurationSection(key)) {
        result.put(key, parameters.get(key));
      }
    }
    return Map.copyOf(result);
  }

  public void setParameter(String definitionId, String parameter, String value) {
    validateDefinitionId(definitionId);
    String normalizedParameter = normalizeParameter(parameter);
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Value is required.");
    }
    YamlConfiguration override = overrideConfiguration(definitionId);
    configureOverrideHeader(override, definitionId);
    override.set("schema-version", 1);
    override.set("parameters." + normalizedParameter, value.trim());
    save(override, overrideFile(definitionId));
  }

  public void removeParameter(String definitionId, String parameter) {
    validateDefinitionId(definitionId);
    YamlConfiguration override = overrideConfiguration(definitionId);
    configureOverrideHeader(override, definitionId);
    override.set("parameters." + normalizeParameter(parameter), null);
    save(override, overrideFile(definitionId));
  }

  public void saveLocation(String name, EventLocation location) {
    String normalizedName = normalizeLocationName(name);
    locations.set("schema-version", 1);
    locations.set("locations." + normalizedName, location.serialize());
    save(locations, locationsFile);
  }

  public Optional<EventLocation> location(String name) {
    return EventLocation.deserialize(
        locations.getString("locations." + normalizeLocationName(name)));
  }

  public Map<String, EventLocation> locations() {
    ConfigurationSection section = locations.getConfigurationSection("locations");
    if (section == null) {
      return Map.of();
    }
    Map<String, EventLocation> result = new LinkedHashMap<>();
    for (String key : section.getKeys(false)) {
      EventLocation.deserialize(section.getString(key)).ifPresent(value -> result.put(key, value));
    }
    return Map.copyOf(result);
  }

  public boolean removeLocation(String name) {
    String normalizedName = normalizeLocationName(name);
    String path = "locations." + normalizedName;
    if (!locations.contains(path)) {
      return false;
    }
    locations.set(path, null);
    save(locations, locationsFile);
    return true;
  }

  private void initializeGlobalConfiguration() {
    boolean changed = false;
    if (!config.isInt("schema-version") || config.getInt("schema-version") < 2) {
      config.set("schema-version", 2);
      changed = true;
    }
    if (config.getString("locale") == null || config.getString("locale").isBlank()) {
      config.set("locale", "en-US");
      changed = true;
    }
    if (changed) {
      save(config, configFile);
    }
  }

  private YamlConfiguration overrideConfiguration(String definitionId) {
    return YamlConfiguration.loadConfiguration(overrideFile(definitionId).toFile());
  }

  private Path overrideFile(String definitionId) {
    return overridesDirectory.resolve(definitionId + ".yml");
  }

  private void configureOverrideHeader(YamlConfiguration override, String definitionId) {
    override
        .options()
        .setHeader(
            List.of(
                "=============================================================================",
                "SpectraEvents — Admin Override: " + definitionId,
                "=============================================================================",
                "This file contains only values changed through the admin panel or command.",
                "The full event definition remains in ../" + definitionId + ".yml.",
                "Delete one parameter to return to its YAML default; do not add event phases here."));
  }

  private void save(YamlConfiguration yaml, Path target) {
    try {
      Files.createDirectories(target.getParent());
      Path temporary =
          Files.createTempFile(target.getParent(), target.getFileName().toString(), ".tmp");
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

  private void validateDefinitionId(String definitionId) {
    if (definitionId == null || !definitionId.matches("[a-z0-9_-]{1,64}")) {
      throw new IllegalArgumentException("Event ID is invalid.");
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

  private static void moveAtomically(Path source, Path target) throws IOException {
    try {
      Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException exception) {
      Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
    }
  }
}
