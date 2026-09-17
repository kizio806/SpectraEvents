package io.github.kizio806.spectraevents.platform.sponge.v26_2;

import com.google.inject.Inject;
import io.github.kizio806.spectraevents.adapter.storage.sqlite.SQLiteEventInstanceRepository;
import io.github.kizio806.spectraevents.application.SpectraEventsApplication;
import io.github.kizio806.spectraevents.platform.sponge.v26_2.action.SpongeActionAdapter;
import io.github.kizio806.spectraevents.platform.sponge.v26_2.lifecycle.SpongeEntityReconciler;
import io.github.kizio806.spectraevents.platform.sponge.v26_2.render.SpongeModelRenderer;
import java.nio.file.Path;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Server;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.api.event.lifecycle.StartingEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

@Plugin("spectraevents")
public class SpongeBootstrap {
  private final PluginContainer container;
  private final Logger logger;
  private final Path configDir;

  private SpectraEventsApplication application;

  @Inject
  public SpongeBootstrap(
      PluginContainer container, Logger logger, @ConfigDir(sharedRoot = false) Path configDir) {
    this.container = container;
    this.logger = logger;
    this.configDir = configDir;
  }

  @Listener
  public void onConstructPlugin(ConstructPluginEvent event) {
    logger.info("SpectraEvents Sponge constructing...");
  }

  @Listener
  public void onServerStarting(StartingEngineEvent<Server> event) {
    Path dbPath = configDir.resolve("events.db");
    configDir.toFile().mkdirs();
    SQLiteEventInstanceRepository sqliteRepository = new SQLiteEventInstanceRepository(dbPath);

    SpongeModelRenderer renderer = new SpongeModelRenderer(this);
    SpongeActionAdapter actionAdapter = new SpongeActionAdapter(this, renderer);
    SpongeEntityReconciler reconciler = new SpongeEntityReconciler(this, renderer);
    SpongeEventTaskScheduler scheduler = new SpongeEventTaskScheduler(this);

    application =
        new SpectraEventsApplication(
            new SpongeLifecycleReporter(this),
            scheduler,
            actionAdapter,
            sqliteRepository,
            reconciler,
            new SpongeCapabilityQuery());

    application.start();
  }

  @Listener
  public void onServerStopping(StoppingEngineEvent<Server> event) {
    if (application != null) {
      application.stop();
    }
  }

  public PluginContainer getContainer() {
    return container;
  }

  public Logger getLogger() {
    return logger;
  }
}
