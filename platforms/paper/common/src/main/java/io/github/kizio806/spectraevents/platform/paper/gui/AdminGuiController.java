package io.github.kizio806.spectraevents.platform.paper.gui;

import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.integration.IntegrationRegistry;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.application.service.EventOrchestrationService;
import io.github.kizio806.spectraevents.application.update.UpdateService;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.plugin.java.JavaPlugin;

/** Controls opening screens and handles click events for the SpectraEvents Admin GUI. */
public final class AdminGuiController implements Listener {
  public enum MenuType {
    MAIN,
    ACTIVE_EVENTS,
    DEFINITIONS,
    INTEGRATIONS,
    UPDATES
  }

  private final JavaPlugin plugin;
  private final EventOrchestrationService orchestrationService;
  private final EventDefinitionRegistry definitionRegistry;
  private final EventInstanceRepository instanceRepository;
  private final IntegrationRegistry integrationRegistry;
  private final UpdateService updateService;

  private final Map<UUID, MenuType> openSessions = new ConcurrentHashMap<>();

  public AdminGuiController(
      JavaPlugin plugin,
      EventOrchestrationService orchestrationService,
      EventDefinitionRegistry definitionRegistry,
      EventInstanceRepository instanceRepository,
      IntegrationRegistry integrationRegistry,
      UpdateService updateService) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.orchestrationService =
        Objects.requireNonNull(orchestrationService, "orchestrationService");
    this.definitionRegistry = Objects.requireNonNull(definitionRegistry, "definitionRegistry");
    this.instanceRepository = Objects.requireNonNull(instanceRepository, "instanceRepository");
    this.integrationRegistry = Objects.requireNonNull(integrationRegistry, "integrationRegistry");
    this.updateService = Objects.requireNonNull(updateService, "updateService");
  }

  public void openMainMenu(Player player) {
    openSessions.put(player.getUniqueId(), MenuType.MAIN);
    player.openInventory(MainScreen.createInventory());
  }

  public void openActiveEventsMenu(Player player) {
    openSessions.put(player.getUniqueId(), MenuType.ACTIVE_EVENTS);
    player.openInventory(ActiveEventsScreen.createInventory(instanceRepository));
  }

  public void openDefinitionsMenu(Player player) {
    openSessions.put(player.getUniqueId(), MenuType.DEFINITIONS);
    player.openInventory(DefinitionsScreen.createInventory(definitionRegistry));
  }

  public void openIntegrationsMenu(Player player) {
    openSessions.put(player.getUniqueId(), MenuType.INTEGRATIONS);
    player.openInventory(IntegrationsScreen.createInventory(integrationRegistry));
  }

  public void openUpdatesMenu(Player player) {
    openSessions.put(player.getUniqueId(), MenuType.UPDATES);
    player.openInventory(UpdatesScreen.createInventory(updateService));
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getInventory().getHolder() instanceof AdminGuiHolder holder)) return;

    event.setCancelled(true);

    if (!(event.getWhoClicked() instanceof Player player)) return;

    int slot = event.getRawSlot();
    switch (holder.menuType()) {
      case MAIN:
        if (slot == 10) openActiveEventsMenu(player);
        else if (slot == 12) openDefinitionsMenu(player);
        else if (slot == 14) openIntegrationsMenu(player);
        else if (slot == 16) openUpdatesMenu(player);
        break;
      case ACTIVE_EVENTS:
      case DEFINITIONS:
        if (slot == 49) openMainMenu(player);
        break;
      case INTEGRATIONS:
        if (slot == 31) openMainMenu(player);
        break;
      case UPDATES:
        if (slot == 22) openMainMenu(player);
        break;
      default:
        break;
    }
  }

  @EventHandler
  public void onInventoryClose(InventoryCloseEvent event) {
    openSessions.remove(event.getPlayer().getUniqueId());
  }
}
