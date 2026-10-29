package io.github.kizio806.spectraevents.application.template;

import java.util.List;

/** Stable identifiers for the official event bundles embedded in every distribution. */
public final class OfficialEventBundles {
  private static final List<String> IDS =
      List.of("airdrop", "boss-portal", "meteor", "metin", "pinata");

  private OfficialEventBundles() {}

  public static List<String> ids() {
    return IDS;
  }

  public static String resourcePath(String id) {
    if (!IDS.contains(id)) {
      throw new IllegalArgumentException("Unknown official event bundle: " + id);
    }
    return "templates/" + id + ".spectra.zip";
  }
}
