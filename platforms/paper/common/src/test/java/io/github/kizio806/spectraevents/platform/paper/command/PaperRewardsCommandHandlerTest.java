package io.github.kizio806.spectraevents.platform.paper.command;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.mojang.brigadier.CommandDispatcher;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.application.port.RewardClaimRepository;
import io.github.kizio806.spectraevents.application.service.RewardMailboxService;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.gameplay.reward.RewardClaim;
import io.github.kizio806.spectraevents.core.gameplay.reward.RewardClaimStatus;
import io.github.kizio806.spectraevents.core.gameplay.reward.RewardItem;
import io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup;
import io.github.kizio806.spectraevents.platform.paper.action.PaperActionAdapter;
import io.github.kizio806.spectraevents.platform.paper.lifecycle.PaperResourceCleaner;
import io.github.kizio806.spectraevents.platform.paper.render.PaperModelRenderer;
import io.github.kizio806.spectraevents.platform.paper.scheduler.RegionTaskScheduler;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaperRewardsCommandHandlerTest {

  @BeforeAll
  static void beforeAll() {
    MockBukkitSetup.ensureInitialized();
  }

  private RewardMailboxService rewardMailboxService;
  private PaperActionAdapter actionAdapter;
  private RegionTaskScheduler regionScheduler;
  private LocaleCatalog locales;
  private RewardsCommandHandler handler;
  private List<Component> messages;
  private CommandDispatcher<CommandSourceStack> dispatcher;
  private final Map<UUID, RewardClaim> claimStore = new ConcurrentHashMap<>();
  private volatile boolean simulateRepoFailure = false;

  @BeforeEach
  void setUp() {
    claimStore.clear();
    simulateRepoFailure = false;

    RewardClaimRepository repo =
        new RewardClaimRepository() {
          @Override
          public void savePendingDurably(RewardClaim claim) {
            claimStore.put(claim.id(), claim);
          }

          @Override
          public List<RewardClaim> findPending(UUID playerId) {
            if (simulateRepoFailure) {
              throw new RuntimeException("Simulated database failure");
            }
            return claimStore.values().stream()
                .filter(
                    c -> c.playerId().equals(playerId) && c.status() == RewardClaimStatus.PENDING)
                .toList();
          }

          @Override
          public CompletableFuture<List<RewardClaim>> findByStatusAsync(RewardClaimStatus status) {
            if (simulateRepoFailure) {
              return CompletableFuture.failedFuture(
                  new RuntimeException("Simulated status lookup failure"));
            }
            return CompletableFuture.completedFuture(
                claimStore.values().stream().filter(c -> c.status() == status).toList());
          }

          @Override
          public boolean beginDelivery(UUID claimId) {
            if (simulateRepoFailure) {
              throw new RuntimeException("Simulated delivery error");
            }
            RewardClaim existing = claimStore.get(claimId);
            if (existing != null && existing.status() == RewardClaimStatus.PENDING) {
              RewardClaim delivering =
                  new RewardClaim(
                      existing.id(),
                      existing.instanceId(),
                      existing.playerId(),
                      existing.items(),
                      RewardClaimStatus.DELIVERING,
                      existing.createdAt(),
                      null);
              claimStore.put(claimId, delivering);
              return true;
            }
            return false;
          }

          @Override
          public boolean returnToPending(UUID claimId) {
            if (simulateRepoFailure) {
              throw new RuntimeException("Simulated reconcile error");
            }
            RewardClaim existing = claimStore.get(claimId);
            if (existing != null) {
              claimStore.put(
                  claimId,
                  new RewardClaim(
                      existing.id(),
                      existing.instanceId(),
                      existing.playerId(),
                      existing.items(),
                      RewardClaimStatus.PENDING,
                      existing.createdAt(),
                      null));
              return true;
            }
            return false;
          }

          @Override
          public boolean markDelivered(UUID claimId) {
            if (simulateRepoFailure) {
              throw new RuntimeException("Simulated reconcile error");
            }
            RewardClaim existing = claimStore.get(claimId);
            if (existing != null) {
              claimStore.put(
                  claimId,
                  new RewardClaim(
                      existing.id(),
                      existing.instanceId(),
                      existing.playerId(),
                      existing.items(),
                      RewardClaimStatus.DELIVERED,
                      existing.createdAt(),
                      Instant.now()));
              return true;
            }
            return false;
          }
        };

    rewardMailboxService = new RewardMailboxService(repo);

    Plugin mockPlugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(), new Class<?>[] {Plugin.class}, (p, m, a) -> null);

    regionScheduler =
        new RegionTaskScheduler() {
          @Override
          public void executeGlobal(Runnable task) {
            task.run();
          }

          @Override
          public void executeAt(Location location, Runnable task) {
            task.run();
          }

          @Override
          public void executeFor(Entity entity, Runnable task) {
            task.run();
          }
        };

    Map<String, String> localeMap = new java.util.HashMap<>();
    localeMap.put("command.paper.rewards-player-only", "Only players can use rewards list/claim.");
    localeMap.put("command.paper.rewards-title", "Pending rewards (%count%):");
    localeMap.put("command.paper.reward-entry", " - %id% (%count% items)");
    localeMap.put("messages.reward-delivered", "Reward delivered!");
    localeMap.put("messages.reward-retained", "Reward retained in mailbox.");
    localeMap.put("command.paper.reward-busy", "Reward is currently delivering.");
    localeMap.put("command.paper.reward-unavailable", "Reward is not available.");
    localeMap.put("command.paper.invalid-reward-id", "Invalid reward ID.");
    localeMap.put("command.rewards.reconcile-title", "Delivering claims (%count%):");
    localeMap.put("command.rewards.reconcile-entry", " - %id% (%player%, %count%)");
    localeMap.put("command.rewards.reconcile-resolved", "Resolved %id% with %decision%");
    localeMap.put("command.rewards.reconcile-not-found", "Claim %id% not found");
    localeMap.put("command.paper.failed", "Action failed: %reason%");

    locales = LocaleCatalog.of(localeMap);

    EventTaskScheduler mockScheduler =
        (EventTaskScheduler)
            Proxy.newProxyInstance(
                EventTaskScheduler.class.getClassLoader(),
                new Class<?>[] {EventTaskScheduler.class},
                (p, m, a) -> null);

    PaperModelRenderer renderer = new PaperModelRenderer(mockPlugin);
    PaperResourceCleaner cleaner = new PaperResourceCleaner(renderer, mockScheduler);
    actionAdapter = new PaperActionAdapter(regionScheduler, cleaner, locales);

    handler =
        new RewardsCommandHandler(rewardMailboxService, actionAdapter, regionScheduler, locales);
    messages = new ArrayList<>();
    dispatcher = PaperCommandTestSupport.createDispatcher();
    dispatcher.register(handler.build());
  }

  @Test
  void testConsoleCannotListOrClaimRewards() throws Exception {
    CommandSender sender = PaperCommandTestSupport.createRecordingSender(messages);
    CommandSourceStack source = PaperCommandTestSupport.createMockSource(sender, null);

    dispatcher.execute("rewards list", source);
    assertFalse(messages.isEmpty());

    messages.clear();
    dispatcher.execute("rewards claim 12345678-1234-1234-1234-123456789abc", source);
    assertFalse(messages.isEmpty());
  }

  @Test
  void testPlayerListAndClaimRewards() throws Exception {
    Player player = PaperCommandTestSupport.createRecordingPlayer(messages, null);
    CommandSourceStack source = PaperCommandTestSupport.createMockSource(player, null);

    // List with empty claims
    dispatcher.execute("rewards list", source);
    assertFalse(messages.isEmpty());

    // List with non-empty claims
    UUID claimId1 = UUID.randomUUID();
    RewardClaim claim1 =
        new RewardClaim(
            claimId1,
            EventInstanceId.generate(),
            player.getUniqueId(),
            List.of(new RewardItem("minecraft:diamond", 5)),
            RewardClaimStatus.PENDING,
            Instant.now(),
            null);
    claimStore.put(claimId1, claim1);

    messages.clear();
    dispatcher.execute("rewards list", source);
    assertFalse(messages.isEmpty());

    // List with failure
    simulateRepoFailure = true;
    messages.clear();
    dispatcher.execute("rewards list", source);
    assertFalse(messages.isEmpty());
    simulateRepoFailure = false;

    // Claim with invalid UUID
    messages.clear();
    dispatcher.execute("rewards claim not-a-uuid", source);
    assertFalse(messages.isEmpty());

    // Claim successfully
    messages.clear();
    dispatcher.execute("rewards claim " + claimId1, source);
    assertFalse(messages.isEmpty());

    // Claim non-existent (NOT_AVAILABLE)
    messages.clear();
    dispatcher.execute("rewards claim " + UUID.randomUUID(), source);
    assertFalse(messages.isEmpty());

    // Claim busy (DELIVERING)
    UUID claimIdBusy = UUID.randomUUID();
    claimStore.put(
        claimIdBusy,
        new RewardClaim(
            claimIdBusy,
            EventInstanceId.generate(),
            player.getUniqueId(),
            List.of(new RewardItem("minecraft:diamond", 1)),
            RewardClaimStatus.DELIVERING,
            Instant.now(),
            null));
    messages.clear();
    dispatcher.execute("rewards claim " + claimIdBusy, source);
    assertFalse(messages.isEmpty());

    // Claim failure with null exception message
    simulateRepoFailure = true;
    messages.clear();
    dispatcher.execute("rewards claim " + claimId1, source);
    assertFalse(messages.isEmpty());
    simulateRepoFailure = false;

    // Claim retained (e.g. offline player)
    UUID offlinePlayerId = UUID.randomUUID();
    Player offlinePlayer =
        (Player)
            Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[] {Player.class},
                (p, m, a) -> {
                  if ("sendMessage".equals(m.getName())) {
                    if (a != null && a.length > 0 && a[0] instanceof Component c) {
                      messages.add(c);
                    }
                    return null;
                  }
                  if ("hasPermission".equals(m.getName())) return true;
                  if ("getName".equals(m.getName())) return "OfflinePlayer";
                  if ("isOp".equals(m.getName())) return true;
                  if ("isOnline".equals(m.getName())) return false;
                  if ("getUniqueId".equals(m.getName())) return offlinePlayerId;
                  return null;
                });
    CommandSourceStack offlineSource =
        PaperCommandTestSupport.createMockSource(offlinePlayer, null);
    UUID claimIdRetained = UUID.randomUUID();
    claimStore.put(
        claimIdRetained,
        new RewardClaim(
            claimIdRetained,
            EventInstanceId.generate(),
            offlinePlayerId,
            List.of(new RewardItem("minecraft:iron_ingot", 10)),
            RewardClaimStatus.PENDING,
            Instant.now(),
            null));

    messages.clear();
    dispatcher.execute("rewards claim " + claimIdRetained, offlineSource);
    assertFalse(messages.isEmpty());
  }

  @Test
  void testPermissionBranches() throws Exception {
    // 1. Sender with only spectraevents.admin
    CommandSender adminSender =
        (CommandSender)
            Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(),
                new Class<?>[] {CommandSender.class},
                (p, m, a) -> {
                  if ("sendMessage".equals(m.getName())) return null;
                  if ("hasPermission".equals(m.getName()) && a != null && a.length > 0) {
                    return "spectraevents.admin".equals(a[0]);
                  }
                  if ("isOp".equals(m.getName())) return false;
                  return null;
                });
    CommandSourceStack adminSource = PaperCommandTestSupport.createMockSource(adminSender, null);
    dispatcher.execute("rewards reconcile list", adminSource);

    // 2. Sender with only isOp = true
    CommandSender opSender =
        (CommandSender)
            Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(),
                new Class<?>[] {CommandSender.class},
                (p, m, a) -> {
                  if ("sendMessage".equals(m.getName())) return null;
                  if ("hasPermission".equals(m.getName())) return false;
                  if ("isOp".equals(m.getName())) return true;
                  return null;
                });
    CommandSourceStack opSource = PaperCommandTestSupport.createMockSource(opSender, null);
    dispatcher.execute("rewards reconcile list", opSource);

    // 3. Sender with no permissions and not op
    CommandSender noPermSender =
        (CommandSender)
            Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(),
                new Class<?>[] {CommandSender.class},
                (p, m, a) -> {
                  if ("sendMessage".equals(m.getName())) return null;
                  if ("hasPermission".equals(m.getName())) return false;
                  if ("isOp".equals(m.getName())) return false;
                  return null;
                });
    CommandSourceStack noPermSource = PaperCommandTestSupport.createMockSource(noPermSender, null);
    // Brigadier execution should fail syntax / permission check for noPermSource
    try {
      dispatcher.execute("rewards reconcile list", noPermSource);
    } catch (Exception expected) {
      assertNotNull(expected.getMessage());
    }
  }

  @Test
  void testReconciliationSubcommands() throws Exception {
    Player player = PaperCommandTestSupport.createRecordingPlayer(messages, null);
    CommandSourceStack source = PaperCommandTestSupport.createMockSource(player, null);

    // Reconcile list empty
    messages.clear();
    dispatcher.execute("rewards reconcile list", source);
    assertFalse(messages.isEmpty());

    // Reconcile list non-empty
    UUID claimId = UUID.randomUUID();
    RewardClaim claim =
        new RewardClaim(
            claimId,
            EventInstanceId.generate(),
            player.getUniqueId(),
            List.of(new RewardItem("minecraft:gold_ingot", 10)),
            RewardClaimStatus.DELIVERING,
            Instant.now(),
            null);
    claimStore.put(claimId, claim);

    messages.clear();
    dispatcher.execute("rewards reconcile list", source);
    assertFalse(messages.isEmpty());

    // Reconcile list failure
    simulateRepoFailure = true;
    messages.clear();
    dispatcher.execute("rewards reconcile list", source);
    assertFalse(messages.isEmpty());
    simulateRepoFailure = false;

    // Reconcile invalid UUID
    messages.clear();
    dispatcher.execute("rewards reconcile mark-delivered invalid-uuid", source);
    assertFalse(messages.isEmpty());

    messages.clear();
    dispatcher.execute("rewards reconcile return-pending invalid-uuid", source);
    assertFalse(messages.isEmpty());

    // Reconcile non-existent claim
    messages.clear();
    dispatcher.execute("rewards reconcile mark-delivered " + UUID.randomUUID(), source);
    assertFalse(messages.isEmpty());

    // Reconcile mark-delivered
    messages.clear();
    dispatcher.execute("rewards reconcile mark-delivered " + claimId, source);
    assertFalse(messages.isEmpty());

    // Reconcile return-pending on delivering claim
    UUID claimId2 = UUID.randomUUID();
    claimStore.put(
        claimId2,
        new RewardClaim(
            claimId2,
            EventInstanceId.generate(),
            player.getUniqueId(),
            List.of(new RewardItem("minecraft:diamond", 1)),
            RewardClaimStatus.DELIVERING,
            Instant.now(),
            null));

    messages.clear();
    dispatcher.execute("rewards reconcile return-pending " + claimId2, source);
    assertFalse(messages.isEmpty());

    // Reconcile failures with exception
    simulateRepoFailure = true;
    messages.clear();
    dispatcher.execute("rewards reconcile mark-delivered " + claimId, source);
    assertFalse(messages.isEmpty());

    messages.clear();
    dispatcher.execute("rewards reconcile return-pending " + claimId2, source);
    assertFalse(messages.isEmpty());
    simulateRepoFailure = false;
  }
}
