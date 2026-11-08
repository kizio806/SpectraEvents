package io.github.kizio806.spectraevents.platform.paper.interaction;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.execution.EventExecutionEngine;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaperEntityDeathRouterTest {

  @BeforeAll
  static void beforeAll() {
    MockBukkitSetup.ensureInitialized();
  }

  private EventRuntimeStateStore stateStore;
  private EventExecutionEngine executionEngine;
  private EventOrchestrationService orchestrationService;
  private PaperEntityDeathRouter router;
  private InMemoryEventInstanceRepository repository;

  private DamageSource mockDamageSource;

  @BeforeEach
  void setUp() {
    mockDamageSource =
        (DamageSource)
            Proxy.newProxyInstance(
                org.bukkit.damage.DamageSource.class.getClassLoader(),
                new Class<?>[] {org.bukkit.damage.DamageSource.class},
                (p, m, a) -> null);
    stateStore = new EventRuntimeStateStore();
    EventDefinitionRegistry registry = new EventDefinitionRegistry();
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

    router = new PaperEntityDeathRouter(orchestrationService, executionEngine);
  }

  @Test
  void testEntityDeathWithoutPdcDoesNothing() {
    World world = MockBukkitSetup.createMockWorld("world");
    LivingEntity entity =
        (LivingEntity)
            MockBukkitSetup.createMockEntityWithDetails(
                EntityType.ZOMBIE, new Location(world, 0, 0, 0));

    EntityDeathEvent event =
        new EntityDeathEvent(entity, mockDamageSource, Collections.emptyList());
    router.onEntityDeath(event);
  }

  @Test
  void testEntityDeathWithInvalidPdcCatchesException() {
    World world = MockBukkitSetup.createMockWorld("world");
    LivingEntity entity =
        (LivingEntity)
            MockBukkitSetup.createMockEntityWithDetails(
                EntityType.ZOMBIE, new Location(world, 0, 0, 0));

    entity
        .getPersistentDataContainer()
        .set(BukkitPdcKeys.EVENT_INSTANCE_ID, PersistentDataType.STRING, "invalid-uuid");

    EntityDeathEvent event =
        new EntityDeathEvent(entity, mockDamageSource, Collections.emptyList());
    router.onEntityDeath(event);
  }

  @Test
  void testEntityDeathWithDelegateInvokesDelegate() {
    World world = MockBukkitSetup.createMockWorld("world");
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

    List<String> invoked = new ArrayList<>();
    router.registerDelegate(
        "boss_event",
        (deadEntity, inst) -> {
          invoked.add(inst.definitionId().value());
        });

    EntityDeathEvent event =
        new EntityDeathEvent(entity, mockDamageSource, Collections.emptyList());
    router.onEntityDeath(event);

    assertEquals(1, invoked.size());
    assertEquals("boss_event", invoked.get(0));
  }

  @Test
  void testEntityDeathWithWaveIdAndKillerRecordsWaveEntityDeath() {
    Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "Slayer");
    org.bukkit.persistence.PersistentDataContainer pdc = MockBukkitSetup.createMockPdc();
    LivingEntity entity =
        (LivingEntity)
            Proxy.newProxyInstance(
                LivingEntity.class.getClassLoader(),
                new Class<?>[] {LivingEntity.class},
                (proxy, method, args) -> {
                  if ("getKiller".equals(method.getName())) return player;
                  if ("getUniqueId".equals(method.getName())) return UUID.randomUUID();
                  if ("getPersistentDataContainer".equals(method.getName())) {
                    return pdc;
                  }
                  return null;
                });

    EventInstanceId instanceId = EventInstanceId.generate();
    EventInstance instance = EventInstance.create(instanceId, new EventDefinitionId("wave_event"));
    repository.save(instance);

    entity
        .getPersistentDataContainer()
        .set(
            BukkitPdcKeys.EVENT_INSTANCE_ID,
            PersistentDataType.STRING,
            instanceId.value().toString());
    entity
        .getPersistentDataContainer()
        .set(BukkitPdcKeys.WAVE_ID, PersistentDataType.STRING, "wave_1");

    EntityDeathEvent event =
        new EntityDeathEvent(entity, mockDamageSource, Collections.emptyList());
    router.onEntityDeath(event);
  }

  @Test
  void testEntityDeathWithWaveIdAndNullKiller() {
    org.bukkit.persistence.PersistentDataContainer pdc = MockBukkitSetup.createMockPdc();
    LivingEntity entity =
        (LivingEntity)
            Proxy.newProxyInstance(
                LivingEntity.class.getClassLoader(),
                new Class<?>[] {LivingEntity.class},
                (proxy, method, args) -> {
                  if ("getKiller".equals(method.getName())) return null;
                  if ("getUniqueId".equals(method.getName())) return UUID.randomUUID();
                  if ("getPersistentDataContainer".equals(method.getName())) {
                    return pdc;
                  }
                  return null;
                });

    EventInstanceId instanceId = EventInstanceId.generate();
    EventInstance instance = EventInstance.create(instanceId, new EventDefinitionId("wave_event"));
    repository.save(instance);

    entity
        .getPersistentDataContainer()
        .set(
            BukkitPdcKeys.EVENT_INSTANCE_ID,
            PersistentDataType.STRING,
            instanceId.value().toString());
    entity
        .getPersistentDataContainer()
        .set(BukkitPdcKeys.WAVE_ID, PersistentDataType.STRING, "wave_1");

    EntityDeathEvent event =
        new EntityDeathEvent(entity, mockDamageSource, Collections.emptyList());
    router.onEntityDeath(event);
  }

  @Test
  void testEntityDeathWithoutDelegateEvaluatesTriggerWithAndWithoutKiller() {
    Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "Slayer");
    org.bukkit.persistence.PersistentDataContainer pdc1 = MockBukkitSetup.createMockPdc();
    LivingEntity entityWithKiller =
        (LivingEntity)
            Proxy.newProxyInstance(
                LivingEntity.class.getClassLoader(),
                new Class<?>[] {LivingEntity.class},
                (proxy, method, args) -> {
                  if ("getKiller".equals(method.getName())) return player;
                  if ("getUniqueId".equals(method.getName())) return UUID.randomUUID();
                  if ("getPersistentDataContainer".equals(method.getName())) {
                    return pdc1;
                  }
                  return null;
                });

    org.bukkit.persistence.PersistentDataContainer pdc2 = MockBukkitSetup.createMockPdc();
    LivingEntity entityWithoutKiller =
        (LivingEntity)
            Proxy.newProxyInstance(
                LivingEntity.class.getClassLoader(),
                new Class<?>[] {LivingEntity.class},
                (proxy, method, args) -> {
                  if ("getKiller".equals(method.getName())) return null;
                  if ("getUniqueId".equals(method.getName())) return UUID.randomUUID();
                  if ("getPersistentDataContainer".equals(method.getName())) {
                    return pdc2;
                  }
                  return null;
                });

    EventInstanceId instanceId = EventInstanceId.generate();
    EventInstance instance = EventInstance.create(instanceId, new EventDefinitionId("plain_event"));
    repository.save(instance);

    entityWithKiller
        .getPersistentDataContainer()
        .set(
            BukkitPdcKeys.EVENT_INSTANCE_ID,
            PersistentDataType.STRING,
            instanceId.value().toString());

    entityWithoutKiller
        .getPersistentDataContainer()
        .set(
            BukkitPdcKeys.EVENT_INSTANCE_ID,
            PersistentDataType.STRING,
            instanceId.value().toString());

    router.onEntityDeath(
        new EntityDeathEvent(entityWithKiller, mockDamageSource, Collections.emptyList()));
    router.onEntityDeath(
        new EntityDeathEvent(entityWithoutKiller, mockDamageSource, Collections.emptyList()));
  }

  @Test
  void testSecondaryConstructor() {
    PaperEntityDeathRouter minimalRouter = new PaperEntityDeathRouter(orchestrationService);
    World world = MockBukkitSetup.createMockWorld("world");
    LivingEntity entity =
        (LivingEntity)
            MockBukkitSetup.createMockEntityWithDetails(
                EntityType.ZOMBIE, new Location(world, 0, 0, 0));
    EntityDeathEvent event =
        new EntityDeathEvent(entity, mockDamageSource, Collections.emptyList());
    minimalRouter.onEntityDeath(event);

    EventInstanceId instanceId = EventInstanceId.generate();
    EventInstance instance = EventInstance.create(instanceId, new EventDefinitionId("plain_event"));
    repository.save(instance);
    entity
        .getPersistentDataContainer()
        .set(
            BukkitPdcKeys.EVENT_INSTANCE_ID,
            PersistentDataType.STRING,
            instanceId.value().toString());
    minimalRouter.onEntityDeath(event);
  }
}
