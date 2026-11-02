package io.github.kizio806.spectraevents.application.config.locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class LocaleCatalogTest {

  @Test
  void testCatalogLookupAndPlaceholders() {
    LocaleCatalog catalog =
        new LocaleCatalog(
            "pl-PL",
            Map.of("hello", "Hello %name%!", "fallback", "Fallback message"),
            Map.of("hello", "Cześć %name%!"));

    assertEquals("pl-PL", catalog.selectedLocale());
    assertEquals("Cześć John!", catalog.message("hello", Map.of("name", "John")));
    assertEquals("Fallback message", catalog.message("fallback"));
    assertEquals("[missing locale: unknown]", catalog.message("unknown"));
    assertEquals("Cześć John!", catalog.resolveTemplate("i18n:hello").replace("%name%", "John"));
    assertEquals("Literal text", catalog.resolveTemplate("Literal text"));
    assertEquals("en-US", LocaleCatalog.englishLocale());
    assertTrue(catalog.localKeys().contains("hello"));

    LocaleCatalog simple = LocaleCatalog.of(Map.of("key", "val"));
    assertEquals("en-US", simple.selectedLocale());
    assertEquals("val", simple.message("key"));

    assertThrows(NullPointerException.class, () -> catalog.message(null));
    assertThrows(NullPointerException.class, () -> catalog.resolveTemplate(null));
  }
}
