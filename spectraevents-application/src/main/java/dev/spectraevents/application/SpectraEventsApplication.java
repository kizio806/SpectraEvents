package dev.spectraevents.application;

import dev.spectraevents.application.config.compiler.EventDefinitionCompiler;
import dev.spectraevents.application.config.loader.DefinitionLoader;
import dev.spectraevents.application.config.registry.EventDefinitionRegistry;
import dev.spectraevents.application.config.yaml.EventSpecYamlParser;
import dev.spectraevents.application.port.LifecycleReporter;
import dev.spectraevents.application.repository.InMemoryEventInstanceRepository;
import dev.spectraevents.application.service.EventOrchestrationService;
import java.util.Objects;

/** Coordinates the platform-neutral application lifecycle. */
public final class SpectraEventsApplication {
  private final LifecycleReporter lifecycleReporter;
  private final EventDefinitionRegistry definitionRegistry;
  private final DefinitionLoader definitionLoader;
  private final EventOrchestrationService orchestrationService;
  private final dev.spectraevents.application.execution.EventExecutionEngine executionEngine;
  private final dev.spectraevents.application.service.EntityReconciliationService
      reconciliationService;
  private dev.spectraevents.application.service.EntityReconciliationReport lastReconciliationReport;

  /**
   * Creates the application composition root with platform ports.
   *
   * @param lifecycleReporter platform-provided lifecycle reporter
   * @param scheduler platform event task scheduler
   * @param platformActionPort platform action execution port
   */
  public SpectraEventsApplication(
      LifecycleReporter lifecycleReporter,
      dev.spectraevents.application.port.EventTaskScheduler scheduler,
      dev.spectraevents.application.port.PlatformActionPort platformActionPort,
      dev.spectraevents.application.port.EventInstanceRepository repository,
      dev.spectraevents.application.port.PlatformEntityReconcilerPort reconcilerPort) {
    this.lifecycleReporter = Objects.requireNonNull(lifecycleReporter, "lifecycleReporter");
    this.definitionRegistry = new EventDefinitionRegistry();
    this.definitionLoader =
        new DefinitionLoader(
            new EventSpecYamlParser(), new EventDefinitionCompiler(), definitionRegistry);

    var targetRepository = repository != null ? repository : new InMemoryEventInstanceRepository();
    var stateStore = new dev.spectraevents.application.execution.EventRuntimeStateStore();

    if (scheduler != null && platformActionPort != null) {
      this.executionEngine =
          new dev.spectraevents.application.execution.EventExecutionEngine(
              targetRepository, definitionRegistry, scheduler, platformActionPort, stateStore);
    } else {
      this.executionEngine = null;
    }

    this.orchestrationService =
        new EventOrchestrationService(targetRepository, definitionRegistry, executionEngine);

    if (reconcilerPort != null) {
      this.reconciliationService =
          new dev.spectraevents.application.service.EntityReconciliationService(
              reconcilerPort, targetRepository, stateStore);
    } else {
      this.reconciliationService = null;
    }
  }

  public SpectraEventsApplication(
      LifecycleReporter lifecycleReporter,
      dev.spectraevents.application.port.EventTaskScheduler scheduler,
      dev.spectraevents.application.port.PlatformActionPort platformActionPort) {
    this(lifecycleReporter, scheduler, platformActionPort, null, null);
  }

  public SpectraEventsApplication(LifecycleReporter lifecycleReporter) {
    this(lifecycleReporter, null, null);
  }

  public EventDefinitionRegistry definitionRegistry() {
    return definitionRegistry;
  }

  public DefinitionLoader definitionLoader() {
    return definitionLoader;
  }

  public EventOrchestrationService orchestrationService() {
    return orchestrationService;
  }

  public dev.spectraevents.application.execution.EventExecutionEngine executionEngine() {
    return executionEngine;
  }

  public dev.spectraevents.application.service.EntityReconciliationService reconciliationService() {
    return reconciliationService;
  }

  public dev.spectraevents.application.service.EntityReconciliationReport
      lastReconciliationReport() {
    return lastReconciliationReport;
  }

  public void setLastReconciliationReport(
      dev.spectraevents.application.service.EntityReconciliationReport report) {
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
}
