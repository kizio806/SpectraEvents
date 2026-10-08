package io.github.kizio806.spectraevents.adapter.delivery.manual;

import io.github.kizio806.spectraevents.application.asset.AssetTargetProfile;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackDescriptor;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackSourcePort;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

public final class ManualUrlResourcePackSource implements ResourcePackSourcePort {

  private final String url;
  private final String sha1;

  public ManualUrlResourcePackSource(String url, String sha1) {
    this.url = url != null ? url.trim() : "";
    this.sha1 = sha1 != null ? sha1.trim() : "";
  }

  @Override
  public CompletableFuture<ResourcePackDescriptor> resolve(
      String pluginVersion, AssetTargetProfile profile) {
    return CompletableFuture.supplyAsync(
        () -> {
          if (url.isEmpty()) {
            throw new IllegalArgumentException("Manual source URL is empty");
          }
          URI uri;
          try {
            uri = URI.create(url);
          } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid manual source URL format", e);
          }
          if (!"https".equalsIgnoreCase(uri.getScheme())
              || uri.getHost() == null
              || uri.getUserInfo() != null
              || uri.getFragment() != null
              || !uri.getPath().endsWith(".zip")) {
            throw new IllegalArgumentException(
                "Manual source URL must be an HTTPS .zip URL without user info or fragment. Provided: "
                    + url);
          }
          if (!sha1.matches("[0-9a-fA-F]{40}")) {
            throw new IllegalArgumentException(
                "Manual source SHA-1 hash must contain exactly 40 hexadecimal characters");
          }

          return new ResourcePackDescriptor(
              "manual-pack", pluginVersion, url, sha1, null, 0L, profile, "MANUAL");
        });
  }
}
