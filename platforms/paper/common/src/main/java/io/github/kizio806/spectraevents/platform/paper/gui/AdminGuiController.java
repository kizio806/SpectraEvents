package io.github.kizio806.spectraevents.platform.paper.gui;

import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.execution.EventLocation;
import io.github.kizio806.spectraevents.application.integration.IntegrationRegistry;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.application.service.EventOrchestrationService;
import io.github.kizio806.spectraevents.application.update.UpdateService;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

/** Controls opening screens and handles click events for the SpectraEvents Admin GUI. */
public final class AdminGuiController implements Listener {
  public enum MenuType {
    MAIN,
    ACTIVE_EVENTS,
    DEFINITIONS,
    /** Detail view for a single event definition — shows phases and offers a Start button. */
    DEFINITION_DETAIL,
    INTEGRATIONS,
    UPDATES
  }

  private final EventDefinitionRegistry definitionRegistry;
  private final EventInstanceRepository instanceRepository;
  private final IntegrationRegistry integrationRegistry;
  private final UpdateService updateService;
  private final EventOrchestrationService orchestrationService;

  private final Map<UUID, MenuType> openSessions = new ConcurrentHashMap<>();

  public AdminGuiController(
      EventDefinitionRegistry definitionRegistry,
      EventInstanceRepository instanceRepository,
      IntegrationRegistry integrationRegistry,
      UpdateService updateService,
      EventOrchestrationService orchestrationService) {
    this.definitionRegistry = Objects.requireNonNull(definitionRegistry, "definitionRegistry");
    this.instanceRepository = Objects.requireNonNull(instanceRepository, "instanceRepository");
    this.integrationRegistry = Objects.requireNonNull(integrationRegistry, "integrationRegistry");
    this.updateService = Objects.requireNonNull(updateService, "updateService");
    this.orchestrationService =
        Objects.requireNonNull(orchestrationService, "orchestrationService");
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

  /**
   * Opens the definition detail screen for the given definition ID.
   *
   * @param player the player opening the screen
   * @param definitionId the definition to display
   */
  public void openDefinitionDetailMenu(Player player, String definitionId) {
    var inv = DefinitionDetailScreen.createInventory(definitionId, definitionRegistry);
    if (inv == null) {
      player.sendMessage(
          Component.text("Definition not found: " + definitionId, NamedTextColor.RED));
      return;
    }
    openSessions.put(player.getUniqueId(), MenuType.DEFINITION_DETAIL);
    player.openInventory(inv);
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
      case MAIN -> {
        if (slot == 10) openActiveEventsMenu(player);
        else if (slot == 12) openDefinitionsMenu(player);
        else if (slot == 14) openIntegrationsMenu(player);
        else if (slot == 16) openUpdatesMenu(player);
      }
      case ACTIVE_EVENTS -> {
        if (slot == 49) openMainMenu(player);
      }
      case DEFINITIONS -> {
        if (slot == 49) {
          openMainMenu(player);
        } else {
          String definitionId = holder.payloadForSlot(slot);
          if (definitionId != null) {
            openDefinitionDetailMenu(player, definitionId);
          }
        }
      }
      case DEFINITION_DETAIL -> {
        if (slot == DefinitionDetailScreen.SLOT_BACK) {
          openDefinitionsMenu(player);
        } else if (slot == DefinitionDetailScreen.SLOT_START) {
          startEventFromGui(player, holder.extra());
        }
      }
      case INTEGRATIONS -> {
        if (slot == 31) openMainMenu(player);
      }
      case UPDATES -> {
        if (slot == 22) openMainMenu(player);
      }
      default -> {}
    }
  }

  @EventHandler
  public void onInventoryClose(InventoryCloseEvent event) {
    openSessions.remove(event.getPlayer().getUniqueId());
  }

  private void startEventFromGui(Player player, String definitionId) {
    if (definitionId == null || definitionId.isBlank()) {
      player.sendMessage(Component.text("No definition selected.", NamedTextColor.RED));
      return;
    }
    try {
      var loc = player.getLocation();
      EventLocation platformLocation =
          new EventLocation(
              loc.getWorld().getName(),
              loc.getX(),
              loc.getY(),
              loc.getZ(),
              loc.getYaw(),
              loc.getPitch());
      var instance = orchestrationService.startDefinition(definitionId, platformLocation);
      player.sendMessage(
          Component.text(
              "Started event '" + definitionId + "' (instance " + instance.id() + ")",
              NamedTextColor.GREEN));
      player.closeInventory();
    } catch (Exception e) {
      player.sendMessage(
          Component.text("Failed to start event: " + e.getMessage(), NamedTextColor.RED));
    }
  }
}
