package io.github.kizio806.spectraevents.application.asset.delivery;

import io.github.kizio806.spectraevents.application.asset.AssetTargetProfile;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/** Cache for storing the resolved resource pack descriptor. */
public class ResourcePackDescriptorCache {

  private final AtomicReference<CachedDescriptor> cache = new AtomicReference<>(null);

  private record CachedDescriptor(
      ResourcePackDescriptor descriptor,
      String pluginVersion,
      AssetTargetProfile profile,
      String sourceConfigId) {}

  public Optional<ResourcePackDescriptor> get(
      String pluginVersion, AssetTargetProfile profile, String sourceConfigId) {
    CachedDescriptor current = cache.get();
    if (current == null) return Optional.empty();

    if (current.pluginVersion().equals(pluginVersion)
        && current.profile() == profile
        && current.sourceConfigId().equals(sourceConfigId)) {
      return Optional.of(current.descriptor());
    }
    return Optional.empty();
  }

  public Optional<ResourcePackDescriptor> getLastKnownGood(
      String pluginVersion, AssetTargetProfile profile) {
    CachedDescriptor current = cache.get();
    if (current == null) return Optional.empty();

    if (current.pluginVersion().equals(pluginVersion) && current.profile() == profile) {
      return Optional.of(current.descriptor());
    }
    return Optional.empty();
  }

  public void put(
      ResourcePackDescriptor descriptor,
      String pluginVersion,
      AssetTargetProfile profile,
      String sourceConfigId) {
    cache.set(new CachedDescriptor(descriptor, pluginVersion, profile, sourceConfigId));
  }

  public void invalidate() {
    cache.set(null);
  }
}
