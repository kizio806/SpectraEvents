package io.github.kizio806.spectraevents.platform.bukkit.gui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.config.locale.FileSystemLocaleLoader;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.integration.IntegrationRegistry;
import io.github.kizio806.spectraevents.application.repository.InMemoryEventInstanceRepository;
import io.github.kizio806.spectraevents.application.service.EventOrchestrationService;
import io.github.kizio806.spectraevents.application.update.UpdateInfo;
import io.github.kizio806.spectraevents.application.update.UpdatePort;
import io.github.kizio806.spectraevents.application.update.UpdateService;
import io.github.kizio806.spectraevents.platform.bukkit.config.BukkitEventSettingsStore;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AdminGuiControllerSessionTest {

  @TempDir Path temporaryDirectory;

  private final UpdatePort dummyUpdatePort =
      new UpdatePort() {
        @Override
        public CompletableFuture<UpdateInfo> checkUpdate(String version, String channel) {
          return CompletableFuture.completedFuture(UpdateInfo.upToDate(version));
        }

        @Override
        public CompletableFuture<Boolean> downloadUpdate(
            String downloadUrl, String targetFileName) {
          return CompletableFuture.completedFuture(false);
        }
      };

  @Test
  void constructorValidatesNullArguments() throws Exception {
    EventDefinitionRegistry defs = new EventDefinitionRegistry();
    InMemoryEventInstanceRepository repo = new InMemoryEventInstanceRepository();
    IntegrationRegistry integrations = new IntegrationRegistry();
    UpdateService updates = new UpdateService("1.0.0", dummyUpdatePort);
    EventOrchestrationService orch = new EventOrchestrationService(repo, defs);
    BukkitEventSettingsStore store = new BukkitEventSettingsStore(temporaryDirectory);
    LocaleCatalog locales = new FileSystemLocaleLoader(temporaryDirectory).loadCatalog("en-US");

    assertThrows(
        NullPointerException.class,
        () -> new AdminGuiController(null, repo, integrations, updates, orch, store, locales));
    assertThrows(
        NullPointerException.class,
        () -> new AdminGuiController(defs, null, integrations, updates, orch, store, locales));
  }

  @Test
  void reportsNoOpenSessionForUnknownPlayer() throws Exception {
    EventDefinitionRegistry defs = new EventDefinitionRegistry();
    InMemoryEventInstanceRepository repo = new InMemoryEventInstanceRepository();
    IntegrationRegistry integrations = new IntegrationRegistry();
    UpdateService updates = new UpdateService("1.0.0", dummyUpdatePort);
    EventOrchestrationService orch = new EventOrchestrationService(repo, defs);
    BukkitEventSettingsStore store = new BukkitEventSettingsStore(temporaryDirectory);
    LocaleCatalog locales = new FileSystemLocaleLoader(temporaryDirectory).loadCatalog("en-US");

    AdminGuiController controller =
        new AdminGuiController(defs, repo, integrations, updates, orch, store, locales);
    UUID playerId = UUID.randomUUID();

    assertFalse(controller.hasOpenSession(playerId));
    assertTrue(controller.getOpenSession(playerId).isEmpty());
  }

  @Test
  void testMenuNavigationAndClicks() throws Exception {
    io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup.ensureInitialized();
    EventDefinitionRegistry defs = new EventDefinitionRegistry();
    InMemoryEventInstanceRepository repo = new InMemoryEventInstanceRepository();
    IntegrationRegistry integrations = new IntegrationRegistry();
    UpdateService updates = new UpdateService("1.0.0", dummyUpdatePort);
    EventOrchestrationService orch = new EventOrchestrationService(repo, defs);
    BukkitEventSettingsStore store = new BukkitEventSettingsStore(temporaryDirectory);
    LocaleCatalog locales = new FileSystemLocaleLoader(temporaryDirectory).loadCatalog("en-US");

    AdminGuiController controller =
        new AdminGuiController(defs, repo, integrations, updates, orch, store, locales);
    UUID playerId = UUID.randomUUID();
    org.bukkit.entity.Player player =
        io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup.createMockPlayer(
            playerId, "GuiAdminTest");
    io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup.registerPlayer(
        player);

    try {
      // 1. Open menus directly
      controller.openMainMenu(player);
      assertTrue(controller.hasOpenSession(playerId));
      org.junit.jupiter.api.Assertions.assertEquals(
          AdminGuiController.MenuType.MAIN, controller.getOpenSession(playerId).orElseThrow());

      controller.openActiveEventsMenu(player);
      org.junit.jupiter.api.Assertions.assertEquals(
          AdminGuiController.MenuType.ACTIVE_EVENTS,
          controller.getOpenSession(playerId).orElseThrow());

      controller.openDefinitionsMenu(player);
      org.junit.jupiter.api.Assertions.assertEquals(
          AdminGuiController.MenuType.DEFINITIONS,
          controller.getOpenSession(playerId).orElseThrow());

      controller.openConfigurationMenu(player);
      org.junit.jupiter.api.Assertions.assertEquals(
          AdminGuiController.MenuType.EVENT_CONFIGURATION,
          controller.getOpenSession(playerId).orElseThrow());

      controller.openLocationsMenu(player);
      org.junit.jupiter.api.Assertions.assertEquals(
          AdminGuiController.MenuType.LOCATIONS, controller.getOpenSession(playerId).orElseThrow());

      controller.openIntegrationsMenu(player);
      org.junit.jupiter.api.Assertions.assertEquals(
          AdminGuiController.MenuType.INTEGRATIONS,
          controller.getOpenSession(playerId).orElseThrow());

      controller.openUpdatesMenu(player);
      org.junit.jupiter.api.Assertions.assertEquals(
          AdminGuiController.MenuType.UPDATES, controller.getOpenSession(playerId).orElseThrow());

      // 2. Open detail menus with missing items (error handling)
      controller.openDefinitionDetailMenu(player, "nonexistent_def");
      controller.openEventInstanceDetailMenu(player, "nonexistent_inst");

      // 3. Test pagination clicks
      org.bukkit.inventory.Inventory activeInv =
          ActiveEventsScreen.createInventory(repo, locales, 0);
      controller.onInventoryClick(
          io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup
              .createClickEvent(player, activeInv, EventGuiPagination.SLOT_PREVIOUS));
      controller.onInventoryClick(
          io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup
              .createClickEvent(player, activeInv, EventGuiPagination.SLOT_NEXT));
      controller.onInventoryClick(
          io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup
              .createClickEvent(player, activeInv, 53)); // refresh

      org.bukkit.inventory.Inventory defsInv = DefinitionsScreen.createInventory(defs, locales, 0);
      controller.onInventoryClick(
          io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup
              .createClickEvent(player, defsInv, EventGuiPagination.SLOT_PREVIOUS));
      controller.onInventoryClick(
          io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup
              .createClickEvent(player, defsInv, EventGuiPagination.SLOT_NEXT));
      controller.onInventoryClick(
          io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup
              .createClickEvent(player, defsInv, 53)); // refresh

      org.bukkit.inventory.Inventory cfgInv =
          EventConfigurationScreen.createInventory(defs, store, locales, 0);
      controller.onInventoryClick(
          io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup
              .createClickEvent(player, cfgInv, EventGuiPagination.SLOT_PREVIOUS));
      controller.onInventoryClick(
          io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup
              .createClickEvent(player, cfgInv, EventGuiPagination.SLOT_NEXT));
      controller.onInventoryClick(
          io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup
              .createClickEvent(player, cfgInv, 53)); // refresh
      controller.onInventoryClick(
          io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup
              .createClickEvent(player, cfgInv, 49)); // main menu

      org.bukkit.inventory.Inventory locsInv = LocationsScreen.createInventory(store, locales, 0);
      controller.onInventoryClick(
          io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup
              .createClickEvent(player, locsInv, EventGuiPagination.SLOT_PREVIOUS));
      controller.onInventoryClick(
          io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup
              .createClickEvent(player, locsInv, EventGuiPagination.SLOT_NEXT));
      controller.onInventoryClick(
          io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup
              .createClickEvent(player, locsInv, 53)); // refresh
      controller.onInventoryClick(
          io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup
              .createClickEvent(player, locsInv, 49)); // main menu

      // 4. Test cancel confirmation abort
      org.bukkit.inventory.Inventory cancelInv =
          EventCancelConfirmationScreen.createInventory("inst_1", locales);
      controller.onInventoryClick(
          io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup
              .createClickEvent(player, cancelInv, EventCancelConfirmationScreen.SLOT_ABORT));

      // 5. Test location remove confirmation abort
      org.bukkit.inventory.Inventory locRemoveInv =
          LocationRemoveConfirmationScreen.createInventory("loc_1", locales);
      controller.onInventoryClick(
          io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup
              .createClickEvent(player, locRemoveInv, LocationRemoveConfirmationScreen.SLOT_ABORT));

      // 6. Test inventory close removes session
      org.bukkit.event.inventory.InventoryCloseEvent closeEvent =
          new org.bukkit.event.inventory.InventoryCloseEvent(
              io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup
                  .createClickEvent(player, activeInv, 0)
                  .getView());
      controller.onInventoryClose(closeEvent);
      assertFalse(controller.hasOpenSession(playerId));
    } finally {
      io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup.unregisterPlayer(
          playerId);
    }
  }
}
