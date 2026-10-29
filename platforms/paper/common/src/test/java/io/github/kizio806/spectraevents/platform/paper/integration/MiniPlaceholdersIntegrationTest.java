package io.github.kizio806.spectraevents.platform.paper.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

class MiniPlaceholdersIntegrationTest {
  @Test
  void retainsStandardMiniMessageFormattingWithoutTheOptionalPlugin() {
    var component =
        MiniPlaceholdersIntegration.createMiniMessage(TagResolver.empty())
            .deserialize("<gold><bold>Spectra</bold></gold>");

    assertEquals("Spectra", PlainTextComponentSerializer.plainText().serialize(component));
    assertEquals(TextDecoration.State.TRUE, component.decoration(TextDecoration.BOLD));
  }
}
