package io.github.kizio806.spectraevents.application.asset.delivery;

import io.github.kizio806.spectraevents.application.asset.AssetTargetProfile;
import java.util.concurrent.CompletableFuture;

/** Port for resolving a resource pack from an external source (e.g. Modrinth, manual URL). */
public interface ResourcePackSourcePort {
  /** Resolves the resource pack descriptor for a given plugin version and target profile. */
  CompletableFuture<ResourcePackDescriptor> resolve(
      String pluginVersion, AssetTargetProfile profile);
}
