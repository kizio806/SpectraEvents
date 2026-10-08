package io.github.kizio806.spectraevents.platform.paper.command;

import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/** Converts command locale entries to Adventure text at the Paper boundary. */
final class CommandText {
  private final LocaleCatalog locales;

  CommandText(LocaleCatalog locales) {
    this.locales = locales;
  }

  String message(String key) {
    return locales.message(key);
  }

  String message(String key, Map<String, ?> placeholders) {
    return locales.message(key, placeholders);
  }

  Component component(String key, NamedTextColor color) {
    return Component.text(locales.message(key), color);
  }

  Component component(String key, NamedTextColor color, Map<String, ?> placeholders) {
    return Component.text(locales.message(key, placeholders), color);
  }
}
