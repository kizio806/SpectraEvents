package io.github.kizio806.spectraevents.platform.spigot.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.config.locale.FileSystemLocaleLoader;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.execution.ExecutionContext;
import io.github.kizio806.spectraevents.application.model.registry.ModelDefinitionRegistry;
import io.github.kizio806.spectraevents.application.model.runtime.ModelAnchor;
import io.github.kizio806.spectraevents.application.model.runtime.ModelRuntimeId;
import io.github.kizio806.spectraevents.application.model.runtime.ModelRuntimeService;
import io.github.kizio806.spectraevents.application.model.runtime.RenderedModelHandle;
import io.github.kizio806.spectraevents.application.model.runtime.RenderedPartHandle;
import io.github.kizio806.spectraevents.application.port.ModelRendererPort;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.execution.action.PlatformActions;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.visual.model.DisplayTransformMode;
import io.github.kizio806.spectraevents.core.visual.model.ItemAssetRef;
import io.github.kizio806.spectraevents.core.visual.model.ModelDefinition;
import io.github.kizio806.spectraevents.core.visual.model.ModelId;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartDefinition;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartId;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartType;
import io.github.kizio806.spectraevents.core.visual.model.ModelRenderProperties;
import io.github.kizio806.spectraevents.core.visual.model.ModelTransform;
import io.github.kizio806.spectraevents.platform.bukkit.loot.BukkitSharedLootHolder;
import io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpigotActionAdapterTest {

  @BeforeAll
  static void beforeAll() {
    MockBukkitSetup.ensureInitialized();
  }

  @TempDir Path tempDir;

  @AfterEach
  void tearDown() {
    MockBukkitSetup.resetCustomDelegates();
  }

  @Test
  void testSpigotActionAdapterScoreboardAndBossbarValidations() throws IOException {
    LocaleCatalog locales = new FileSystemLocaleLoader(tempDir).loadCatalog("en-US");

    BukkitScheduler scheduler =
        (BukkitScheduler)
            Proxy.newProxyInstance(
                BukkitScheduler.class.getClassLoader(),
                new Class<?>[] {BukkitScheduler.class},
                (proxy, method, args) -> {
                  if ("runTask".equals(method.getName()) && args != null && args.length >= 2) {
                    if (args[1] instanceof Runnable r) {
                      r.run();
                    }
                  }
                  return null;
                });

    Server server =
        (Server)
            Proxy.newProxyInstance(
                Server.class.getClassLoader(),
                new Class<?>[] {Server.class},
                (proxy, method, args) -> {
                  if ("getScheduler".equals(method.getName())) {
                    return scheduler;
                  }
                  return null;
                });

    Plugin plugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (proxy, method, args) -> {
                  if ("getServer".equals(method.getName())) {
                    return server;
                  }
                  if ("getLogger".equals(method.getName())) {
                    return Logger.getLogger("test");
                  }
                  return null;
                });

    Audience mockAudience =
        (Audience)
            Proxy.newProxyInstance(
                Audience.class.getClassLoader(),
                new Class<?>[] {Audience.class},
                (proxy, method, args) -> null);

    try (BukkitAudiences adventure =
        (BukkitAudiences)
            Proxy.newProxyInstance(
                BukkitAudiences.class.getClassLoader(),
                new Class<?>[] {BukkitAudiences.class},
                (proxy, method, args) -> {
                  if ("sender".equals(method.getName()) || "all".equals(method.getName())) {
                    return mockAudience;
                  }
                  return null;
                })) {
      SpigotActionAdapter adapter = new SpigotActionAdapter(plugin, adventure, locales);
      assertNotNull(adapter);

      EventInstanceId id = EventInstanceId.generate();
      EventInstance instance = EventInstance.create(id, new EventDefinitionId("test"));
      EventRuntimeState state = new EventRuntimeState(id);

      PlatformActions.UpdateScoreboardAction updateAction =
          new PlatformActions.UpdateScoreboardAction(java.util.List.of("line1"));
      assertTrue(
          adapter
              .handleUpdateScoreboard(instance, state, updateAction, ExecutionContext.EMPTY)
              .join());

      assertThrows(
          NullPointerException.class,
          () -> adapter.handleUpdateScoreboard(null, state, updateAction, ExecutionContext.EMPTY));
      assertThrows(
          NullPointerException.class,
          () ->
              adapter.handleUpdateScoreboard(instance, null, updateAction, ExecutionContext.EMPTY));
      assertThrows(
          NullPointerException.class,
          () -> adapter.handleUpdateScoreboard(instance, state, null, ExecutionContext.EMPTY));

      assertThrows(
          NullPointerException.class,
          () ->
              adapter.handleRemoveBossbar(
                  instance,
                  null,
                  new PlatformActions.RemoveBossbarAction(),
                  ExecutionContext.EMPTY));
      assertThrows(
          NullPointerException.class,
          () -> adapter.handleRemoveBossbar(instance, state, null, ExecutionContext.EMPTY));

      assertThrows(
          NullPointerException.class,
          () ->
              adapter.handleRemoveScoreboard(
                  instance,
                  null,
                  new PlatformActions.RemoveScoreboardAction(),
                  ExecutionContext.EMPTY));
      assertThrows(
          NullPointerException.class,
          () -> adapter.handleRemoveScoreboard(instance, state, null, ExecutionContext.EMPTY));

      // Test Show Bossbar & Update Bossbar
      PlatformActions.ShowBossbarAction showBossbar =
          new PlatformActions.ShowBossbarAction("RED", "SOLID", "Boss Title", "HITS");
      assertTrue(
          adapter.handleShowBossbar(instance, state, showBossbar, ExecutionContext.EMPTY).join());

      PlatformActions.UpdateBossbarAction updateBossbar =
          new PlatformActions.UpdateBossbarAction("Updated Boss Title", "HITS");
      assertTrue(
          adapter
              .handleUpdateBossbar(instance, state, updateBossbar, ExecutionContext.EMPTY)
              .join());

      // Test Show Scoreboard & Remove Scoreboard
      PlatformActions.ShowScoreboardAction showScoreboard =
          new PlatformActions.ShowScoreboardAction("Score Title", List.of("Line A", "Line B"));
      assertTrue(
          adapter
              .handleShowScoreboard(instance, state, showScoreboard, ExecutionContext.EMPTY)
              .join());

      assertTrue(
          adapter
              .handleRemoveScoreboard(
                  instance,
                  state,
                  new PlatformActions.RemoveScoreboardAction(),
                  ExecutionContext.EMPTY)
              .join());

      assertTrue(
          adapter
              .handleRemoveBossbar(
                  instance,
                  state,
                  new PlatformActions.RemoveBossbarAction(),
                  ExecutionContext.EMPTY)
              .join());

      // Refresh HUD
      adapter.refreshHud(instance, state);

      // Entity tracking and cleanup with bossbar and scoreboard
      UUID recoveredEntity = UUID.randomUUID();
      adapter.registerRecoveredEntity(id, recoveredEntity);

      PlatformActions.ShowBossbarAction showBar =
          new PlatformActions.ShowBossbarAction("RED", "SOLID", "Boss Title", "HITS");
      adapter.handleShowBossbar(instance, state, showBar, ExecutionContext.EMPTY).join();

      PlatformActions.ShowScoreboardAction showBoard =
          new PlatformActions.ShowScoreboardAction("Score Title", List.of("Line A"));
      adapter.handleShowScoreboard(instance, state, showBoard, ExecutionContext.EMPTY).join();

      // 1 entity + 1 bossbar + 1 scoreboard = 3
      assertEquals(3, adapter.resourceCount(id));

      adapter.cleanupAll();
      assertEquals(0, adapter.resourceCount(id));

      assertNotNull(adapter.getBossBarManager());
      assertNotNull(adapter.getScoreboardManager());
    }
  }

  @Test
  void testPresentationMessagingAndTitles() throws IOException {
    LocaleCatalog locales = new FileSystemLocaleLoader(tempDir).loadCatalog("en-US");

    Plugin plugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (proxy, method, args) -> {
                  if ("getLogger".equals(method.getName())) {
                    return Logger.getLogger("test");
                  }
                  return null;
                });

    Audience mockAudience =
        (Audience)
            Proxy.newProxyInstance(
                Audience.class.getClassLoader(),
                new Class<?>[] {Audience.class},
                (proxy, method, args) -> null);

    try (BukkitAudiences adventure =
        (BukkitAudiences)
            Proxy.newProxyInstance(
                BukkitAudiences.class.getClassLoader(),
                new Class<?>[] {BukkitAudiences.class},
                (proxy, method, args) -> {
                  if ("sender".equals(method.getName()) || "all".equals(method.getName())) {
                    return mockAudience;
                  }
                  return null;
                })) {
      SpigotActionAdapter adapter = new SpigotActionAdapter(plugin, adventure, locales);

      EventInstanceId id = EventInstanceId.generate();
      EventInstance instance = EventInstance.create(id, new EventDefinitionId("test"));
      EventRuntimeState state = new EventRuntimeState(id);

      Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "PlayerPresentation");

      // Messaging
      adapter.sendPlayerMessage(player, "Hello <yellow>%id%</yellow>", instance, state);
      adapter.broadcastMessage("Broadcast <red>%id%</red>", instance, state);
      adapter.broadcastRawMessage("Raw message");
      adapter.showTitle(
          "<blue>Title</blue>", "<green>Subtitle</green>", 10, 70, 20, instance, state);

      // Entity custom name
      World world = Bukkit.getWorlds().getFirst();
      Entity entity = world.spawnEntity(new Location(world, 0, 64, 0), EntityType.ZOMBIE);
      adapter.setEntityCustomName(entity, "<gold>Boss %id%</gold>", instance, state);

      // Shared loot inventory
      BukkitSharedLootHolder holder = new BukkitSharedLootHolder(id);
      Inventory inv = adapter.createSharedLootInventory(holder, 27, "Loot: %id%", instance, state);
      assertNotNull(inv);

      // Player join event
      PlayerJoinEvent joinEvent =
          io.github.kizio806.spectraevents.platform.bukkit.compat.BukkitPlatformBridge
              .createPlayerJoinEvent(player);
      adapter.onPlayerJoin(joinEvent);
    }
  }

  @Test
  void testExecutionMethodsAndAsyncScheduler() throws IOException {
    LocaleCatalog locales = new FileSystemLocaleLoader(tempDir).loadCatalog("en-US");

    Plugin plugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (proxy, method, args) -> {
                  if ("getLogger".equals(method.getName())) {
                    return Logger.getLogger("test");
                  }
                  return null;
                });

    try (BukkitAudiences adventure =
        (BukkitAudiences)
            Proxy.newProxyInstance(
                BukkitAudiences.class.getClassLoader(),
                new Class<?>[] {BukkitAudiences.class},
                (proxy, method, args) -> null)) {
      SpigotActionAdapter adapter = new SpigotActionAdapter(plugin, adventure, locales);
      EventInstanceId id = EventInstanceId.generate();
      World world = Bukkit.getWorlds().getFirst();
      Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "PlayerExec");

      // Primary thread execution
      assertTrue(adapter.executeAt(id, new Location(world, 0, 64, 0), () -> {}).join());
      assertTrue(adapter.executeGlobal(id, () -> {}).join());
      assertTrue(adapter.executeFor(id, player, () -> {}).join());

      // Off-primary-thread execution (via scheduler)
      MockBukkitSetup.setPrimaryThread(false);
      assertTrue(adapter.executeGlobal(id, () -> {}).join());
      MockBukkitSetup.setPrimaryThread(true);

      // Shutting down
      adapter.cleanupAll();
      assertFalse(adapter.executeGlobal(id, () -> {}).join());
    }
  }

  @Test
  void testModelRuntimeServiceIntegrationAndEntityCleanup() throws IOException {
    LocaleCatalog locales = new FileSystemLocaleLoader(tempDir).loadCatalog("en-US");

    Plugin plugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (proxy, method, args) -> {
                  if ("getLogger".equals(method.getName())) {
                    return Logger.getLogger("test");
                  }
                  return null;
                });

    try (BukkitAudiences adventure =
        (BukkitAudiences)
            Proxy.newProxyInstance(
                BukkitAudiences.class.getClassLoader(),
                new Class<?>[] {BukkitAudiences.class},
                (proxy, method, args) -> null)) {
      SpigotActionAdapter adapter = new SpigotActionAdapter(plugin, adventure, locales);
      EventInstanceId id = EventInstanceId.generate();

      // Spawned entity registration & removal
      World world = Bukkit.getWorlds().getFirst();
      Entity entity = world.spawnEntity(new Location(world, 0, 64, 0), EntityType.ZOMBIE);
      adapter.registerSpawnedEntity(id, entity);
      assertEquals(1, adapter.resourceCount(id));

      // Model service integration
      ModelDefinitionRegistry modelReg = new ModelDefinitionRegistry();
      ModelId modelId = new ModelId("dummy_model");
      ModelPartDefinition part =
          new ModelPartDefinition(
              new ModelPartId("part1"),
              null,
              ModelPartType.ITEM_DISPLAY,
              ModelTransform.IDENTITY,
              ModelTransform.IDENTITY,
              ModelRenderProperties.DEFAULT,
              new ItemAssetRef("minecraft:diamond_chestplate", DisplayTransformMode.HEAD));
      ModelDefinition modelDef = new ModelDefinition(modelId, List.of(part), List.of());
      modelReg.register(modelDef);

      ModelRendererPort rendererPort =
          (ModelRendererPort)
              Proxy.newProxyInstance(
                  ModelRendererPort.class.getClassLoader(),
                  new Class<?>[] {ModelRendererPort.class},
                  (proxy, method, args) -> {
                    if (method.getReturnType().equals(boolean.class)) {
                      return true;
                    }
                    if ("spawnModel".equals(method.getName())) {
                      ModelRuntimeId rtId = (ModelRuntimeId) args[0];
                      EventInstanceId ownId = (EventInstanceId) args[3];
                      return new RenderedModelHandle(
                          rtId,
                          modelId,
                          ownId,
                          ModelAnchor.of("world", 0, 0, 0),
                          Map.of(
                              new ModelPartId("part1"),
                              new RenderedPartHandle(new ModelPartId("part1"), UUID.randomUUID())),
                          Map.of());
                    }
                    return null;
                  });

      ModelRuntimeService modelService = new ModelRuntimeService(modelReg, rendererPort);
      adapter.setModelRuntimeService(modelService);

      EventInstanceId otherId = EventInstanceId.generate();
      modelService.spawnModel(modelId, ModelAnchor.of("world", 0, 0, 0), id);
      modelService.spawnModel(modelId, ModelAnchor.of("world", 0, 0, 0), otherId);

      // Model adds 1 part to resource count (1 entity + 1 part = 2)
      assertEquals(2, adapter.resourceCount(id));

      // Cleanup removes id entity and id model, but leaves otherId model untouched
      adapter.cleanupEvent(id);
      assertEquals(0, adapter.resourceCount(id));
      assertEquals(1, modelService.getActiveInstances().size());

      // Cleanup event with no owned entities
      adapter.cleanupEvent(EventInstanceId.generate());
    }
  }

  @Test
  void testExecuteWhenServerIsNull() throws Exception {
    org.bukkit.plugin.Plugin dummyPlugin =
        (org.bukkit.plugin.Plugin)
            Proxy.newProxyInstance(
                org.bukkit.plugin.Plugin.class.getClassLoader(),
                new Class<?>[] {org.bukkit.plugin.Plugin.class},
                (proxy, method, args) -> null);

    try (BukkitAudiences dummyAudiences =
        (BukkitAudiences)
            Proxy.newProxyInstance(
                BukkitAudiences.class.getClassLoader(),
                new Class<?>[] {BukkitAudiences.class},
                (proxy, method, args) -> null)) {
      LocaleCatalog locales = new FileSystemLocaleLoader(tempDir).loadCatalog("en-US");
      SpigotActionAdapter localAdapter =
          new SpigotActionAdapter(dummyPlugin, dummyAudiences, locales);

      Server currentServer = Bukkit.getServer();
      Field serverField = Bukkit.class.getDeclaredField("server");
      serverField.setAccessible(true);
      try {
        serverField.set(null, null);
        CompletableFuture<Boolean> future =
            localAdapter.executeGlobal(EventInstanceId.generate(), () -> {});
        assertTrue(future.isDone());
      } finally {
        serverField.set(null, currentServer);
      }
    }
  }
}
