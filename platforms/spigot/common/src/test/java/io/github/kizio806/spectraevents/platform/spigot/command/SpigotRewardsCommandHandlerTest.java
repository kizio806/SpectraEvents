package io.github.kizio806.spectraevents.platform.spigot.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.SpectraEventsApplication;
import io.github.kizio806.spectraevents.application.config.locale.FileSystemLocaleLoader;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.application.port.LifecycleReporter;
import io.github.kizio806.spectraevents.application.port.ModelRendererPort;
import io.github.kizio806.spectraevents.application.port.PlatformActionPort;
import io.github.kizio806.spectraevents.application.port.PlatformCapabilityQuery;
import io.github.kizio806.spectraevents.application.port.PlatformEntityReconcilerPort;
import io.github.kizio806.spectraevents.application.port.RewardClaimRepository;
import io.github.kizio806.spectraevents.application.repository.InMemoryEventInstanceRepository;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.gameplay.reward.RewardClaim;
import io.github.kizio806.spectraevents.core.gameplay.reward.RewardClaimStatus;
import io.github.kizio806.spectraevents.core.gameplay.reward.RewardItem;
import io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup;
import io.github.kizio806.spectraevents.platform.spigot.action.SpigotActionAdapter;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpigotRewardsCommandHandlerTest {

  @BeforeAll
  static void beforeAll() {
    MockBukkitSetup.ensureInitialized();
  }

  @TempDir Path tempDir;
  private SpigotCommandHelper helper;
  private Plugin dummyPlugin;
  private BukkitAudiences dummyAudiences;
  private SpectraEventsApplication application;
  private SpigotActionAdapter actionAdapter;
  private DelegatingRepo repository;
  private List<String> playerMessages;
  private Player player;

  @AfterEach
  void tearDown() {
    if (dummyAudiences != null) {
      dummyAudiences.close();
    }
  }

  @BeforeEach
  void setUp() throws IOException {
    LocaleCatalog locales = new FileSystemLocaleLoader(tempDir).loadCatalog("en-US");
    helper = new SpigotCommandHelper(locales);

    dummyPlugin =
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

    dummyAudiences =
        (BukkitAudiences)
            Proxy.newProxyInstance(
                BukkitAudiences.class.getClassLoader(),
                new Class<?>[] {BukkitAudiences.class},
                (proxy, method, args) -> null);

    actionAdapter = new SpigotActionAdapter(dummyPlugin, dummyAudiences, locales);

    LifecycleReporter reporter =
        (LifecycleReporter)
            Proxy.newProxyInstance(
                LifecycleReporter.class.getClassLoader(),
                new Class<?>[] {LifecycleReporter.class},
                (proxy, method, args) -> null);

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
                (proxy, method, args) -> null);

    PlatformEntityReconcilerPort reconciler =
        (PlatformEntityReconcilerPort)
            Proxy.newProxyInstance(
                PlatformEntityReconcilerPort.class.getClassLoader(),
                new Class<?>[] {PlatformEntityReconcilerPort.class},
                (proxy, method, args) -> null);

    PlatformCapabilityQuery capabilityQuery =
        (PlatformCapabilityQuery)
            Proxy.newProxyInstance(
                PlatformCapabilityQuery.class.getClassLoader(),
                new Class<?>[] {PlatformCapabilityQuery.class},
                (proxy, method, args) -> null);

    ModelRendererPort renderer =
        (ModelRendererPort)
            Proxy.newProxyInstance(
                ModelRendererPort.class.getClassLoader(),
                new Class<?>[] {ModelRendererPort.class},
                (proxy, method, args) -> null);

    repository = new DelegatingRepo();

    application =
        new SpectraEventsApplication(
            reporter, scheduler, actionPort, repository, reconciler, capabilityQuery, renderer);

    playerMessages = new ArrayList<>();
    Player basePlayer = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "TestPlayer");
    player =
        (Player)
            Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[] {Player.class},
                (proxy, method, args) -> {
                  if ("sendMessage".equals(method.getName()) && args != null && args.length > 0) {
                    playerMessages.add(String.valueOf(args[0]));
                    return null;
                  }
                  if ("hasPermission".equals(method.getName())) {
                    return true;
                  }
                  if ("isOp".equals(method.getName())) {
                    return true;
                  }
                  return method.invoke(basePlayer, args);
                });
  }

  @Test
  void rejectsNullConstructorParameters() {
    assertThrows(
        NullPointerException.class,
        () -> new SpigotRewardsCommandHandler(null, null, null, helper));
  }

  @Test
  void tabCompletesSubcommandsForPermittedSender() {
    SpigotRewardsCommandHandler handler =
        new SpigotRewardsCommandHandler(dummyPlugin, application, actionAdapter, helper);

    List<String> completions = handler.tabComplete(player, new String[] {""});
    assertEquals(List.of("list", "claim"), completions);

    List<String> nullSender = handler.tabComplete(null, new String[] {""});
    assertTrue(nullSender.isEmpty());

    CommandSender noPermSender =
        (CommandSender)
            Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(),
                new Class<?>[] {CommandSender.class},
                (proxy, method, args) -> {
                  if ("hasPermission".equals(method.getName())) {
                    return false;
                  }
                  return null;
                });
    assertTrue(handler.tabComplete(noPermSender, new String[] {""}).isEmpty());
    assertTrue(handler.tabComplete(player, new String[] {"list", "extra"}).isEmpty());
  }

  @Test
  void testExecuteWithNonPlayerSender() {
    CommandSender sender =
        (CommandSender)
            Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(),
                new Class<?>[] {CommandSender.class},
                (proxy, method, args) -> {
                  if ("hasPermission".equals(method.getName())) {
                    return true;
                  }
                  return null;
                });

    SpigotRewardsCommandHandler handler =
        new SpigotRewardsCommandHandler(dummyPlugin, application, actionAdapter, helper);

    assertThrows(IllegalStateException.class, () -> handler.execute(sender, new String[] {"list"}));
  }

  @Test
  void testExecuteUnknownSubcommand() {
    SpigotRewardsCommandHandler handler =
        new SpigotRewardsCommandHandler(dummyPlugin, application, actionAdapter, helper);

    assertThrows(
        IllegalArgumentException.class,
        () -> handler.execute(player, new String[] {"unknown_sub"}));
  }

  @Test
  void testExecuteClaimMissingArgument() {
    SpigotRewardsCommandHandler handler =
        new SpigotRewardsCommandHandler(dummyPlugin, application, actionAdapter, helper);

    assertThrows(
        IllegalArgumentException.class, () -> handler.execute(player, new String[] {"claim"}));
  }

  @Test
  void testExecuteListEmptyAndNonEmpty() {
    SpigotRewardsCommandHandler handler =
        new SpigotRewardsCommandHandler(dummyPlugin, application, actionAdapter, helper);

    handler.execute(player, new String[] {});
    assertFalse(playerMessages.isEmpty());

    playerMessages.clear();
    UUID claimId = UUID.randomUUID();
    RewardClaim claim =
        new RewardClaim(
            claimId,
            EventInstanceId.generate(),
            player.getUniqueId(),
            List.of(new RewardItem("minecraft:diamond", 1)),
            RewardClaimStatus.PENDING,
            Instant.now(),
            null);
    repository.savePendingDurably(claim);

    handler.execute(player, new String[] {"list"});
    assertFalse(playerMessages.isEmpty());

    // Failure simulation
    playerMessages.clear();
    repository.failAsync = true;
    handler.execute(player, new String[] {"list"});
    assertFalse(playerMessages.isEmpty());
    repository.failAsync = false;
  }

  @Test
  void testExecuteClaimVariations() {
    SpigotRewardsCommandHandler handler =
        new SpigotRewardsCommandHandler(dummyPlugin, application, actionAdapter, helper);

    UUID claimId = UUID.randomUUID();
    RewardClaim claim =
        new RewardClaim(
            claimId,
            EventInstanceId.generate(),
            player.getUniqueId(),
            List.of(new RewardItem("minecraft:diamond", 1)),
            RewardClaimStatus.PENDING,
            Instant.now(),
            null);
    repository.savePendingDurably(claim);

    // Claim successfully
    handler.execute(player, new String[] {"claim", claimId.toString()});
    assertFalse(playerMessages.isEmpty());

    // Claim again (not available)
    playerMessages.clear();
    handler.execute(player, new String[] {"claim", claimId.toString()});
    assertFalse(playerMessages.isEmpty());

    // Claim busy (beginDelivery returns false)
    UUID claimIdBusy = UUID.randomUUID();
    RewardClaim claimBusy =
        new RewardClaim(
            claimIdBusy,
            EventInstanceId.generate(),
            player.getUniqueId(),
            List.of(new RewardItem("minecraft:gold_ingot", 2)),
            RewardClaimStatus.PENDING,
            Instant.now(),
            null);
    repository.savePendingDurably(claimBusy);
    repository.simulateBusy = true;
    playerMessages.clear();
    handler.execute(player, new String[] {"claim", claimIdBusy.toString()});
    assertFalse(playerMessages.isEmpty());
    repository.simulateBusy = false;

    // Claim retained (deliverRewardItems returns false because actionAdapter is shutting down)
    UUID claimIdRetained = UUID.randomUUID();
    RewardClaim claimRetained =
        new RewardClaim(
            claimIdRetained,
            EventInstanceId.generate(),
            player.getUniqueId(),
            List.of(new RewardItem("minecraft:iron_ingot", 2)),
            RewardClaimStatus.PENDING,
            Instant.now(),
            null);
    repository.savePendingDurably(claimRetained);
    actionAdapter.cleanupAll(); // sets shuttingDown = true
    playerMessages.clear();
    handler.execute(player, new String[] {"claim", claimIdRetained.toString()});
    assertFalse(playerMessages.isEmpty());

    // Claim failure (returnToPending fails when returning retained claim)
    UUID claimIdFail = UUID.randomUUID();
    RewardClaim claimFail =
        new RewardClaim(
            claimIdFail,
            EventInstanceId.generate(),
            player.getUniqueId(),
            List.of(new RewardItem("minecraft:emerald", 1)),
            RewardClaimStatus.PENDING,
            Instant.now(),
            null);
    repository.savePendingDurably(claimFail);
    repository.failReconcile = true;
    playerMessages.clear();
    handler.execute(player, new String[] {"claim", claimIdFail.toString()});
    assertFalse(playerMessages.isEmpty());
    repository.failReconcile = false;
  }

  @Test
  void testReconcileMissingArguments() {
    CommandSender sender =
        (CommandSender)
            Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(),
                new Class<?>[] {CommandSender.class},
                (proxy, method, args) -> {
                  if ("hasPermission".equals(method.getName())) {
                    return true;
                  }
                  return null;
                });

    SpigotRewardsCommandHandler handler =
        new SpigotRewardsCommandHandler(dummyPlugin, application, actionAdapter, helper);

    assertThrows(
        IllegalArgumentException.class,
        () -> handler.execute(sender, new String[] {"reconcile", "only-one-arg"}));
  }

  @Test
  void testReconcileInvalidUuid() {
    CommandSender sender =
        (CommandSender)
            Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(),
                new Class<?>[] {CommandSender.class},
                (proxy, method, args) -> {
                  if ("hasPermission".equals(method.getName())) {
                    return true;
                  }
                  return null;
                });

    SpigotRewardsCommandHandler handler =
        new SpigotRewardsCommandHandler(dummyPlugin, application, actionAdapter, helper);

    assertThrows(
        IllegalArgumentException.class,
        () -> handler.execute(sender, new String[] {"reconcile", "not-a-uuid", "force_deliver"}));
  }

  @Test
  void testReconcileListEmptyAndNonEmpty() {
    SpigotRewardsCommandHandler handler =
        new SpigotRewardsCommandHandler(dummyPlugin, application, actionAdapter, helper);

    handler.execute(player, new String[] {"reconcile"});
    assertFalse(playerMessages.isEmpty());

    UUID claimId = UUID.randomUUID();
    RewardClaim claim =
        new RewardClaim(
            claimId,
            EventInstanceId.generate(),
            player.getUniqueId(),
            List.of(new RewardItem("minecraft:iron_ingot", 3)),
            RewardClaimStatus.PENDING,
            Instant.now(),
            null);
    repository.savePendingDurably(claim);
    repository.beginDelivery(claimId);

    playerMessages.clear();
    handler.execute(player, new String[] {"reconcile", "list"});
    assertFalse(playerMessages.isEmpty());

    playerMessages.clear();
    repository.failAsync = true;
    handler.execute(player, new String[] {"reconcile", "list"});
    assertFalse(playerMessages.isEmpty());
    repository.failAsync = false;
  }

  @Test
  void testReconcileDecisions() {
    SpigotRewardsCommandHandler handler =
        new SpigotRewardsCommandHandler(dummyPlugin, application, actionAdapter, helper);

    UUID claimId = UUID.randomUUID();
    RewardClaim claim =
        new RewardClaim(
            claimId,
            EventInstanceId.generate(),
            player.getUniqueId(),
            List.of(new RewardItem("minecraft:iron_ingot", 3)),
            RewardClaimStatus.PENDING,
            Instant.now(),
            null);
    repository.savePendingDurably(claim);
    repository.beginDelivery(claimId);

    // mark-delivered success
    playerMessages.clear();
    handler.execute(player, new String[] {"reconcile", "mark-delivered", claimId.toString()});
    assertFalse(playerMessages.isEmpty());

    // return-pending on unknown claim (returns not found)
    playerMessages.clear();
    handler.execute(
        player, new String[] {"reconcile", "return-pending", UUID.randomUUID().toString()});
    assertFalse(playerMessages.isEmpty());

    // failure
    playerMessages.clear();
    repository.failReconcile = true;
    handler.execute(player, new String[] {"reconcile", "mark-delivered", claimId.toString()});
    assertFalse(playerMessages.isEmpty());
    repository.failReconcile = false;
  }

  private static final class DelegatingRepo
      implements EventInstanceRepository, RewardClaimRepository {

    private final InMemoryEventInstanceRepository delegate = new InMemoryEventInstanceRepository();
    private volatile boolean failAsync;
    private volatile boolean simulateBusy;
    private volatile boolean failReconcile;

    @Override
    public void save(EventInstance eventInstance) {
      delegate.save(eventInstance);
    }

    @Override
    public Optional<EventInstance> findById(EventInstanceId eventInstanceId) {
      return delegate.findById(eventInstanceId);
    }

    @Override
    public List<EventInstance> findAll() {
      return delegate.findAll();
    }

    @Override
    public boolean remove(EventInstanceId eventInstanceId) {
      return delegate.remove(eventInstanceId);
    }

    @Override
    public void saveState(EventRuntimeState state) {
      delegate.saveState(state);
    }

    @Override
    public Optional<EventRuntimeState> findState(EventInstanceId eventInstanceId) {
      return delegate.findState(eventInstanceId);
    }

    @Override
    public void savePendingDurably(RewardClaim claim) {
      delegate.savePendingDurably(claim);
    }

    @Override
    public List<RewardClaim> findPending(UUID playerId) {
      if (failAsync) {
        throw new RuntimeException("Simulated findPending failure");
      }
      return delegate.findPending(playerId);
    }

    @Override
    public CompletableFuture<List<RewardClaim>> findByStatusAsync(RewardClaimStatus status) {
      if (failAsync) {
        return CompletableFuture.failedFuture(new RuntimeException("Simulated async failure"));
      }
      return delegate.findByStatusAsync(status);
    }

    @Override
    public boolean beginDelivery(UUID claimId) {
      if (simulateBusy) {
        return false;
      }
      return delegate.beginDelivery(claimId);
    }

    @Override
    public boolean returnToPending(UUID claimId) {
      if (failReconcile) {
        throw new RuntimeException("Simulated returnToPending failure");
      }
      return delegate.returnToPending(claimId);
    }

    @Override
    public boolean markDelivered(UUID claimId) {
      if (failReconcile) {
        throw new RuntimeException("Simulated markDelivered failure");
      }
      return delegate.markDelivered(claimId);
    }
  }
}
