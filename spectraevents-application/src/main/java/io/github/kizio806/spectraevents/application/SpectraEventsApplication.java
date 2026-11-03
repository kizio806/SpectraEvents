package io.github.kizio806.spectraevents.application;

import io.github.kizio806.spectraevents.application.asset.AssetPipelineService;
import io.github.kizio806.spectraevents.application.asset.AssetTargetProfile;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackDeliveryCoordinator;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackDeliverySettings;
import io.github.kizio806.spectraevents.application.config.compiler.EventDefinitionCompiler;
import io.github.kizio806.spectraevents.application.config.loader.DefinitionLoader;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.config.registry.RegisteredEventDefinition;
import io.github.kizio806.spectraevents.application.config.validation.ValidationDiagnostic;
import io.github.kizio806.spectraevents.application.config.yaml.EventSpecYamlParser;
import io.github.kizio806.spectraevents.application.execution.EventExecutionEngine;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeStateStore;
import io.github.kizio806.spectraevents.application.integration.IntegrationRegistry;
import io.github.kizio806.spectraevents.application.model.animation.registry.AnimationDefinitionRegistry;
import io.github.kizio806.spectraevents.application.model.animation.runtime.ActiveAnimationRegistry;
import io.github.kizio806.spectraevents.application.model.animation.runtime.AnimationRuntimeService;
import io.github.kizio806.spectraevents.application.model.compiler.ModelCompiler;
import io.github.kizio806.spectraevents.application.model.registry.ModelDefinitionRegistry;
import io.github.kizio806.spectraevents.application.model.runtime.ModelRuntimeService;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.application.port.LifecycleReporter;
import io.github.kizio806.spectraevents.application.port.ModelRendererPort;
import io.github.kizio806.spectraevents.application.port.PlatformActionPort;
import io.github.kizio806.spectraevents.application.port.PlatformCapabilityQuery;
import io.github.kizio806.spectraevents.application.port.PlatformEntityReconcilerPort;
import io.github.kizio806.spectraevents.application.port.RewardClaimRepository;
import io.github.kizio806.spectraevents.application.repository.InMemoryEventInstanceRepository;
import io.github.kizio806.spectraevents.application.schedule.EventSchedule;
import io.github.kizio806.spectraevents.application.schedule.EventScheduleService;
import io.github.kizio806.spectraevents.application.service.EntityReconciliationReport;
import io.github.kizio806.spectraevents.application.service.EntityReconciliationService;
import io.github.kizio806.spectraevents.application.service.EventOrchestrationService;
import io.github.kizio806.spectraevents.application.service.RewardMailboxService;
import io.github.kizio806.spectraevents.application.validation.OperationalReadinessValidator;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/** Coordinates the platform-neutral application lifecycle. */
public final class SpectraEventsApplication {
  private final LifecycleReporter lifecycleReporter;
  private final EventDefinitionRegistry definitionRegistry;
  private final DefinitionLoader definitionLoader;
  private final EventOrchestrationService orchestrationService;
  private final EventExecutionEngine executionEngine;
  private final EntityReconciliationService reconciliationService;
  private EntityReconciliationReport lastReconciliationReport;
  private final PlatformCapabilityQuery capabilityQuery;
  private final EventInstanceRepository repository;
  private final RewardMailboxService rewardMailboxService;
  private final EventScheduleService scheduleService;

  private final ModelDefinitionRegistry modelDefinitionRegistry;
  private final ModelCompiler modelCompiler;
  private final ModelRuntimeService modelRuntimeService;

  private final AnimationDefinitionRegistry animationDefinitionRegistry;
  private final ActiveAnimationRegistry activeAnimationRegistry;
  private final AnimationRuntimeService animationRuntimeService;
  private final IntegrationRegistry integrationRegistry = new IntegrationRegistry();
  private final OperationalReadinessValidator operationalReadinessValidator =
      new OperationalReadinessValidator();
  private ResourcePackDeliveryCoordinator resourcePackCoordinator;
  private ResourcePackDeliverySettings resourcePackSettings;
  private AssetTargetProfile assetTargetProfile;
  private AssetPipelineService assetPipelineService;

