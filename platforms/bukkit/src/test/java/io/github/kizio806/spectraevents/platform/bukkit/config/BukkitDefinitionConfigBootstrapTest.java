package io.github.kizio806.spectraevents.platform.bukkit.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.config.compiler.EventDefinitionCompiler;
import io.github.kizio806.spectraevents.application.config.loader.DefinitionLoadResult;
import io.github.kizio806.spectraevents.application.config.loader.DefinitionLoader;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.config.validation.ValidationDiagnostic;
import io.github.kizio806.spectraevents.application.config.validation.ValidationDiagnostic.Severity;
import io.github.kizio806.spectraevents.application.config.yaml.EventSpecYamlParser;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BukkitDefinitionConfigBootstrapTest {

  @TempDir Path tempDir;

  @Test
  void formatsDiagnosticsCorrectly() {
    Plugin dummyPlugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (proxy, method, args) -> {
                  if ("getDataFolder".equals(method.getName())) {
                    return tempDir.toFile();
                  }
                  return null;
                });

    DefinitionLoader loader =
        new DefinitionLoader(
            new EventSpecYamlParser(),
            new EventDefinitionCompiler(),
            new EventDefinitionRegistry());

    BukkitDefinitionConfigBootstrap bootstrap =
        new BukkitDefinitionConfigBootstrap(dummyPlugin, loader);

    List<ValidationDiagnostic> diagnostics =
        List.of(
            new ValidationDiagnostic(Severity.ERROR, "E100", "spec.name", "Name is missing"),
            new ValidationDiagnostic(Severity.ERROR, "E200", "phases.0", "Invalid duration"));

    String formatted = bootstrap.formatDiagnostics(diagnostics);
    assertEquals("E100 @ spec.name: Name is missing; E200 @ phases.0: Invalid duration", formatted);
    assertEquals("unknown error", bootstrap.formatDiagnostics(List.of()));
    assertNotNull(bootstrap.eventsDirectory());
  }

  @Test
  void logsLoadedDefinitionsAndFailures() {
    Plugin dummyPlugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (proxy, method, args) -> {
                  if ("getDataFolder".equals(method.getName())) {
                    return tempDir.toFile();
                  }
                  if ("getLogger".equals(method.getName())) {
                    return Logger.getLogger("TestPlugin");
                  }
                  return null;
                });

    DefinitionLoader loader =
        new DefinitionLoader(
            new EventSpecYamlParser(),
            new EventDefinitionCompiler(),
            new EventDefinitionRegistry());

    BukkitDefinitionConfigBootstrap bootstrap =
        new BukkitDefinitionConfigBootstrap(dummyPlugin, loader);

    io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId defId =
        new io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId(
            "sample_event");
    io.github.kizio806.spectraevents.core.event.phase.PhaseId phaseId =
        new io.github.kizio806.spectraevents.core.event.phase.PhaseId("start");
    io.github.kizio806.spectraevents.core.event.phase.PhaseDefinition phase =
        new io.github.kizio806.spectraevents.core.event.phase.PhaseDefinition(
            phaseId, java.util.Set.of());
    io.github.kizio806.spectraevents.core.event.definition.EventDefinition def =
        new io.github.kizio806.spectraevents.core.event.definition.EventDefinition(
            defId, phaseId, java.util.Map.of(phaseId, phase));
    io.github.kizio806.spectraevents.application.config.registry.RegisteredEventDefinition regDef =
        new io.github.kizio806.spectraevents.application.config.registry.RegisteredEventDefinition(
            def, "sample.yml");
    DefinitionLoadResult.SourceFileFailure failure =
        new DefinitionLoadResult.SourceFileFailure(
            "bad.yml",
            List.of(new ValidationDiagnostic(Severity.ERROR, "E001", "spec.name", "missing name")));
    DefinitionLoadResult resultWithItems =
        new DefinitionLoadResult(List.of(regDef), List.of(failure));

    bootstrap.logLoadResult(resultWithItems);
  }

  @Test
  void shouldDelegateFileSystemOperations() throws Exception {
    Plugin dummyPlugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (proxy, method, args) -> {
                  if ("getDataFolder".equals(method.getName())) {
                    return tempDir.toFile();
                  }
                  if ("getLogger".equals(method.getName())) {
                    return Logger.getLogger("TestPlugin");
                  }
                  return null;
                });

    EventDefinitionRegistry registry = new EventDefinitionRegistry();
    DefinitionLoader loader =
        new DefinitionLoader(new EventSpecYamlParser(), new EventDefinitionCompiler(), registry);

    BukkitDefinitionConfigBootstrap bootstrap =
        new BukkitDefinitionConfigBootstrap(dummyPlugin, loader);

    assertNotNull(bootstrap.fileSystemLoader());
    bootstrap.ensureDefaultConfiguration();
    assertTrue(Files.exists(bootstrap.eventsDirectory()));

    DefinitionLoadResult loadResult = bootstrap.loadFromDisk();
    assertNotNull(loadResult);
    bootstrap.logLoadResult(loadResult);

    DefinitionLoadResult reloadResult = bootstrap.reloadFromDisk();
    assertNotNull(reloadResult);

    DefinitionLoadResult validateResult = bootstrap.validateFromDisk();
    assertNotNull(validateResult);
  }

  @Test
  void rejectsNullArgumentsInConstructor() {
    DefinitionLoader loader =
        new DefinitionLoader(
            new EventSpecYamlParser(),
            new EventDefinitionCompiler(),
            new EventDefinitionRegistry());
    assertThrows(NullPointerException.class, () -> new BukkitDefinitionConfigBootstrap(null, null));
    assertThrows(
        NullPointerException.class, () -> new BukkitDefinitionConfigBootstrap(null, loader));
  }
}
