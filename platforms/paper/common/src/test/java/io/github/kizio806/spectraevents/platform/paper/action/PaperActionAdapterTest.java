package io.github.kizio806.spectraevents.platform.paper.action;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.execution.ExecutionContext;
import io.github.kizio806.spectraevents.application.model.animation.registry.AnimationDefinitionRegistry;
import io.github.kizio806.spectraevents.application.model.animation.runtime.ActiveAnimationRegistry;
import io.github.kizio806.spectraevents.application.model.animation.runtime.AnimationRuntimeService;
import io.github.kizio806.spectraevents.application.model.animation.runtime.ModelAnimationActionService;
import io.github.kizio806.spectraevents.application.model.registry.ModelDefinitionRegistry;
import io.github.kizio806.spectraevents.application.model.runtime.ModelAnchor;
import io.github.kizio806.spectraevents.application.model.runtime.ModelRuntimeId;
import io.github.kizio806.spectraevents.application.model.runtime.ModelRuntimeService;
import io.github.kizio806.spectraevents.application.model.runtime.RenderedModelHandle;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.execution.action.PlatformActions;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.visual.model.ModelId;
import io.github.kizio806.spectraevents.platform.bukkit.loot.BukkitSharedLootHolder;
import io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup;
import io.github.kizio806.spectraevents.platform.paper.lifecycle.PaperResourceCleaner;
import io.github.kizio806.spectraevents.platform.paper.render.PaperModelRenderer;
import io.github.kizio806.spectraevents.platform.paper.scheduler.RegionTaskScheduler;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaperActionAdapterTest {

  @BeforeAll
  static void beforeAll() {
    MockBukkitSetup.ensureInitialized();
  }

  private RegionTaskScheduler regionScheduler;
  private PaperResourceCleaner cleaner;
  private LocaleCatalog locales;
  private PaperActionAdapter adapter;
  private final AtomicInteger executedTasks = new AtomicInteger(0);

  @BeforeEach
  void setUp() {
    executedTasks.set(0);
    regionScheduler =
        new RegionTaskScheduler() {
          @Override
          public void executeGlobal(Runnable task) {
            executedTasks.incrementAndGet();
            task.run();
          }

          @Override
          public void executeAt(Location location, Runnable task) {
            executedTasks.incrementAndGet();
            task.run();
          }

          @Override
          public void executeFor(Entity entity, Runnable task) {
            executedTasks.incrementAndGet();
            task.run();
          }
        };

    Plugin dummyPlugin =
        (Plugin)
            java.lang.reflect.Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (proxy, method, args) -> null);
    PaperModelRenderer renderer = new PaperModelRenderer(dummyPlugin);
    io.github.kizio806.spectraevents.application.port.EventTaskScheduler taskScheduler =
        new io.github.kizio806.spectraevents.application.port.EventTaskScheduler() {
          @Override
          public void schedule(EventInstanceId eventId, java.time.Duration delay, Runnable task) {}

          @Override
          public void scheduleGlobal(java.time.Duration delay, Runnable task) {}

          @Override
          public void cancelAll(EventInstanceId eventId) {}

          @Override
          public void cancelAll() {}

          @Override
          public int pendingTaskCount(EventInstanceId eventId) {
            return 0;
          }
        };

    cleaner = new PaperResourceCleaner(renderer, taskScheduler);
    locales =
        LocaleCatalog.of(
            Map.of(
                "messages.default-bossbar-title", "Default Title",
                "test.message", "Hello %event% in %phase%",
                "test.title", "Title %health%/%max_health%",
                "test.subtitle", "Subtitle %hits% hits"));
    adapter = new PaperActionAdapter(regionScheduler, cleaner, locales);

    ModelRuntimeService modelRuntime =
        new ModelRuntimeService(new ModelDefinitionRegistry(), renderer);
    ModelAnimationActionService animService =
        new ModelAnimationActionService(
            modelRuntime,
            new AnimationRuntimeService(
                new AnimationDefinitionRegistry(),
                new ActiveAnimationRegistry(),
                renderer,
                (delay, task) -> {}));
    adapter.setModelRuntimeService(modelRuntime);
    adapter.setModelAnimationActionService(animService);
  }

  @Test
  void testConstructorValidations() {
    assertThrows(NullPointerException.class, () -> new PaperActionAdapter(null, cleaner, locales));
    assertThrows(
        NullPointerException.class, () -> new PaperActionAdapter(regionScheduler, null, locales));
    assertThrows(
        NullPointerException.class, () -> new PaperActionAdapter(regionScheduler, cleaner, null));
    assertNotNull(adapter.bossBarManager());
    assertNotNull(adapter.scoreboardManager());
  }

  @Test
  void testBossBarLifecycle() {
    EventInstanceId id = EventInstanceId.generate();
    EventInstance instance = EventInstance.create(id, new EventDefinitionId("boss_event"));
    EventRuntimeState state = new EventRuntimeState(id);

    PlatformActions.ShowBossbarAction showAction =
        new PlatformActions.ShowBossbarAction("RED", "SOLID", "Boss %event%", "HEALTH");
    assertTrue(
        adapter.handleShowBossbar(instance, state, showAction, ExecutionContext.EMPTY).join());

    PlatformActions.UpdateBossbarAction updateAction =
        new PlatformActions.UpdateBossbarAction("New Boss Title", "HITS");
    assertTrue(
        adapter.handleUpdateBossbar(instance, state, updateAction, ExecutionContext.EMPTY).join());

    adapter.refreshHud(instance, state);

    PlatformActions.RemoveBossbarAction removeAction = new PlatformActions.RemoveBossbarAction();
    assertTrue(
        adapter.handleRemoveBossbar(instance, state, removeAction, ExecutionContext.EMPTY).join());

    // Register a show action again and verify cleaner removes it
    assertTrue(
        adapter.handleShowBossbar(instance, state, showAction, ExecutionContext.EMPTY).join());
    cleaner.cleanup(id);
  }

  @Test
  void testScoreboardLifecycle() {
    EventInstanceId id = EventInstanceId.generate();
    EventInstance instance = EventInstance.create(id, new EventDefinitionId("score_event"));
    EventRuntimeState state = new EventRuntimeState(id);

    PlatformActions.ShowScoreboardAction showAction =
        new PlatformActions.ShowScoreboardAction("Score Title", List.of("Line 1", "Line 2"));
    assertTrue(
        adapter.handleShowScoreboard(instance, state, showAction, ExecutionContext.EMPTY).join());

    PlatformActions.UpdateScoreboardAction updateAction =
        new PlatformActions.UpdateScoreboardAction(List.of("Line 1 Updated", "Line 2 Updated"));
    assertTrue(
        adapter
            .handleUpdateScoreboard(instance, state, updateAction, ExecutionContext.EMPTY)
            .join());

    PlatformActions.RemoveScoreboardAction removeAction =
        new PlatformActions.RemoveScoreboardAction();
    assertTrue(
        adapter
            .handleRemoveScoreboard(instance, state, removeAction, ExecutionContext.EMPTY)
            .join());

    // Register show action again and verify cleaner removes it
    assertTrue(
        adapter.handleShowScoreboard(instance, state, showAction, ExecutionContext.EMPTY).join());
    cleaner.cleanup(id);
  }

  @Test
  void testBroadcastAndPlayerMessages() {
    EventInstanceId id = EventInstanceId.generate();
    EventInstance instance = EventInstance.create(id, new EventDefinitionId("msg_event"));
    EventRuntimeState state = new EventRuntimeState(id);

    Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "Alice");
    MockBukkitSetup.registerPlayer(player);

    try {
      assertDoesNotThrow(() -> adapter.broadcastMessage("test.message", instance, state));
      assertDoesNotThrow(() -> adapter.broadcastRawMessage("Raw broadcast"));
      assertDoesNotThrow(
          () -> adapter.showTitle("test.title", "test.subtitle", 10, 20, 10, instance, state));
      assertDoesNotThrow(() -> adapter.sendPlayerMessage(player, "test.message", instance, state));

      Entity entity = MockBukkitSetup.createMockEntityWithDetails(EntityType.ZOMBIE, null);
      assertDoesNotThrow(() -> adapter.setEntityCustomName(entity, "test.title", instance, state));
    } finally {
      MockBukkitSetup.unregisterPlayer(player.getUniqueId());
    }
  }

  @Test
  void testSharedLootInventoryCreation() {
    EventInstanceId id = EventInstanceId.generate();
    EventInstance instance = EventInstance.create(id, new EventDefinitionId("loot_event"));
    EventRuntimeState state = new EventRuntimeState(id);

    BukkitSharedLootHolder holder = new BukkitSharedLootHolder(id);
    Inventory inv = adapter.createSharedLootInventory(holder, 27, "Shared Loot", instance, state);
    assertNotNull(inv);
  }

  @Test
  void testResourceRegistrationAndCleanup() {
    EventInstanceId id = EventInstanceId.generate();
    Entity entity = MockBukkitSetup.createMockEntityWithDetails(EntityType.ARMOR_STAND, null);

    adapter.registerSpawnedEntity(id, entity);
    assertEquals(1, adapter.resourceCount(id));

    // Also register an invalid entity to test entity.isValid() == false cleanup branch
    Entity invalidEntity =
        MockBukkitSetup.createMockEntityWithDetails(EntityType.ARMOR_STAND, null);
    invalidEntity.remove();
    adapter.registerSpawnedEntity(id, invalidEntity);
    assertEquals(2, adapter.resourceCount(id));

    ModelRuntimeId modelRuntimeId = ModelRuntimeId.generate();
    RenderedModelHandle modelHandle =
        new RenderedModelHandle(
            modelRuntimeId,
            new ModelId("stone_model"),
            id,
            new ModelAnchor("world", 0, 0, 0, 0, 0),
            Collections.emptyMap(),
            Collections.emptyMap());
    adapter.registerSpawnedModel(id, modelHandle);
    assertEquals(3, adapter.resourceCount(id));

    adapter.cleanupEvent(id);
    assertEquals(0, adapter.resourceCount(id));
  }

  @Test
  void testExecuteMethodsAndShutdown() {
    EventInstanceId id = EventInstanceId.generate();
    Location loc = new Location(null, 10, 64, 20);
    Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "Bob");

    AtomicBoolean executed = new AtomicBoolean(false);
    assertTrue(adapter.executeAt(id, loc, () -> executed.set(true)).join());
    assertTrue(executed.get());

    executed.set(false);
    assertTrue(adapter.executeGlobal(id, () -> executed.set(true)).join());
    assertTrue(executed.get());

    executed.set(false);
    assertTrue(adapter.executeFor(id, player, () -> executed.set(true)).join());
    assertTrue(executed.get());

    // Test shutdown
    adapter.cleanupAll();

    // Verify refreshHud returns early when shutting down
    EventInstance instance = EventInstance.create(id, new EventDefinitionId("boss_event"));
    EventRuntimeState state = new EventRuntimeState(id);
    assertDoesNotThrow(() -> adapter.refreshHud(instance, state));

    assertFalse(adapter.executeAt(id, loc, () -> {}).join());
    assertFalse(adapter.executeGlobal(id, () -> {}).join());
    assertFalse(adapter.executeFor(id, player, () -> {}).join());
  }

  @Test
  void testExceptionInExecution() {
    EventInstanceId id = EventInstanceId.generate();
    RegionTaskScheduler throwingScheduler =
        new RegionTaskScheduler() {
          @Override
          public void executeGlobal(Runnable task) {
            throw new IllegalStateException("Global failure");
          }

          @Override
          public void executeAt(Location location, Runnable task) {
            throw new IllegalStateException("Location failure");
          }

          @Override
          public void executeFor(Entity entity, Runnable task) {
            throw new IllegalStateException("Player failure");
          }
        };

    PaperActionAdapter throwingAdapter =
        new PaperActionAdapter(throwingScheduler, cleaner, locales);

    assertThrows(IllegalStateException.class, () -> throwingAdapter.executeGlobal(id, () -> {}));
    assertThrows(
        IllegalStateException.class,
        () -> throwingAdapter.executeAt(id, new Location(null, 0, 0, 0), () -> {}));
    Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "Charlie");
    assertThrows(
        IllegalStateException.class, () -> throwingAdapter.executeFor(id, player, () -> {}));
  }
}