  /**
   * Creates the application composition root with platform ports.
   *
   * @param lifecycleReporter platform-provided lifecycle reporter
   * @param scheduler platform event task scheduler
   * @param platformActionPort platform action execution port
   * @param repository event instance repository
   * @param reconcilerPort platform entity reconciler port
   * @param capabilityQuery platform capability query port
   * @param modelRendererPort platform 3D model renderer port
   */
  public SpectraEventsApplication(
      LifecycleReporter lifecycleReporter,
      EventTaskScheduler scheduler,
      PlatformActionPort platformActionPort,
      EventInstanceRepository repository,
      PlatformEntityReconcilerPort reconcilerPort,
      PlatformCapabilityQuery capabilityQuery,
      ModelRendererPort modelRendererPort) {
    this.lifecycleReporter = Objects.requireNonNull(lifecycleReporter, "lifecycleReporter");
    this.definitionRegistry = new EventDefinitionRegistry();
    this.definitionLoader =
        new DefinitionLoader(
            new EventSpecYamlParser(), new EventDefinitionCompiler(), definitionRegistry);

    this.modelDefinitionRegistry = new ModelDefinitionRegistry();
    this.modelCompiler = new ModelCompiler();
    this.animationDefinitionRegistry = new AnimationDefinitionRegistry();
    this.activeAnimationRegistry = new ActiveAnimationRegistry();
    if (modelRendererPort != null) {
      this.modelRuntimeService =
          new ModelRuntimeService(modelDefinitionRegistry, modelRendererPort);
    } else {
      this.modelRuntimeService = null;
    }

    if (modelRendererPort != null && scheduler != null) {
      this.animationRuntimeService =
          new AnimationRuntimeService(
              animationDefinitionRegistry,
              modelDefinitionRegistry,
              activeAnimationRegistry,
              modelRendererPort,
              scheduler::scheduleGlobal);
    } else {
      this.animationRuntimeService = null;
    }

    var targetRepository = repository != null ? repository : new InMemoryEventInstanceRepository();
    this.repository = targetRepository;
    if (!(targetRepository instanceof RewardClaimRepository rewardClaimRepository)) {
      throw new IllegalArgumentException(
          "Event instance repository must also provide durable reward-claim storage");
    }
    this.rewardMailboxService = new RewardMailboxService(rewardClaimRepository);
    var stateStore = new EventRuntimeStateStore();

    if (scheduler != null && platformActionPort != null) {
      this.executionEngine =
          new EventExecutionEngine(
              targetRepository, definitionRegistry, scheduler, platformActionPort, stateStore);
    } else {
      this.executionEngine = null;
    }

    this.orchestrationService =
        new EventOrchestrationService(targetRepository, definitionRegistry, executionEngine);
    this.scheduleService =
        scheduler == null
            ? null
            : new EventScheduleService(
                scheduler,
                orchestrationService,
                Clock.systemUTC(),
                message ->
                    Logger.getLogger(SpectraEventsApplication.class.getName()).info(message));

    this.assetPipelineService = null; // Will be properly wired by Bootstrap later

    if (reconcilerPort != null) {
      this.reconciliationService =
          new EntityReconciliationService(reconcilerPort, targetRepository, stateStore);
    } else {
      this.reconciliationService = null;
    }

    this.capabilityQuery = capabilityQuery;
  }

  public SpectraEventsApplication(
      LifecycleReporter lifecycleReporter,
      EventTaskScheduler scheduler,
      PlatformActionPort platformActionPort,
      EventInstanceRepository repository,
      PlatformEntityReconcilerPort reconcilerPort,
      PlatformCapabilityQuery capabilityQuery) {
    this(
        lifecycleReporter,
        scheduler,
        platformActionPort,
        repository,
        reconcilerPort,
        capabilityQuery,
        null);
  }

  public SpectraEventsApplication(
      LifecycleReporter lifecycleReporter,
      EventTaskScheduler scheduler,
      PlatformActionPort platformActionPort) {
    this(lifecycleReporter, scheduler, platformActionPort, null, null, null, null);
  }

  public SpectraEventsApplication(LifecycleReporter lifecycleReporter) {
    this(lifecycleReporter, null, null, null, null, null, null);
  }

