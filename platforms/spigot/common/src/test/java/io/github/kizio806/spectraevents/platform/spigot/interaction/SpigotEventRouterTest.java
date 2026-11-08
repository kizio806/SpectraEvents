package io.github.kizio806.spectraevents.platform.spigot.interaction;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.execution.EventExecutionEngine;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeStateStore;
import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.application.port.PlatformActionPort;
import io.github.kizio806.spectraevents.application.repository.InMemoryEventInstanceRepository;
import io.github.kizio806.spectraevents.application.service.EventOrchestrationService;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.platform.bukkit.metadata.BukkitPdcKeys;
import io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Material;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SpigotEventRouterTest {

  @BeforeAll
  static void beforeAll() {
    MockBukkitSetup.ensureInitialized();
  }

  private EventRuntimeStateStore stateStore;
  private EventExecutionEngine executionEngine;
  private EventOrchestrationService orchestrationService;
  private SpigotEventRouter router;
  private InMemoryEventInstanceRepository repository;
  private DamageSource mockDamageSource;

  @BeforeEach
  void setUp() {
    stateStore = new EventRuntimeStateStore();
    EventDefinitionRegistry defRegistry = new EventDefinitionRegistry();

    mockDamageSource =
        (DamageSource)
            Proxy.newProxyInstance(
                DamageSource.class.getClassLoader(),
                new Class<?>[] {DamageSource.class},
                (proxy, method, args) -> null);

    repository = new InMemoryEventInstanceRepository();

    EventTaskScheduler scheduler =
        (EventTaskScheduler)
            Proxy.newProxyInstance(
                EventTaskScheduler.class.getClassLoader(),
                new Class<?>[] {EventTaskScheduler.class},
                (proxy, method, args) -> null);

    PlatformActionPort actionPort =
        (PlatformActionPort)
            Proxy.newProxyInstance(
                PlatformActionPort.class.getClassLoader(),
                new Class<?>[] {PlatformActionPort.class},
                (proxy, method, args) -> CompletableFuture.completedFuture(true));

    executionEngine =
        new EventExecutionEngine(repository, defRegistry, scheduler, actionPort, stateStore);

    orchestrationService = new EventOrchestrationService(repository, defRegistry, executionEngine);

    router = new SpigotEventRouter(orchestrationService, executionEngine);
  }

  @Test
  void shouldIgnoreInteractionsWithoutPdc() {
    Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "Steve");
    Entity entity = MockBukkitSetup.createMockEntityWithDetails(EntityType.ZOMBIE, null);

    PlayerInteractEntityEvent event =
        new PlayerInteractEntityEvent(player, entity, EquipmentSlot.HAND);
    router.onInteract(event);
    assertFalse(event.isCancelled());
  }

  @Test
  void shouldCancelAndRouteValidInteraction() {
    EventInstanceId id = EventInstanceId.generate();
    EventInstance instance = EventInstance.create(id, new EventDefinitionId("test_event"));
    repository.save(instance);

    EventRuntimeState state = new EventRuntimeState(id);
    stateStore.put(state);

    Entity entity = MockBukkitSetup.createMockEntityWithDetails(EntityType.ZOMBIE, null);
    entity
        .getPersistentDataContainer()
        .set(BukkitPdcKeys.EVENT_INSTANCE_ID, PersistentDataType.STRING, id.toString());

    Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "Steve");
    PlayerInteractEntityEvent event =
        new PlayerInteractEntityEvent(player, entity, EquipmentSlot.HAND);
    router.onInteract(event);
    assertTrue(event.isCancelled());
  }

  @Test
  void shouldHandleCombatDamageFromPlayer() {
    EventInstanceId id = EventInstanceId.generate();
    EventInstance instance = EventInstance.create(id, new EventDefinitionId("boss_event"));
    repository.save(instance);

    EventRuntimeState state = new EventRuntimeState(id);
    stateStore.put(state);

    Entity entity = MockBukkitSetup.createMockEntityWithDetails(EntityType.ZOMBIE, null);
    entity
        .getPersistentDataContainer()
        .set(BukkitPdcKeys.EVENT_INSTANCE_ID, PersistentDataType.STRING, id.toString());

    Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "Warrior");

    EntityDamageByEntityEvent event =
        new EntityDamageByEntityEvent(
            player, entity, DamageCause.ENTITY_ATTACK, mockDamageSource, 10.0);
    router.onDamage(event);
    assertTrue(event.isCancelled());
    assertEquals(10, state.contribution().contributions().get(player.getUniqueId()));
  }

  @Test
  void shouldIgnoreDamageFromNonPlayer() {
    Entity damager = MockBukkitSetup.createMockEntityWithDetails(EntityType.SKELETON, null);
    Entity victim = MockBukkitSetup.createMockEntityWithDetails(EntityType.ZOMBIE, null);

    EntityDamageByEntityEvent event =
        new EntityDamageByEntityEvent(
            damager, victim, DamageCause.ENTITY_ATTACK, mockDamageSource, 5.0);
    router.onDamage(event);
    assertFalse(event.isCancelled());

    // Also test damage from Player on entity without PDC
    Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "PlayerWithoutPdcTarget");
    EntityDamageByEntityEvent playerEvent =
        new EntityDamageByEntityEvent(
            player, victim, DamageCause.ENTITY_ATTACK, mockDamageSource, 5.0);
    router.onDamage(playerEvent);
    assertFalse(playerEvent.isCancelled());
  }

  @Test
  void testInvalidUuidInPdcReturnsNull() {
    Entity entity = MockBukkitSetup.createMockEntityWithDetails(EntityType.ZOMBIE, null);
    entity
        .getPersistentDataContainer()
        .set(BukkitPdcKeys.EVENT_INSTANCE_ID, PersistentDataType.STRING, "not-a-valid-uuid");

    Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "Steve");
    PlayerInteractEntityEvent event =
        new PlayerInteractEntityEvent(player, entity, EquipmentSlot.HAND);
    router.onInteract(event);
    assertFalse(event.isCancelled());
  }

  @Test
  void testStaleEventInstanceThrowsIllegalArgumentExceptionHandledGracefully() {
    EventInstanceId id = EventInstanceId.generate();
    // Do NOT save instance to repository -> orchestrationService.getEventInfo throws
    // IllegalArgumentException
    Entity entity = MockBukkitSetup.createMockEntityWithDetails(EntityType.ZOMBIE, null);
    entity
        .getPersistentDataContainer()
        .set(BukkitPdcKeys.EVENT_INSTANCE_ID, PersistentDataType.STRING, id.toString());

    Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "Steve");

    // 1. Interaction with stale event returns true and cancels
    PlayerInteractEntityEvent interactEvent =
        new PlayerInteractEntityEvent(player, entity, EquipmentSlot.HAND);
    router.onInteract(interactEvent);
    assertTrue(interactEvent.isCancelled());

    // 2. Combat damage with stale event returns true and cancels
    EntityDamageByEntityEvent damageEvent =
        new EntityDamageByEntityEvent(
            player, entity, DamageCause.ENTITY_ATTACK, mockDamageSource, 10.0);
    router.onDamage(damageEvent);
    assertTrue(damageEvent.isCancelled());

    // 3. Death with stale event
    EntityDeathEvent deathEvent =
        new EntityDeathEvent(
            (LivingEntity) entity, mockDamageSource, List.of(new ItemStack(Material.ROTTEN_FLESH)));
    router.onDeath(deathEvent);
  }

  @Test
  void testDeathWithoutPdcAndWithKillerAndWithoutWaveId() {
    // 1. Death without PDC
    Entity plainMob = MockBukkitSetup.createMockEntityWithDetails(EntityType.ZOMBIE, null);
    EntityDeathEvent plainDeath =
        new EntityDeathEvent(
            (LivingEntity) plainMob,
            mockDamageSource,
            List.of(new ItemStack(Material.ROTTEN_FLESH)));

    // 2. Death with killer and without waveId
    EventInstanceId id = EventInstanceId.generate();
    EventInstance instance = EventInstance.create(id, new EventDefinitionId("test_event"));
    repository.save(instance);

    Player killer = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "Killer");
    Entity mobWithInstance = MockBukkitSetup.createMockEntityWithDetails(EntityType.ZOMBIE, null);
    mobWithInstance
        .getPersistentDataContainer()
        .set(BukkitPdcKeys.EVENT_INSTANCE_ID, PersistentDataType.STRING, id.toString());

    // Configure mock killer
    LivingEntity livingMob =
        (LivingEntity)
            Proxy.newProxyInstance(
                LivingEntity.class.getClassLoader(),
                new Class<?>[] {LivingEntity.class},
                (proxy, method, args) -> {
                  if ("getKiller".equals(method.getName())) {
                    return killer;
                  }
                  if ("getPersistentDataContainer".equals(method.getName())) {
                    return mobWithInstance.getPersistentDataContainer();
                  }
                  if ("getUniqueId".equals(method.getName())) {
                    return mobWithInstance.getUniqueId();
                  }
                  return null;
                });

    EntityDeathEvent deathWithKiller =
        new EntityDeathEvent(
            livingMob, mockDamageSource, List.of(new ItemStack(Material.ROTTEN_FLESH)));
    assertDoesNotThrow(
        () -> {
          router.onDeath(plainDeath);
          router.onDeath(deathWithKiller);
        });
  }

  @Test
  void shouldHandleEntityDeathAndWaveTracking() {
    EventInstanceId id = EventInstanceId.generate();
    EventInstance instance = EventInstance.create(id, new EventDefinitionId("wave_event"));
    repository.save(instance);

    EventRuntimeState state = new EventRuntimeState(id);
    state.beginWave("wave_1");
    Entity mob = MockBukkitSetup.createMockEntityWithDetails(EntityType.ZOMBIE, null);
    mob.getPersistentDataContainer()
        .set(BukkitPdcKeys.EVENT_INSTANCE_ID, PersistentDataType.STRING, id.toString());
    mob.getPersistentDataContainer()
        .set(BukkitPdcKeys.WAVE_ID, PersistentDataType.STRING, "wave_1");
    state.trackWaveEntity("wave_1", mob.getUniqueId());
    stateStore.put(state);

    assertEquals(1, state.trackedWaveEntityCount());

    EntityDeathEvent deathEvent =
        new EntityDeathEvent(
            (LivingEntity) mob, mockDamageSource, List.of(new ItemStack(Material.ROTTEN_FLESH)));
    router.onDeath(deathEvent);

    assertEquals(0, state.trackedWaveEntityCount());
  }
}
