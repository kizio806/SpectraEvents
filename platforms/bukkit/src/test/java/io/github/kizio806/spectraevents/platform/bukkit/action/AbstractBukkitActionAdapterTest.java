package io.github.kizio806.spectraevents.platform.bukkit.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.execution.EventLocation;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.execution.EventZone;
import io.github.kizio806.spectraevents.application.execution.ExecutionContext;
import io.github.kizio806.spectraevents.application.execution.FatalActionException;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.execution.action.CoreActions;
import io.github.kizio806.spectraevents.core.event.execution.action.PlatformActions;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.gameplay.reward.RewardItem;
import io.github.kizio806.spectraevents.platform.bukkit.integration.item.CustomItemProvider;
import io.github.kizio806.spectraevents.platform.bukkit.loot.BukkitSharedLootHolder;
import io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AbstractBukkitActionAdapterTest {

  @BeforeAll
  static void beforeAll() {
    MockBukkitSetup.ensureInitialized();
  }

  private TestBukkitActionAdapter adapter;

  static class TestBukkitActionAdapter extends AbstractBukkitActionAdapter {
    final List<String> broadcastMessages = new ArrayList<>();
    final List<String> sentMessages = new ArrayList<>();
    final List<String> rawMessages = new ArrayList<>();
    final List<String> shownTitles = new ArrayList<>();
    final List<Entity> spawnedEntities = new ArrayList<>();

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
    protected void registerSpawnedEntity(EventInstanceId instanceId, Entity entity) {
      spawnedEntities.add(entity);
    }

    final List<io.github.kizio806.spectraevents.application.model.runtime.RenderedModelHandle>
        spawnedModels = new ArrayList<>();

    @Override
    protected void registerSpawnedModel(
        EventInstanceId instanceId,
        io.github.kizio806.spectraevents.application.model.runtime.RenderedModelHandle handle) {
      spawnedModels.add(handle);
    }

    @Override
    protected void setEntityCustomName(
        Entity entity, String template, EventInstance instance, EventRuntimeState state) {
      entity.setCustomName(template);
    }

    @Override
    protected void sendPlayerMessage(
        CommandSender sender,
        String messageTemplate,
        EventInstance instance,
        EventRuntimeState state) {
      sentMessages.add(messageTemplate);
    }

    @Override
    protected void broadcastMessage(
        String messageTemplate, EventInstance instance, EventRuntimeState state) {
      broadcastMessages.add(messageTemplate);
    }

    @Override
    protected void broadcastRawMessage(String rawMessage) {
      rawMessages.add(rawMessage);
    }

    @Override
    protected void showTitle(
        String titleTemplate,
        String subtitleTemplate,
        int fadeIn,
        int stay,
        int fadeOut,
        EventInstance instance,
        EventRuntimeState state) {
      shownTitles.add(titleTemplate + ":" + subtitleTemplate);
    }

    @Override
    protected Inventory createSharedLootInventory(
        BukkitSharedLootHolder holder,
        int size,
        String titleTemplate,
        EventInstance instance,
        EventRuntimeState state) {
      return MockBukkitSetup.createMockInventory(holder, size, titleTemplate);
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

  @Test
  void shouldRegisterAndUnregisterItemProvidersSafely() {
    CustomItemProvider provider =
        new CustomItemProvider() {
          @Override
          public boolean isAvailable() {
            return true;
          }

          @Override
          public ItemStack resolveItem(String materialName, int amount) {
            return "custom:diamond".equals(materialName)
                ? new ItemStack(Material.DIAMOND, amount)
                : null;
          }
        };

    adapter.registerItemProvider(provider);
    assertTrue(adapter.itemProviders().contains(provider));

    ItemStack resolved = adapter.resolveItemStack("custom:diamond", 5);
    assertNotNull(resolved);
    assertEquals(Material.DIAMOND, resolved.getType());
    assertEquals(5, resolved.getAmount());

    adapter.unregisterItemProvider(provider);
    assertFalse(adapter.itemProviders().contains(provider));

    // After unregistration, custom item resolution falls back or fails
    assertThrows(FatalActionException.class, () -> adapter.resolveItemStack("custom:diamond", 1));
  }

  @Test
  void shouldSupportConcurrentProviderRegistrationAndItemResolution() throws Exception {
    int threadCount = 8;
    int iterations = 100;
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch completionLatch = new CountDownLatch(threadCount);
    AtomicBoolean failure = new AtomicBoolean(false);

    List<Thread> threads = new ArrayList<>();
    for (int t = 0; t < threadCount; t++) {
      final int threadIndex = t;
      Thread thread =
          new Thread(
              () -> {
                try {
                  startLatch.await();
                  for (int i = 0; i < iterations; i++) {
                    if (threadIndex % 2 == 0) {
                      CustomItemProvider provider =
                          new CustomItemProvider() {
                            @Override
                            public boolean isAvailable() {
                              return false;
                            }

                            @Override
                            public ItemStack resolveItem(String id, int qty) {
                              return null;
                            }
                          };
                      adapter.registerItemProvider(provider);
                      adapter.unregisterItemProvider(provider);
                    } else {
                      ItemStack stack = adapter.resolveItemStack("minecraft:diamond", 1);
                      if (stack == null || stack.getType() != Material.DIAMOND) {
                        failure.set(true);
                      }
                    }
                  }
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                  failure.set(true);
                } catch (FatalActionException | IllegalArgumentException e) {
                  failure.set(true);
                } finally {
                  completionLatch.countDown();
                }
              });
      threads.add(thread);
      thread.start();
    }

    startLatch.countDown();
    completionLatch.await();

    for (Thread thread : threads) {
      thread.join();
    }

    assertFalse(failure.get(), "Concurrent registration and resolution failed");
  }

  @Test
  void shouldBroadcastMessageAction() {
    EventInstanceId id = EventInstanceId.generate();
    EventInstance instance = EventInstance.create(id, new EventDefinitionId("test"));
    EventRuntimeState state = new EventRuntimeState(id);

    assertTrue(
        adapter
            .executeAction(instance, state, new PlatformActions.BroadcastMessageAction(""))
            .join());
    assertTrue(adapter.broadcastMessages.isEmpty());

    assertTrue(
        adapter
            .executeAction(
                instance, state, new PlatformActions.BroadcastMessageAction("Hello world!"))
            .join());
    assertEquals(1, adapter.broadcastMessages.size());
    assertEquals("Hello world!", adapter.broadcastMessages.getFirst());
  }

  @Test
  void shouldSendMessageAction() {
    EventInstanceId id = EventInstanceId.generate();
    EventInstance instance = EventInstance.create(id, new EventDefinitionId("test"));
    EventRuntimeState state = new EventRuntimeState(id);

    assertThrows(
        FatalActionException.class,
        () ->
            adapter.executeAction(
                instance,
                state,
                new PlatformActions.SendMessageAction("test"),
                ExecutionContext.EMPTY));

    Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "Steve");
    ExecutionContext playerContext = ExecutionContext.withActor(player);
    assertTrue(
        adapter
            .executeAction(
                instance,
                state,
                new PlatformActions.SendMessageAction("Player message"),
                playerContext)
            .join());
    assertEquals(1, adapter.sentMessages.size());
    assertEquals("Player message", adapter.sentMessages.getFirst());

    ExecutionContext consoleContext = ExecutionContext.withActor(Bukkit.getConsoleSender());
    assertTrue(
        adapter
            .executeAction(
                instance,
                state,
                new PlatformActions.SendMessageAction("Console message"),
                consoleContext)
            .join());
    assertEquals(2, adapter.sentMessages.size());
    assertEquals("Console message", adapter.sentMessages.get(1));

    assertTrue(
        adapter
            .executeAction(
                instance, state, new PlatformActions.SendMessageAction(""), playerContext)
            .join());
    assertEquals(2, adapter.sentMessages.size());
  }

  @Test
  void shouldShowTitleAction() {
    EventInstanceId id = EventInstanceId.generate();
    EventInstance instance = EventInstance.create(id, new EventDefinitionId("test"));
    EventRuntimeState state = new EventRuntimeState(id);

    assertThrows(
        FatalActionException.class,
        () ->
            adapter.executeAction(
                instance, state, new PlatformActions.ShowTitleAction("", "sub", 10, 20, 10)));
    assertThrows(
        FatalActionException.class,
        () ->
            adapter.executeAction(
                instance, state, new PlatformActions.ShowTitleAction("title", "sub", -1, 20, 10)));
    assertThrows(
        FatalActionException.class,
        () ->
            adapter.executeAction(
                instance, state, new PlatformActions.ShowTitleAction("title", "sub", 10, 0, 10)));
    assertThrows(
        FatalActionException.class,
        () ->
            adapter.executeAction(
                instance, state, new PlatformActions.ShowTitleAction("title", "sub", 10, 20, -1)));

    assertTrue(
        adapter
            .executeAction(
                instance,
                state,
                new PlatformActions.ShowTitleAction("Title", "Subtitle", 10, 70, 20))
            .join());
    assertEquals(1, adapter.shownTitles.size());
    assertEquals("Title:Subtitle", adapter.shownTitles.getFirst());
  }

  @Test
  void shouldPlaySoundAndSpawnParticles() {
    EventInstanceId id = EventInstanceId.generate();
    EventInstance instance = EventInstance.create(id, new EventDefinitionId("test"));
    EventRuntimeState state = new EventRuntimeState(id);

    assertThrows(
        FatalActionException.class,
        () ->
            adapter.executeAction(
                instance,
                state,
                new PlatformActions.PlaySoundAction(
                    "minecraft:entity.experience_orb.pickup", 1.0f, 1.0f)));
    assertThrows(
        FatalActionException.class,
        () ->
            adapter.executeAction(
                instance, state, new PlatformActions.SpawnParticlesAction("minecraft:flame", 10)));

    state.setPlatformLocation(new EventLocation("world", 10.0, 64.0, 20.0, 0.0f, 0.0f));

    assertTrue(
        adapter
            .executeAction(
                instance,
                state,
                new PlatformActions.PlaySoundAction(
                    "minecraft:entity.experience_orb.pickup", 1.0f, 1.0f))
            .join());
    assertTrue(
        adapter
            .executeAction(
                instance, state, new PlatformActions.SpawnParticlesAction("minecraft:flame", 25))
            .join());

    assertThrows(
        FatalActionException.class,
        () ->
            adapter.executeAction(
                instance, state, new PlatformActions.SpawnParticlesAction("minecraft:flame", -1)));
    assertThrows(
        FatalActionException.class,
        () ->
            adapter.executeAction(
                instance,
                state,
                new PlatformActions.SpawnParticlesAction("minecraft:flame", 10_001)));
  }

  @Test
  void shouldSpawnBossAndMobs() {
    EventInstanceId id = EventInstanceId.generate();
    EventInstance instance = EventInstance.create(id, new EventDefinitionId("test"));
    EventRuntimeState state = new EventRuntimeState(id);

    assertThrows(
        FatalActionException.class,
        () ->
            adapter.executeAction(
                instance,
                state,
                new PlatformActions.SpawnBossAction(0, 0, 0, "minecraft:zombie", "Boss Zombie")));
    assertThrows(
        FatalActionException.class,
        () ->
            adapter.executeAction(
                instance,
                state,
                new PlatformActions.SpawnMobsAction(
                    "wave_1",
                    List.of(
                        new PlatformActions.MobSpawn(
                            "minecraft:skeleton", "Skeleton Minion", 3, 5.0)))));

    state.setPlatformLocation(new Location(Bukkit.getWorld("world"), 10.0, 64.0, 20.0));

    assertTrue(
        adapter
            .executeAction(
                instance,
                state,
                new PlatformActions.SpawnBossAction(0, 0, 0, "minecraft:zombie", "Boss Zombie"))
            .join());
    assertTrue(state.bossEntityId().isPresent());
    assertFalse(adapter.spawnedEntities.isEmpty());

    int initialCount = adapter.spawnedEntities.size();
    assertTrue(
        adapter
            .executeAction(
                instance,
                state,
                new PlatformActions.SpawnMobsAction(
                    "wave_1",
                    List.of(
                        new PlatformActions.MobSpawn(
                            "minecraft:skeleton", "Skeleton Minion", 3, 5.0))))
            .join());
    assertEquals(initialCount + 3, adapter.spawnedEntities.size());
    assertEquals(3, state.trackedWaveEntityCount());

    assertThrows(
        FatalActionException.class,
        () ->
            adapter.executeAction(
                instance,
                state,
                new PlatformActions.SpawnMobsAction(
                    "wave_2",
                    List.of(
                        new PlatformActions.MobSpawn(
                            "minecraft:skeleton", "Skeleton", 129, 5.0)))));
  }

  @Test
  void shouldDropLootAndReleaseGroundLoot() {
    EventInstanceId id = EventInstanceId.generate();
    EventInstance instance = EventInstance.create(id, new EventDefinitionId("test"));
    EventRuntimeState state = new EventRuntimeState(id);

    assertThrows(
        FatalActionException.class,
        () ->
            adapter.executeAction(
                instance,
                state,
                new PlatformActions.DropLootAction(
                    5.0, List.of(new PlatformActions.LootItem("minecraft:diamond", 1, 100)))));
    assertThrows(
        FatalActionException.class,
        () ->
            adapter.executeAction(
                instance, state, new PlatformActions.ReleaseGroundLootAction(5.0)));

    state.setPlatformLocation(new EventLocation("world", 10.0, 64.0, 20.0, 0.0f, 0.0f));

    assertThrows(
        FatalActionException.class,
        () ->
            adapter.executeAction(
                instance, state, new PlatformActions.DropLootAction(150.0, List.of())));
    assertThrows(
        FatalActionException.class,
        () ->
            adapter.executeAction(
                instance, state, new PlatformActions.ReleaseGroundLootAction(150.0)));

    assertTrue(
        adapter
            .executeAction(
                instance,
                state,
                new PlatformActions.DropLootAction(
                    5.0, List.of(new PlatformActions.LootItem("minecraft:diamond", 2, 100))))
            .join());

    state.initializeSharedLoot(List.of(new CoreActions.LootStack("minecraft:diamond", 5)));
    assertTrue(
        adapter
            .executeAction(instance, state, new PlatformActions.ReleaseGroundLootAction(5.0))
            .join());
  }

  @Test
  void shouldOpenSharedLootAction() {
    EventInstanceId id = EventInstanceId.generate();
    EventInstance instance = EventInstance.create(id, new EventDefinitionId("test"));
    EventRuntimeState state = new EventRuntimeState(id);
    state.initializeSharedLoot(List.of(new CoreActions.LootStack("minecraft:diamond", 5)));

    PlatformActions.OpenSharedLootAction action =
        new PlatformActions.OpenSharedLootAction("Shared Chest");
    assertFalse(adapter.executeAction(instance, state, action, ExecutionContext.EMPTY).join());

    Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "Steve");
    assertTrue(
        adapter.executeAction(instance, state, action, ExecutionContext.withActor(player)).join());
  }

  @Test
  void shouldDeliverRewardItemsAndOrDrop() {
    Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "Steve");
    MockBukkitSetup.registerPlayer(player);

    try {
      assertFalse(
          adapter
              .deliverRewardOrDrop(
                  UUID.randomUUID(), List.of(new RewardItem("minecraft:diamond", 5)))
              .join());

      assertTrue(
          adapter
              .deliverRewardOrDrop(
                  player.getUniqueId(), List.of(new RewardItem("minecraft:diamond", 5)))
              .join());

      adapter.beginShutdown();
      assertFalse(
          adapter
              .deliverRewardOrDrop(
                  player.getUniqueId(), List.of(new RewardItem("minecraft:diamond", 5)))
              .join());
    } finally {
      MockBukkitSetup.unregisterPlayer(player.getUniqueId());
    }
  }

  @Test
  void shouldAwardPodiumAction() {
    EventInstanceId id = EventInstanceId.generate();
    EventInstance instance = EventInstance.create(id, new EventDefinitionId("test"));
    EventRuntimeState state = new EventRuntimeState(id);

    PlatformActions.PodiumPool pool =
        new PlatformActions.PodiumPool(
            List.of(new PlatformActions.LootItem("minecraft:diamond", 3, 100)));
    PlatformActions.AwardPodiumAction podiumAction =
        new PlatformActions.AwardPodiumAction(List.of(pool, pool, pool));

    assertThrows(
        FatalActionException.class, () -> adapter.executeAction(instance, state, podiumAction));

    EventLocation center = new EventLocation("world", 10.0, 64.0, 20.0, 0.0f, 0.0f);
    state.setEventZone(EventZone.at(center, 50.0));

    UUID winnerId = UUID.randomUUID();
    Player winner = MockBukkitSetup.createMockPlayer(winnerId, "WinnerPlayer");
    MockBukkitSetup.registerPlayer(winner);

    try {
      state.recordDamage(winnerId, 100);

      assertTrue(
          adapter.executeAction(instance, state, podiumAction, ExecutionContext.EMPTY).join());
      assertEquals(1, adapter.rawMessages.size());
      assertTrue(adapter.rawMessages.getFirst().contains("WinnerPlayer"));
    } finally {
      MockBukkitSetup.unregisterPlayer(winnerId);
    }
  }

  @Test
  void shouldHandleModelServicesAndCleanup() {
    EventInstanceId id = EventInstanceId.generate();
    EventInstance instance = EventInstance.create(id, new EventDefinitionId("test"));
    EventRuntimeState state = new EventRuntimeState(id);
    state.setPlatformLocation(new EventLocation("world", 10.0, 64.0, 20.0, 0.0f, 0.0f));

    assertThrows(
        FatalActionException.class,
        () ->
            adapter.executeAction(
                instance, state, new PlatformActions.SpawnModelAction("metin_stone", 0)));
    assertThrows(
        FatalActionException.class,
        () -> adapter.executeAction(instance, state, new PlatformActions.MoveModelAction()));
    assertThrows(
        FatalActionException.class,
        () ->
            adapter.executeAction(
                instance, state, new PlatformActions.PlayAnimationAction("spin")));

    adapter.closeSharedLootInventories(id);
    adapter.stopModelAnimations(id);
    adapter.stopAllModelAnimations();
    adapter.cleanupEvent(id);
    adapter.cleanupAll();
    assertEquals(0, adapter.resourceCount(id));
  }

  @Test
  void shouldRunGuardedSafely() {
    EventInstanceId id = EventInstanceId.generate();
    CompletableFuture<Boolean> successFuture = new CompletableFuture<>();
    adapter.runGuarded(id, () -> {}, successFuture);
    assertTrue(successFuture.join());

    CompletableFuture<Boolean> failFuture = new CompletableFuture<>();
    adapter.runGuarded(
        id,
        () -> {
          throw new RuntimeException("Guarded failure");
        },
        failFuture);
    assertFalse(failFuture.join());

    // When fatalActionHandler throws, runGuarded catches and logs without propagating
    adapter.setFatalActionHandler(
        (eid, th) -> {
          throw new RuntimeException("Handler exploded");
        });
    CompletableFuture<Boolean> handlerFailFuture = new CompletableFuture<>();
    adapter.runGuarded(
        id,
        () -> {
          throw new RuntimeException("Original failure");
        },
        handlerFailFuture);
    assertFalse(handlerFailFuture.join());
  }

  @Test
  void testModelRuntimeAndAnimationActionFlows() {
    EventInstanceId id = EventInstanceId.generate();
    EventInstance instance = EventInstance.create(id, new EventDefinitionId("test_def"));
    EventRuntimeState state = new EventRuntimeState(id);
    state.setPlatformLocation(new EventLocation("world", 10.0, 64.0, 20.0, 0.0f, 0.0f));

    io.github.kizio806.spectraevents.application.model.registry.ModelDefinitionRegistry
        defRegistry =
            new io.github.kizio806.spectraevents.application.model.registry
                .ModelDefinitionRegistry();
    io.github.kizio806.spectraevents.core.visual.model.ModelId modelId =
        new io.github.kizio806.spectraevents.core.visual.model.ModelId("metin_stone");
    io.github.kizio806.spectraevents.core.visual.model.ModelPartDefinition part =
        new io.github.kizio806.spectraevents.core.visual.model.ModelPartDefinition(
            new io.github.kizio806.spectraevents.core.visual.model.ModelPartId("part1"),
            null,
            io.github.kizio806.spectraevents.core.visual.model.ModelPartType.ITEM_DISPLAY,
            io.github.kizio806.spectraevents.core.visual.model.ModelTransform.IDENTITY,
            io.github.kizio806.spectraevents.core.visual.model.ModelTransform.IDENTITY,
            io.github.kizio806.spectraevents.core.visual.model.ModelRenderProperties.DEFAULT,
            new io.github.kizio806.spectraevents.core.visual.model.ItemAssetRef(
                "minecraft:diamond",
                io.github.kizio806.spectraevents.core.visual.model.DisplayTransformMode.HEAD));
    io.github.kizio806.spectraevents.core.visual.model.ModelDefinition modelDef =
        new io.github.kizio806.spectraevents.core.visual.model.ModelDefinition(
            modelId, List.of(part), List.of());
    defRegistry.register(modelDef);

    java.util.concurrent.atomic.AtomicBoolean modelSpawned =
        new java.util.concurrent.atomic.AtomicBoolean();
    java.util.concurrent.atomic.AtomicBoolean modelMoved =
        new java.util.concurrent.atomic.AtomicBoolean();
    java.util.concurrent.atomic.AtomicBoolean modelRemoved =
        new java.util.concurrent.atomic.AtomicBoolean();

    io.github.kizio806.spectraevents.application.port.ModelRendererPort mockRenderer =
        new io.github.kizio806.spectraevents.application.port.ModelRendererPort() {
          @Override
          public io.github.kizio806.spectraevents.application.model.runtime.RenderedModelHandle
              spawnModel(
                  io.github.kizio806.spectraevents.application.model.runtime.ModelRuntimeId
                      runtimeId,
                  io.github.kizio806.spectraevents.core.visual.model.ModelDefinition definition,
                  io.github.kizio806.spectraevents.application.model.runtime.ModelAnchor anchor,
                  EventInstanceId ownerEventId) {
            modelSpawned.set(true);
            return new io.github.kizio806.spectraevents.application.model.runtime
                .RenderedModelHandle(
                runtimeId, definition.id(), ownerEventId, anchor, Map.of(), Map.of());
          }

          @Override
          public boolean updateModelTransform(
              io.github.kizio806.spectraevents.application.model.runtime.RenderedModelHandle handle,
              io.github.kizio806.spectraevents.application.model.runtime.ModelAnchor newAnchor) {
            modelMoved.set(true);
            return true;
          }

          @Override
          public boolean updatePartTransform(
              io.github.kizio806.spectraevents.application.model.runtime.RenderedModelHandle handle,
              io.github.kizio806.spectraevents.core.visual.model.ModelPartId partId,
              io.github.kizio806.spectraevents.core.visual.model.ModelTransform newLocalTransform) {
            return true;
          }

          @Override
          public boolean removeModel(
              io.github.kizio806.spectraevents.application.model.runtime.RenderedModelHandle
                  handle) {
            modelRemoved.set(true);
            return true;
          }

          @Override
          public void removeAll() {}

          @Override
          public List<
                  io.github.kizio806.spectraevents.application.model.runtime.DiscoveredModelEntity>
              reconcileEntities() {
            return List.of();
          }
        };

    io.github.kizio806.spectraevents.application.model.runtime.ModelRuntimeService
        modelRuntimeService =
            new io.github.kizio806.spectraevents.application.model.runtime.ModelRuntimeService(
                defRegistry, mockRenderer);
    adapter.setModelRuntimeService(modelRuntimeService);
    org.junit.jupiter.api.Assertions.assertSame(modelRuntimeService, adapter.modelRuntimeService());

    io.github.kizio806.spectraevents.application.model.animation.registry
            .AnimationDefinitionRegistry
        animDefRegistry =
            new io.github.kizio806.spectraevents.application.model.animation.registry
                .AnimationDefinitionRegistry();
    io.github.kizio806.spectraevents.application.model.animation.runtime.ActiveAnimationRegistry
        activeAnimRegistry =
            new io.github.kizio806.spectraevents.application.model.animation.runtime
                .ActiveAnimationRegistry();
    io.github.kizio806.spectraevents.application.port.AnimationSchedulerPort mockScheduler =
        (delay, task) -> task.run();
    io.github.kizio806.spectraevents.application.model.animation.runtime.AnimationRuntimeService
        animRuntime =
            new io.github.kizio806.spectraevents.application.model.animation.runtime
                .AnimationRuntimeService(
                animDefRegistry, defRegistry, activeAnimRegistry, mockRenderer, mockScheduler);
    io.github.kizio806.spectraevents.application.model.animation.runtime.ModelAnimationActionService
        animActionService =
            new io.github.kizio806.spectraevents.application.model.animation.runtime
                .ModelAnimationActionService(modelRuntimeService, animRuntime);
    adapter.setModelAnimationActionService(animActionService);
    org.junit.jupiter.api.Assertions.assertSame(
        animActionService, adapter.modelAnimationActionService());

    // 1. Spawn model
    CompletableFuture<Boolean> spawnFuture =
        adapter.executeAction(
            instance, state, new PlatformActions.SpawnModelAction("metin_stone", 1));
    assertTrue(spawnFuture.join());
    assertTrue(modelSpawned.get());
    assertEquals(1, adapter.spawnedModels.size());

    // 2. Move model
    CompletableFuture<Boolean> moveFuture =
        adapter.executeAction(instance, state, new PlatformActions.MoveModelAction());
    assertTrue(moveFuture.join());
    assertTrue(modelMoved.get());

    // 3. Remove model
    CompletableFuture<Boolean> removeFuture =
        adapter.executeAction(instance, state, new PlatformActions.RemoveModelAction());
    assertTrue(removeFuture.join());
    assertTrue(modelRemoved.get());

    // 4. Stop all animations with active instance
    adapter.stopAllModelAnimations();
  }

  @Test
  void testOpenSharedLootAction() {
    EventInstanceId id = EventInstanceId.generate();
    EventInstance instance = EventInstance.create(id, new EventDefinitionId("loot_event"));
    EventRuntimeState state = new EventRuntimeState(id);
    state.setPlatformLocation(new EventLocation("world", 10.0, 64.0, 20.0, 0.0f, 0.0f));
    state.initializeSharedLoot(List.of(new CoreActions.LootStack("minecraft:emerald", 10)));

    PlatformActions.OpenSharedLootAction lootAction =
        new PlatformActions.OpenSharedLootAction("Shared Chest");

    // Non-player actor returns false
    CompletableFuture<Boolean> nonPlayerResult =
        adapter.executeAction(instance, state, lootAction, ExecutionContext.EMPTY);
    assertFalse(nonPlayerResult.join());

    // Player actor opens inventory
    UUID playerId = UUID.randomUUID();
    Player player = MockBukkitSetup.createMockPlayer(playerId, "LootPlayer");
    MockBukkitSetup.registerPlayer(player);
    try {
      CompletableFuture<Boolean> playerResult =
          adapter.executeAction(
              instance, state, lootAction, ExecutionContext.withActor(player, playerId));
      assertTrue(playerResult.join());
    } finally {
      MockBukkitSetup.unregisterPlayer(playerId);
    }
  }

  @Test
  void testGiveItemActionBranches() {
    EventInstanceId id = EventInstanceId.generate();
    EventInstance instance = EventInstance.create(id, new EventDefinitionId("give_event"));
    EventRuntimeState state = new EventRuntimeState(id);
    UUID playerId = UUID.randomUUID();
    Player player = MockBukkitSetup.createMockPlayer(playerId, "GivePlayer");
    MockBukkitSetup.registerPlayer(player);

    try {
      // Missing player context
      assertThrows(
          FatalActionException.class,
          () ->
              adapter.executeAction(
                  instance,
                  state,
                  new PlatformActions.GiveItemAction("minecraft:diamond", 1),
                  ExecutionContext.EMPTY));

      // Invalid amount
      assertThrows(
          FatalActionException.class,
          () ->
              adapter.executeAction(
                  instance,
                  state,
                  new PlatformActions.GiveItemAction("minecraft:diamond", 0),
                  ExecutionContext.withActor(player, playerId)));

      // Unknown material
      assertThrows(
          FatalActionException.class,
          () ->
              adapter.executeAction(
                  instance,
                  state,
                  new PlatformActions.GiveItemAction("unknown_nonexistent_mat_xyz", 1),
                  ExecutionContext.withActor(player, playerId)));

      // Valid give item
      CompletableFuture<Boolean> success =
          adapter.executeAction(
              instance,
              state,
              new PlatformActions.GiveItemAction("minecraft:diamond", 5),
              ExecutionContext.withActor(player, playerId));
      assertTrue(success.join());
    } finally {
      MockBukkitSetup.unregisterPlayer(playerId);
    }
  }

  @Test
  void testSoundParticleAndLootResolutions() {
    assertEquals(Particle.FLAME, adapter.resolveParticle(null));
    assertEquals(Particle.FLAME, adapter.resolveParticle(""));
    assertEquals(Particle.FLAME, adapter.resolveParticle("unknown_particle_xyz"));
    assertEquals(Particle.FLAME, adapter.resolveParticle("minecraft:flame"));

    org.junit.jupiter.api.Assertions.assertNull(adapter.resolveSound(null));
    org.junit.jupiter.api.Assertions.assertNull(adapter.resolveSound("   "));
    org.junit.jupiter.api.Assertions.assertNull(adapter.resolveSound("nonexistent.sound.name.xyz"));

    ItemStack lootStack = adapter.resolveLootItemStack("unknown_mat_xyz", 3);
    assertEquals(Material.DIAMOND, lootStack.getType());
    assertEquals(3, lootStack.getAmount());

    ItemStack formattedStack = adapter.resolveLootItemStack("minecraft:gold_ingot", 2);
    assertEquals(Material.GOLD_INGOT, formattedStack.getType());
  }

  @Test
  void testPodiumLabelAndDeliveryLogic() {
    assertEquals("no eligible finalists", adapter.podiumLabel(List.of()));

    UUID winnerId = UUID.randomUUID();
    Player player = MockBukkitSetup.createMockPlayer(winnerId, "Champion");
    MockBukkitSetup.registerPlayer(player);
    try {
      String label = adapter.podiumLabel(List.of(winnerId, UUID.randomUUID()));
      assertTrue(label.contains("Champion"));

      PlatformActions.PodiumPool pool =
          new PlatformActions.PodiumPool(
              List.of(new PlatformActions.LootItem("minecraft:iron_ingot", 4, 100)));
      adapter.deliverPodiumPool(player, pool);
    } finally {
      MockBukkitSetup.unregisterPlayer(winnerId);
    }
  }

  @Test
  void testCanContainAllCalculations() {
    Inventory inventory = Bukkit.createInventory(null, 9);

    // 1. Empty inventory can contain items
    assertTrue(adapter.canContainAll(inventory, List.of(new ItemStack(Material.DIAMOND, 64))));

    // 2. Partial stack combines correctly
    inventory.setItem(0, new ItemStack(Material.DIAMOND, 32));
    assertTrue(adapter.canContainAll(inventory, List.of(new ItemStack(Material.DIAMOND, 32))));

    // 3. Spilling over partial stack into empty slot
    inventory.setItem(0, new ItemStack(Material.DIAMOND, 60));
    assertTrue(adapter.canContainAll(inventory, List.of(new ItemStack(Material.DIAMOND, 10))));

    // 4. Full inventory with air or other items
    for (int i = 0; i < inventory.getSize(); i++) {
      inventory.setItem(i, new ItemStack(Material.STONE, 64));
    }
    assertFalse(adapter.canContainAll(inventory, List.of(new ItemStack(Material.DIAMOND, 1))));

    // 5. One empty slot but item exceeds max stack size of that slot
    inventory.setItem(0, null);
    assertTrue(adapter.canContainAll(inventory, List.of(new ItemStack(Material.DIAMOND, 64))));
    assertFalse(adapter.canContainAll(inventory, List.of(new ItemStack(Material.DIAMOND, 65))));
  }

  @Test
  void testDeliverRewardItemsBranches() throws Exception {
    UUID id = UUID.randomUUID();
    Player player = MockBukkitSetup.createMockPlayer(id, "MailboxReceiver");
    MockBukkitSetup.registerPlayer(player);
    try {
      // 1. Deliver to offline player returns false
      Player offlinePlayer =
          MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "OfflineUser", false);
      assertFalse(adapter.deliverRewardItems(offlinePlayer, List.of()).get());

      // 2. Deliver when inventory can contain all items
      List<RewardItem> items = List.of(new RewardItem("minecraft:diamond", 2));
      assertTrue(adapter.deliverRewardItems(player, items).get());

      // 3. Deliver when inventory is completely full -> drops items
      for (int i = 0; i < player.getInventory().getSize(); i++) {
        player.getInventory().setItem(i, new ItemStack(Material.STONE, 64));
      }
      assertTrue(adapter.deliverRewardItems(player, items).get());
    } finally {
      MockBukkitSetup.unregisterPlayer(id);
    }
  }

  @Test
  void testCloseSharedLootInventories() {
    EventInstanceId id = EventInstanceId.generate();
    UUID playerId = UUID.randomUUID();
    Player player = MockBukkitSetup.createMockPlayer(playerId, "LootViewer");
    MockBukkitSetup.registerPlayer(player);
    try {
      Inventory lootInv =
          MockBukkitSetup.createMockInventory(new BukkitSharedLootHolder(id), 27, "Loot");
      player.openInventory(lootInv);
      adapter.closeSharedLootInventories(id);
    } finally {
      MockBukkitSetup.unregisterPlayer(playerId);
    }
  }

  @Test
  void testResolveLootItemStackWithCustomProvider() {
    CustomItemProvider provider =
        new CustomItemProvider() {
          @Override
          public boolean isAvailable() {
            return true;
          }

          @Override
          public ItemStack resolveItem(String itemId, int amount) {
            if ("custom:sword".equals(itemId)) {
              return new ItemStack(Material.DIAMOND_SWORD, amount);
            }
            return null;
          }
        };

    adapter.registerItemProvider(provider);
    assertEquals(1, adapter.itemProviders().size());

    ItemStack customStack = adapter.resolveLootItemStack("custom:sword", 1);
    assertEquals(Material.DIAMOND_SWORD, customStack.getType());

    ItemStack fallbackStack = adapter.resolveLootItemStack("custom:missing", 1);
    assertEquals(Material.DIAMOND, fallbackStack.getType());

    adapter.unregisterItemProvider(provider);
    assertTrue(adapter.itemProviders().isEmpty());
  }
}
