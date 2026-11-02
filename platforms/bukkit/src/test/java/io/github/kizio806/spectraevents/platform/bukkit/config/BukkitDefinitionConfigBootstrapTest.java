package io.github.kizio806.spectraevents.platform.bukkit.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kizio806.spectraevents.application.config.compiler.EventDefinitionCompiler;
import io.github.kizio806.spectraevents.application.config.loader.DefinitionLoader;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.config.validation.ValidationDiagnostic;
import io.github.kizio806.spectraevents.application.config.validation.ValidationDiagnostic.Severity;
import io.github.kizio806.spectraevents.application.config.yaml.EventSpecYamlParser;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.List;
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
    assertNotNull(bootstrap.eventsDirectory());
  }

  @Test
  void rejectsNullArgumentsInConstructor() {
    assertThrows(NullPointerException.class, () -> new BukkitDefinitionConfigBootstrap(null, null));
  }
}
