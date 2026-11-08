package io.github.kizio806.spectraevents.platform.spigot.command;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.SpectraEventsApplication;
import io.github.kizio806.spectraevents.application.config.compiler.EventDefinitionCompiler;
import io.github.kizio806.spectraevents.application.config.loader.DefinitionLoader;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.config.yaml.EventSpecYamlParser;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.application.port.LifecycleReporter;
import io.github.kizio806.spectraevents.application.port.PlatformActionPort;
import io.github.kizio806.spectraevents.application.port.PlatformCapability;
import io.github.kizio806.spectraevents.application.port.PlatformCapabilityQuery;
import io.github.kizio806.spectraevents.application.port.PlatformDescriptor;
import io.github.kizio806.spectraevents.application.port.PlatformEntityReconcilerPort;
import io.github.kizio806.spectraevents.application.repository.InMemoryEventInstanceRepository;
import io.github.kizio806.spectraevents.application.update.UpdatePort;
import io.github.kizio806.spectraevents.application.update.UpdateService;
import io.github.kizio806.spectraevents.platform.bukkit.config.BukkitDefinitionConfigBootstrap;
import io.github.kizio806.spectraevents.platform.bukkit.config.BukkitEventSettingsStore;
import io.github.kizio806.spectraevents.platform.bukkit.gui.AdminGuiController;
import io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup;
import io.github.kizio806.spectraevents.platform.spigot.action.SpigotActionAdapter;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpigotMainCommandTest {

  @BeforeAll
  static void beforeAll() {
    MockBukkitSetup.ensureInitialized();
  }

  @TempDir Path tempDir;

  private Plugin plugin;
  private SpectraEventsApplication application;
  private EventInstanceRepository repository;
  private BukkitDefinitionConfigBootstrap definitions;
  private SpigotActionAdapter actionAdapter;
  private BukkitEventSettingsStore settingsStore;
  private AdminGuiController guiController;
  private LocaleCatalog locales;
  private BukkitAudiences audiences;

  @AfterEach
  void tearDown() {
    if (audiences != null) {
      audiences.close();
    }
  }

  private SpigotMainCommand mainCommand;
  private Command dummyBukkitCommand;

  @BeforeEach
  void setUp() throws Exception {
    plugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (proxy, method, args) -> {
                  if ("getDataFolder".equals(method.getName())) {
                    return tempDir.toFile();
                  }
                  if ("getLogger".equals(method.getName())) {
                    return Logger.getLogger("MainCmdTest");
                  }
                  if ("getDescription".equals(method.getName())) {
                    return new PluginDescriptionFile("SpectraEvents", "1.0.0", "Main");
                  }
                  return null;
                });

    repository = new InMemoryEventInstanceRepository();

    EventTaskScheduler scheduler =
        (EventTaskScheduler)
            Proxy.newProxyInstance(
                EventTaskScheduler.class.getClassLoader(),
                new Class<?>[] {EventTaskScheduler.class},
                (proxy, method, args) -> null);

    PlatformActionPort actionPort =
        (PlatformActionPort)
            Proxy.newProxyInstance(
                PlatformActionPort.class.getClassLoader(),
                new Class<?>[] {PlatformActionPort.class},
                (proxy, method, args) -> CompletableFuture.completedFuture(true));

    PlatformCapabilityQuery capabilityQuery =
        new PlatformCapabilityQuery() {
          @Override
          public boolean hasCapability(PlatformCapability cap) {
            return false;
          }

          @Override
          public PlatformDescriptor platformDescriptor() {
            return new PlatformDescriptor("spigot", "MockServer", "26.1.0", "26.1.0-R0.1-SNAPSHOT");
          }
        };

    LifecycleReporter reporter =
        new LifecycleReporter() {
          @Override
          public void started() {}

          @Override
          public void stopped() {}
        };

    PlatformEntityReconcilerPort reconciler =
        (PlatformEntityReconcilerPort)
            Proxy.newProxyInstance(
                PlatformEntityReconcilerPort.class.getClassLoader(),
                new Class<?>[] {PlatformEntityReconcilerPort.class},
                (proxy, method, args) -> {
                  if ("scanLoadedEntities".equals(method.getName())) {
                    return Collections.emptyList();
                  }
                  return null;
                });

    application =
        new SpectraEventsApplication(
            reporter, scheduler, actionPort, repository, reconciler, capabilityQuery);

    DefinitionLoader loader =
        new DefinitionLoader(
            new EventSpecYamlParser(),
            new EventDefinitionCompiler(),
            application.definitionRegistry());

    definitions = new BukkitDefinitionConfigBootstrap(plugin, loader);
    definitions.ensureDefaultConfiguration();

    Map<String, String> localeMap = new HashMap<>();
    localeMap.put("command.spigot.help-general", "General Help");
    localeMap.put("command.spigot.help-events", "Events Help");
    localeMap.put("command.spigot.help-rewards", "Rewards Help");
    localeMap.put("command.spigot.help-definitions", "Definitions Help");
    localeMap.put("command.spigot.help-schedules", "Schedules Help");
    localeMap.put("command.spigot.status", "Status: %definitions% defs, %running% running");
    localeMap.put("command.spigot.version", "Version: %version%");
    localeMap.put("command.spigot.failed", "Failed: %reason%");
    localeMap.put("command.spigot.player-only", "Players only");
    localeMap.put("command.gui-player-only", "GUI player only");
    localeMap.put("command.location.usage", "Location usage");
    localeMap.put("command.location.list-title", "Locations (%count%):");
    localeMap.put("command.models.title", "Models (%count%):");
    localeMap.put("command.spigot.unsupported", "Unsupported: %feature% on %platform%");
    locales = LocaleCatalog.of(localeMap);

    this.audiences =
        (BukkitAudiences)
            Proxy.newProxyInstance(
                BukkitAudiences.class.getClassLoader(),
                new Class<?>[] {BukkitAudiences.class},
                (proxy, method, args) -> null);

    UpdatePort dummyUpdatePort =
        (UpdatePort)
            Proxy.newProxyInstance(
                UpdatePort.class.getClassLoader(),
                new Class<?>[] {UpdatePort.class},
                (proxy, method, args) -> CompletableFuture.completedFuture(null));
    UpdateService updateService = new UpdateService("1.0.0", dummyUpdatePort);

    actionAdapter = new SpigotActionAdapter(plugin, audiences, locales);
    settingsStore = new BukkitEventSettingsStore(tempDir.resolve("settings.yml"));
    guiController =
        new AdminGuiController(
            application.definitionRegistry(),
            repository,
            application.integrationRegistry(),
            updateService,
            application.orchestrationService(),
            settingsStore,
            locales);

    mainCommand =
        new SpigotMainCommand(
            plugin,
            application,
            repository,
            definitions,
            actionAdapter,
            settingsStore,
            guiController,
            updateService,
            locales);

    dummyBukkitCommand =
        new Command("spectraevents") {
          @Override
          public boolean execute(CommandSender sender, String commandLabel, String[] args) {
            return false;
          }
        };
  }

  @Test
  void shouldDispatchHelpAndDiagnostics() {
    ConsoleCommandSender console = org.bukkit.Bukkit.getConsoleSender();

    // No args -> help
    assertTrue(mainCommand.onCommand(console, dummyBukkitCommand, "spectraevents", new String[0]));

    // "help" -> help
    assertTrue(
        mainCommand.onCommand(console, dummyBukkitCommand, "spectraevents", new String[] {"help"}));

    // "version"
    assertTrue(
        mainCommand.onCommand(
            console, dummyBukkitCommand, "spectraevents", new String[] {"version"}));

    // "status"
    assertTrue(
        mainCommand.onCommand(
            console, dummyBukkitCommand, "spectraevents", new String[] {"status"}));

    // "doctor"
    assertTrue(
        mainCommand.onCommand(
            console, dummyBukkitCommand, "spectraevents", new String[] {"doctor"}));

    // "validate"
    assertTrue(
        mainCommand.onCommand(
            console, dummyBukkitCommand, "spectraevents", new String[] {"validate"}));
  }

  @Test
  void shouldHandleAdminCommand() {
    ConsoleCommandSender console = org.bukkit.Bukkit.getConsoleSender();
    // Admin from console should send player-only message
    assertTrue(
        mainCommand.onCommand(
            console, dummyBukkitCommand, "spectraevents", new String[] {"admin"}));

    Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "AdminUser");
    // Admin from player opens menu
    assertTrue(
        mainCommand.onCommand(player, dummyBukkitCommand, "spectraevents", new String[] {"admin"}));
  }

  @Test
  void shouldDispatchSubcommands() {
    ConsoleCommandSender console = org.bukkit.Bukkit.getConsoleSender();

    // Event list
    assertTrue(
        mainCommand.onCommand(
            console, dummyBukkitCommand, "spectraevents", new String[] {"event", "list"}));

    // Location list
    assertTrue(
        mainCommand.onCommand(
            console, dummyBukkitCommand, "spectraevents", new String[] {"location", "list"}));

    // Model list
    assertTrue(
        mainCommand.onCommand(
            console, dummyBukkitCommand, "spectraevents", new String[] {"model"}));

    // Non-existent subcommand returns false
    assertFalse(
        mainCommand.onCommand(
            console, dummyBukkitCommand, "spectraevents", new String[] {"non_existent_sub"}));
  }

  @Test
  void testConstructorWithAssetPipelineAndMinimalFeatures() throws Exception {
    Path sourceDir = tempDir.resolve("sources");
    Path outputDir = tempDir.resolve("output");
    java.nio.file.Files.createDirectories(sourceDir);
    java.nio.file.Files.createDirectories(outputDir);

    io.github.kizio806.spectraevents.application.asset.AssetPipelineService assetPipeline =
        new io.github.kizio806.spectraevents.application.asset.AssetPipelineService(
            path -> null,
            new io.github.kizio806.spectraevents.application.asset.ResourcePackBuilder(outputDir),
            sourceDir,
            io.github.kizio806.spectraevents.application.asset.AssetTargetProfile.PROFILE_26_1);

    application.setAssetPipelineService(assetPipeline);

    SpigotMainCommand fullCmd =
        new SpigotMainCommand(
            plugin,
            application,
            repository,
            definitions,
            actionAdapter,
            settingsStore,
            guiController,
            new UpdateService(
                "1.0.0",
                new io.github.kizio806.spectraevents.application.update.UpdatePort() {
                  @Override
                  public CompletableFuture<
                          io.github.kizio806.spectraevents.application.update.UpdateInfo>
                      checkUpdate(String v, String c) {
                    return CompletableFuture.completedFuture(null);
                  }

                  @Override
                  public CompletableFuture<Boolean> downloadUpdate(String u, String t) {
                    return CompletableFuture.completedFuture(false);
                  }
                }),
            locales);

    ConsoleCommandSender console = org.bukkit.Bukkit.getConsoleSender();
    // Test asset and assets subcommands
    assertTrue(
        fullCmd.onCommand(console, dummyBukkitCommand, "spectraevents", new String[] {"asset"}));
    assertTrue(
        fullCmd.onCommand(console, dummyBukkitCommand, "spectraevents", new String[] {"assets"}));

    // Minimal application with all optional features null
    SpectraEventsApplication minimalApp =
        new SpectraEventsApplication(
            new LifecycleReporter() {
              @Override
              public void started() {}

              @Override
              public void stopped() {}
            },
            null,
            null,
            repository,
            null,
            null,
            null);

    SpigotMainCommand minimalCmd =
        new SpigotMainCommand(
            plugin, minimalApp, repository, definitions, actionAdapter, null, null, null, locales);

    assertNotNull(minimalCmd);
  }

  @Test
  void testSecondaryConstructorAndNullGuiController() {
    SpigotMainCommand secondary =
        new SpigotMainCommand(plugin, application, repository, definitions, actionAdapter, locales);
    ConsoleCommandSender console = org.bukkit.Bukkit.getConsoleSender();

    // Admin with guiController == null sends unsupported
    assertTrue(
        secondary.onCommand(console, dummyBukkitCommand, "spectraevents", new String[] {"admin"}));
  }

  @Test
  void testExceptionCatchBlocks() {
    ConsoleCommandSender console = org.bukkit.Bukkit.getConsoleSender();

    // IllegalArgumentException catch block (e.g. unknown sub to event)
    assertFalse(
        mainCommand.onCommand(
            console, dummyBukkitCommand, "spectraevents", new String[] {"event", "unknown_sub"}));

    // RuntimeException catch block (e.g. sender lacks permission throws IllegalStateException)
    CommandSender unprivilegedSender =
        (CommandSender)
            Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(),
                new Class<?>[] {CommandSender.class},
                (proxy, method, args) -> {
                  if ("hasPermission".equals(method.getName())) {
                    return false;
                  }
                  if ("isOp".equals(method.getName())) {
                    return false;
                  }
                  return null;
                });

    assertFalse(
        mainCommand.onCommand(
            unprivilegedSender, dummyBukkitCommand, "spectraevents", new String[] {"status"}));
  }

  @Test
  void testTabCompleteEdgeCases() {
    ConsoleCommandSender console = org.bukkit.Bukkit.getConsoleSender();

    // Unknown subcommand tab complete (arguments.length >= 2)
    List<String> unknown =
        mainCommand.onTabComplete(
            console, dummyBukkitCommand, "spectraevents", new String[] {"unknown_sub", "arg"});
    assertTrue(unknown.isEmpty());

    // Empty arguments
    List<String> empty =
        mainCommand.onTabComplete(console, dummyBukkitCommand, "spectraevents", new String[0]);
    assertTrue(empty.isEmpty());
  }

  @Test
  void shouldTabCompleteSubcommands() {
    ConsoleCommandSender console = org.bukkit.Bukkit.getConsoleSender();

    List<String> rootCompletions =
        mainCommand.onTabComplete(console, dummyBukkitCommand, "spectraevents", new String[] {""});
    assertNotNull(rootCompletions);
    assertTrue(rootCompletions.contains("help"));
    assertTrue(rootCompletions.contains("event"));
    assertTrue(rootCompletions.contains("location"));
    assertTrue(rootCompletions.contains("model"));

    List<String> eventCompletions =
        mainCommand.onTabComplete(
            console, dummyBukkitCommand, "spectraevents", new String[] {"event", ""});
    assertNotNull(eventCompletions);
  }
}
