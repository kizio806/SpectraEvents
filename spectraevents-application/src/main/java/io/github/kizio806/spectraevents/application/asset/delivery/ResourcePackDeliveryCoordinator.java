package io.github.kizio806.spectraevents.application.asset.delivery;

import io.github.kizio806.spectraevents.application.asset.AssetTargetProfile;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Resolves a configured pack asynchronously and retains only a verified last known good descriptor.
 */
public final class ResourcePackDeliveryCoordinator {
  public enum Status {
    UNRESOLVED,
    RESOLVING,
    READY,
    FAILED
  }

  private final ResourcePackSourcePort source;
  private final ResourcePackDescriptorCache cache;
  private final String pluginVersion;
  private final AssetTargetProfile profile;
  private final String sourceConfigId;
  private volatile Status status = Status.UNRESOLVED;
  private volatile Throwable lastFailure;

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

  public static ResourcePackDeliveryCoordinator failed(Throwable failure) {
    ResourcePackSourcePort failedSource =
        (version, prof) -> CompletableFuture.failedFuture(failure);
    ResourcePackDeliveryCoordinator coordinator =
        new ResourcePackDeliveryCoordinator(
            failedSource,
            new ResourcePackDescriptorCache(),
            "unknown",
            AssetTargetProfile.PROFILE_26_1,
            "failed");
    coordinator.status = Status.FAILED;
    coordinator.lastFailure = failure;
    return coordinator;
  }

  public CompletableFuture<ResourcePackDescriptor> refresh() {
    this.status = Status.RESOLVING;
    return source
        .resolve(pluginVersion, profile)
        .thenApply(
            descriptor -> {
              cache.put(descriptor, pluginVersion, profile, sourceConfigId);
              this.status = Status.READY;
              this.lastFailure = null;
              return descriptor;
            })
        .whenComplete(
            (descriptor, throwable) -> {
              if (throwable != null) {
                this.status = Status.FAILED;
                this.lastFailure =
                    throwable instanceof CompletionException completionException
                        ? completionException.getCause()
                        : throwable;
              }
            });
  }

  public Status status() {
    return status;
  }

  public Optional<Throwable> lastFailure() {
    return Optional.ofNullable(lastFailure);
  }

  public String lastFailureMessage() {
    if (lastFailure == null) {
      return "";
    }
    String message = lastFailure.getMessage();
    if (message != null && !message.isBlank()) {
      return message;
    }
    return lastFailure.getClass().getSimpleName();
  }

  private static String requireText(String value, String label) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(label + " must not be blank");
    }
    return value;
  }
}
