package io.github.kizio806.spectraevents.platform.paper.command;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.brigadier.CommandDispatcher;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.config.spec.EventParameterSpec;
import io.github.kizio806.spectraevents.application.config.spec.EventParameterType;
import io.github.kizio806.spectraevents.application.config.spec.EventSpec;
import io.github.kizio806.spectraevents.application.config.spec.PhaseSpec;
import io.github.kizio806.spectraevents.application.execution.EventExecutionEngine;
import io.github.kizio806.spectraevents.application.execution.EventLocation;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeStateStore;
import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.application.port.PlatformActionPort;
import io.github.kizio806.spectraevents.application.repository.InMemoryEventInstanceRepository;
import io.github.kizio806.spectraevents.application.service.EventOrchestrationService;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinition;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.phase.PhaseDefinition;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleState;
import io.github.kizio806.spectraevents.platform.bukkit.config.BukkitEventSettingsStore;
import io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup;
import io.github.kizio806.spectraevents.platform.paper.scheduler.RegionTaskScheduler;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PaperEventCommandHandlerTest {

  @BeforeAll
  static void beforeAll() {
    MockBukkitSetup.ensureInitialized();
  }

  @TempDir Path tempDir;
  private EventOrchestrationService orchestrationService;
  private InMemoryEventInstanceRepository instanceRepo;
  private EventDefinitionRegistry defRegistry;
  private BukkitEventSettingsStore settingsStore;
  private RegionTaskScheduler regionScheduler;
  private LocaleCatalog locales;
  private EventCommandHandler handler;
  private List<Component> messages;
  private CommandDispatcher<CommandSourceStack> dispatcher;
  private CommandSourceStack source;
  private EventRuntimeStateStore stateStore;

  @BeforeEach
  void setUp() {
    instanceRepo = new InMemoryEventInstanceRepository();
    defRegistry = new EventDefinitionRegistry();
    settingsStore = new BukkitEventSettingsStore(tempDir.resolve("settings.yml"));
    stateStore = new EventRuntimeStateStore();

    EventTaskScheduler mockScheduler =
        (EventTaskScheduler)
            Proxy.newProxyInstance(
                EventTaskScheduler.class.getClassLoader(),
                new Class<?>[] {EventTaskScheduler.class},
                (p, m, a) -> {
                  if (m.getReturnType().equals(int.class)) return 0;
                  if (m.getReturnType().equals(boolean.class)) return false;
                  return null;
                });

    PlatformActionPort mockActionPort =
        (PlatformActionPort)
            Proxy.newProxyInstance(
                PlatformActionPort.class.getClassLoader(),
                new Class<?>[] {PlatformActionPort.class},
                (p, m, a) -> {
                  if (m.getReturnType().equals(int.class)) return 0;
                  if (m.getReturnType().equals(boolean.class)) return false;
                  if (m.getReturnType().equals(CompletableFuture.class)) {
                    return CompletableFuture.completedFuture(true);
                  }
                  return null;
                });

    EventExecutionEngine executionEngine =
        new EventExecutionEngine(
            instanceRepo, defRegistry, mockScheduler, mockActionPort, stateStore);

    orchestrationService =
        new EventOrchestrationService(instanceRepo, defRegistry, executionEngine);

    regionScheduler =
        new RegionTaskScheduler() {
          @Override
          public void executeGlobal(Runnable task) {
            task.run();
          }

          @Override
          public void executeAt(Location location, Runnable task) {
            task.run();
          }

          @Override
          public void executeFor(Entity entity, Runnable task) {
            task.run();
          }
        };

    Map<String, String> localeMap = new java.util.HashMap<>();
    localeMap.put("command.events.usage-title", "Usage: /spectraevents event <subcommand>");
    localeMap.put("command.events.usage-start", " - start <event-id>");
    localeMap.put("command.events.usage-manage", " - list, cancel, inspect");
    localeMap.put("command.events.usage-config", " - config <event-id> show|set");
    localeMap.put("command.events.value-required", "Value required: %example%");
    localeMap.put("command.events.list-title", "Active Events (%count%):");
    localeMap.put("command.events.list-entry", " - %id% (%definition%, %state%)");
    localeMap.put("command.events.started", "Started %event% in %world% (%id%)");
    localeMap.put("command.events.start-failed", "Start failed: %reason%");
    localeMap.put("command.events.overrides", "Overrides for %event%: %values%");
    localeMap.put("command.events.configuration-error", "Configuration error: %reason%");
    localeMap.put("command.events.setting-saved", "Setting %parameter% saved for %event%");
    localeMap.put("command.events.setting-not-saved", "Setting not saved: %reason%");
    localeMap.put("command.events.cancelled", "Cancelled %id%");
    localeMap.put("command.events.cancel-failed", "Cancel failed: %reason%");
    localeMap.put("command.events.trigger-result", "Trigger %trigger%: %handled%");
    localeMap.put("command.events.trigger-failed", "Trigger failed: %reason%");
    localeMap.put("command.events.inspect", "Inspect %id%: %state%");
    localeMap.put("command.events.inspect-failed", "Inspect failed: %reason%");
    localeMap.put("command.events.recovery-guidance", "Needs recovery!");
    localeMap.put("command.events.claim-guidance", "Reward claimed: %claim%");
    localeMap.put("command.events.unclaimed", "None");

    locales = LocaleCatalog.of(localeMap);

    handler =
        new EventCommandHandler(
            orchestrationService,
            instanceRepo,
            defRegistry,
            settingsStore,
            regionScheduler,
            locales);

    messages = new ArrayList<>();
    CommandSender sender = PaperCommandTestSupport.createRecordingSender(messages);
    source = PaperCommandTestSupport.createMockSource(sender, null);

    dispatcher = PaperCommandTestSupport.createDispatcher();
    dispatcher.register(handler.build());
  }

  private void registerSampleDefinition() {
    PhaseSpec initialPhase = new PhaseSpec(Set.of(), List.of(), List.of());
    EventParameterSpec editableParam =
        new EventParameterSpec(
            "duration",
            EventParameterType.INTEGER,
            60,
            BigDecimal.ONE,
            new BigDecimal("120"),
            BigDecimal.ONE,
            true);
    EventParameterSpec nonEditableParam =
        new EventParameterSpec(
            "internal-id", EventParameterType.INTEGER, 1, null, null, null, false);
    EventSpec spec =
        new EventSpec(
            "meteor",
            "1.0",
            "idle",
            Map.of("idle", initialPhase),
            Map.of(),
            Map.of(),
            Map.of("duration", editableParam, "internal-id", nonEditableParam));
    PhaseDefinition phaseDef =
        new PhaseDefinition(new PhaseId("idle"), Set.of(), List.of(), List.of());
    EventDefinition eventDef =
        new EventDefinition(
            new EventDefinitionId("meteor"),
            new PhaseId("idle"),
            Map.of(new PhaseId("idle"), phaseDef));
    defRegistry.register(eventDef, "events/meteor.yml", spec);
  }

  @Test
  void testEventUsageAndList() throws Exception {
    dispatcher.execute("event", source);
    assertFalse(messages.isEmpty());

    messages.clear();
    dispatcher.execute("event list", source);
    assertFalse(messages.isEmpty());

    // With active instance
    EventInstanceId id = EventInstanceId.generate();
    EventInstance instance = EventInstance.create(id, new EventDefinitionId("meteor"));
    instanceRepo.save(instance);

    messages.clear();
    dispatcher.execute("event list", source);
    assertFalse(messages.isEmpty());
  }

  @Test
  void testEventStartAsConsoleAndPlayer() throws Exception {
    registerSampleDefinition();

    // Start with console source
    messages.clear();
    dispatcher.execute("event start meteor", source);
    assertFalse(messages.isEmpty());

    // Start with player source
    World world = MockBukkitSetup.createMockWorld("world");
    Player player =
        PaperCommandTestSupport.createRecordingPlayer(messages, new Location(world, 10, 64, 10));
    CommandSourceStack playerSource =
        PaperCommandTestSupport.createMockSource(player, new Location(world, 10, 64, 10));

    messages.clear();
    dispatcher.execute("event start meteor", playerSource);
    assertFalse(messages.isEmpty());

    // Start with named location
    settingsStore.saveLocation("arena", new EventLocation("world", 100, 64, 100, 0, 0));
    messages.clear();
    dispatcher.execute("event start meteor location arena", playerSource);
    assertFalse(messages.isEmpty());

    // Start with unknown location
    messages.clear();
    dispatcher.execute("event start meteor location non_existent", playerSource);
    assertFalse(messages.isEmpty());
  }

  @Test
  void testEventConfigurationShowAndSet() throws Exception {
    registerSampleDefinition();

    // Config definition without subcommand (triggers lambda$build$5)
    messages.clear();
    dispatcher.execute("event config meteor", source);
    assertFalse(messages.isEmpty());

    // Show configuration
    messages.clear();
    dispatcher.execute("event config meteor show", source);
    assertFalse(messages.isEmpty());

    // Show configuration for unregistered event
    messages.clear();
    dispatcher.execute("event config unknown_event show", source);
    assertFalse(messages.isEmpty());

    // Set gui editable configuration
    messages.clear();
    dispatcher.execute("event config meteor set duration 90", source);
    assertFalse(messages.isEmpty());

    // Set configuration for unregistered event (triggers lambda$setConfiguration$0)
    messages.clear();
    dispatcher.execute("event config unknown_event set duration 90", source);
    assertFalse(messages.isEmpty());

    // Set non-gui editable configuration (throws IllegalArgumentException)
    messages.clear();
    dispatcher.execute("event config meteor set internal-id custom", source);
    assertFalse(messages.isEmpty());

    // Set non-existent configuration
    messages.clear();
    dispatcher.execute("event config meteor set unknown_param 123", source);
    assertFalse(messages.isEmpty());
  }

  @Test
  void testEventTriggerExecution() throws Exception {
    EventInstanceId id = EventInstanceId.generate();
    EventInstance instance = EventInstance.create(id, new EventDefinitionId("meteor"));
    instanceRepo.save(instance);

    // Trigger as console
    messages.clear();
    dispatcher.execute("event trigger " + id + " interaction", source);
    assertFalse(messages.isEmpty());

    // Trigger as player
    World world = MockBukkitSetup.createMockWorld("world");
    Player player =
        PaperCommandTestSupport.createRecordingPlayer(messages, new Location(world, 10, 64, 10));
    CommandSourceStack playerSource =
        PaperCommandTestSupport.createMockSource(player, new Location(world, 10, 64, 10));

    messages.clear();
    dispatcher.execute("event trigger " + id + " interaction", playerSource);
    assertFalse(messages.isEmpty());
  }

  @Test
  void testEventInspectWithVariousStates() throws Exception {
    EventInstanceId id = EventInstanceId.generate();
    EventInstance instance =
        EventInstance.reconstitute(
            id,
            new EventDefinitionId("meteor"),
            EventLifecycleState.COMPLETED,
            new PhaseId("idle"));
    instanceRepo.save(instance);

    // Inspect terminal state without runtime state (needsRecoveryGuidance = false, claimant = null)
    messages.clear();
    dispatcher.execute("event inspect " + id, source);
    assertFalse(messages.isEmpty());

    // Inspect with runtime state and claimant
    EventRuntimeState state = new EventRuntimeState(id);
    state.tryClaim("player-claimant");
    stateStore.put(state);

    EventInstance runningInstance =
        EventInstance.reconstitute(
            id, new EventDefinitionId("meteor"), EventLifecycleState.RUNNING, new PhaseId("idle"));
    instanceRepo.save(runningInstance);

    messages.clear();
    dispatcher.execute("event inspect " + id, source);
    assertFalse(messages.isEmpty());
  }

  @Test
  void testNeedsRecoveryGuidanceDirect() {
    assertFalse(EventCommandHandler.needsRecoveryGuidance(true, false, 0, 0));
    assertTrue(EventCommandHandler.needsRecoveryGuidance(false, false, 0, 0));
    assertTrue(EventCommandHandler.needsRecoveryGuidance(true, true, 1, 0));
    assertTrue(EventCommandHandler.needsRecoveryGuidance(true, true, 0, 1));
  }

  @Test
  void testEventCancelSuccessAndFailure() throws Exception {
    EventInstanceId id = EventInstanceId.generate();
    EventInstance instance =
        EventInstance.reconstitute(
            id, new EventDefinitionId("meteor"), EventLifecycleState.RUNNING, new PhaseId("idle"));
    instanceRepo.save(instance);

    // Successful cancellation
    messages.clear();
    dispatcher.execute("event cancel " + id, source);
    assertFalse(messages.isEmpty());

    // Failed cancellation (instance does not exist in repo)
    messages.clear();
    dispatcher.execute("event cancel " + EventInstanceId.generate(), source);
    assertFalse(messages.isEmpty());
  }

  @Test
  void testMissingArguments() throws Exception {
    messages.clear();
    dispatcher.execute("event start", source);
    assertFalse(messages.isEmpty());

    messages.clear();
    dispatcher.execute("event cancel", source);
    assertFalse(messages.isEmpty());

    messages.clear();
    dispatcher.execute("event trigger", source);
    assertFalse(messages.isEmpty());

    messages.clear();
    dispatcher.execute("event inspect", source);
    assertFalse(messages.isEmpty());

    messages.clear();
    dispatcher.execute("event config", source);
    assertFalse(messages.isEmpty());
  }

  @Test
  void testEventStartFailuresAndNullSettings() throws Exception {
    registerSampleDefinition();

    // Start with non-existent definition
    messages.clear();
    dispatcher.execute("event start unknown_event", source);
    assertFalse(messages.isEmpty());

    // Start with null settings store
    EventCommandHandler noSettingsHandler =
        new EventCommandHandler(
            orchestrationService, instanceRepo, defRegistry, null, regionScheduler, locales);
    CommandDispatcher<CommandSourceStack> noSettingsDispatcher =
        PaperCommandTestSupport.createDispatcher();
    noSettingsDispatcher.register(noSettingsHandler.build());

    messages.clear();
    noSettingsDispatcher.execute("event start meteor", source);
    assertFalse(messages.isEmpty());

    // Named location with null settingsStore
    messages.clear();
    noSettingsDispatcher.execute("event start meteor location arena", source);
    assertFalse(messages.isEmpty());

    // Config show/set with null settingsStore
    messages.clear();
    noSettingsDispatcher.execute("event config meteor show", source);
    assertFalse(messages.isEmpty());

    messages.clear();
    noSettingsDispatcher.execute("event config meteor set duration 10", source);
    assertFalse(messages.isEmpty());
  }

  @Test
  void testEventStartAsyncFailure() throws Exception {
    registerSampleDefinition();

    EventTaskScheduler mockScheduler =
        (EventTaskScheduler)
            Proxy.newProxyInstance(
                EventTaskScheduler.class.getClassLoader(),
                new Class<?>[] {EventTaskScheduler.class},
                (p, m, a) -> {
                  if (m.getReturnType().equals(int.class)) return 0;
                  if (m.getReturnType().equals(boolean.class)) return false;
                  return null;
                });

    PlatformActionPort mockActionPort =
        (PlatformActionPort)
            Proxy.newProxyInstance(
                PlatformActionPort.class.getClassLoader(),
                new Class<?>[] {PlatformActionPort.class},
                (p, m, a) -> {
                  if (m.getReturnType().equals(int.class)) return 0;
                  if (m.getReturnType().equals(boolean.class)) return false;
                  if (m.getReturnType().equals(CompletableFuture.class)) {
                    return CompletableFuture.completedFuture(true);
                  }
                  return null;
                });

    io.github.kizio806.spectraevents.application.port.EventInstanceRepository failingRepo =
        new io.github.kizio806.spectraevents.application.port.EventInstanceRepository() {
          @Override
          public void save(EventInstance eventInstance) {}

          @Override
          public java.util.Optional<EventInstance> findById(EventInstanceId eventInstanceId) {
            return java.util.Optional.empty();
          }

          @Override
          public List<EventInstance> findAll() {
            return List.of();
          }

          @Override
          public boolean remove(EventInstanceId eventInstanceId) {
            return false;
          }

          @Override
          public CompletableFuture<Void> saveWithStateDurablyAsync(
              EventInstance eventInstance, EventRuntimeState state) {
            return CompletableFuture.failedFuture(new RuntimeException("Async start failure"));
          }
        };

    EventExecutionEngine failingEngine =
        new EventExecutionEngine(
            failingRepo, defRegistry, mockScheduler, mockActionPort, stateStore);

    EventOrchestrationService failingOrchestrator =
        new EventOrchestrationService(failingRepo, defRegistry, failingEngine);

    EventCommandHandler failingHandler =
        new EventCommandHandler(
            failingOrchestrator, failingRepo, defRegistry, settingsStore, regionScheduler, locales);
    CommandDispatcher<CommandSourceStack> failingDispatcher =
        PaperCommandTestSupport.createDispatcher();
    failingDispatcher.register(failingHandler.build());

    // Async failure with console sender
    messages.clear();
    failingDispatcher.execute("event start meteor", source);
    assertFalse(messages.isEmpty());

    // Async failure with player sender
    World world = MockBukkitSetup.createMockWorld("world");
    Player player =
        PaperCommandTestSupport.createRecordingPlayer(messages, new Location(world, 10, 64, 10));
    CommandSourceStack playerSource =
        PaperCommandTestSupport.createMockSource(player, new Location(world, 10, 64, 10));

    messages.clear();
    failingDispatcher.execute("event start meteor", playerSource);
    assertFalse(messages.isEmpty());

    // Async failure with null message in exception
    io.github.kizio806.spectraevents.application.port.EventInstanceRepository nullMsgRepo =
        new io.github.kizio806.spectraevents.application.port.EventInstanceRepository() {
          @Override
          public void save(EventInstance eventInstance) {}

          @Override
          public java.util.Optional<EventInstance> findById(EventInstanceId eventInstanceId) {
            return java.util.Optional.empty();
          }

          @Override
          public List<EventInstance> findAll() {
            return List.of();
          }

          @Override
          public boolean remove(EventInstanceId eventInstanceId) {
            return false;
          }

          @Override
          public CompletableFuture<Void> saveWithStateDurablyAsync(
              EventInstance eventInstance, EventRuntimeState state) {
            return CompletableFuture.failedFuture(new NullPointerException());
          }
        };

    EventExecutionEngine nullMsgEngine =
        new EventExecutionEngine(
            nullMsgRepo, defRegistry, mockScheduler, mockActionPort, stateStore);

    EventOrchestrationService nullMsgOrchestrator =
        new EventOrchestrationService(nullMsgRepo, defRegistry, nullMsgEngine);

    EventCommandHandler nullMsgHandler =
        new EventCommandHandler(
            nullMsgOrchestrator, nullMsgRepo, defRegistry, settingsStore, regionScheduler, locales);
    CommandDispatcher<CommandSourceStack> nullMsgDispatcher =
        PaperCommandTestSupport.createDispatcher();
    nullMsgDispatcher.register(nullMsgHandler.build());

    messages.clear();
    nullMsgDispatcher.execute("event start meteor", source);
    assertFalse(messages.isEmpty());
  }

  @Test
  void testInspectAndTriggerWithInvalidInstanceId() throws Exception {
    messages.clear();
    dispatcher.execute("event inspect not-a-uuid", source);
    assertFalse(messages.isEmpty());

    messages.clear();
    dispatcher.execute("event trigger not-a-uuid some_trigger", source);
    assertFalse(messages.isEmpty());
  }

  @Test
  void testInspectRecoveryGuidance() throws Exception {
    EventInstanceId id = EventInstanceId.generate();
    // Running instance without runtime state present -> needsRecoveryGuidance is true
    EventInstance instance =
        EventInstance.reconstitute(
            id, new EventDefinitionId("meteor"), EventLifecycleState.RUNNING, new PhaseId("idle"));
    instanceRepo.save(instance);

    messages.clear();
    dispatcher.execute("event inspect " + id, source);
    assertFalse(messages.isEmpty());
  }

  @Test
  void testSuggestions() {
    registerSampleDefinition();
    EventInstanceId id = EventInstanceId.generate();
    EventInstance instance = EventInstance.create(id, new EventDefinitionId("meteor"));
    instanceRepo.save(instance);
    settingsStore.saveLocation("spawn_area", new EventLocation("world", 0, 64, 0, 0, 0));

    // Active instances suggestions
    var activeSuggestions =
        dispatcher.getCompletionSuggestions(dispatcher.parse("event cancel ", source)).join();
    assertFalse(activeSuggestions.getList().isEmpty());

    // Definitions suggestions
    var defSuggestions =
        dispatcher.getCompletionSuggestions(dispatcher.parse("event start met", source)).join();
    assertFalse(defSuggestions.getList().isEmpty());

    // Locations suggestions
    var locSuggestions =
        dispatcher
            .getCompletionSuggestions(dispatcher.parse("event start meteor location sp", source))
            .join();
    assertFalse(locSuggestions.getList().isEmpty());

    // Parameters suggestions
    var paramSuggestions =
        dispatcher
            .getCompletionSuggestions(dispatcher.parse("event config meteor set du", source))
            .join();
    assertFalse(paramSuggestions.getList().isEmpty());
  }
}
