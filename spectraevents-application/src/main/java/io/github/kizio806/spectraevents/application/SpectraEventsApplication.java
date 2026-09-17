package io.github.kizio806.spectraevents.application;

import io.github.kizio806.spectraevents.application.config.compiler.EventDefinitionCompiler;
import io.github.kizio806.spectraevents.application.config.loader.DefinitionLoader;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.config.yaml.EventSpecYamlParser;
import io.github.kizio806.spectraevents.application.port.LifecycleReporter;
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

  /**
   * Creates the application composition root with platform ports.
   *
   * @param lifecycleReporter platform-provided lifecycle reporter
   * @param scheduler platform event task scheduler
   * @param platformActionPort platform action execution port
   */
  public SpectraEventsApplication(
      LifecycleReporter lifecycleReporter,
      io.github.kizio806.spectraevents.application.port.EventTaskScheduler scheduler,
      io.github.kizio806.spectraevents.application.port.PlatformActionPort platformActionPort,
      io.github.kizio806.spectraevents.application.port.EventInstanceRepository repository,
      io.github.kizio806.spectraevents.application.port.PlatformEntityReconcilerPort
          reconcilerPort) {
    this.lifecycleReporter = Objects.requireNonNull(lifecycleReporter, "lifecycleReporter");
    this.definitionRegistry = new EventDefinitionRegistry();
    this.definitionLoader =
        new DefinitionLoader(
            new EventSpecYamlParser(), new EventDefinitionCompiler(), definitionRegistry);

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

    if (reconcilerPort != null) {
      this.reconciliationService =
          new io.github.kizio806.spectraevents.application.service.EntityReconciliationService(
              reconcilerPort, targetRepository, stateStore);
    } else {
      this.reconciliationService = null;
    }
  }

  public SpectraEventsApplication(
      LifecycleReporter lifecycleReporter,
      io.github.kizio806.spectraevents.application.port.EventTaskScheduler scheduler,
      io.github.kizio806.spectraevents.application.port.PlatformActionPort platformActionPort) {
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
}
