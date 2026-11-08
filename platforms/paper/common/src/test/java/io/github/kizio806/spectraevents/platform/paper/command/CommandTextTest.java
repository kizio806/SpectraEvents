package io.github.kizio806.spectraevents.platform.paper.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;

class CommandTextTest {

  @Test
  void resolvesMessagesAndComponents() {
    LocaleCatalog locales =
        LocaleCatalog.of(
            Map.of(
                "cmd.success", "Operation successful",
                "cmd.greet", "Hello %name%"));

    CommandText text = new CommandText(locales);

    assertEquals("Operation successful", text.message("cmd.success"));
    assertEquals("Hello Bob", text.message("cmd.greet", Map.of("name", "Bob")));

    Component comp1 = text.component("cmd.success", NamedTextColor.GREEN);
    assertNotNull(comp1);
    assertEquals(Component.text("Operation successful", NamedTextColor.GREEN), comp1);

    Component comp2 = text.component("cmd.greet", NamedTextColor.AQUA, Map.of("name", "Bob"));
    assertNotNull(comp2);
    assertEquals(Component.text("Hello Bob", NamedTextColor.AQUA), comp2);
  }
}
