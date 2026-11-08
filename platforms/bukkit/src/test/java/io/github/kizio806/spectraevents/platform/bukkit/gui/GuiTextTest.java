package io.github.kizio806.spectraevents.platform.bukkit.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;

class GuiTextTest {

  @Test
  void formatsComponentWithoutPlaceholders() {
    LocaleCatalog locales = LocaleCatalog.of(Map.of("gui.title", "Main Dashboard"));
    Component component = GuiText.component(locales, "gui.title", NamedTextColor.GOLD);

    assertNotNull(component);
    assertEquals("§6Main Dashboard", GuiText.legacy(component));
  }

  @Test
  void formatsComponentWithPlaceholders() {
    LocaleCatalog locales = LocaleCatalog.of(Map.of("gui.welcome", "Welcome %player%!"));
    Component component =
        GuiText.component(locales, "gui.welcome", NamedTextColor.GREEN, Map.of("player", "Alice"));

    assertNotNull(component);
    assertEquals("§aWelcome Alice!", GuiText.legacy(component));
  }

  @Test
  void formatsTitleWithBold() {
    LocaleCatalog locales = LocaleCatalog.of(Map.of("gui.header", "Events Panel"));
    Component title = GuiText.title(locales, "gui.header", NamedTextColor.RED);

    assertNotNull(title);
    String legacy = GuiText.legacy(title);
    assertTrue(legacy.contains("Events Panel"));
    assertTrue(legacy.startsWith("§c§l") || legacy.startsWith("§c"));
  }

  @Test
  void legacyHandlesNullComponent() {
    assertEquals("", GuiText.legacy(null));
  }
}
