package io.github.kizio806.spectraevents.platform.bukkit.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.execution.EventExecutionEngine;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeStateStore;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.application.port.PlatformActionPort;
import io.github.kizio806.spectraevents.core.event.execution.action.CoreActions;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.platform.bukkit.metadata.BukkitPdcKeys;
import io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BukkitSharedLootListenerTest {

  @BeforeAll
  static void beforeAll() {
    MockBukkitSetup.ensureInitialized();
  }

  private Plugin dummyPlugin;
  private EventInstanceRepository dummyRepository;
  private EventRuntimeStateStore stateStore;
  private EventExecutionEngine engine;
  private List<EventRuntimeState> savedStates;
  private CompletableFuture<Void> saveFuture;

  @BeforeEach
  void setUp() {
    savedStates = new ArrayList<>();
    saveFuture = CompletableFuture.completedFuture(null);

    dummyPlugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (proxy, method, args) -> {
                  if ("getLogger".equals(method.getName())) {
                    return Logger.getLogger("TestPlugin");
                  }
                  return null;
                });

    dummyRepository =
        (EventInstanceRepository)
            Proxy.newProxyInstance(
                EventInstanceRepository.class.getClassLoader(),
                new Class<?>[] {EventInstanceRepository.class},
                (proxy, method, args) -> {
                  if ("findActiveById".equals(method.getName())
                      || "findState".equals(method.getName())) {
                    return Optional.empty();
                  }
                  if ("saveStateDurablyAsync".equals(method.getName())) {
                    savedStates.add((EventRuntimeState) args[0]);
                    return saveFuture;
                  }
                  return null;
                });

    EventTaskScheduler dummyScheduler =
        (EventTaskScheduler)
            Proxy.newProxyInstance(
                EventTaskScheduler.class.getClassLoader(),
                new Class<?>[] {EventTaskScheduler.class},
                (proxy, method, args) -> null);

    PlatformActionPort dummyActionPort =
        (PlatformActionPort)
            Proxy.newProxyInstance(
                PlatformActionPort.class.getClassLoader(),
                new Class<?>[] {PlatformActionPort.class},
                (proxy, method, args) -> CompletableFuture.completedFuture(true));

    stateStore = new EventRuntimeStateStore();
    engine =
        new EventExecutionEngine(
            dummyRepository,
            new EventDefinitionRegistry(),
            dummyScheduler,
            dummyActionPort,
            stateStore);
  }

  @Test
  void rejectsNullConstructorArguments() {
    assertThrows(
        NullPointerException.class,
        () -> new BukkitSharedLootListener(null, dummyRepository, engine, (p, r) -> {}));
    assertThrows(
        NullPointerException.class,
        () -> new BukkitSharedLootListener(dummyPlugin, null, engine, (p, r) -> {}));
    assertThrows(
        NullPointerException.class,
        () -> new BukkitSharedLootListener(dummyPlugin, dummyRepository, null, (p, r) -> {}));
    assertThrows(
        NullPointerException.class,
        () -> new BukkitSharedLootListener(dummyPlugin, dummyRepository, engine, null));
  }

  @Test
  void ignoresPickupForItemsWithoutPdc() {
    BukkitSharedLootListener listener =
        new BukkitSharedLootListener(dummyPlugin, dummyRepository, engine, (p, r) -> {});

    Item dummyItem = MockBukkitSetup.createMockItemEntity(new ItemStack(Material.DIAMOND, 1), null);

    Player dummyPlayer = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "Steve");

    EntityPickupItemEvent event = new EntityPickupItemEvent(dummyPlayer, dummyItem, 0);
    listener.onGroundLootPickup(event);
    assertFalse(event.isCancelled());
  }

  @Test
  void ignoresDespawnForItemsWithoutPdc() {
    BukkitSharedLootListener listener =
        new BukkitSharedLootListener(dummyPlugin, dummyRepository, engine, (p, r) -> {});

    Item dummyItem = MockBukkitSetup.createMockItemEntity(new ItemStack(Material.DIAMOND, 1), null);
    Location loc = new Location(null, 0, 0, 0);
    ItemDespawnEvent event = new ItemDespawnEvent(dummyItem, loc);
    listener.onGroundLootDespawn(event);
    assertFalse(event.isCancelled());
  }

  @Test
  void shouldIgnoreClickWhenHolderIsNotSharedLootHolder() {
    BukkitSharedLootListener listener =
        new BukkitSharedLootListener(dummyPlugin, dummyRepository, engine, (p, r) -> r.run());

    Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "Steve");
    Inventory regularInv = MockBukkitSetup.createMockInventory(null, 27, "Regular Chest");
    InventoryClickEvent event = MockBukkitSetup.createClickEvent(player, regularInv, 0);

    listener.onClick(event);
    assertFalse(event.isCancelled());
  }

  @Test
  void shouldCancelClickWhenHolderIsSharedLootHolder() {
    EventInstanceId id = EventInstanceId.generate();
    BukkitSharedLootListener listener =
        new BukkitSharedLootListener(dummyPlugin, dummyRepository, engine, (p, r) -> r.run());

    Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "Steve");
    Inventory lootInv =
        MockBukkitSetup.createMockInventory(new BukkitSharedLootHolder(id), 27, "Shared Loot");

    // Click with rawSlot out of bounds (< 0)
    InventoryClickEvent outOfBoundsEvent = MockBukkitSetup.createClickEvent(player, lootInv, -1);
    listener.onClick(outOfBoundsEvent);
    assertTrue(outOfBoundsEvent.isCancelled());

    // Click when state does not exist in store
    InventoryClickEvent noStateEvent = MockBukkitSetup.createClickEvent(player, lootInv, 5);
    listener.onClick(noStateEvent);
    assertTrue(noStateEvent.isCancelled());
  }

  @Test
  void shouldConsumeSharedLootSlotOnValidClick() {
    EventInstanceId id = EventInstanceId.generate();
    EventRuntimeState state = new EventRuntimeState(id);
    state.initializeSharedLoot(List.of(new CoreActions.LootStack("minecraft:diamond", 5)));
    stateStore.put(state);

    BukkitSharedLootListener listener =
        new BukkitSharedLootListener(dummyPlugin, dummyRepository, engine, (p, r) -> r.run());

    Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "Steve");
    Inventory lootInv =
        MockBukkitSetup.createMockInventory(new BukkitSharedLootHolder(id), 27, "Shared Loot");
    lootInv.setItem(0, new ItemStack(Material.DIAMOND, 5));

    InventoryClickEvent event = MockBukkitSetup.createClickEvent(player, lootInv, 0);
    listener.onClick(event);

    assertTrue(event.isCancelled());
    assertEquals(1, savedStates.size());
    assertNull(lootInv.getItem(0));
    assertTrue(state.sharedLootEmpty());

    // Clicking an already empty slot
    InventoryClickEvent secondEvent = MockBukkitSetup.createClickEvent(player, lootInv, 0);
    listener.onClick(secondEvent);
    assertTrue(secondEvent.isCancelled());
    assertEquals(1, savedStates.size()); // Not saved again
  }

  @Test
  void shouldHandleDurableSaveFailureOnSharedLootClick() {
    EventInstanceId id = EventInstanceId.generate();
    EventRuntimeState state = new EventRuntimeState(id);
    state.initializeSharedLoot(List.of(new CoreActions.LootStack("minecraft:gold_ingot", 2)));
    stateStore.put(state);

    CompletableFuture<Void> failedFuture = new CompletableFuture<>();
    failedFuture.completeExceptionally(new RuntimeException("Database error"));
    saveFuture = failedFuture;

    BukkitSharedLootListener listener =
        new BukkitSharedLootListener(dummyPlugin, dummyRepository, engine, (p, r) -> r.run());

    Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "Steve");
    Inventory lootInv =
        MockBukkitSetup.createMockInventory(new BukkitSharedLootHolder(id), 27, "Shared Loot");
    lootInv.setItem(0, new ItemStack(Material.GOLD_INGOT, 2));

    InventoryClickEvent event = MockBukkitSetup.createClickEvent(player, lootInv, 0);
    listener.onClick(event);

    assertTrue(event.isCancelled());
    // Slot in state was consumed, but failure handler logged and failed event
    assertNotNull(lootInv.getItem(0)); // Did not clear inventory because failure occurred
  }

  @Test
  void shouldHandleGroundLootPickupByNonPlayer() {
    EventInstanceId id = EventInstanceId.generate();
    BukkitSharedLootListener listener =
        new BukkitSharedLootListener(dummyPlugin, dummyRepository, engine, (p, r) -> r.run());

    Item groundItem =
        MockBukkitSetup.createMockItemEntity(new ItemStack(Material.DIAMOND, 1), null);
    groundItem
        .getPersistentDataContainer()
        .set(BukkitPdcKeys.EVENT_INSTANCE_ID, PersistentDataType.STRING, id.toString());
    groundItem
        .getPersistentDataContainer()
        .set(BukkitPdcKeys.GROUND_LOOT_SLOT, PersistentDataType.INTEGER, 0);

    Entity mockMob = MockBukkitSetup.createMockEntityWithDetails(EntityType.ZOMBIE, null);
    EntityPickupItemEvent event =
        new EntityPickupItemEvent((org.bukkit.entity.LivingEntity) mockMob, groundItem, 0);

    listener.onGroundLootPickup(event);
    assertTrue(event.isCancelled());
  }

  @Test
  void shouldHandleGroundLootPickupByPlayer() {
    EventInstanceId id = EventInstanceId.generate();
    EventRuntimeState state = new EventRuntimeState(id);
    state.initializeSharedLoot(List.of(new CoreActions.LootStack("minecraft:iron_ingot", 10)));
    stateStore.put(state);

    BukkitSharedLootListener listener =
        new BukkitSharedLootListener(dummyPlugin, dummyRepository, engine, (p, r) -> r.run());

    Item groundItem =
        MockBukkitSetup.createMockItemEntity(new ItemStack(Material.IRON_INGOT, 10), null);
    groundItem
        .getPersistentDataContainer()
        .set(BukkitPdcKeys.EVENT_INSTANCE_ID, PersistentDataType.STRING, id.toString());
    groundItem
        .getPersistentDataContainer()
        .set(BukkitPdcKeys.GROUND_LOOT_SLOT, PersistentDataType.INTEGER, 0);

    Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "Steve");
    EntityPickupItemEvent event = new EntityPickupItemEvent(player, groundItem, 0);

    listener.onGroundLootPickup(event);
    assertTrue(event.isCancelled());
    assertEquals(1, savedStates.size());
    assertFalse(groundItem.isValid()); // Removed after pickup
    assertTrue(state.sharedLootEmpty());
  }

  @Test
  void shouldHandleGroundLootDespawn() {
    EventInstanceId id = EventInstanceId.generate();
    EventRuntimeState state = new EventRuntimeState(id);
    state.initializeSharedLoot(List.of(new CoreActions.LootStack("minecraft:diamond", 1)));
    stateStore.put(state);

    BukkitSharedLootListener listener =
        new BukkitSharedLootListener(dummyPlugin, dummyRepository, engine, (p, r) -> r.run());

    Item groundItem =
        MockBukkitSetup.createMockItemEntity(new ItemStack(Material.DIAMOND, 1), null);
    groundItem
        .getPersistentDataContainer()
        .set(BukkitPdcKeys.EVENT_INSTANCE_ID, PersistentDataType.STRING, id.toString());
    groundItem
        .getPersistentDataContainer()
        .set(BukkitPdcKeys.GROUND_LOOT_SLOT, PersistentDataType.INTEGER, 0);

    ItemDespawnEvent event = new ItemDespawnEvent(groundItem, new Location(null, 0, 0, 0));
    listener.onGroundLootDespawn(event);

    assertEquals(1, savedStates.size());
    assertTrue(state.sharedLootEmpty());
  }

  @Test
  void shouldHandleInvalidPdcInGroundLootGracefully() {
    BukkitSharedLootListener listener =
        new BukkitSharedLootListener(dummyPlugin, dummyRepository, engine, (p, r) -> r.run());

    Item groundItem =
        MockBukkitSetup.createMockItemEntity(new ItemStack(Material.DIAMOND, 1), null);
    groundItem
        .getPersistentDataContainer()
        .set(BukkitPdcKeys.EVENT_INSTANCE_ID, PersistentDataType.STRING, "not-a-uuid");
    groundItem
        .getPersistentDataContainer()
        .set(BukkitPdcKeys.GROUND_LOOT_SLOT, PersistentDataType.INTEGER, 0);

    Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "Steve");
    EntityPickupItemEvent event = new EntityPickupItemEvent(player, groundItem, 0);

    listener.onGroundLootPickup(event);
    assertTrue(event.isCancelled()); // Handled without uncaught exception
  }

  @Test
  void shouldConsumeSharedLootSlotWhenNotEmptied() {
    EventInstanceId id = EventInstanceId.generate();
    EventRuntimeState state = new EventRuntimeState(id);
    state.initializeSharedLoot(
        List.of(
            new CoreActions.LootStack("minecraft:diamond", 5),
            new CoreActions.LootStack("minecraft:emerald", 1)));
    stateStore.put(state);

    BukkitSharedLootListener listener =
        new BukkitSharedLootListener(dummyPlugin, dummyRepository, engine, (p, r) -> r.run());

    Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "Steve");
    Inventory lootInv =
        MockBukkitSetup.createMockInventory(new BukkitSharedLootHolder(id), 27, "Shared Loot");
    lootInv.setItem(0, new ItemStack(Material.DIAMOND, 5));
    lootInv.setItem(1, new ItemStack(Material.EMERALD, 1));

    InventoryClickEvent event = MockBukkitSetup.createClickEvent(player, lootInv, 0);
    listener.onClick(event);

    assertTrue(event.isCancelled());
    assertFalse(state.sharedLootEmpty());
  }

  @Test
  void shouldConsumeGroundLootWhenItemIsAlreadyInvalid() {
    EventInstanceId id = EventInstanceId.generate();
    EventRuntimeState state = new EventRuntimeState(id);
    state.initializeSharedLoot(List.of(new CoreActions.LootStack("minecraft:diamond", 1)));
    stateStore.put(state);

    BukkitSharedLootListener listener =
        new BukkitSharedLootListener(dummyPlugin, dummyRepository, engine, (p, r) -> r.run());

    Item groundItem =
        MockBukkitSetup.createMockItemEntity(new ItemStack(Material.DIAMOND, 1), null);
    groundItem
        .getPersistentDataContainer()
        .set(BukkitPdcKeys.EVENT_INSTANCE_ID, PersistentDataType.STRING, id.toString());
    groundItem
        .getPersistentDataContainer()
        .set(BukkitPdcKeys.GROUND_LOOT_SLOT, PersistentDataType.INTEGER, 0);
    // Invalidate ground item before pickup completes
    groundItem.remove();

    Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "SteveValid");
    EntityPickupItemEvent event = new EntityPickupItemEvent(player, groundItem, 0);

    listener.onGroundLootPickup(event);
    assertTrue(event.isCancelled());
  }

  @Test
  void shouldHandleGroundLootPickupWithEmptySlotInState() {
    EventInstanceId id = EventInstanceId.generate();
    EventRuntimeState state = new EventRuntimeState(id);
    // Empty shared loot
    stateStore.put(state);

    BukkitSharedLootListener listener =
        new BukkitSharedLootListener(dummyPlugin, dummyRepository, engine, (p, r) -> r.run());

    Item groundItem =
        MockBukkitSetup.createMockItemEntity(new ItemStack(Material.DIAMOND, 1), null);
    groundItem
        .getPersistentDataContainer()
        .set(BukkitPdcKeys.EVENT_INSTANCE_ID, PersistentDataType.STRING, id.toString());
    groundItem
        .getPersistentDataContainer()
        .set(BukkitPdcKeys.GROUND_LOOT_SLOT, PersistentDataType.INTEGER, 5);

    Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "SteveSlot5");
    EntityPickupItemEvent event = new EntityPickupItemEvent(player, groundItem, 0);

    listener.onGroundLootPickup(event);
    assertTrue(event.isCancelled());
  }

  @Test
  void shouldHandleGroundLootPickupMissingState() {
    BukkitSharedLootListener listener =
        new BukkitSharedLootListener(dummyPlugin, dummyRepository, engine, (p, r) -> r.run());

    EventInstanceId id = EventInstanceId.generate();
    // Do not save state in store -> state is null

    Item groundItem =
        MockBukkitSetup.createMockItemEntity(new ItemStack(Material.GOLD_INGOT, 1), null);
    groundItem
        .getPersistentDataContainer()
        .set(BukkitPdcKeys.EVENT_INSTANCE_ID, PersistentDataType.STRING, id.toString());
    groundItem
        .getPersistentDataContainer()
        .set(BukkitPdcKeys.GROUND_LOOT_SLOT, PersistentDataType.INTEGER, 0);

    Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "Miner");
    EntityPickupItemEvent event = new EntityPickupItemEvent(player, groundItem, 0);

    listener.onGroundLootPickup(event);
    assertTrue(event.isCancelled());
  }

  @Test
  void shouldCancelGroundLootPickupForNonPlayerEntity() {
    BukkitSharedLootListener listener =
        new BukkitSharedLootListener(dummyPlugin, dummyRepository, engine, (p, r) -> r.run());

    Item groundItem =
        MockBukkitSetup.createMockItemEntity(new ItemStack(Material.DIAMOND, 1), null);
    groundItem
        .getPersistentDataContainer()
        .set(
            BukkitPdcKeys.EVENT_INSTANCE_ID,
            PersistentDataType.STRING,
            EventInstanceId.generate().toString());
    groundItem
        .getPersistentDataContainer()
        .set(BukkitPdcKeys.GROUND_LOOT_SLOT, PersistentDataType.INTEGER, 0);

    org.bukkit.entity.LivingEntity zombie =
        (org.bukkit.entity.LivingEntity)
            Proxy.newProxyInstance(
                org.bukkit.entity.LivingEntity.class.getClassLoader(),
                new Class<?>[] {org.bukkit.entity.LivingEntity.class},
                (proxy, method, args) -> null);
    EntityPickupItemEvent event = new EntityPickupItemEvent(zombie, groundItem, 0);

    listener.onGroundLootPickup(event);
    assertTrue(event.isCancelled());
  }

  @Test
  void shouldHandleGroundLootDespawnPersistenceFailure() {
    EventInstanceId id = EventInstanceId.generate();
    EventRuntimeState state = new EventRuntimeState(id);
    state.initializeSharedLoot(List.of(new CoreActions.LootStack("minecraft:diamond", 1)));
    stateStore.put(state);

    saveFuture = CompletableFuture.failedFuture(new RuntimeException("Simulated despawn failure"));

    BukkitSharedLootListener listener =
        new BukkitSharedLootListener(dummyPlugin, dummyRepository, engine, (p, r) -> r.run());

    Item groundItem =
        MockBukkitSetup.createMockItemEntity(new ItemStack(Material.DIAMOND, 1), null);
    groundItem
        .getPersistentDataContainer()
        .set(BukkitPdcKeys.EVENT_INSTANCE_ID, PersistentDataType.STRING, id.toString());
    groundItem
        .getPersistentDataContainer()
        .set(BukkitPdcKeys.GROUND_LOOT_SLOT, PersistentDataType.INTEGER, 0);

    ItemDespawnEvent event = new ItemDespawnEvent(groundItem, new Location(null, 0, 0, 0));
    listener.onGroundLootDespawn(event);

    assertEquals(1, savedStates.size());
  }

  @Test
  void shouldHandleGroundLootPickupWithUnknownMaterialAndFailure() {
    EventInstanceId id = EventInstanceId.generate();
    EventRuntimeState state = new EventRuntimeState(id);
    state.initializeSharedLoot(List.of(new CoreActions.LootStack("unknown:invalid_item", 2)));
    stateStore.put(state);

    BukkitSharedLootListener listener =
        new BukkitSharedLootListener(dummyPlugin, dummyRepository, engine, (p, r) -> r.run());

    Item groundItem =
        MockBukkitSetup.createMockItemEntity(new ItemStack(Material.DIAMOND, 1), null);
    groundItem
        .getPersistentDataContainer()
        .set(BukkitPdcKeys.EVENT_INSTANCE_ID, PersistentDataType.STRING, id.toString());
    groundItem
        .getPersistentDataContainer()
        .set(BukkitPdcKeys.GROUND_LOOT_SLOT, PersistentDataType.INTEGER, 0);

    Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "Miner2");
    EntityPickupItemEvent event = new EntityPickupItemEvent(player, groundItem, 0);

    listener.onGroundLootPickup(event);
    assertTrue(event.isCancelled());

    // Also test with offline player when save completes
    EventRuntimeState state2 = new EventRuntimeState(id);
    state2.initializeSharedLoot(List.of(new CoreActions.LootStack("minecraft:iron_ingot", 1)));
    stateStore.put(state2);

    Player offlinePlayer =
        (Player)
            Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[] {Player.class},
                (proxy, method, args) -> {
                  if ("isOnline".equals(method.getName())) return false;
                  if ("getUniqueId".equals(method.getName())) return UUID.randomUUID();
                  return null;
                });
    EntityPickupItemEvent eventOffline = new EntityPickupItemEvent(offlinePlayer, groundItem, 0);
    listener.onGroundLootPickup(eventOffline);
  }

  @Test
  void shouldHandleInventoryClickPersistenceFailureOrOfflinePlayer() {
    EventInstanceId id = EventInstanceId.generate();
    EventRuntimeState state = new EventRuntimeState(id);
    state.initializeSharedLoot(List.of(new CoreActions.LootStack("minecraft:diamond", 1)));
    stateStore.put(state);

    saveFuture = CompletableFuture.failedFuture(new RuntimeException("Simulated click failure"));

    BukkitSharedLootListener listener =
        new BukkitSharedLootListener(dummyPlugin, dummyRepository, engine, (p, r) -> r.run());

    BukkitSharedLootHolder holder = new BukkitSharedLootHolder(id);
    Inventory inventory = MockBukkitSetup.createMockInventory(holder, 27, "Shared Loot");
    inventory.setItem(0, new ItemStack(Material.DIAMOND, 1));

    Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "Looter");
    InventoryClickEvent event = MockBukkitSetup.createClickEvent(player, inventory, 0);

    listener.onClick(event);
    assertTrue(event.isCancelled());
  }
}
