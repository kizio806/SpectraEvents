package io.github.kizio806.spectraevents.platform.bukkit.loot;

import io.github.kizio806.spectraevents.application.execution.EventExecutionEngine;
import io.github.kizio806.spectraevents.application.execution.ExecutionContext;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.core.event.execution.action.CoreActions;
import io.github.kizio806.spectraevents.core.event.execution.trigger.CoreTriggers;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.platform.bukkit.concurrent.AsyncFutures;
import io.github.kizio806.spectraevents.platform.bukkit.metadata.BukkitPdcKeys;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Shared Bukkit event listener handling atomic player interactions with shared loot containers and
 * ground loot items.
 */
public final class BukkitSharedLootListener implements Listener {
  private final Plugin plugin;
  private final EventInstanceRepository repository;
  private final EventExecutionEngine engine;
  private final BiConsumer<Player, Runnable> playerTaskRunner;

  public BukkitSharedLootListener(
      Plugin plugin,
      EventInstanceRepository repository,
      EventExecutionEngine engine,
      BiConsumer<Player, Runnable> playerTaskRunner) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.repository = Objects.requireNonNull(repository, "repository");
    this.engine = Objects.requireNonNull(engine, "engine");
    this.playerTaskRunner = Objects.requireNonNull(playerTaskRunner, "playerTaskRunner");
  }

  @EventHandler(ignoreCancelled = true)
  public void onClick(InventoryClickEvent event) {
    if (!(event.getInventory().getHolder() instanceof BukkitSharedLootHolder holder)) {
      return;
    }
    event.setCancelled(true);
    if (!(event.getWhoClicked() instanceof Player player)
        || event.getRawSlot() < 0
        || event.getRawSlot() >= event.getInventory().getSize()) {
      return;
    }
    var state = engine.stateStore().get(holder.instanceId()).orElse(null);
    if (state == null) {
      return;
    }
    var taken = state.takeSharedLootSlot(event.getRawSlot());
    if (taken.isEmpty()) {
      return;
    }
    var stack = taken.orElseThrow();
    AsyncFutures.discard(
        repository
            .saveStateDurablyAsync(state)
            .whenComplete(
                (ignored, failure) ->
                    playerTaskRunner.accept(
                        player,
                        () -> {
                          if (failure != null || !player.isOnline()) {
                            fail(holder.instanceId(), failure);
                            return;
                          }
                          player.getInventory().addItem(item(stack));
                          event.getInventory().setItem(event.getRawSlot(), null);
                          ExecutionContext context =
                              ExecutionContext.withActor(player, player.getUniqueId());
                          engine.evaluateTrigger(
                              holder.instanceId(),
                              new CoreTriggers.LootItemTakenTrigger(),
                              context);
                          if (state.sharedLootEmpty()) {
                            engine.evaluateTrigger(
                                holder.instanceId(),
                                new CoreTriggers.LootContainerEmptiedTrigger(),
                                context);
                          }
                        })));
  }

  @EventHandler(ignoreCancelled = true)
  public void onGroundLootPickup(EntityPickupItemEvent event) {
    Item item = event.getItem();
    String rawId =
        item.getPersistentDataContainer()
            .get(BukkitPdcKeys.EVENT_INSTANCE_ID, PersistentDataType.STRING);
    Integer slot =
        item.getPersistentDataContainer()
            .get(BukkitPdcKeys.GROUND_LOOT_SLOT, PersistentDataType.INTEGER);
    if (rawId == null || slot == null) {
      return;
    }
    if (!(event.getEntity() instanceof Player)) {
      event.setCancelled(true);
      return;
    }
    event.setCancelled(true);
    consumeGroundLoot(rawId, slot, (Player) event.getEntity(), item);
  }

  @EventHandler(ignoreCancelled = true)
  public void onGroundLootDespawn(ItemDespawnEvent event) {
    Item item = event.getEntity();
    String rawId =
        item.getPersistentDataContainer()
            .get(BukkitPdcKeys.EVENT_INSTANCE_ID, PersistentDataType.STRING);
    Integer slot =
        item.getPersistentDataContainer()
            .get(BukkitPdcKeys.GROUND_LOOT_SLOT, PersistentDataType.INTEGER);
    if (rawId != null && slot != null) {
      consumeGroundLoot(rawId, slot, null, item);
    }
  }

  private void consumeGroundLoot(String rawId, int slot, Player player, Item groundItem) {
    try {
      EventInstanceId instanceId = new EventInstanceId(UUID.fromString(rawId));
      var state =
          engine
              .stateStore()
              .get(instanceId)
              .or(() -> repository.findState(instanceId))
              .orElse(null);
      if (state == null) {
        return;
      }
      var taken = state.takeSharedLootSlot(slot);
      if (taken.isEmpty()) {
        return;
      }
      AsyncFutures.discard(
          repository
              .saveStateDurablyAsync(state)
              .whenComplete(
                  (ignored, failure) -> {
                    if (player == null) {
                      if (failure != null) {
                        fail(instanceId, failure);
                      }
                      return;
                    }
                    playerTaskRunner.accept(
                        player,
                        () -> {
                          if (failure != null || !player.isOnline()) {
                            fail(instanceId, failure);
                            return;
                          }
                          player.getInventory().addItem(item(taken.orElseThrow()));
                          if (groundItem.isValid()) {
                            groundItem.remove();
                          }
                          engine.evaluateTrigger(
                              instanceId,
                              new CoreTriggers.LootItemTakenTrigger(),
                              ExecutionContext.withActor(player, player.getUniqueId()));
                        });
                  }));
    } catch (IllegalArgumentException exception) {
      plugin
          .getLogger()
          .warning("Ignoring invalid ground-loot reference: " + exception.getMessage());
    }
  }

  private ItemStack item(CoreActions.LootStack stack) {
    Material material = Material.matchMaterial(stack.material().replace("minecraft:", ""));
    return new ItemStack(material == null ? Material.DIAMOND : material, stack.amount());
  }

  private void fail(EventInstanceId instanceId, Throwable failure) {
    if (failure != null) {
      plugin.getLogger().warning("Could not durably consume shared loot: " + failure.getMessage());
      engine.failEvent(instanceId, failure);
    }
  }
}
