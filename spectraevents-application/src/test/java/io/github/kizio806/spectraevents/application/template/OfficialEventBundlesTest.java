package io.github.kizio806.spectraevents.application.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OfficialEventBundlesTest {

  @Test
  void testIdsAndResourcePath() {
    assertFalse(OfficialEventBundles.ids().isEmpty());
    assertTrue(OfficialEventBundles.ids().contains("metin"));

    assertEquals("templates/metin.spectra.zip", OfficialEventBundles.resourcePath("metin"));
    assertEquals("templates/meteor.spectra.zip", OfficialEventBundles.resourcePath("meteor"));

    assertThrows(
        IllegalArgumentException.class, () -> OfficialEventBundles.resourcePath("unknown"));
  }
}
