package io.github.kizio806.spectraevents.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kizio806.spectraevents.application.asset.AssetTargetProfile;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackDeliveryCoordinator;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackDeliverySettings;
import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.application.port.LifecycleReporter;
import io.github.kizio806.spectraevents.application.port.ModelRendererPort;
import io.github.kizio806.spectraevents.application.port.PlatformActionPort;
import io.github.kizio806.spectraevents.application.port.PlatformCapability;
import io.github.kizio806.spectraevents.application.port.PlatformCapabilityQuery;
import io.github.kizio806.spectraevents.application.port.PlatformDescriptor;
import io.github.kizio806.spectraevents.application.port.PlatformEntityReconcilerPort;
import io.github.kizio806.spectraevents.application.repository.InMemoryEventInstanceRepository;
import io.github.kizio806.spectraevents.application.schedule.CronExpression;
import io.github.kizio806.spectraevents.application.schedule.EventSchedule;
import io.github.kizio806.spectraevents.application.service.EntityReconciliationReport;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinition;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.phase.PhaseDefinition;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class SpectraEventsApplicationTest {

  @Test
  void reportsLifecycleInOrder() {
    RecordingLifecycleReporter reporter = new RecordingLifecycleReporter();
    SpectraEventsApplication application = new SpectraEventsApplication(reporter);

    application.start();
    application.stop();

    assertEquals(List.of("started", "stopped"), reporter.events);
  }

  @Test
  void testWiringAndAccessors() {
    RecordingLifecycleReporter reporter = new RecordingLifecycleReporter();
    EventTaskScheduler scheduler =
        new EventTaskScheduler() {
          @Override
          public void schedule(EventInstanceId eventId, Duration delay, Runnable task) {}

          @Override
          public void scheduleGlobal(Duration delay, Runnable task) {}

          @Override
          public void cancelAll(EventInstanceId eventId) {}

          @Override
          public void cancelAll() {}
        };

    PlatformActionPort actionPort =
        (executionId, action, executionContext) -> CompletableFuture.completedFuture(null);
    InMemoryEventInstanceRepository repository = new InMemoryEventInstanceRepository();
    PlatformEntityReconcilerPort reconcilerPort =
        new PlatformEntityReconcilerPort() {
          @Override
          public List<DiscoveredEntity> scanLoadedEntities() {
            return List.of();
          }

          @Override
          public void removeEntity(Object platformEntityReference) {}

          @Override
          public void restoreInstance(
              EventInstanceId instanceId, List<DiscoveredEntity> entities) {}
        };

    PlatformCapabilityQuery capabilityQuery =
        new PlatformCapabilityQuery() {
          @Override
          public boolean hasCapability(PlatformCapability capability) {
            return true;
          }

          @Override
          public PlatformDescriptor platformDescriptor() {
            return new PlatformDescriptor("Paper", "Paper", "26.1.0", "26.1.0-R0.1-SNAPSHOT");
          }
        };
    SpectraEventsApplication app =
        new SpectraEventsApplication(
            reporter,
            scheduler,
            actionPort,
            repository,
            reconcilerPort,
            capabilityQuery,
            (ModelRendererPort) null);

    assertNotNull(app.definitionRegistry());
    assertNotNull(app.definitionLoader());
    assertNotNull(app.modelDefinitionRegistry());
    assertNotNull(app.modelCompiler());
    assertNull(app.modelRuntimeService());
    assertNull(app.assetPipelineService());
    app.setAssetPipelineService(null);
    assertNull(app.assetPipelineService());
    assertNotNull(app.animationDefinitionRegistry());
    assertNotNull(app.activeAnimationRegistry());
    assertNull(app.animationRuntimeService());
    assertNotNull(app.orchestrationService());
    assertNotNull(app.rewardMailboxService());
    assertNotNull(app.executionEngine());
    assertNotNull(app.reconciliationService());
    assertNotNull(app.scheduleService());
    assertNotNull(app.integrationRegistry());
    assertNotNull(app.operationalReadinessValidator());
    assertEquals(capabilityQuery, app.capabilityQuery());

    // Settings & coordinators
    ResourcePackDeliverySettings packSettings =
        new ResourcePackDeliverySettings(false, false, "", "", "", "", "");
    app.setResourcePackSettings(packSettings);
    assertEquals(packSettings, app.resourcePackSettings());

    app.setAssetTargetProfile(AssetTargetProfile.PROFILE_26_1);
    assertEquals(AssetTargetProfile.PROFILE_26_1, app.assetTargetProfile());

    ResourcePackDeliveryCoordinator failedCoord =
        ResourcePackDeliveryCoordinator.failed(new RuntimeException("test"));
    app.setResourcePackCoordinator(failedCoord);
    assertEquals(failedCoord, app.resourcePackCoordinator());

    EntityReconciliationReport report = new EntityReconciliationReport(1, 0, 0, 0, 0);
    app.setLastReconciliationReport(report);
    assertEquals(report, app.lastReconciliationReport());

    // Start & stop with execution engine and schedules
    app.start();
    app.startSchedules(List.of());
    app.stop();

    // Event readiness validation for non-existent event
    var diagnostics = app.validateEventReadiness("non_existent_event");
    assertEquals(1, diagnostics.size());
    assertEquals("SE-READY-DEF-001", diagnostics.getFirst().code());

    PhaseId phase = new PhaseId("waiting");
    EventDefinition sampleDef =
        new EventDefinition(
            new EventDefinitionId("meteor"),
            phase,
            Map.of(phase, new PhaseDefinition(phase, Set.of())));
    app.definitionRegistry().register(sampleDef, "events/meteor.yml");
    var meteorDiagnostics = app.validateEventReadiness("meteor");
    assertNotNull(meteorDiagnostics);
  }

  @Test
  void testWiringWithModelRendererAndOverloadedConstructors() {
    RecordingLifecycleReporter reporter = new RecordingLifecycleReporter();
    AtomicReference<Runnable> globalTask = new AtomicReference<>();
    EventTaskScheduler scheduler =
        new EventTaskScheduler() {
          @Override
          public void schedule(EventInstanceId eventId, Duration delay, Runnable task) {}

          @Override
          public void scheduleGlobal(Duration delay, Runnable task) {
            globalTask.set(task);
          }

          @Override
          public void cancelAll(EventInstanceId eventId) {}

          @Override
          public void cancelAll() {}
        };

    PlatformActionPort actionPort =
        (executionId, action, executionContext) -> CompletableFuture.completedFuture(null);
    InMemoryEventInstanceRepository repository = new InMemoryEventInstanceRepository();
    ModelRendererPort modelRenderer =
        (ModelRendererPort)
            Proxy.newProxyInstance(
                ModelRendererPort.class.getClassLoader(),
                new Class<?>[] {ModelRendererPort.class},
                (p, m, a) -> {
                  if (m.getReturnType().equals(boolean.class)) return true;
                  return null;
                });

    SpectraEventsApplication app =
        new SpectraEventsApplication(
            reporter, scheduler, actionPort, repository, null, null, modelRenderer);

    assertNotNull(app.modelRuntimeService());
    assertNotNull(app.animationRuntimeService());

    // Test overloaded constructors
    SpectraEventsApplication app2 =
        new SpectraEventsApplication(reporter, scheduler, actionPort, repository, null, null);
    assertNotNull(app2);

    SpectraEventsApplication app3 = new SpectraEventsApplication(reporter, scheduler, actionPort);
    assertNotNull(app3);

    // Test non-reward repository throws IllegalArgumentException
    io.github.kizio806.spectraevents.application.port.EventInstanceRepository nonRewardRepo =
        (io.github.kizio806.spectraevents.application.port.EventInstanceRepository)
            Proxy.newProxyInstance(
                io.github.kizio806.spectraevents.application.port.EventInstanceRepository.class
                    .getClassLoader(),
                new Class<?>[] {
                  io.github.kizio806.spectraevents.application.port.EventInstanceRepository.class
                },
                (p, m, a) -> null);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new SpectraEventsApplication(
                reporter, scheduler, actionPort, nonRewardRepo, null, null, null));

    // Test scheduleService logger diagnostic callback via startSchedules with matching cron
    PhaseId phase = new PhaseId("p1");
    EventDefinition sampleDef =
        new EventDefinition(
            new EventDefinitionId("meteor"),
            phase,
            Map.of(phase, new PhaseDefinition(phase, Set.of())));
    app.definitionRegistry().register(sampleDef, "events/meteor.yml");

    EventSchedule schedule =
        new EventSchedule(
            "test-sched",
            "meteor",
            CronExpression.parse("* * * * *"),
            ZoneId.of("UTC"),
            new io.github.kizio806.spectraevents.application.execution.EventLocation(
                "world", 0, 64, 0, 0, 0));

    app.startSchedules(List.of(schedule));
    if (globalTask.get() != null) {
      globalTask.get().run();
    }
    app.stop();
  }

  @Test
  void testStartSchedulesWithoutSchedulerThrows() {
    RecordingLifecycleReporter reporter = new RecordingLifecycleReporter();
    SpectraEventsApplication app = new SpectraEventsApplication(reporter);
    assertThrows(IllegalStateException.class, () -> app.startSchedules(List.of()));
  }

  private static final class RecordingLifecycleReporter implements LifecycleReporter {
    private final List<String> events = new ArrayList<>();

    @Override
    public void started() {
      events.add("started");
    }

    @Override
    public void stopped() {
      events.add("stopped");
    }
  }
}
