package io.github.kizio806.spectraevents.platform.bukkit.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.execution.ExecutionContext;
import io.github.kizio806.spectraevents.application.execution.FatalActionException;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.execution.action.PlatformActions;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.platform.bukkit.integration.item.CustomItemProvider;
import io.github.kizio806.spectraevents.platform.bukkit.loot.BukkitSharedLootHolder;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AbstractBukkitActionAdapterTest {

  private TestBukkitActionAdapter adapter;

  static class TestBukkitActionAdapter extends AbstractBukkitActionAdapter {
    TestBukkitActionAdapter(LocaleCatalog locales) {
      super(locales);
    }

    @Override
    protected CompletableFuture<Boolean> executeAt(
        EventInstanceId instanceId, Location location, Runnable action) {
      action.run();
      return CompletableFuture.completedFuture(true);
    }

    @Override
    protected CompletableFuture<Boolean> executeGlobal(
        EventInstanceId instanceId, Runnable action) {
      action.run();
      return CompletableFuture.completedFuture(true);
    }

    @Override
    protected CompletableFuture<Boolean> executeFor(
        EventInstanceId instanceId, Player player, Runnable action) {
      action.run();
      return CompletableFuture.completedFuture(true);
    }

    @Override
    protected void registerSpawnedEntity(EventInstanceId instanceId, Entity entity) {}

    @Override
    protected void setEntityCustomName(
        Entity entity, String template, EventInstance instance, EventRuntimeState state) {}

    @Override
    protected void sendPlayerMessage(
        CommandSender sender,
        String messageTemplate,
        EventInstance instance,
        EventRuntimeState state) {}

    @Override
    protected void broadcastMessage(
        String messageTemplate, EventInstance instance, EventRuntimeState state) {}

    @Override
    protected void broadcastRawMessage(String rawMessage) {}

    @Override
    protected void showTitle(
        String titleTemplate,
        String subtitleTemplate,
        int fadeIn,
        int stay,
        int fadeOut,
        EventInstance instance,
        EventRuntimeState state) {}

    @Override
    protected Inventory createSharedLootInventory(
        BukkitSharedLootHolder holder,
        int size,
        String titleTemplate,
        EventInstance instance,
        EventRuntimeState state) {
      return null;
    }

    @Override
    public void cleanupEvent(EventInstanceId instanceId) {}

    @Override
    public void cleanupAll() {}

    @Override
    public int resourceCount(EventInstanceId instanceId) {
      return 0;
    }

    @Override
    public void refreshHud(EventInstance instance, EventRuntimeState state) {}

    @Override
    protected CompletableFuture<Boolean> handleShowBossbar(
        EventInstance instance,
        EventRuntimeState state,
        PlatformActions.ShowBossbarAction action,
        ExecutionContext context) {
      return CompletableFuture.completedFuture(true);
    }

    @Override
    protected CompletableFuture<Boolean> handleUpdateBossbar(
        EventInstance instance,
        EventRuntimeState state,
        PlatformActions.UpdateBossbarAction action,
        ExecutionContext context) {
      return CompletableFuture.completedFuture(true);
    }

    @Override
    protected CompletableFuture<Boolean> handleRemoveBossbar(
        EventInstance instance,
        EventRuntimeState state,
        PlatformActions.RemoveBossbarAction action,
        ExecutionContext context) {
      return CompletableFuture.completedFuture(true);
    }

    @Override
    protected CompletableFuture<Boolean> handleShowScoreboard(
        EventInstance instance,
        EventRuntimeState state,
        PlatformActions.ShowScoreboardAction action,
        ExecutionContext context) {
      return CompletableFuture.completedFuture(true);
    }

    @Override
    protected CompletableFuture<Boolean> handleUpdateScoreboard(
        EventInstance instance,
        EventRuntimeState state,
        PlatformActions.UpdateScoreboardAction action,
        ExecutionContext context) {
      return CompletableFuture.completedFuture(true);
    }

    @Override
    protected CompletableFuture<Boolean> handleRemoveScoreboard(
        EventInstance instance,
        EventRuntimeState state,
        PlatformActions.RemoveScoreboardAction action,
        ExecutionContext context) {
      return CompletableFuture.completedFuture(true);
    }
  }

  @BeforeEach
  void setUp() {
    LocaleCatalog locales = LocaleCatalog.of(Map.of("messages.metin-podium", "Podium: %podium%"));
    adapter = new TestBukkitActionAdapter(locales);
  }

  @Test
  void shouldResolveKnownMaterial() {
    Material diamond = adapter.resolveMaterial("minecraft:diamond");
    assertEquals(Material.DIAMOND, diamond);

    Material stone = adapter.resolveMaterial("STONE");
    assertEquals(Material.STONE, stone);
  }

  @Test
  void shouldFailOnUnknownMaterial() {
    assertThrows(
        FatalActionException.class,
        () -> adapter.resolveMaterial("minecraft:non_existent_material_12345"));
  }

  @Test
  void shouldResolveEntityType() {
    EntityType type = adapter.resolveEntityType("minecraft:zombie");
    assertEquals(EntityType.ZOMBIE, type);
  }

  @Test
  void shouldFailOnUnknownEntityType() {
    assertThrows(
        FatalActionException.class,
        () -> adapter.resolveEntityType("minecraft:invalid_entity_type_xyz"));
  }

  @Test
  void shouldResolveKnownParticle() {
    Particle particle = adapter.resolveParticle("minecraft:flame");
    assertEquals(Particle.FLAME, particle);
  }

  @Test
  void shouldFallbackOnUnknownParticle() {
    assertEquals(Particle.FLAME, adapter.resolveParticle("minecraft:invalid_particle_abc"));
  }

  @Test
  void shouldHandleSoundResolutionSafely() {
    // In standalone unit tests without a loaded Bukkit server, resolution completes without
    // throwing
    adapter.resolveSound("minecraft:block.end_portal.spawn");
    org.junit.jupiter.api.Assertions.assertNull(adapter.resolveSound(null));
    org.junit.jupiter.api.Assertions.assertNull(
        adapter.resolveSound("minecraft:non_existent_sound_12345"));
  }

  @Test
  void shouldFormatPodiumLabelWhenEmpty() {
    assertEquals("no eligible finalists", adapter.podiumLabel(List.of()));
  }

  @Test
  void shouldValidateRange() {
    adapter.requireRange(5, 1, 10, "test_field");
    assertThrows(FatalActionException.class, () -> adapter.requireRange(0, 1, 10, "test_field"));
    assertThrows(FatalActionException.class, () -> adapter.requireRange(11, 1, 10, "test_field"));
  }

  @Test
  void shouldSupportCustomItemProviders() {
    ItemStack mockItem = new ItemStack(Material.GOLD_INGOT, 5);
    adapter.registerItemProvider(
        new CustomItemProvider() {
          @Override
          public ItemStack resolveItem(String id, int amount) {
            if ("nexo:ruby".equals(id)) {
              return mockItem;
            }
            return null;
          }

          @Override
          public boolean isAvailable() {
            return true;
          }
        });

    ItemStack resolved = adapter.resolveItemStack("nexo:ruby", 5);
    assertNotNull(resolved);
    assertEquals(Material.GOLD_INGOT, resolved.getType());

    ItemStack fallback = adapter.resolveItemStack("minecraft:iron_ingot", 3);
    assertNotNull(fallback);
    assertEquals(Material.IRON_INGOT, fallback.getType());
  }

  @Test
  void shouldTrackShutdownState() {
    assertFalse(adapter.isShuttingDown());
    adapter.beginShutdown();
    assertTrue(adapter.isShuttingDown());
  }

  @Test
  void shouldHandleFatalActionErrors() {
    var errors = new java.util.ArrayList<Throwable>();
    adapter.setFatalActionHandler((id, err) -> errors.add(err));

    RuntimeException ex = new RuntimeException("Test action error");
    EventInstanceId id = EventInstanceId.generate();
    adapter.runFailureHandler(id, ex);

    assertEquals(1, errors.size());
    assertEquals("Test action error", errors.getFirst().getMessage());
  }

  @Test
  void shouldValidateRemoveModelAndGiveItem() {
    EventInstanceId id = EventInstanceId.generate();
    EventInstance instance = EventInstance.create(id, new EventDefinitionId("test"));
    EventRuntimeState state = new EventRuntimeState(id);

    PlatformActions.RemoveModelAction removeModelAction = new PlatformActions.RemoveModelAction();
    assertThrows(
        NullPointerException.class,
        () -> adapter.handleRemoveModel(null, state, removeModelAction, ExecutionContext.EMPTY));
    assertThrows(
        NullPointerException.class,
        () -> adapter.handleRemoveModel(instance, null, removeModelAction, ExecutionContext.EMPTY));
    assertThrows(
        NullPointerException.class,
        () -> adapter.handleRemoveModel(instance, state, null, ExecutionContext.EMPTY));
    assertTrue(
        adapter
            .handleRemoveModel(instance, state, removeModelAction, ExecutionContext.EMPTY)
            .join());

    PlatformActions.GiveItemAction giveItemAction =
        new PlatformActions.GiveItemAction("minecraft:diamond", 1);
    assertThrows(
        NullPointerException.class,
        () -> adapter.handleGiveItem(null, state, giveItemAction, ExecutionContext.EMPTY));
    assertThrows(
        NullPointerException.class,
        () -> adapter.handleGiveItem(instance, null, giveItemAction, ExecutionContext.EMPTY));
    assertThrows(
        NullPointerException.class,
        () -> adapter.handleGiveItem(instance, state, null, ExecutionContext.EMPTY));
    assertThrows(
        FatalActionException.class,
        () -> adapter.handleGiveItem(instance, state, giveItemAction, ExecutionContext.EMPTY));
  }
}