  public EventDefinitionRegistry definitionRegistry() {
    return definitionRegistry;
  }

  public DefinitionLoader definitionLoader() {
    return definitionLoader;
  }

  public ModelDefinitionRegistry modelDefinitionRegistry() {
    return modelDefinitionRegistry;
  }

  public ModelCompiler modelCompiler() {
    return modelCompiler;
  }

  public ModelRuntimeService modelRuntimeService() {
    return modelRuntimeService;
  }

  public AnimationDefinitionRegistry animationDefinitionRegistry() {
    return animationDefinitionRegistry;
  }

  public ActiveAnimationRegistry activeAnimationRegistry() {
    return activeAnimationRegistry;
  }

  public AnimationRuntimeService animationRuntimeService() {
    return animationRuntimeService;
  }

  public AssetPipelineService assetPipelineService() {
    return assetPipelineService;
  }

  public void setAssetPipelineService(AssetPipelineService assetPipelineService) {
    this.assetPipelineService = assetPipelineService;
  }

  public EventOrchestrationService orchestrationService() {
    return orchestrationService;
  }

  public RewardMailboxService rewardMailboxService() {
    return rewardMailboxService;
  }

  public EventExecutionEngine executionEngine() {
    return executionEngine;
  }

  public EntityReconciliationService reconciliationService() {
    return reconciliationService;
  }

  public EntityReconciliationReport lastReconciliationReport() {
    return lastReconciliationReport;
  }

  public void setLastReconciliationReport(EntityReconciliationReport report) {
    this.lastReconciliationReport = report;
  }

  /** Reports that the application has started. */
  public void start() {
    lifecycleReporter.started();
    if (executionEngine != null) {
      executionEngine.recoverTimers();
    }
  }

  /** Enables minute-based schedule dispatch after configuration has been loaded. */
  public void startSchedules(List<EventSchedule> schedules) {
    if (scheduleService == null) {
      throw new IllegalStateException(
          "Event scheduling is unavailable without a platform scheduler");
    }
    scheduleService.start(schedules);
  }

  public EventScheduleService scheduleService() {
    return scheduleService;
  }

  /** Reports that the application is stopping. */
  public void stop() {
    if (scheduleService != null) {
      scheduleService.stop();
    }
    if (executionEngine != null) {
      executionEngine.shutdown();
    }
    repository.close();
    lifecycleReporter.stopped();
  }

  public PlatformCapabilityQuery capabilityQuery() {
    return capabilityQuery;
  }

  public IntegrationRegistry integrationRegistry() {
    return integrationRegistry;
  }

  public OperationalReadinessValidator operationalReadinessValidator() {
    return operationalReadinessValidator;
  }

  public ResourcePackDeliveryCoordinator resourcePackCoordinator() {
    return resourcePackCoordinator;
  }

  public void setResourcePackCoordinator(ResourcePackDeliveryCoordinator coordinator) {
    this.resourcePackCoordinator = coordinator;
  }

  public ResourcePackDeliverySettings resourcePackSettings() {
    return resourcePackSettings;
  }

  public void setResourcePackSettings(ResourcePackDeliverySettings settings) {
    this.resourcePackSettings = settings;
  }

  public AssetTargetProfile assetTargetProfile() {
    return assetTargetProfile;
  }

  public void setAssetTargetProfile(AssetTargetProfile profile) {
    this.assetTargetProfile = profile;
  }

  public List<ValidationDiagnostic> validateEventReadiness(String eventId) {
    RegisteredEventDefinition registered = definitionRegistry.findById(eventId).orElse(null);
    if (registered == null) {
      return List.of(
          new ValidationDiagnostic(
              ValidationDiagnostic.Severity.ERROR,
              "SE-READY-DEF-001",
              eventId,
              "Unknown event definition '" + eventId + "'"));
    }
    return operationalReadinessValidator.validateDefinitionReadiness(
        registered.definition(),
        modelDefinitionRegistry,
        animationDefinitionRegistry,
        assetPipelineService,
        resourcePackSettings,
        resourcePackCoordinator,
        assetTargetProfile);
  }
}
