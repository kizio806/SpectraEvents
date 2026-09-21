package io.github.kizio806.spectraevents.platform.paper.gui;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Custom InventoryHolder marker for SpectraEvents Admin GUI screens. */
public final class AdminGuiHolder implements InventoryHolder {
  private final AdminGuiController.MenuType menuType;

  /**
   * Optional extra context payload. For {@link AdminGuiController.MenuType#DEFINITION_DETAIL} this
   * holds the definition ID string; for all other screen types it is {@code null}.
   */
  private final String extra;

  /** Slot-to-payload index used by list screens to resolve which item was clicked. */
  private final Map<Integer, String> slotPayloads;

  private Inventory inventory;

  private AdminGuiHolder(
      AdminGuiController.MenuType menuType, String extra, Map<Integer, String> slotPayloads) {
    this.menuType = menuType;
    this.extra = extra;
    this.slotPayloads = Collections.unmodifiableMap(new HashMap<>(slotPayloads));
  }

  public static Inventory createInventory(
      AdminGuiController.MenuType menuType, int size, Component title) {
    return createInventory(menuType, null, Map.of(), size, title);
  }

  public static Inventory createInventory(
      AdminGuiController.MenuType menuType,
      String extra,
      Map<Integer, String> slotPayloads,
      int size,
      Component title) {
    AdminGuiHolder holder = new AdminGuiHolder(menuType, extra, slotPayloads);
    Inventory inventory = Bukkit.createInventory(holder, size, title);
    holder.inventory = inventory;
    return inventory;
  }

  public AdminGuiController.MenuType menuType() {
    return menuType;
  }

  /** Returns the optional context payload, or {@code null} when not set. */
  public String extra() {
    return extra;
  }

  /**
   * Returns the definition ID associated with the given inventory slot, or {@code null} when the
   * slot carries no payload.
   */
  public String payloadForSlot(int slot) {
    return slotPayloads.get(slot);
  }

  @Override
  public Inventory getInventory() {
    return Objects.requireNonNull(inventory, "inventory");
  }
}
