package io.github.kizio806.spectraevents.application.asset.delivery;

import io.github.kizio806.spectraevents.application.asset.AssetTargetProfile;
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
  }
}
