package io.github.kizio806.spectraevents.platform.bukkit.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.config.spec.EventParameterSpec;
import io.github.kizio806.spectraevents.application.config.spec.EventParameterType;
import io.github.kizio806.spectraevents.application.config.spec.EventSpec;
import io.github.kizio806.spectraevents.application.config.spec.PhaseSpec;
import io.github.kizio806.spectraevents.application.execution.EventExecutionEngine;
import io.github.kizio806.spectraevents.application.execution.EventLocation;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeStateStore;
import io.github.kizio806.spectraevents.application.integration.IntegrationRegistry;
import io.github.kizio806.spectraevents.application.integration.IntegrationState;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.application.port.PlatformActionPort;
import io.github.kizio806.spectraevents.application.service.EventOrchestrationService;
import io.github.kizio806.spectraevents.application.update.UpdateCheckStatus;
import io.github.kizio806.spectraevents.application.update.UpdateInfo;
import io.github.kizio806.spectraevents.application.update.UpdatePort;
import io.github.kizio806.spectraevents.application.update.UpdateService;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinition;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.phase.PhaseDefinition;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleState;
import io.github.kizio806.spectraevents.core.gameplay.health.Health;
import io.github.kizio806.spectraevents.platform.bukkit.config.BukkitEventSettingsStore;
import io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup;
import java.io.File;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AdminGuiScreensTest {

  @TempDir File tempDir;

  private LocaleCatalog locales;
  private EventDefinitionRegistry definitionRegistry;
  private EventInstanceRepository instanceRepository;
  private IntegrationRegistry integrationRegistry;
  private UpdateService updateService;
  private EventOrchestrationService orchestrationService;
  private BukkitEventSettingsStore settingsStore;
  private AdminGuiController controller;
  private Player player;
  private EventInstance runningInstance;

  @BeforeAll
  static void beforeAll() {
    MockBukkitSetup.ensureInitialized();
  }

  @BeforeEach
  void setUp() {
    locales = LocaleCatalog.of(Map.of());
    definitionRegistry = new EventDefinitionRegistry();
    integrationRegistry = new IntegrationRegistry();

    EventDefinitionId defId = new EventDefinitionId("test_event");
    PhaseId phaseId = new PhaseId("phase1");
    PhaseDefinition phase1 = new PhaseDefinition(phaseId, Set.of(), List.of(), List.of());
    EventDefinition def = new EventDefinition(defId, phaseId, Map.of(phaseId, phase1));

    java.math.BigDecimal one = java.math.BigDecimal.ONE;
    java.math.BigDecimal hundred = java.math.BigDecimal.valueOf(100);
    Map<String, EventParameterSpec> params = new HashMap<>();
    params.put(
        "duration",
        new EventParameterSpec(
            "duration",
            EventParameterType.DURATION,
            "5m",
            java.math.BigDecimal.valueOf(60),
            java.math.BigDecimal.valueOf(3600),
            java.math.BigDecimal.valueOf(60),
            true));
    params.put(
        "enabled",
        new EventParameterSpec(
            "enabled", EventParameterType.BOOLEAN, "true", null, null, one, true));
    params.put(
        "max-players",
        new EventParameterSpec(
            "max-players", EventParameterType.INTEGER, 10, one, hundred, one, true));
    params.put(
        "ratio",
        new EventParameterSpec("ratio", EventParameterType.DECIMAL, 1.5, one, hundred, one, true));

    Map<String, PhaseSpec> phaseSpecs =
        Map.of(phaseId.value(), new PhaseSpec(Set.of(), List.of(), List.of()));
    EventSpec sourceSpec =
        new EventSpec(defId.value(), "1", phaseId.value(), phaseSpecs, Map.of(), Map.of(), params);
    definitionRegistry.register(def, "test.yml", sourceSpec);

    runningInstance =
        EventInstance.reconstitute(
            EventInstanceId.generate(), defId, EventLifecycleState.RUNNING, phaseId);

    List<EventInstance> instances = new ArrayList<>();
    instances.add(runningInstance);

    instanceRepository =
        (EventInstanceRepository)
            Proxy.newProxyInstance(
                EventInstanceRepository.class.getClassLoader(),
                new Class<?>[] {EventInstanceRepository.class},
                (proxy, method, args) -> {
                  return switch (method.getName()) {
                    case "findAll" -> List.copyOf(instances);
                    case "findById" -> Optional.of(runningInstance);
                    default -> null;
                  };
                });

    integrationRegistry.register("Vault", IntegrationState.ACTIVE, "Economy integration");
    integrationRegistry.register("PlaceholderAPI", IntegrationState.AVAILABLE, "Placeholders");
    integrationRegistry.register("LuckPerms", IntegrationState.ABSENT, "Permissions");
    integrationRegistry.register("WorldGuard", IntegrationState.FAILED, "Regions");
    integrationRegistry.register("Nexo", IntegrationState.DEGRADED, "Items");

    UpdatePort updatePort =
        (UpdatePort)
            Proxy.newProxyInstance(
                UpdatePort.class.getClassLoader(),
                new Class<?>[] {UpdatePort.class},
                (proxy, method, args) -> {
                  if ("checkUpdate".equals(method.getName())) {
                    return CompletableFuture.completedFuture(
                        new UpdateInfo(
                            "1.0.0",
                            "1.1.0",
                            true,
                            "https://example.com",
                            "New release with fixes",
                            Instant.now(),
                            UpdateCheckStatus.UPDATE_AVAILABLE));
                  }
                  if ("downloadUpdate".equals(method.getName())) {
                    return CompletableFuture.completedFuture(true);
                  }
                  return null;
                });
    updateService = new UpdateService("1.0.0", updatePort);

    EventTaskScheduler scheduler =
        (EventTaskScheduler)
            Proxy.newProxyInstance(
                EventTaskScheduler.class.getClassLoader(),
                new Class<?>[] {EventTaskScheduler.class},
                (proxy, method, args) -> null);

    PlatformActionPort platformActionPort =
        (PlatformActionPort)
            Proxy.newProxyInstance(
                PlatformActionPort.class.getClassLoader(),
                new Class<?>[] {PlatformActionPort.class},
                (proxy, method, args) -> CompletableFuture.completedFuture(true));

    EventRuntimeState state = new EventRuntimeState(runningInstance.id());
    state.setHealth(new Health(500, 1000));

    EventRuntimeStateStore stateStore = new EventRuntimeStateStore();
    stateStore.put(state);

    EventExecutionEngine executionEngine =
        new EventExecutionEngine(
            instanceRepository, definitionRegistry, scheduler, platformActionPort, stateStore);

    orchestrationService =
        new EventOrchestrationService(instanceRepository, definitionRegistry, executionEngine);

    settingsStore = new BukkitEventSettingsStore(tempDir.toPath());
    settingsStore.setParameter("test_event", "duration", "15m");
    settingsStore.setParameter("test_event", "enabled", "false");
    settingsStore.saveLocation("spawn_point", new EventLocation("world", 10.0, 64.0, 20.0, 0f, 0f));

    controller =
        new AdminGuiController(
            definitionRegistry,
            instanceRepository,
            integrationRegistry,
            updateService,
            orchestrationService,
            settingsStore,
            locales);

    player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "AdminPlayer");
  }

  @Test
  void testAdminGuiHolderAndPagination() {
    Inventory inv =
        AdminGuiHolder.createInventory(
            AdminGuiController.MenuType.MAIN,
            "custom-extra",
            Map.of(3, "payload3"),
            27,
            Component.text("Custom Title"));

    assertNotNull(inv);
    AdminGuiHolder holder = (AdminGuiHolder) inv.getHolder();
    assertNotNull(holder);
    assertEquals(AdminGuiController.MenuType.MAIN, holder.menuType());
    assertEquals("custom-extra", holder.extra());
    assertEquals("payload3", holder.payloadForSlot(3));
    assertNull(holder.payloadForSlot(1));
    assertEquals(inv, holder.getInventory());

    // Pagination helper tests
    assertEquals(0, EventGuiPagination.pageFor(-1, 100));
    assertEquals(0, EventGuiPagination.pageFor(0, 100));
    assertEquals(1, EventGuiPagination.pageFor(1, 100));
    assertEquals(2, EventGuiPagination.pageFor(10, 100)); // clamped to last page
    assertEquals(0, EventGuiPagination.lastPageFor(0));
    assertEquals(0, EventGuiPagination.lastPageFor(45));
    assertEquals(1, EventGuiPagination.lastPageFor(46));

    List<String> items = List.of("a", "b", "c");
    assertEquals(items, EventGuiPagination.itemsOnPage(items, 0));
    assertEquals(List.of(), EventGuiPagination.itemsOnPage(items, 2));

    Inventory pagInv =
        AdminGuiHolder.createInventory(
            AdminGuiController.MenuType.ACTIVE_EVENTS, 54, Component.text("Pagination"));
    EventGuiPagination.addControls(pagInv, 0, 90, locales);
    assertNotNull(pagInv.getItem(EventGuiPagination.SLOT_NEXT));
    assertNull(pagInv.getItem(EventGuiPagination.SLOT_PREVIOUS));

    pagInv.clear();
    EventGuiPagination.addControls(pagInv, 1, 90, locales);
    assertNotNull(pagInv.getItem(EventGuiPagination.SLOT_PREVIOUS));
    assertNull(pagInv.getItem(EventGuiPagination.SLOT_NEXT));
  }

  @Test
  void testMainScreen() {
    Inventory inv = MainScreen.createInventory(locales);
    assertNotNull(inv);
    assertEquals(27, inv.getSize());
    assertNotNull(inv.getItem(4));
    assertNotNull(inv.getItem(10));
    assertNotNull(inv.getItem(12));
    assertNotNull(inv.getItem(14));
    assertNotNull(inv.getItem(16));
    assertNotNull(inv.getItem(20));
    assertNotNull(inv.getItem(22));

    assertNotNull(MainScreen.createFillerItem(Material.GRAY_STAINED_GLASS_PANE));
  }

  @Test
  void testDashboardScreen() {
    Inventory inv =
        DashboardScreen.createInventory(
            definitionRegistry, instanceRepository, integrationRegistry, locales);
    assertNotNull(inv);
    assertEquals(27, inv.getSize());
    assertNotNull(inv.getItem(10));
    assertNotNull(inv.getItem(12));
    assertNotNull(inv.getItem(14));
    assertNotNull(inv.getItem(16));
    assertNotNull(inv.getItem(22));
    assertNotNull(inv.getItem(26));
  }

  @Test
  void testActiveEventsScreen() {
    Inventory inv = ActiveEventsScreen.createInventory(instanceRepository, locales, 0);
    assertNotNull(inv);
    assertEquals(54, inv.getSize());
    assertNotNull(inv.getItem(0)); // running instance item
    assertNotNull(inv.getItem(49)); // back button
    assertNotNull(inv.getItem(53)); // refresh
  }

  @Test
  void testDefinitionsScreen() {
    Inventory inv = DefinitionsScreen.createInventory(definitionRegistry, locales, 0);
    assertNotNull(inv);
    assertEquals(54, inv.getSize());
    assertNotNull(inv.getItem(0)); // test_event item
    assertNotNull(inv.getItem(49)); // back button
    assertNotNull(inv.getItem(53)); // refresh
  }

  @Test
  void testDefinitionDetailScreen() {
    assertNull(DefinitionDetailScreen.createInventory("non_existent", definitionRegistry, locales));

    Inventory inv =
        DefinitionDetailScreen.createInventory("test_event", definitionRegistry, locales);
    assertNotNull(inv);
    assertEquals(54, inv.getSize());
    assertNotNull(inv.getItem(0)); // phase1
    assertNotNull(inv.getItem(DefinitionDetailScreen.SLOT_BACK));
    assertNotNull(inv.getItem(DefinitionDetailScreen.SLOT_START));
  }

  @Test
  void testEventInstanceDetailScreen() throws Exception {
    EventExecutionEngine engine = orchestrationService.executionEngine();

    EventRuntimeState state = engine.stateStore().get(runningInstance.id()).orElseThrow();
    state.setHitCounter(new io.github.kizio806.spectraevents.core.gameplay.hits.HitCounter(3, 10));
    state.setPlatformLocation(
        new io.github.kizio806.spectraevents.application.execution.EventLocation(
            "world", 10.0, 64.0, 20.0, 0f, 0f));
    state.setTimerDeadlineMillis(System.currentTimeMillis() + 60000L);
    state.setLockedUntilMillis(System.currentTimeMillis() + 60000L);

    Inventory inv = EventInstanceDetailScreen.createInventory(runningInstance, engine, locales);
    assertNotNull(inv);
    assertEquals(54, inv.getSize());
    assertNotNull(inv.getItem(22)); // overview item
    assertNotNull(inv.getItem(EventInstanceDetailScreen.SLOT_BACK));
    assertNotNull(inv.getItem(EventInstanceDetailScreen.SLOT_CANCEL));
    assertNotNull(inv.getItem(EventInstanceDetailScreen.SLOT_REFRESH));

    EventInstance unstartedInstance =
        EventInstance.reconstitute(
            EventInstanceId.generate(),
            runningInstance.definitionId(),
            EventLifecycleState.CREATED,
            null);
    Inventory emptyStatusInv =
        EventInstanceDetailScreen.createInventory(unstartedInstance, engine, locales);
    assertNotNull(emptyStatusInv);

    java.lang.reflect.Constructor<EventInstanceDetailScreen> ctor =
        EventInstanceDetailScreen.class.getDeclaredConstructor();
    ctor.setAccessible(true);
    assertNotNull(ctor.newInstance());
  }

  @Test
  void testEventCancelConfirmationScreen() {
    Inventory inv =
        EventCancelConfirmationScreen.createInventory(runningInstance.id().toString(), locales);
    assertNotNull(inv);
    assertEquals(45, inv.getSize());
    assertNotNull(inv.getItem(EventCancelConfirmationScreen.SLOT_CONFIRM));
    assertNotNull(inv.getItem(EventCancelConfirmationScreen.SLOT_ABORT));
  }

  @Test
  void testEventConfigurationScreen() {
    Inventory inv =
        EventConfigurationScreen.createInventory(definitionRegistry, settingsStore, locales, 0);
    assertNotNull(inv);
    assertEquals(54, inv.getSize());
    assertNotNull(inv.getItem(0)); // test_event item
    assertNotNull(inv.getItem(49)); // back
    assertNotNull(inv.getItem(53)); // refresh
  }

  @Test
  void testEventConfigurationDetailScreen() {
    Inventory inv =
        EventConfigurationDetailScreen.createInventory(
            "test_event", definitionRegistry, settingsStore, locales);
    assertNotNull(inv);
    assertEquals(54, inv.getSize());
    assertNotNull(inv.getItem(10)); // parameter item
    assertNotNull(inv.getItem(EventConfigurationDetailScreen.SLOT_BACK));
    assertNotNull(inv.getItem(EventConfigurationDetailScreen.SLOT_REFRESH));
  }

  @Test
  void testLocationsScreenAndRemoveConfirmation() {
    Inventory inv = LocationsScreen.createInventory(settingsStore, locales, 0);
    assertNotNull(inv);
    assertEquals(54, inv.getSize());
    assertNotNull(inv.getItem(0)); // spawn_point location item
    assertNotNull(inv.getItem(45)); // back
    assertNotNull(inv.getItem(49)); // refresh
    assertNotNull(inv.getItem(53)); // close

    Inventory confirmInv = LocationRemoveConfirmationScreen.createInventory("spawn_point", locales);
    assertNotNull(confirmInv);
    assertEquals(45, confirmInv.getSize());
    assertNotNull(confirmInv.getItem(LocationRemoveConfirmationScreen.SLOT_CONFIRM));
    assertNotNull(confirmInv.getItem(LocationRemoveConfirmationScreen.SLOT_ABORT));
  }

  @Test
  void testIntegrationsScreen() {
    Inventory inv = IntegrationsScreen.createInventory(integrationRegistry, locales);
    assertNotNull(inv);
    assertEquals(36, inv.getSize());
    assertNotNull(inv.getItem(9)); // Vault (ACTIVE)
    assertNotNull(inv.getItem(10)); // PlaceholderAPI (AVAILABLE)
    assertNotNull(inv.getItem(11)); // LuckPerms (ABSENT)
    assertNotNull(inv.getItem(12)); // WorldGuard (FAILED)
    assertNotNull(inv.getItem(13)); // Nexo (DEGRADED)
    assertNotNull(inv.getItem(31)); // back button
  }

  @Test
  void testUpdatesScreen() {
    Inventory inv = UpdatesScreen.createInventory(updateService, locales);
    assertNotNull(inv);
    assertEquals(27, inv.getSize());
    assertNotNull(inv.getItem(13)); // update info item
    assertNotNull(inv.getItem(22)); // back button
  }

  @Test
  void testAdminGuiControllerNavigationAndClicks() {
    // Open main screen
    controller.openMainMenu(player);
    assertTrue(controller.hasOpenSession(player.getUniqueId()));
    assertEquals(
        AdminGuiController.MenuType.MAIN,
        controller.getOpenSession(player.getUniqueId()).orElseThrow());

    Inventory mainInv = MainScreen.createInventory(locales);

    // Click slot 4 -> opens DASHBOARD
    InventoryClickEvent click4 = MockBukkitSetup.createClickEvent(player, mainInv, 4);
    controller.onInventoryClick(click4);
    assertEquals(
        AdminGuiController.MenuType.DASHBOARD,
        controller.getOpenSession(player.getUniqueId()).orElseThrow());

    // Click slot 10 -> opens ACTIVE_EVENTS
    InventoryClickEvent click10 = MockBukkitSetup.createClickEvent(player, mainInv, 10);
    controller.onInventoryClick(click10);
    assertEquals(
        AdminGuiController.MenuType.ACTIVE_EVENTS,
        controller.getOpenSession(player.getUniqueId()).orElseThrow());

    // Click slot 12 -> opens DEFINITIONS
    InventoryClickEvent click12 = MockBukkitSetup.createClickEvent(player, mainInv, 12);
    controller.onInventoryClick(click12);
    assertEquals(
        AdminGuiController.MenuType.DEFINITIONS,
        controller.getOpenSession(player.getUniqueId()).orElseThrow());

    // Click slot 14 -> opens INTEGRATIONS
    InventoryClickEvent click14 = MockBukkitSetup.createClickEvent(player, mainInv, 14);
    controller.onInventoryClick(click14);
    assertEquals(
        AdminGuiController.MenuType.INTEGRATIONS,
        controller.getOpenSession(player.getUniqueId()).orElseThrow());

    // Click slot 16 -> opens UPDATES
    InventoryClickEvent click16 = MockBukkitSetup.createClickEvent(player, mainInv, 16);
    controller.onInventoryClick(click16);
    assertEquals(
        AdminGuiController.MenuType.UPDATES,
        controller.getOpenSession(player.getUniqueId()).orElseThrow());

    // Click slot 20 -> opens EVENT_CONFIGURATION
    InventoryClickEvent click20 = MockBukkitSetup.createClickEvent(player, mainInv, 20);
    controller.onInventoryClick(click20);
    assertEquals(
        AdminGuiController.MenuType.EVENT_CONFIGURATION,
        controller.getOpenSession(player.getUniqueId()).orElseThrow());

    // Click slot 22 -> opens LOCATIONS
    InventoryClickEvent click22 = MockBukkitSetup.createClickEvent(player, mainInv, 22);
    controller.onInventoryClick(click22);
    assertEquals(
        AdminGuiController.MenuType.LOCATIONS,
        controller.getOpenSession(player.getUniqueId()).orElseThrow());

    // Test Dashboard clicks
    Inventory dashInv =
        DashboardScreen.createInventory(
            definitionRegistry, instanceRepository, integrationRegistry, locales);
    controller.onInventoryClick(MockBukkitSetup.createClickEvent(player, dashInv, 22)); // refresh
    assertEquals(
        AdminGuiController.MenuType.DASHBOARD,
        controller.getOpenSession(player.getUniqueId()).orElseThrow());
    controller.onInventoryClick(
        MockBukkitSetup.createClickEvent(player, dashInv, 26)); // back to main
    assertEquals(
        AdminGuiController.MenuType.MAIN,
        controller.getOpenSession(player.getUniqueId()).orElseThrow());

    // Test Active Events clicks
    Inventory activeInv = ActiveEventsScreen.createInventory(instanceRepository, locales, 0);
    controller.onInventoryClick(MockBukkitSetup.createClickEvent(player, activeInv, 49)); // refresh
    controller.onInventoryClick(
        MockBukkitSetup.createClickEvent(player, activeInv, 0)); // click instance -> opens detail
    assertEquals(
        AdminGuiController.MenuType.EVENT_INSTANCE_DETAIL,
        controller.getOpenSession(player.getUniqueId()).orElseThrow());

    // Test Instance Detail clicks
    Inventory instanceDetailInv =
        EventInstanceDetailScreen.createInventory(
            runningInstance, orchestrationService.executionEngine(), locales);
    controller.onInventoryClick(
        MockBukkitSetup.createClickEvent(
            player, instanceDetailInv, EventInstanceDetailScreen.SLOT_REFRESH));
    controller.onInventoryClick(
        MockBukkitSetup.createClickEvent(
            player, instanceDetailInv, EventInstanceDetailScreen.SLOT_CANCEL));
    assertEquals(
        AdminGuiController.MenuType.EVENT_CANCEL_CONFIRMATION,
        controller.getOpenSession(player.getUniqueId()).orElseThrow());

    controller.onInventoryClick(
        MockBukkitSetup.createClickEvent(
            player, instanceDetailInv, EventInstanceDetailScreen.SLOT_BACK));
    assertEquals(
        AdminGuiController.MenuType.ACTIVE_EVENTS,
        controller.getOpenSession(player.getUniqueId()).orElseThrow());

    // Test Cancel Confirmation clicks
    Inventory cancelInv =
        EventCancelConfirmationScreen.createInventory(runningInstance.id().toString(), locales);
    controller.onInventoryClick(
        MockBukkitSetup.createClickEvent(
            player, cancelInv, EventCancelConfirmationScreen.SLOT_CONFIRM));
    assertEquals(
        AdminGuiController.MenuType.ACTIVE_EVENTS,
        controller.getOpenSession(player.getUniqueId()).orElseThrow());

    // Test Definitions clicks
    Inventory defsInv = DefinitionsScreen.createInventory(definitionRegistry, locales, 0);
    controller.onInventoryClick(
        MockBukkitSetup.createClickEvent(player, defsInv, 0)); // click def -> opens detail
    assertEquals(
        AdminGuiController.MenuType.DEFINITION_DETAIL,
        controller.getOpenSession(player.getUniqueId()).orElseThrow());

    // Test Definition Detail clicks
    Inventory defDetailInv =
        DefinitionDetailScreen.createInventory("test_event", definitionRegistry, locales);
    controller.onInventoryClick(
        MockBukkitSetup.createClickEvent(player, defDetailInv, DefinitionDetailScreen.SLOT_BACK));
    assertEquals(
        AdminGuiController.MenuType.DEFINITIONS,
        controller.getOpenSession(player.getUniqueId()).orElseThrow());
    controller.onInventoryClick(
        MockBukkitSetup.createClickEvent(player, defDetailInv, DefinitionDetailScreen.SLOT_START));

    // Test start event failure branches (blank definition and unregistered definition)
    Inventory blankDefInv =
        AdminGuiHolder.createInventory(
            AdminGuiController.MenuType.DEFINITION_DETAIL, "", Map.of(), 54, Component.empty());
    controller.onInventoryClick(
        MockBukkitSetup.createClickEvent(player, blankDefInv, DefinitionDetailScreen.SLOT_START));
    Inventory unregDefInv =
        AdminGuiHolder.createInventory(
            AdminGuiController.MenuType.DEFINITION_DETAIL,
            "unregistered_event",
            Map.of(),
            54,
            Component.empty());
    controller.onInventoryClick(
        MockBukkitSetup.createClickEvent(player, unregDefInv, DefinitionDetailScreen.SLOT_START));

    // Test cancel event failure branch (invalid instance ID)
    Inventory badCancelInv =
        AdminGuiHolder.createInventory(
            AdminGuiController.MenuType.EVENT_CANCEL_CONFIRMATION,
            "invalid-uuid",
            Map.of(),
            54,
            Component.empty());
    controller.onInventoryClick(
        MockBukkitSetup.createClickEvent(
            player, badCancelInv, EventCancelConfirmationScreen.SLOT_CONFIRM));

    // Test Configuration Detail clicks and adjustments (duration, integer, decimal)
    Inventory configDetailInv =
        EventConfigurationDetailScreen.createInventory(
            "test_event", definitionRegistry, settingsStore, locales);
    controller.onInventoryClick(
        MockBukkitSetup.createClickEvent(
            player, configDetailInv, EventConfigurationDetailScreen.SLOT_REFRESH));
    controller.onInventoryClick(
        MockBukkitSetup.createClickEvent(
            player,
            configDetailInv,
            10,
            org.bukkit.event.inventory.ClickType.LEFT)); // duration increase
    controller.onInventoryClick(
        MockBukkitSetup.createClickEvent(
            player,
            configDetailInv,
            10,
            org.bukkit.event.inventory.ClickType.RIGHT)); // duration decrease
    controller.onInventoryClick(
        MockBukkitSetup.createClickEvent(
            player,
            configDetailInv,
            12,
            org.bukkit.event.inventory.ClickType.LEFT)); // max-players increase
    controller.onInventoryClick(
        MockBukkitSetup.createClickEvent(
            player,
            configDetailInv,
            12,
            org.bukkit.event.inventory.ClickType.RIGHT)); // max-players decrease
    controller.onInventoryClick(
        MockBukkitSetup.createClickEvent(
            player,
            configDetailInv,
            13,
            org.bukkit.event.inventory.ClickType.LEFT)); // ratio increase
    controller.onInventoryClick(
        MockBukkitSetup.createClickEvent(
            player,
            configDetailInv,
            13,
            org.bukkit.event.inventory.ClickType.RIGHT)); // ratio decrease
    controller.onInventoryClick(
        MockBukkitSetup.createClickEvent(
            player, configDetailInv, EventConfigurationDetailScreen.SLOT_BACK));
    assertEquals(
        AdminGuiController.MenuType.EVENT_CONFIGURATION,
        controller.getOpenSession(player.getUniqueId()).orElseThrow());

    // Test Locations clicks and save location
    Inventory locsInv = LocationsScreen.createInventory(settingsStore, locales, 0);
    controller.onInventoryClick(
        MockBukkitSetup.createClickEvent(player, locsInv, 45)); // slot 45: save current location
    assertEquals(
        AdminGuiController.MenuType.LOCATIONS,
        controller.getOpenSession(player.getUniqueId()).orElseThrow());

    controller.onInventoryClick(
        MockBukkitSetup.createClickEvent(
            player, locsInv, 0)); // click location -> remove confirmation
    assertEquals(
        AdminGuiController.MenuType.LOCATION_REMOVE_CONFIRMATION,
        controller.getOpenSession(player.getUniqueId()).orElseThrow());

    Inventory locRemoveInv =
        LocationRemoveConfirmationScreen.createInventory("spawn_point", locales);
    controller.onInventoryClick(
        MockBukkitSetup.createClickEvent(
            player, locRemoveInv, LocationRemoveConfirmationScreen.SLOT_CONFIRM));
    assertEquals(
        AdminGuiController.MenuType.LOCATIONS,
        controller.getOpenSession(player.getUniqueId()).orElseThrow());

    // Test removing missing location
    Inventory missingLocInv =
        LocationRemoveConfirmationScreen.createInventory("non_existent_loc", locales);
    controller.onInventoryClick(
        MockBukkitSetup.createClickEvent(
            player, missingLocInv, LocationRemoveConfirmationScreen.SLOT_CONFIRM));

    // Test pagination with non-numeric extra
    Inventory invalidPageInv =
        AdminGuiHolder.createInventory(
            AdminGuiController.MenuType.LOCATIONS, "invalid_page", Map.of(), 54, Component.empty());
    controller.onInventoryClick(
        MockBukkitSetup.createClickEvent(player, invalidPageInv, EventGuiPagination.SLOT_PREVIOUS));

    // Test Integrations click back
    Inventory intInv = IntegrationsScreen.createInventory(integrationRegistry, locales);
    controller.onInventoryClick(MockBukkitSetup.createClickEvent(player, intInv, 31));
    assertEquals(
        AdminGuiController.MenuType.MAIN,
        controller.getOpenSession(player.getUniqueId()).orElseThrow());

    // Test Updates click back
    Inventory updInv = UpdatesScreen.createInventory(updateService, locales);
    controller.onInventoryClick(MockBukkitSetup.createClickEvent(player, updInv, 22));
    assertEquals(
        AdminGuiController.MenuType.MAIN,
        controller.getOpenSession(player.getUniqueId()).orElseThrow());

    // Test session close
    InventoryCloseEvent closeEvent =
        new InventoryCloseEvent(MockBukkitSetup.createClickEvent(player, mainInv, 0).getView());
    controller.onInventoryClose(closeEvent);
    assertFalse(controller.hasOpenSession(player.getUniqueId()));
  }
}
