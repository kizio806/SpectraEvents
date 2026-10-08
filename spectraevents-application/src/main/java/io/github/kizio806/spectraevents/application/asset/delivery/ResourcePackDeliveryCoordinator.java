package io.github.kizio806.spectraevents.application.asset.delivery;

import io.github.kizio806.spectraevents.application.asset.AssetTargetProfile;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Resolves a configured pack asynchronously and retains only a verified last known good descriptor.
 */
public final class ResourcePackDeliveryCoordinator {
  private final ResourcePackSourcePort source;
  private final ResourcePackDescriptorCache cache;
  private final String pluginVersion;
  private final AssetTargetProfile profile;
  private final String sourceConfigId;

  public ResourcePackDeliveryCoordinator(
      ResourcePackSourcePort source,
      ResourcePackDescriptorCache cache,
      String pluginVersion,
      AssetTargetProfile profile,
      String sourceConfigId) {
    this.source = Objects.requireNonNull(source, "source");
    this.cache = Objects.requireNonNull(cache, "cache");
    this.pluginVersion = requireText(pluginVersion, "pluginVersion");
    this.profile = Objects.requireNonNull(profile, "profile");
    this.sourceConfigId = requireText(sourceConfigId, "sourceConfigId");
  }

  public CompletableFuture<ResourcePackDescriptor> refresh() {
    return source
        .resolve(pluginVersion, profile)
        .thenApply(
            descriptor -> {
              cache.put(descriptor, pluginVersion, profile, sourceConfigId);
              return descriptor;
            });
  }

  private static String requireText(String value, String label) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(label + " must not be blank");
    }
    return value;
  }
}
