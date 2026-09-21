package io.github.kizio806.spectraevents.application.asset.delivery;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Explicit administrator-owned delivery settings for a verified externally hosted local ZIP. */
public record ResourcePackDeliverySettings(
    boolean enabled, boolean required, String url, String sha1, String prompt) {
  public static final String DEFAULT_PROMPT =
      "<yellow>Server resources are required for SpectraEvents.</yellow>";

  public ResourcePackDeliverySettings {
    url = url == null ? "" : url.trim();
    sha1 = sha1 == null ? "" : sha1.trim().toLowerCase(java.util.Locale.ROOT);
    prompt = prompt == null || prompt.isBlank() ? DEFAULT_PROMPT : prompt;
    if (enabled) {
      URI uri = URI.create(url);
      if (!"https".equalsIgnoreCase(uri.getScheme())
          || uri.getHost() == null
          || uri.getUserInfo() != null
          || uri.getFragment() != null
          || !uri.getPath().endsWith(".zip")) {
        throw new IllegalArgumentException(
            "Enabled resource-pack delivery requires a host-based HTTPS .zip URL");
      }
      if (!sha1.matches("[0-9a-f]{40}")) {
        throw new IllegalArgumentException(
            "Enabled resource-pack delivery requires a 40-character SHA-1");
      }
    }
  }

  public static ResourcePackDeliverySettings disabled() {
    return new ResourcePackDeliverySettings(false, false, "", "", DEFAULT_PROMPT);
  }

  public String sourceConfigId() {
    return sha256(enabled + "\\n" + required + "\\n" + url + "\\n" + sha1 + "\\n" + prompt);
  }

  private static String sha256(String value) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder result = new StringBuilder(digest.length * 2);
      for (byte part : digest) {
        result.append(String.format(java.util.Locale.ROOT, "%02x", part));
      }
      return result.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is required by the Java runtime", exception);
    }
  }
}
