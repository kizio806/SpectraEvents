package io.github.kizio806.spectraevents.platform.bukkit.gui;

import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

/** Converts catalog entries and Adventure components at the Bukkit UI boundary. */
final class GuiText {
  private GuiText() {}

  static Component component(LocaleCatalog locales, String key, NamedTextColor color) {
    return Component.text(locales.message(key), color);
  }

  static Component component(
      LocaleCatalog locales, String key, NamedTextColor color, Map<String, ?> placeholders) {
    return Component.text(locales.message(key, placeholders), color);
  }

  static Component title(LocaleCatalog locales, String key, NamedTextColor color) {
    return Component.text(locales.message(key), color, TextDecoration.BOLD);
  }

  static String legacy(Component component) {
    if (component == null) {
      return "";
    }
    return LegacyComponentSerializer.legacySection().serialize(component);
  }

  static void send(Player player, Component component) {
    player.sendMessage(legacy(component));
  }
}
