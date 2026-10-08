package io.github.kizio806.spectraevents.application.config.locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemLocaleLoaderTest {
  private static final List<String> SHIPPED_EVENT_FILES =
      List.of("airdrop", "boss-portal", "meteor", "metin", "pinata");
  private static final Pattern PLAYER_FACING_ACTION_FIELD =
      Pattern.compile("(?m)^\\s*(?:message|title|subtitle|name):\\s+\\\"([^\\\"]+)\\\"");
  private static final String LOCALE_REFERENCE_PREFIX = "i18n:";

  @TempDir Path temporaryDirectory;

  @Test
  void extractsAllSupportedLocalesWithoutOverwritingAnOperatorTranslation() throws Exception {
    FileSystemLocaleLoader loader = new FileSystemLocaleLoader(temporaryDirectory);
    Files.createDirectories(loader.localesDirectory());
    Path polish = loader.localesDirectory().resolve("pl-PL.yml");
    Files.writeString(polish, "schema-version: 1\nmessages: { custom: zachowaj }\n");

    loader.ensureBundledLocales();

    assertEquals("schema-version: 1\nmessages: { custom: zachowaj }\n", Files.readString(polish));
    assertTrue(Files.isRegularFile(loader.localesDirectory().resolve("en-US.yml")));
    assertTrue(Files.isRegularFile(loader.localesDirectory().resolve("de-DE.yml")));
    assertTrue(Files.isRegularFile(loader.localesDirectory().resolve("es-ES.yml")));
    assertTrue(Files.isRegularFile(loader.localesDirectory().resolve("fr-FR.yml")));
    assertTrue(Files.isRegularFile(loader.localesDirectory().resolve("pt-BR.yml")));
  }

  @Test
  void providesBundledEnglishForKeysMissingFromAnOperatorLocale() throws Exception {
    FileSystemLocaleLoader loader = new FileSystemLocaleLoader(temporaryDirectory);
    Files.createDirectories(loader.localesDirectory());
    Files.writeString(
        loader.localesDirectory().resolve("pl-PL.yml"),
        "schema-version: 1\nmessages: { reward-delivered: Nagroda odebrana. }\n");

    LocaleCatalog catalog = loader.loadCatalog("pl-PL");

    assertEquals("Nagroda odebrana.", catalog.message("messages.reward-delivered"));
    assertEquals(
        "Wymagany pakiet zasobów SpectraEvents nie został wczytany.",
        catalog.message("messages.resource-pack-required"));
  }

  @Test
  void shipsACompleteTranslationForEverySupportedLocale() throws Exception {
    FileSystemLocaleLoader loader = new FileSystemLocaleLoader(temporaryDirectory);
    var englishKeys = loader.loadCatalog("en-US").localKeys();

    for (String locale : List.of("de-DE", "es-ES", "fr-FR", "pl-PL", "pt-BR")) {
      assertEquals(englishKeys, loader.loadCatalog(locale).localKeys(), locale);
    }
  }

  @Test
  void shipsOnlyLocalizedPlayerFacingTextInEveryOfficialEvent() throws Exception {
    FileSystemLocaleLoader loader = new FileSystemLocaleLoader(temporaryDirectory);
    List<String> localeReferences =
        SHIPPED_EVENT_FILES.stream()
            .flatMap(event -> playerFacingLocaleReferences(event).stream())
            .toList();

    assertTrue(!localeReferences.isEmpty(), "Official events must contain player-facing actions");
    for (String reference : localeReferences) {
      assertTrue(reference.startsWith(LOCALE_REFERENCE_PREFIX), reference);
      String key = reference.substring(LOCALE_REFERENCE_PREFIX.length());
      for (String locale : List.of("en-US", "de-DE", "es-ES", "fr-FR", "pl-PL", "pt-BR")) {
        assertTrue(
            !loader.loadCatalog(locale).message(key).startsWith("[missing locale:"),
            () -> locale + " is missing " + key);
      }
    }
  }

  private List<String> playerFacingLocaleReferences(String event) {
    String source = readResource("/events/" + event + ".yml");
    Matcher matcher = PLAYER_FACING_ACTION_FIELD.matcher(source);
    java.util.ArrayList<String> values = new java.util.ArrayList<>();
    while (matcher.find()) {
      values.add(matcher.group(1));
    }
    return values;
  }

  private String readResource(String resource) {
    try (InputStream input = getClass().getResourceAsStream(resource)) {
      if (input == null) {
        throw new IllegalStateException("Missing test resource " + resource);
      }
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException exception) {
      throw new IllegalStateException("Could not read test resource " + resource, exception);
    }
  }
}
