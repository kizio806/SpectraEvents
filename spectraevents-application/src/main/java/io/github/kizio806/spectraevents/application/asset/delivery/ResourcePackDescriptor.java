package io.github.kizio806.spectraevents.application.asset.delivery;

import io.github.kizio806.spectraevents.application.asset.AssetTargetProfile;
import java.net.URI;
import java.util.Objects;

/** A neutral descriptor of a resolved resource pack available for delivery. */
public record ResourcePackDescriptor(
    String id,
    String version,
    String url,
    String sha1,
    String sha512,
    long fileSize,
    AssetTargetProfile profile,
    String source) {

  public ResourcePackDescriptor {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(version, "version");
    Objects.requireNonNull(url, "url");
    Objects.requireNonNull(sha1, "sha1");
    Objects.requireNonNull(profile, "profile");
    Objects.requireNonNull(source, "source");
    if (id.isBlank() || version.isBlank() || source.isBlank()) {
      throw new IllegalArgumentException("id, version, and source must not be blank");
    }
    URI uri = URI.create(url);
    if (!"https".equalsIgnoreCase(uri.getScheme())
        || uri.getHost() == null
        || uri.getUserInfo() != null
        || uri.getFragment() != null) {
      throw new IllegalArgumentException(
          "Resource-pack URL must be a host-based HTTPS URL without user info or fragment");
    }
    if (!sha1.matches("[0-9a-fA-F]{40}")) {
      throw new IllegalArgumentException(
          "Resource-pack SHA-1 must be exactly 40 hexadecimal characters");
    }
    if (sha512 != null && !sha512.matches("[0-9a-fA-F]{128}")) {
      throw new IllegalArgumentException(
          "Resource-pack SHA-512 must be exactly 128 hexadecimal characters");
    }
    if (fileSize < 0L) {
      throw new IllegalArgumentException("Resource-pack fileSize must not be negative");
    }
  }
}
