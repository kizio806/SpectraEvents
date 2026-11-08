package io.github.kizio806.spectraevents.platform.spigot.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.SpectraEventsApplication;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.application.port.LifecycleReporter;
import io.github.kizio806.spectraevents.application.port.ModelRendererPort;
import io.github.kizio806.spectraevents.application.port.PlatformActionPort;
import io.github.kizio806.spectraevents.application.port.PlatformCapabilityQuery;
import io.github.kizio806.spectraevents.application.port.PlatformEntityReconcilerPort;
import io.github.kizio806.spectraevents.application.repository.InMemoryEventInstanceRepository;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinition;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.phase.PhaseDefinition;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleState;
import io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SpigotEventCommandHandlerTest {

  @BeforeAll
  static void beforeAll() {
    MockBukkitSetup.ensureInitialized();
  }

  private SpigotEventCommandHandler handler;
  private SpectraEventsApplication application;
  private TestRepository repository;
  private List<String> messages;
  private SpigotCommandRoutingTest.DummySender sender;
  private AtomicInteger resourceCountStub;

  @BeforeEach
  void setUp() {
    messages = new ArrayList<>();
    sender = new SpigotCommandRoutingTest.DummySender(messages);
    sender.op = true;

    Map<String, String> localeMap = new HashMap<>();
    localeMap.put("command.spigot.events-title", "Events (%count%):");
    localeMap.put("command.spigot.event-entry", " - %id% (%definition%, %phase%)");
    localeMap.put("command.spigot.event-started", "Started %id%");
    localeMap.put("command.spigot.event-cancelled", "Cancelled %id%");
    localeMap.put("command.spigot.trigger-result", "Trigger %trigger% result %handled%");
    localeMap.put("command.spigot.inspect", "Event %id% state: %state%");
    localeMap.put("command.spigot.inspect-recovery-guidance", "Recovery needed");
    localeMap.put("command.spigot.inspect-claim-guidance", "Claim guidance");
    localeMap.put("command.spigot.failed", "Failed: %reason%");

    LocaleCatalog locales = LocaleCatalog.of(localeMap);
    SpigotCommandHelper helper = new SpigotCommandHelper(locales);
    repository = new TestRepository();

    resourceCountStub = new AtomicInteger(0);

    LifecycleReporter reporter =
        (LifecycleReporter)
            Proxy.newProxyInstance(
                LifecycleReporter.class.getClassLoader(),
                new Class<?>[] {LifecycleReporter.class},
                (proxy, method, args) -> null);

    EventTaskScheduler scheduler =
        (EventTaskScheduler)
            Proxy.newProxyInstance(
                EventTaskScheduler.class.getClassLoader(),
                new Class<?>[] {EventTaskScheduler.class},
                (proxy, method, args) -> {
                  if ("pendingTaskCount".equals(method.getName())) {
                    return 0;
                  }
                  return null;
                });

    PlatformActionPort actionPort =
        (PlatformActionPort)
            Proxy.newProxyInstance(
                PlatformActionPort.class.getClassLoader(),
                new Class<?>[] {PlatformActionPort.class},
                (proxy, method, args) -> {
                  if ("resourceCount".equals(method.getName())) {
                    return resourceCountStub.get();
                  }
                  return null;
                });

    PlatformEntityReconcilerPort reconciler =
        (PlatformEntityReconcilerPort)
            Proxy.newProxyInstance(
                PlatformEntityReconcilerPort.class.getClassLoader(),
                new Class<?>[] {PlatformEntityReconcilerPort.class},
                (proxy, method, args) -> null);

    PlatformCapabilityQuery capabilityQuery =
        (PlatformCapabilityQuery)
            Proxy.newProxyInstance(
                PlatformCapabilityQuery.class.getClassLoader(),
                new Class<?>[] {PlatformCapabilityQuery.class},
                (proxy, method, args) -> null);

    ModelRendererPort renderer =
        (ModelRendererPort)
            Proxy.newProxyInstance(
                ModelRendererPort.class.getClassLoader(),
                new Class<?>[] {ModelRendererPort.class},
                (proxy, method, args) -> null);

    application =
        new SpectraEventsApplication(
            reporter, scheduler, actionPort, repository, reconciler, capabilityQuery, renderer);

    PhaseDefinition phaseDef =
        new PhaseDefinition(new PhaseId("idle"), Set.of(), List.of(), List.of());
    EventDefinition eventDef =
        new EventDefinition(
            new EventDefinitionId("meteor"),
            new PhaseId("idle"),
            Map.of(new PhaseId("idle"), phaseDef));
    application.definitionRegistry().register(eventDef, "events/meteor.yml");

    Plugin dummyPlugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (proxy, method, args) -> null);

    handler = new SpigotEventCommandHandler(dummyPlugin, application, repository, helper);
  }

  @Test
  void testListEvents() {
    handler.execute(sender, new String[] {});
    assertFalse(messages.isEmpty());

    messages.clear();
    EventInstanceId id = EventInstanceId.generate();
    EventInstance inst =
        EventInstance.reconstitute(
            id, new EventDefinitionId("meteor"), EventLifecycleState.RUNNING, new PhaseId("idle"));
    repository.save(inst);

    EventInstanceId idNoPhase = EventInstanceId.generate();
    EventInstance instNoPhase =
        EventInstance.reconstitute(
            idNoPhase, new EventDefinitionId("meteor"), EventLifecycleState.RUNNING, null);
    repository.save(instNoPhase);

    handler.execute(sender, new String[] {"list"});
    assertFalse(messages.isEmpty());
  }

  @Test
  void testUnknownSubcommandThrowsException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> handler.execute(sender, new String[] {"nonexistent"}));
  }

  @Test
  void testMissingArgumentsThrowException() {
    assertThrows(
        IllegalArgumentException.class, () -> handler.execute(sender, new String[] {"start"}));
    assertThrows(
        IllegalArgumentException.class, () -> handler.execute(sender, new String[] {"cancel"}));
    assertThrows(
        IllegalArgumentException.class, () -> handler.execute(sender, new String[] {"inspect"}));
    assertThrows(
        IllegalArgumentException.class, () -> handler.execute(sender, new String[] {"trigger"}));
  }

  @Test
  void testStartEventSuccessAndFailure() {
    // Start valid definition
    handler.execute(sender, new String[] {"start", "meteor"});
    assertFalse(messages.isEmpty());
    assertTrue(messages.stream().anyMatch(m -> m.contains("Started")));

    // Start with failure during persistence
    repository.failSave = true;
    messages.clear();
    handler.execute(sender, new String[] {"start", "meteor"});
    assertFalse(messages.isEmpty());
    assertTrue(messages.stream().anyMatch(m -> m.contains("Failed")));
    repository.failSave = false;

    // Start unknown definition
    assertThrows(
        IllegalArgumentException.class,
        () -> handler.execute(sender, new String[] {"start", "unknown_def"}));

    // Player sender where player location world is null
    Player mockPlayerNoWorld =
        (Player)
            Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[] {Player.class},
                (proxy, method, args) -> {
                  if ("hasPermission".equals(method.getName()) || "isOp".equals(method.getName())) {
                    return true;
                  }
                  if ("getLocation".equals(method.getName())) {
                    return new Location(null, 0, 0, 0);
                  }
                  return null;
                });

    assertThrows(
        IllegalStateException.class,
        () -> handler.execute(mockPlayerNoWorld, new String[] {"start", "meteor"}));
  }

  private static final class TestRepository
      implements EventInstanceRepository,
          io.github.kizio806.spectraevents.application.port.RewardClaimRepository {
    private final InMemoryEventInstanceRepository delegate = new InMemoryEventInstanceRepository();
    private volatile boolean failSave;

    @Override
    public void save(EventInstance eventInstance) {
      delegate.save(eventInstance);
    }

    @Override
    public java.util.Optional<EventInstance> findById(EventInstanceId eventInstanceId) {
      return delegate.findById(eventInstanceId);
    }

    @Override
    public List<EventInstance> findAll() {
      return delegate.findAll();
    }

    @Override
    public boolean remove(EventInstanceId eventInstanceId) {
      return delegate.remove(eventInstanceId);
    }

    @Override
    public void saveState(EventRuntimeState state) {
      delegate.saveState(state);
    }

    @Override
    public java.util.Optional<EventRuntimeState> findState(EventInstanceId eventInstanceId) {
      return delegate.findState(eventInstanceId);
    }

    @Override
    public java.util.concurrent.CompletableFuture<Void> saveWithStateDurablyAsync(
        EventInstance eventInstance, EventRuntimeState state) {
      if (failSave) {
        return java.util.concurrent.CompletableFuture.failedFuture(
            new RuntimeException("Disk full"));
      }
      return delegate.saveWithStateDurablyAsync(eventInstance, state);
    }

    @Override
    public void savePendingDurably(
        io.github.kizio806.spectraevents.core.gameplay.reward.RewardClaim claim) {
      delegate.savePendingDurably(claim);
    }

    @Override
    public List<io.github.kizio806.spectraevents.core.gameplay.reward.RewardClaim> findPending(
        UUID playerId) {
      return delegate.findPending(playerId);
    }

    @Override
    public java.util.concurrent.CompletableFuture<
            List<io.github.kizio806.spectraevents.core.gameplay.reward.RewardClaim>>
        findByStatusAsync(
            io.github.kizio806.spectraevents.core.gameplay.reward.RewardClaimStatus status) {
      return delegate.findByStatusAsync(status);
    }

    @Override
    public boolean beginDelivery(UUID claimId) {
      return delegate.beginDelivery(claimId);
    }

    @Override
    public boolean returnToPending(UUID claimId) {
      return delegate.returnToPending(claimId);
    }

    @Override
    public boolean markDelivered(UUID claimId) {
      return delegate.markDelivered(claimId);
    }
  }

  @Test
  void testCancelEvent() {
    // Start an event first
    EventInstance instance = application.orchestrationService().startDefinition("meteor");
    messages.clear();

    handler.execute(sender, new String[] {"cancel", instance.id().toString()});
    assertFalse(messages.isEmpty());
    assertTrue(messages.stream().anyMatch(m -> m.contains("Cancelled")));
  }

  @Test
  void testTriggerEvent() {
    EventInstance instance = application.orchestrationService().startDefinition("meteor");
    messages.clear();

    // Console trigger
    handler.execute(sender, new String[] {"trigger", instance.id().toString(), "interaction"});
    assertFalse(messages.isEmpty());
    assertTrue(messages.stream().anyMatch(m -> m.contains("Trigger")));

    // Player trigger
    Player mockPlayer = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "TriggerPlayer");
    messages.clear();
    handler.execute(mockPlayer, new String[] {"trigger", instance.id().toString(), "interaction"});
  }

  @Test
  void testInspectEventVariations() {
    EventInstance instance = application.orchestrationService().startDefinition("meteor");
    messages.clear();

    // Running instance with claimant
    EventRuntimeState state =
        application.executionEngine().stateStore().get(instance.id()).orElseThrow();
    state.tryClaim("PlayerClaimant");
    handler.execute(sender, new String[] {"inspect", instance.id().toString()});
    assertFalse(messages.isEmpty());
    assertTrue(messages.stream().anyMatch(m -> m.contains("Claim guidance")));

    // Running instance with missing runtime state -> recovery guidance branch 1
    messages.clear();
    EventInstanceId idNoRuntime = EventInstanceId.generate();
    EventInstance instNoRuntime =
        EventInstance.create(idNoRuntime, new EventDefinitionId("meteor"));
    repository.save(instNoRuntime);
    handler.execute(sender, new String[] {"inspect", idNoRuntime.toString()});
    assertFalse(messages.isEmpty());
    assertTrue(messages.stream().anyMatch(m -> m.contains("Recovery needed")));

    // Terminal instance with leftover resources -> recovery guidance branch 2
    messages.clear();
    EventInstanceId idTerminalWithResources = EventInstanceId.generate();
    EventInstance instTerminal =
        EventInstance.reconstitute(
            idTerminalWithResources,
            new EventDefinitionId("meteor"),
            EventLifecycleState.COMPLETED,
            new PhaseId("idle"));
    repository.save(instTerminal);
    resourceCountStub.set(3); // non-zero resources
    handler.execute(sender, new String[] {"inspect", idTerminalWithResources.toString()});
    assertFalse(messages.isEmpty());
    assertTrue(messages.stream().anyMatch(m -> m.contains("Recovery needed")));
  }

  @Test
  void testTabComplete() {
    List<String> nullSender = handler.tabComplete(null, new String[] {""});
    assertTrue(nullSender.isEmpty());

    SpigotCommandRoutingTest.DummySender noPermSender =
        new SpigotCommandRoutingTest.DummySender(new ArrayList<>());
    noPermSender.op = false;
    noPermSender.adminPerm = false;
    noPermSender.specificPerm = false;
    List<String> noPermResult = handler.tabComplete(noPermSender, new String[] {""});
    assertTrue(noPermResult.isEmpty());

    List<String> subcommands = handler.tabComplete(sender, new String[] {""});
    assertEquals(List.of("list", "start", "trigger", "inspect", "cancel"), subcommands);

    List<String> filtered = handler.tabComplete(sender, new String[] {"c"});
    assertEquals(List.of("cancel"), filtered);

    // Tab complete start definition argument
    List<String> defCompletions = handler.tabComplete(sender, new String[] {"start", "met"});
    assertEquals(List.of("meteor"), defCompletions);

    List<String> emptyCompletions = handler.tabComplete(sender, new String[] {"other", "arg"});
    assertTrue(emptyCompletions.isEmpty());
  }
}
