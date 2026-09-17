package io.github.kizio806.spectraevents.platform.paper.integration;

import io.github.kizio806.spectraevents.application.execution.EventRuntimeStateStore;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import java.util.List;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class PlaceholderAPIIntegration {

  public PlaceholderAPIIntegration(
      EventInstanceRepository repository, EventRuntimeStateStore stateStore) {
    try {
      if (org.bukkit.Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
        new SpectraExpansion(repository, stateStore).register();
      }
    } catch (NoClassDefFoundError | Exception ignored) {
    }
  }

  private static class SpectraExpansion extends PlaceholderExpansion {

    private final EventInstanceRepository repository;
    private final EventRuntimeStateStore stateStore;

    public SpectraExpansion(EventInstanceRepository repository, EventRuntimeStateStore stateStore) {
      this.repository = repository;
      this.stateStore = stateStore;
    }

    @Override
    public @NotNull String getIdentifier() {
      return "spectra";
    }

    @Override
    public @NotNull String getAuthor() {
      return "SpectraTeam";
    }

    @Override
    public @NotNull String getVersion() {
      return "1.0.0";
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
      if (params.equalsIgnoreCase("active_count")) {
        List<EventInstance> active =
            repository.findAll().stream()
                .filter(
                    i ->
                        i.state()
                            == io.github.kizio806.spectraevents.core.event.runtime
                                .EventLifecycleState.RUNNING)
                .toList();
        return String.valueOf(active.size());
      }
      return null;
    }
  }
}
