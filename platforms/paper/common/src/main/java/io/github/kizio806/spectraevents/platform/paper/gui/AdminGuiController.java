package io.github.kizio806.spectraevents.platform.paper.gui;

import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.execution.EventLocation;
import io.github.kizio806.spectraevents.application.integration.IntegrationRegistry;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.application.service.EventOrchestrationService;
import io.github.kizio806.spectraevents.application.update.UpdateService;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.platform.paper.config.PaperEventSettingsStore;
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
    DASHBOARD,
    ACTIVE_EVENTS,
    DEFINITIONS,
    /** Detail view for a single event definition — shows phases and offers a Start button. */
    DEFINITION_DETAIL,
    EVENT_INSTANCE_DETAIL,
    EVENT_CANCEL_CONFIRMATION,
    EVENT_CONFIGURATION,
    EVENT_CONFIGURATION_DETAIL,
    LOCATIONS,
    LOCATION_REMOVE_CONFIRMATION,
    INTEGRATIONS,
    UPDATES
  }

  private final EventDefinitionRegistry definitionRegistry;
  private final EventInstanceRepository instanceRepository;
  private final IntegrationRegistry integrationRegistry;
  private final UpdateService updateService;
  private final EventOrchestrationService orchestrationService;
  private final PaperEventSettingsStore settingsStore;

  private final Map<UUID, MenuType> openSessions = new ConcurrentHashMap<>();

  public AdminGuiController(
      EventDefinitionRegistry definitionRegistry,
      EventInstanceRepository instanceRepository,
      IntegrationRegistry integrationRegistry,
      UpdateService updateService,
      EventOrchestrationService orchestrationService,
      PaperEventSettingsStore settingsStore) {
    this.definitionRegistry = Objects.requireNonNull(definitionRegistry, "definitionRegistry");
    this.instanceRepository = Objects.requireNonNull(instanceRepository, "instanceRepository");
    this.integrationRegistry = Objects.requireNonNull(integrationRegistry, "integrationRegistry");
    this.updateService = Objects.requireNonNull(updateService, "updateService");
    this.orchestrationService =
        Objects.requireNonNull(orchestrationService, "orchestrationService");
    this.settingsStore = Objects.requireNonNull(settingsStore, "settingsStore");
  }

  public void openMainMenu(Player player) {
    openSessions.put(player.getUniqueId(), MenuType.MAIN);
    player.openInventory(MainScreen.createInventory());
  }

  private void openDashboard(Player player) {
    openSessions.put(player.getUniqueId(), MenuType.DASHBOARD);
    player.openInventory(
        DashboardScreen.createInventory(
            definitionRegistry, instanceRepository, integrationRegistry));
  }

  public void openActiveEventsMenu(Player player) {
    openActiveEventsMenu(player, 0);
  }

  private void openActiveEventsMenu(Player player, int page) {
    openSessions.put(player.getUniqueId(), MenuType.ACTIVE_EVENTS);
    player.openInventory(ActiveEventsScreen.createInventory(instanceRepository, page));
  }

  public void openDefinitionsMenu(Player player) {
    openDefinitionsMenu(player, 0);
  }

  private void openDefinitionsMenu(Player player, int page) {
    openSessions.put(player.getUniqueId(), MenuType.DEFINITIONS);
    player.openInventory(DefinitionsScreen.createInventory(definitionRegistry, page));
  }

  public void openEventInstanceDetailMenu(Player player, String instanceId) {
    try {
      EventInstance instance = orchestrationService.getEventInfo(instanceId);
      openSessions.put(player.getUniqueId(), MenuType.EVENT_INSTANCE_DETAIL);
      player.openInventory(
          EventInstanceDetailScreen.createInventory(
              instance,
              Objects.requireNonNull(orchestrationService.executionEngine(), "executionEngine")));
    } catch (IllegalArgumentException exception) {
      player.sendMessage(Component.text("Event instance no longer exists.", NamedTextColor.RED));
      openActiveEventsMenu(player);
    }
  }

  private void openEventCancellationConfirmation(Player player, String instanceId) {
    openSessions.put(player.getUniqueId(), MenuType.EVENT_CANCEL_CONFIRMATION);
    player.openInventory(EventCancelConfirmationScreen.createInventory(instanceId));
  }

  public void openConfigurationMenu(Player player) {
    openConfigurationMenu(player, 0);
  }

  private void openConfigurationMenu(Player player, int page) {
    openSessions.put(player.getUniqueId(), MenuType.EVENT_CONFIGURATION);
    player.openInventory(
        EventConfigurationScreen.createInventory(definitionRegistry, settingsStore, page));
  }

  private void openConfigurationDetailMenu(Player player, String definitionId) {
    openSessions.put(player.getUniqueId(), MenuType.EVENT_CONFIGURATION_DETAIL);
    player.openInventory(
        EventConfigurationDetailScreen.createInventory(
            definitionId, definitionRegistry, settingsStore));
  }

  public void openLocationsMenu(Player player) {
    openLocationsMenu(player, 0);
  }

  private void openLocationsMenu(Player player, int page) {
    openSessions.put(player.getUniqueId(), MenuType.LOCATIONS);
    player.openInventory(LocationsScreen.createInventory(settingsStore, page));
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
        if (slot == 4) openDashboard(player);
        else if (slot == 10) openActiveEventsMenu(player);
        else if (slot == 12) openDefinitionsMenu(player);
        else if (slot == 14) openIntegrationsMenu(player);
        else if (slot == 16) openUpdatesMenu(player);
        else if (slot == 20) openConfigurationMenu(player);
        else if (slot == 22) openLocationsMenu(player);
      }
      case DASHBOARD -> {
        if (slot == 22) {
          openDashboard(player);
        } else if (slot == 26) {
          openMainMenu(player);
        }
      }
      case ACTIVE_EVENTS -> {
        if (slot == 49) {
          openMainMenu(player);
        } else if (slot == EventGuiPagination.SLOT_PREVIOUS) {
          openActiveEventsMenu(player, currentPage(holder) - 1);
        } else if (slot == EventGuiPagination.SLOT_NEXT) {
          openActiveEventsMenu(player, currentPage(holder) + 1);
        } else if (slot == 53) {
          openActiveEventsMenu(player, currentPage(holder));
        } else {
          String instanceId = holder.payloadForSlot(slot);
          if (instanceId != null) {
            openEventInstanceDetailMenu(player, instanceId);
          }
        }
      }
      case DEFINITIONS -> {
        if (slot == 49) {
          openMainMenu(player);
        } else if (slot == EventGuiPagination.SLOT_PREVIOUS) {
          openDefinitionsMenu(player, currentPage(holder) - 1);
        } else if (slot == EventGuiPagination.SLOT_NEXT) {
          openDefinitionsMenu(player, currentPage(holder) + 1);
        } else if (slot == 53) {
          openDefinitionsMenu(player, currentPage(holder));
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
      case EVENT_INSTANCE_DETAIL -> {
        if (slot == EventInstanceDetailScreen.SLOT_BACK) {
          openActiveEventsMenu(player);
        } else if (slot == EventInstanceDetailScreen.SLOT_CANCEL) {
          openEventCancellationConfirmation(player, holder.extra());
        } else if (slot == EventInstanceDetailScreen.SLOT_REFRESH) {
          openEventInstanceDetailMenu(player, holder.extra());
        }
      }
      case EVENT_CANCEL_CONFIRMATION -> {
        if (slot == EventCancelConfirmationScreen.SLOT_CONFIRM) {
          cancelEventFromGui(player, holder.extra());
        } else if (slot == EventCancelConfirmationScreen.SLOT_ABORT) {
          openEventInstanceDetailMenu(player, holder.extra());
        }
      }
      case EVENT_CONFIGURATION -> {
        if (slot == 49) {
          openMainMenu(player);
        } else if (slot == EventGuiPagination.SLOT_PREVIOUS) {
          openConfigurationMenu(player, currentPage(holder) - 1);
        } else if (slot == EventGuiPagination.SLOT_NEXT) {
          openConfigurationMenu(player, currentPage(holder) + 1);
        } else if (slot == 53) {
          openConfigurationMenu(player, currentPage(holder));
        } else {
          String definitionId = holder.payloadForSlot(slot);
          if (definitionId != null) {
            openConfigurationDetailMenu(player, definitionId);
          }
        }
      }
      case EVENT_CONFIGURATION_DETAIL -> {
        if (slot == EventConfigurationDetailScreen.SLOT_BACK) {
          openConfigurationMenu(player);
        } else if (slot == EventConfigurationDetailScreen.SLOT_REFRESH) {
          openConfigurationDetailMenu(player, holder.extra());
        } else {
          String parameter = holder.payloadForSlot(slot);
          if (parameter != null) {
            adjustConfiguration(player, holder.extra(), parameter, event.isRightClick());
          }
        }
      }
      case LOCATIONS -> {
        if (slot == 49) {
          openMainMenu(player);
        } else if (slot == 45) {
          saveCurrentLocation(player);
        } else if (slot == EventGuiPagination.SLOT_PREVIOUS) {
          openLocationsMenu(player, currentPage(holder) - 1);
        } else if (slot == EventGuiPagination.SLOT_NEXT) {
          openLocationsMenu(player, currentPage(holder) + 1);
        } else if (slot == 53) {
          openLocationsMenu(player, currentPage(holder));
        } else {
          String locationName = holder.payloadForSlot(slot);
          if (locationName != null) {
            openLocationRemovalConfirmation(player, locationName);
          }
        }
      }
      case LOCATION_REMOVE_CONFIRMATION -> {
        if (slot == LocationRemoveConfirmationScreen.SLOT_CONFIRM) {
          removeLocationFromGui(player, holder.extra());
        } else if (slot == LocationRemoveConfirmationScreen.SLOT_ABORT) {
          openLocationsMenu(player);
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
      var registered =
          definitionRegistry
              .get(new EventDefinitionId(definitionId))
              .orElseThrow(() -> new IllegalArgumentException("Definition is not registered."));
      var engine =
          Objects.requireNonNull(orchestrationService.executionEngine(), "executionEngine");
      var instance =
          engine.startEvent(
              new io.github.kizio806.spectraevents.application.config.compiler
                      .EventDefinitionCompiler()
                  .compile(registered.sourceSpec(), settingsStore.overridesFor(definitionId)),
              platformLocation);
      player.sendMessage(
          Component.text(
              "Started " + definitionId + " (" + instance.id() + ")", NamedTextColor.GREEN));
      player.closeInventory();
    } catch (Exception e) {
      player.sendMessage(
          Component.text("Failed to start event: " + e.getMessage(), NamedTextColor.RED));
    }
  }

  private void adjustConfiguration(
      Player player, String definitionId, String parameter, boolean decrease) {
    try {
      var declaration =
          definitionRegistry
              .findById(definitionId)
              .orElseThrow()
              .sourceSpec()
              .parameters()
              .get(parameter);
      Object value =
          settingsStore
              .overridesFor(definitionId)
              .getOrDefault(parameter, declaration.defaultValue());
      String adjusted = adjustedValue(declaration, value, decrease);
      new io.github.kizio806.spectraevents.application.config.compiler.EventDefinitionCompiler()
          .compile(
              definitionRegistry.findById(definitionId).orElseThrow().sourceSpec(),
              java.util.Map.of(parameter, adjusted));
      settingsStore.setParameter(definitionId, parameter, adjusted);
      player.sendMessage(
          Component.text(
              "Saved " + parameter + " for " + definitionId + ": " + adjusted + ".",
              NamedTextColor.GREEN));
      openConfigurationDetailMenu(player, definitionId);
    } catch (IllegalArgumentException | IllegalStateException exception) {
      player.sendMessage(
          Component.text("Setting was not saved: " + exception.getMessage(), NamedTextColor.RED));
    }
  }

  private String adjustedValue(
      io.github.kizio806.spectraevents.application.config.spec.EventParameterSpec declaration,
      Object value,
      boolean decrease) {
    if (declaration.type()
        == io.github.kizio806.spectraevents.application.config.spec.EventParameterType.DURATION) {
      long seconds = durationSeconds(String.valueOf(value));
      long adjusted =
          Math.max(
              1L,
              seconds
                  + (decrease ? -declaration.step().longValue() : declaration.step().longValue()));
      return adjusted + "s";
    }
    java.math.BigDecimal adjusted =
        new java.math.BigDecimal(String.valueOf(value))
            .add(decrease ? declaration.step().negate() : declaration.step());
    if (declaration.minimum() != null) {
      adjusted = adjusted.max(declaration.minimum()).min(declaration.maximum());
    }
    if (declaration.type()
        == io.github.kizio806.spectraevents.application.config.spec.EventParameterType.INTEGER) {
      return adjusted.toBigIntegerExact().toString();
    }
    return adjusted.stripTrailingZeros().toPlainString();
  }

  private long durationSeconds(String value) {
    if (value.endsWith("ms")) {
      return Math.max(1L, Long.parseLong(value.substring(0, value.length() - 2)) / 1_000L);
    }
    if (value.endsWith("m")) {
      return Long.parseLong(value.substring(0, value.length() - 1)) * 60L;
    }
    if (value.endsWith("h")) {
      return Long.parseLong(value.substring(0, value.length() - 1)) * 3_600L;
    }
    if (value.endsWith("s")) {
      return Long.parseLong(value.substring(0, value.length() - 1));
    }
    throw new IllegalArgumentException("Saved duration is invalid.");
  }

  private void cancelEventFromGui(Player player, String instanceId) {
    try {
      orchestrationService.cancelEvent(Objects.requireNonNull(instanceId, "instanceId"));
      player.sendMessage(Component.text("Event cancelled and cleaned up.", NamedTextColor.GREEN));
      openActiveEventsMenu(player);
    } catch (IllegalArgumentException | IllegalStateException exception) {
      player.sendMessage(
          Component.text("Could not cancel event: " + exception.getMessage(), NamedTextColor.RED));
      openActiveEventsMenu(player);
    }
  }

  private void saveCurrentLocation(Player player) {
    String name = "location_" + (settingsStore.locations().size() + 1);
    var location = player.getLocation();
    settingsStore.saveLocation(
        name,
        new EventLocation(
            location.getWorld().getName(),
            location.getX(),
            location.getY(),
            location.getZ(),
            location.getYaw(),
            location.getPitch()));
    player.sendMessage(
        Component.text("Saved current location as " + name + ".", NamedTextColor.GREEN));
    openLocationsMenu(player);
  }

  private void openLocationRemovalConfirmation(Player player, String locationName) {
    openSessions.put(player.getUniqueId(), MenuType.LOCATION_REMOVE_CONFIRMATION);
    player.openInventory(LocationRemoveConfirmationScreen.createInventory(locationName));
  }

  private void removeLocationFromGui(Player player, String locationName) {
    if (locationName == null || !settingsStore.removeLocation(locationName)) {
      player.sendMessage(Component.text("That location no longer exists.", NamedTextColor.RED));
    } else {
      player.sendMessage(Component.text("Event location removed.", NamedTextColor.GREEN));
    }
    openLocationsMenu(player);
  }

  private int currentPage(AdminGuiHolder holder) {
    try {
      return Math.max(0, Integer.parseInt(Objects.requireNonNullElse(holder.extra(), "0")));
    } catch (NumberFormatException ignored) {
      return 0;
    }
  }
}
