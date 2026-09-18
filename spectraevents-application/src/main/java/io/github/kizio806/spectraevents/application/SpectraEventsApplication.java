package io.github.kizio806.spectraevents.application;

import io.github.kizio806.spectraevents.application.config.compiler.EventDefinitionCompiler;
import io.github.kizio806.spectraevents.application.config.loader.DefinitionLoader;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.config.yaml.EventSpecYamlParser;
import io.github.kizio806.spectraevents.application.model.animation.registry.AnimationDefinitionRegistry;
import io.github.kizio806.spectraevents.application.model.animation.runtime.ActiveAnimationRegistry;
import io.github.kizio806.spectraevents.application.model.animation.runtime.AnimationRuntimeService;
import io.github.kizio806.spectraevents.application.model.compiler.ModelCompiler;
import io.github.kizio806.spectraevents.application.model.loader.ModelLoader;
import io.github.kizio806.spectraevents.application.model.registry.ModelDefinitionRegistry;
import io.github.kizio806.spectraevents.application.model.runtime.ModelRuntimeService;
import io.github.kizio806.spectraevents.application.port.LifecycleReporter;
import io.github.kizio806.spectraevents.application.port.ModelRendererPort;
import io.github.kizio806.spectraevents.application.port.PlatformCapabilityQuery;
import io.github.kizio806.spectraevents.application.repository.InMemoryEventInstanceRepository;
import io.github.kizio806.spectraevents.application.service.EventOrchestrationService;
import java.util.Objects;

/** Coordinates the platform-neutral application lifecycle. */
public final class SpectraEventsApplication {
  private final LifecycleReporter lifecycleReporter;
  private final EventDefinitionRegistry definitionRegistry;
  private final DefinitionLoader definitionLoader;
  private final EventOrchestrationService orchestrationService;
  private final io.github.kizio806.spectraevents.application.execution.EventExecutionEngine
      executionEngine;
  private final io.github.kizio806.spectraevents.application.service.EntityReconciliationService
      reconciliationService;
  private io.github.kizio806.spectraevents.application.service.EntityReconciliationReport
      lastReconciliationReport;
  private final PlatformCapabilityQuery capabilityQuery;

  private final ModelDefinitionRegistry modelDefinitionRegistry;
  private final ModelCompiler modelCompiler;
  private final ModelLoader modelLoader;
  private final ModelRuntimeService modelRuntimeService;

  private final AnimationDefinitionRegistry animationDefinitionRegistry;
  private final ActiveAnimationRegistry activeAnimationRegistry;
  private final AnimationRuntimeService animationRuntimeService;
  private io.github.kizio806.spectraevents.application.asset.AssetPipelineService
      assetPipelineService;

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
      io.github.kizio806.spectraevents.application.port.EventTaskScheduler scheduler,
      io.github.kizio806.spectraevents.application.port.PlatformActionPort platformActionPort,
      io.github.kizio806.spectraevents.application.port.EventInstanceRepository repository,
      io.github.kizio806.spectraevents.application.port.PlatformEntityReconcilerPort reconcilerPort,
      PlatformCapabilityQuery capabilityQuery,
      ModelRendererPort modelRendererPort) {
    this.lifecycleReporter = Objects.requireNonNull(lifecycleReporter, "lifecycleReporter");
    this.definitionRegistry = new EventDefinitionRegistry();
    this.definitionLoader =
        new DefinitionLoader(
            new EventSpecYamlParser(), new EventDefinitionCompiler(), definitionRegistry);

    this.modelDefinitionRegistry = new ModelDefinitionRegistry();
    this.modelCompiler = new ModelCompiler();
    this.modelLoader = new ModelLoader(modelCompiler, modelDefinitionRegistry);

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
              activeAnimationRegistry,
              modelRendererPort,
              (delay, task) -> scheduler.schedule(null, delay, task));
    } else {
      this.animationRuntimeService = null;
    }

    var targetRepository = repository != null ? repository : new InMemoryEventInstanceRepository();
    var stateStore =
        new io.github.kizio806.spectraevents.application.execution.EventRuntimeStateStore();

    if (scheduler != null && platformActionPort != null) {
      this.executionEngine =
          new io.github.kizio806.spectraevents.application.execution.EventExecutionEngine(
              targetRepository, definitionRegistry, scheduler, platformActionPort, stateStore);
    } else {
      this.executionEngine = null;
    }

    this.orchestrationService =
        new EventOrchestrationService(targetRepository, definitionRegistry, executionEngine);

    this.assetPipelineService = null; // Will be properly wired by Bootstrap later

    if (reconcilerPort != null) {
      this.reconciliationService =
          new io.github.kizio806.spectraevents.application.service.EntityReconciliationService(
              reconcilerPort, targetRepository, stateStore);
    } else {
      this.reconciliationService = null;
    }

    this.capabilityQuery = capabilityQuery;
  }

  public SpectraEventsApplication(
      LifecycleReporter lifecycleReporter,
      io.github.kizio806.spectraevents.application.port.EventTaskScheduler scheduler,
      io.github.kizio806.spectraevents.application.port.PlatformActionPort platformActionPort,
      io.github.kizio806.spectraevents.application.port.EventInstanceRepository repository,
      io.github.kizio806.spectraevents.application.port.PlatformEntityReconcilerPort reconcilerPort,
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
      io.github.kizio806.spectraevents.application.port.EventTaskScheduler scheduler,
      io.github.kizio806.spectraevents.application.port.PlatformActionPort platformActionPort) {
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

  public ModelLoader modelLoader() {
    return modelLoader;
  }

  public ModelRuntimeService modelRuntimeService() {
    return modelRuntimeService;
  }

  public io.github.kizio806.spectraevents.application.model.animation.registry
          .AnimationDefinitionRegistry
      animationDefinitionRegistry() {
    return animationDefinitionRegistry;
  }

  public io.github.kizio806.spectraevents.application.model.animation.runtime
          .ActiveAnimationRegistry
      activeAnimationRegistry() {
    return activeAnimationRegistry;
  }

  public io.github.kizio806.spectraevents.application.model.animation.runtime
          .AnimationRuntimeService
      animationRuntimeService() {
    return animationRuntimeService;
  }

  public io.github.kizio806.spectraevents.application.asset.AssetPipelineService
      assetPipelineService() {
    return assetPipelineService;
  }

  public void setAssetPipelineService(
      io.github.kizio806.spectraevents.application.asset.AssetPipelineService
          assetPipelineService) {
    this.assetPipelineService = assetPipelineService;
  }

  public EventOrchestrationService orchestrationService() {
    return orchestrationService;
  }

  public io.github.kizio806.spectraevents.application.execution.EventExecutionEngine
      executionEngine() {
    return executionEngine;
  }

  public io.github.kizio806.spectraevents.application.service.EntityReconciliationService
      reconciliationService() {
    return reconciliationService;
  }

  public io.github.kizio806.spectraevents.application.service.EntityReconciliationReport
      lastReconciliationReport() {
    return lastReconciliationReport;
  }

  public void setLastReconciliationReport(
      io.github.kizio806.spectraevents.application.service.EntityReconciliationReport report) {
    this.lastReconciliationReport = report;
  }

  /** Reports that the application has started. */
  public void start() {
    lifecycleReporter.started();
    if (executionEngine != null) {
      executionEngine.recoverTimers();
    }
  }

  /** Reports that the application is stopping. */
  public void stop() {
    lifecycleReporter.stopped();
  }

  public PlatformCapabilityQuery capabilityQuery() {
    return capabilityQuery;
  }
}
