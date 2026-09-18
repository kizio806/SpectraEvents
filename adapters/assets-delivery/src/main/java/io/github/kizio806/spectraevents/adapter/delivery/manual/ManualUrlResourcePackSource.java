package io.github.kizio806.spectraevents.adapter.delivery.manual;

import io.github.kizio806.spectraevents.application.asset.AssetTargetProfile;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackDescriptor;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackSourcePort;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

public class ManualUrlResourcePackSource implements ResourcePackSourcePort {

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
          if (!url.startsWith("https://")) {
            throw new IllegalArgumentException(
                "Manual source URL must use HTTPS. Provided: " + url);
          }
          if (sha1.isEmpty()) {
            throw new IllegalArgumentException("Manual source SHA-1 hash is missing");
          }
          try {
            URI.create(url); // Validate URL format
          } catch (Exception e) {
            throw new IllegalArgumentException("Invalid manual source URL format", e);
          }

          return new ResourcePackDescriptor(
              "manual-pack", pluginVersion, url, sha1, null, 0L, profile, "MANUAL");
        });
  }
}
