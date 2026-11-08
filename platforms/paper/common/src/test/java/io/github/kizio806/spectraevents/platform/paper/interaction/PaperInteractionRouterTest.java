package io.github.kizio806.spectraevents.platform.paper.interaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.config.compiled.ConfiguredTriggerDefinition;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.execution.EventExecutionEngine;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeStateStore;
import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.application.port.PlatformActionPort;
import io.github.kizio806.spectraevents.application.repository.InMemoryEventInstanceRepository;
import io.github.kizio806.spectraevents.application.service.EventOrchestrationService;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinition;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.execution.TransitionRule;
import io.github.kizio806.spectraevents.core.event.phase.PhaseDefinition;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleState;
import io.github.kizio806.spectraevents.platform.bukkit.metadata.BukkitPdcKeys;
import io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup;
import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaperInteractionRouterTest {

  @BeforeAll
  static void beforeAll() {
    MockBukkitSetup.ensureInitialized();
  }

  private EventRuntimeStateStore stateStore;
  private EventExecutionEngine executionEngine;
  private EventOrchestrationService orchestrationService;
  private EventDefinitionRegistry registry;
  private LocaleCatalog locales;
  private PaperInteractionRouter router;
  private InMemoryEventInstanceRepository repository;

  private DamageSource mockDamageSource;

  @BeforeEach
  void setUp() {
    mockDamageSource =
        (DamageSource)
            Proxy.newProxyInstance(
                DamageSource.class.getClassLoader(),
                new Class<?>[] {DamageSource.class},
                (p, m, a) -> null);
    stateStore = new EventRuntimeStateStore();
    registry = new EventDefinitionRegistry();
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
    repository = new InMemoryEventInstanceRepository();

    executionEngine =
        new EventExecutionEngine(repository, registry, scheduler, actionPort, stateStore);

    orchestrationService = new EventOrchestrationService(repository, registry, executionEngine);

    locales =
        LocaleCatalog.of(
            Map.of(
                "messages.interaction-locked", "Locked for %seconds%s",
                "messages.interaction-invalid-instance", "Invalid instance: %id%"));

    router = new PaperInteractionRouter(orchestrationService, executionEngine, locales);
  }

  private EntityDamageByEntityEvent createDamageEvent(
      Entity damager, Entity victim, double damage) {
    try {
      Constructor<EntityDamageByEntityEvent> ctor =
          EntityDamageByEntityEvent.class.getConstructor(
              Entity.class, Entity.class, DamageCause.class, DamageSource.class, double.class);
      return ctor.newInstance(damager, victim, DamageCause.ENTITY_ATTACK, mockDamageSource, damage);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void testInteractEntityWithoutPdcDoesNothing() {
    World world = MockBukkitSetup.createMockWorld("world");
    Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "TestPlayer");
    LivingEntity entity =
        (LivingEntity)
            MockBukkitSetup.createMockEntityWithDetails(
                EntityType.ZOMBIE, new Location(world, 0, 0, 0));

    PlayerInteractEntityEvent event =
        new PlayerInteractEntityEvent(player, entity, EquipmentSlot.HAND);
    router.onPlayerInteractEntity(event);

    assertFalse(event.isCancelled());
  }

  @Test
  void testInteractEntityWithInvalidInstanceCancelsAndWarns() {
    World world = MockBukkitSetup.createMockWorld("world");
    Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "TestPlayer");
    LivingEntity entity =
        (LivingEntity)
            MockBukkitSetup.createMockEntityWithDetails(
                EntityType.ZOMBIE, new Location(world, 0, 0, 0));

    entity
        .getPersistentDataContainer()
        .set(BukkitPdcKeys.EVENT_INSTANCE_ID, PersistentDataType.STRING, "not-a-uuid");

    PlayerInteractEntityEvent event =
        new PlayerInteractEntityEvent(player, entity, EquipmentSlot.HAND);
    router.onPlayerInteractEntity(event);

    assertTrue(event.isCancelled());
  }

  @Test
  void testInteractEntityWithDelegateInvokesDelegate() {
    World world = MockBukkitSetup.createMockWorld("world");
    Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "TestPlayer");
    LivingEntity entity =
        (LivingEntity)
            MockBukkitSetup.createMockEntityWithDetails(
                EntityType.ZOMBIE, new Location(world, 0, 0, 0));

    EventInstanceId instanceId = EventInstanceId.generate();
    EventInstance instance = EventInstance.create(instanceId, new EventDefinitionId("meteor"));
    repository.save(instance);

    entity
        .getPersistentDataContainer()
        .set(
            BukkitPdcKeys.EVENT_INSTANCE_ID,
            PersistentDataType.STRING,
            instanceId.value().toString());
    entity
        .getPersistentDataContainer()
        .set(BukkitPdcKeys.MODEL_DEFINITION_ID, PersistentDataType.STRING, "meteor_model");

    List<String> invoked = new ArrayList<>();
    router.registerDelegate(
        "meteor",
        (p, inst, phase, modelId) -> {
          invoked.add(
              p.getName() + ":" + inst.definitionId().value() + ":" + phase + ":" + modelId);
        });

    PlayerInteractEntityEvent event =
        new PlayerInteractEntityEvent(player, entity, EquipmentSlot.HAND);
    router.onPlayerInteractEntity(event);

    assertTrue(event.isCancelled());
    assertEquals(1, invoked.size());
    assertEquals(player.getName() + ":meteor:none:meteor_model", invoked.get(0));
  }

  @Test
  void testInteractEntityWithLockedStateNotifiesPlayer() {
    World world = MockBukkitSetup.createMockWorld("world");
    Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "TestPlayer");
    LivingEntity entity =
        (LivingEntity)
            MockBukkitSetup.createMockEntityWithDetails(
                EntityType.ZOMBIE, new Location(world, 0, 0, 0));

    EventInstanceId instanceId = EventInstanceId.generate();
    EventInstance instance = EventInstance.create(instanceId, new EventDefinitionId("chest_event"));
    repository.save(instance);

    entity
        .getPersistentDataContainer()
        .set(
            BukkitPdcKeys.EVENT_INSTANCE_ID,
            PersistentDataType.STRING,
            instanceId.value().toString());

    EventRuntimeState state = new EventRuntimeState(instanceId);
    state.setLockedUntilMillis(System.currentTimeMillis() + 3000);
    stateStore.put(state);

    PlayerInteractEntityEvent event =
        new PlayerInteractEntityEvent(player, entity, EquipmentSlot.HAND);
    router.onPlayerInteractEntity(event);

    assertTrue(event.isCancelled());
  }

  @Test
  void testInteractEntityWithoutDelegateAndNoEngine() {
    World world = MockBukkitSetup.createMockWorld("world");
    Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "TestPlayer");
    LivingEntity entity =
        (LivingEntity)
            MockBukkitSetup.createMockEntityWithDetails(
                EntityType.ZOMBIE, new Location(world, 0, 0, 0));

    EventInstanceId instanceId = EventInstanceId.generate();
    EventInstance instance =
        EventInstance.create(instanceId, new EventDefinitionId("simple_event"));
    repository.save(instance);

    entity
        .getPersistentDataContainer()
        .set(
            BukkitPdcKeys.EVENT_INSTANCE_ID,
            PersistentDataType.STRING,
            instanceId.value().toString());

    PaperInteractionRouter noEngineRouter =
        new PaperInteractionRouter(orchestrationService, null, locales);
    PlayerInteractEntityEvent event =
        new PlayerInteractEntityEvent(player, entity, EquipmentSlot.HAND);
    noEngineRouter.onPlayerInteractEntity(event);

    assertTrue(event.isCancelled());
  }

  @Test
  void testDamageCombatWithoutPdcDoesNothing() {
    World world = MockBukkitSetup.createMockWorld("world");
    Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "TestPlayer");
    LivingEntity entity =
        (LivingEntity)
            MockBukkitSetup.createMockEntityWithDetails(
                EntityType.ZOMBIE, new Location(world, 0, 0, 0));

    EntityDamageByEntityEvent event = createDamageEvent(player, entity, 10.0);
    router.onEntityDamageByEntity(event);

    assertFalse(event.isCancelled());
  }

  @Test
  void testDamageCombatNonPlayerDamagerDoesNothing() {
    World world = MockBukkitSetup.createMockWorld("world");
    LivingEntity damager =
        (LivingEntity)
            MockBukkitSetup.createMockEntityWithDetails(
                EntityType.SKELETON, new Location(world, 0, 0, 0));
    LivingEntity victim =
        (LivingEntity)
            MockBukkitSetup.createMockEntityWithDetails(
                EntityType.ZOMBIE, new Location(world, 0, 0, 0));

    EntityDamageByEntityEvent event = createDamageEvent(damager, victim, 5.0);
    router.onEntityDamageByEntity(event);

    assertFalse(event.isCancelled());
  }

  @Test
  void testDamageCombatWithPdcEvaluatesTriggerAndCancels() {
    World world = MockBukkitSetup.createMockWorld("world");
    Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "TestPlayer");
    LivingEntity entity =
        (LivingEntity)
            MockBukkitSetup.createMockEntityWithDetails(
                EntityType.ZOMBIE, new Location(world, 0, 0, 0));

    EventInstanceId instanceId = EventInstanceId.generate();
    EventInstance instance = EventInstance.create(instanceId, new EventDefinitionId("boss_event"));
    repository.save(instance);

    entity
        .getPersistentDataContainer()
        .set(
            BukkitPdcKeys.EVENT_INSTANCE_ID,
            PersistentDataType.STRING,
            instanceId.value().toString());

    EntityDamageByEntityEvent event = createDamageEvent(player, entity, 15.0);
    router.onEntityDamageByEntity(event);

    assertTrue(event.isCancelled());
  }

  @Test
  void testDamageCombatWithNullExecutionEngineDelegatesToEntityEvent() {
    World world = MockBukkitSetup.createMockWorld("world");
    Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "TestPlayer");
    LivingEntity entity =
        (LivingEntity)
            MockBukkitSetup.createMockEntityWithDetails(
                EntityType.ZOMBIE, new Location(world, 0, 0, 0));

    EventInstanceId instanceId = EventInstanceId.generate();
    EventInstance instance = EventInstance.create(instanceId, new EventDefinitionId("boss_event"));
    repository.save(instance);

    entity
        .getPersistentDataContainer()
        .set(
            BukkitPdcKeys.EVENT_INSTANCE_ID,
            PersistentDataType.STRING,
            instanceId.value().toString());

    PaperInteractionRouter nullEngineRouter =
        new PaperInteractionRouter(orchestrationService, null, locales);
    EntityDamageByEntityEvent event = createDamageEvent(player, entity, 15.0);
    nullEngineRouter.onEntityDamageByEntity(event);

    assertTrue(event.isCancelled());
  }

  @Test
  void testDamageCombatWithInvalidPdcCatchesException() {
    World world = MockBukkitSetup.createMockWorld("world");
    Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "TestPlayer");
    LivingEntity entity =
        (LivingEntity)
            MockBukkitSetup.createMockEntityWithDetails(
                EntityType.ZOMBIE, new Location(world, 0, 0, 0));

    entity
        .getPersistentDataContainer()
        .set(BukkitPdcKeys.EVENT_INSTANCE_ID, PersistentDataType.STRING, "invalid-uuid");

    EntityDamageByEntityEvent event = createDamageEvent(player, entity, 15.0);
    router.onEntityDamageByEntity(event);

    assertTrue(event.isCancelled());
  }

  @Test
  void testInteractEntityHandledWithLock() {
    World world = MockBukkitSetup.createMockWorld("world");
    Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "TestPlayer");
    LivingEntity entity =
        (LivingEntity)
            MockBukkitSetup.createMockEntityWithDetails(
                EntityType.ZOMBIE, new Location(world, 0, 0, 0));

    PhaseDefinition activePhase =
        new PhaseDefinition(
            new PhaseId("active"),
            Set.of(new PhaseId("next")),
            List.of(
                new TransitionRule(
                    new ConfiguredTriggerDefinition("interaction"),
                    List.of(),
                    Optional.of(new PhaseId("next")),
                    List.of())),
            List.of());
    PhaseDefinition nextPhase =
        new PhaseDefinition(new PhaseId("next"), Set.of(), List.of(), List.of());
    EventDefinition definition =
        new EventDefinition(
            new EventDefinitionId("locked_event"),
            new PhaseId("active"),
            Map.of(new PhaseId("active"), activePhase, new PhaseId("next"), nextPhase));
    registry.register(definition, "events/locked.yml");

    EventInstanceId instanceId = EventInstanceId.generate();
    EventInstance instance =
        EventInstance.reconstitute(
            instanceId,
            new EventDefinitionId("locked_event"),
            EventLifecycleState.RUNNING,
            new PhaseId("active"));
    repository.save(instance);

    entity
        .getPersistentDataContainer()
        .set(
            BukkitPdcKeys.EVENT_INSTANCE_ID,
            PersistentDataType.STRING,
            instanceId.value().toString());

    // 1. With locked state
    EventRuntimeState state = new EventRuntimeState(instanceId);
    state.setLockedUntilMillis(System.currentTimeMillis() + 10000);
    stateStore.put(state);

    PlayerInteractEntityEvent event =
        new PlayerInteractEntityEvent(player, entity, EquipmentSlot.HAND);
    router.onPlayerInteractEntity(event);
    assertTrue(event.isCancelled());

    // 2. With unlocked state (not locked)
    state.setLockedUntilMillis(0);
    PlayerInteractEntityEvent event2 =
        new PlayerInteractEntityEvent(player, entity, EquipmentSlot.HAND);
    router.onPlayerInteractEntity(event2);
    assertTrue(event2.isCancelled());
  }
}
