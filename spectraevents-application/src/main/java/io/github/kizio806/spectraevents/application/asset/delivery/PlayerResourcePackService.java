package io.github.kizio806.spectraevents.application.asset.delivery;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Service managing resource pack delivery and readiness states per player. */
public class PlayerResourcePackService {

  private final Map<UUID, PlayerResourcePackState> states = new ConcurrentHashMap<>();
  private final ResourcePackDescriptorCache cache;

  // Configured policy
  private final boolean required;
  private final String prompt;

  public PlayerResourcePackService(
      ResourcePackDescriptorCache cache, boolean required, String prompt) {
    this.cache = cache;
    this.required = required;
    this.prompt = prompt;
  }

  public void updateState(UUID playerId, PlayerResourcePackState state) {
    states.put(playerId, state);
  }

  public PlayerResourcePackState getState(UUID playerId) {
    return states.getOrDefault(playerId, PlayerResourcePackState.NOT_REQUESTED);
  }

  public void removePlayer(UUID playerId) {
    states.remove(playerId);
  }

  public PlayerAssetReadiness getReadiness(UUID playerId) {
    PlayerResourcePackState state = getState(playerId);
    return switch (state) {
      case LOADED -> PlayerAssetReadiness.READY;
      case FAILED -> PlayerAssetReadiness.FAILED;
      case DECLINED -> PlayerAssetReadiness.DECLINED;
      case NOT_REQUESTED, REQUESTED, ACCEPTED, DOWNLOADED -> PlayerAssetReadiness.NOT_READY;
    };
  }

  /** Determines if a pack is available and should be sent. */
  public java.util.Optional<ResourcePackDescriptor> getAvailablePack(
      String pluginVersion,
      io.github.kizio806.spectraevents.application.asset.AssetTargetProfile profile,
      String sourceConfigId) {
    return cache.get(pluginVersion, profile, sourceConfigId);
  }

  public boolean isRequired() {
    return required;
  }

  public String getPrompt() {
    return prompt;
  }
}
