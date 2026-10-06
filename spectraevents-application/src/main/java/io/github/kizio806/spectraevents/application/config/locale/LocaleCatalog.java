package io.github.kizio806.spectraevents.application.config.locale;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Immutable locale catalog with an English fallback for every plugin-owned message. */
public final class LocaleCatalog {
  private static final String ENGLISH = "en-US";

  private final String selectedLocale;
  private final Map<String, String> english;
  private final Map<String, String> selected;

  LocaleCatalog(String selectedLocale, Map<String, String> english, Map<String, String> selected) {
    this.selectedLocale = Objects.requireNonNull(selectedLocale, "selectedLocale");
    this.english = Map.copyOf(new LinkedHashMap<>(english));
    this.selected = Map.copyOf(new LinkedHashMap<>(selected));
  }

  public String selectedLocale() {
    return selectedLocale;
  }

  public String message(String key) {
    Objects.requireNonNull(key, "key");
    return selected.getOrDefault(key, english.getOrDefault(key, "[missing locale: " + key + "]"));
  }

  public String message(String key, Map<String, ?> placeholders) {
    String resolved = message(key);
    for (Map.Entry<String, ?> entry : placeholders.entrySet()) {
      resolved = resolved.replace("%" + entry.getKey() + "%", String.valueOf(entry.getValue()));
    }
    return resolved;
  }

  /** Resolves {@code i18n:key}; literal YAML text remains valid for custom event authors. */
  public String resolveTemplate(String template) {
    Objects.requireNonNull(template, "template");
    if (!template.startsWith("i18n:")) {
      return template;
    }
    return message(template.substring("i18n:".length()));
  }

  static String englishLocale() {
    return ENGLISH;
  }

  Set<String> localKeys() {
    return selected.keySet();
  }
}
