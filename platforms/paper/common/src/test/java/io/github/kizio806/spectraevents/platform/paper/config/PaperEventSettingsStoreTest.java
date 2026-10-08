package io.github.kizio806.spectraevents.platform.paper.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PaperEventSettingsStoreTest {
  @TempDir Path temporaryDirectory;

  @Test
  void writesNewGlobalConfigurationWithEnglishAsTheDefaultLocale() throws Exception {
    PaperEventSettingsStore store = new PaperEventSettingsStore(temporaryDirectory);

    assertEquals("en-US", store.locale());
    assertFalse(Files.readString(temporaryDirectory.resolve("config.yml")).contains("overrides:"));
  }

  @Test
  void writesOnlyTheSelectedEventOverride() throws Exception {
    PaperEventSettingsStore store = new PaperEventSettingsStore(temporaryDirectory);

    store.setParameter("airdrop", "announcement-delay", "16m");

    assertEquals("16m", store.overridesFor("airdrop").get("announcement-delay"));
    assertFalse(Files.readString(temporaryDirectory.resolve("config.yml")).contains("airdrop"));
    assertTrue(
        Files.readString(temporaryDirectory.resolve("events/overrides/airdrop.yml"))
            .contains("Admin Override: airdrop"));
  }
}
