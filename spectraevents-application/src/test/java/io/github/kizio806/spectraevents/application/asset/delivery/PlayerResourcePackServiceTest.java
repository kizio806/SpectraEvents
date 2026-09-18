package io.github.kizio806.spectraevents.application.asset.delivery;

import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PlayerResourcePackServiceTest {

  @Test
  void testStateTransitions() {
    ResourcePackDescriptorCache cache = new ResourcePackDescriptorCache();
    PlayerResourcePackService service = new PlayerResourcePackService(cache, true, "prompt");

    UUID player = UUID.randomUUID();
    Assertions.assertEquals(PlayerResourcePackState.NOT_REQUESTED, service.getState(player));
    Assertions.assertEquals(PlayerAssetReadiness.NOT_READY, service.getReadiness(player));

    service.updateState(player, PlayerResourcePackState.LOADED);
    Assertions.assertEquals(PlayerResourcePackState.LOADED, service.getState(player));
    Assertions.assertEquals(PlayerAssetReadiness.READY, service.getReadiness(player));

    service.updateState(player, PlayerResourcePackState.DECLINED);
    Assertions.assertEquals(PlayerAssetReadiness.DECLINED, service.getReadiness(player));

    service.updateState(player, PlayerResourcePackState.FAILED);
    Assertions.assertEquals(PlayerAssetReadiness.FAILED, service.getReadiness(player));

    service.removePlayer(player);
    Assertions.assertEquals(PlayerResourcePackState.NOT_REQUESTED, service.getState(player));
  }
}
