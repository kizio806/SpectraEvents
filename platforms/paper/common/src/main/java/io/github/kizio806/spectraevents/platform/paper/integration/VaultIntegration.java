package io.github.kizio806.spectraevents.platform.paper.integration;

import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.execution.ExecutionContext;
import io.github.kizio806.spectraevents.application.execution.IntegrationActionResolver;
import io.github.kizio806.spectraevents.application.integration.IntegrationInitializationContext;
import io.github.kizio806.spectraevents.application.integration.PlatformIntegrationModule;
import io.github.kizio806.spectraevents.core.event.execution.action.ActionDefinition;
import io.github.kizio806.spectraevents.core.event.execution.action.IntegrationActions;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultIntegration implements IntegrationActionResolver, PlatformIntegrationModule {

  @Override
  public String requiredPluginName() {
    return "Vault";
  }

  @Override
  public void initialize(IntegrationInitializationContext context) {
    context.application().executionEngine().registerActionResolver(this);
  }

  @Override
  public boolean supports(String actionType) {
    return "give_money".equals(actionType) || "take_money".equals(actionType);
  }

  @Override
  public CompletableFuture<Boolean> execute(
      ActionDefinition action,
      EventInstance instance,
      EventRuntimeState state,
      ExecutionContext context) {
    if (context == null || context.actor() == null) {
      return CompletableFuture.completedFuture(false);
    }
    OfflinePlayer target = null;
    if (context.actor() instanceof Player p) {
      target = p;
    } else if (context.actor() instanceof UUID uuid) {
      target = org.bukkit.Bukkit.getOfflinePlayer(uuid);
    }

    if (target == null) return CompletableFuture.completedFuture(false);

    try {
      RegisteredServiceProvider<Economy> rsp =
          org.bukkit.Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
      if (rsp == null) {
        return CompletableFuture.completedFuture(false);
      }
      Economy econ = rsp.getProvider();

      double amount =
          switch (action) {
            case IntegrationActions.GiveMoneyAction giveMoney -> giveMoney.amount();
            case IntegrationActions.TakeMoneyAction takeMoney -> takeMoney.amount();
            default -> Double.NaN;
          };
      if (!Double.isFinite(amount) || amount <= 0.0)
        return CompletableFuture.completedFuture(false);

      if ("give_money".equals(action.type())) {
        return CompletableFuture.completedFuture(
            econ.depositPlayer(target, amount).transactionSuccess());
      } else if ("take_money".equals(action.type())) {
        return CompletableFuture.completedFuture(
            econ.withdrawPlayer(target, amount).transactionSuccess());
      }
    } catch (NoClassDefFoundError | NumberFormatException expected) {
      // Integration missing
    }
    return CompletableFuture.completedFuture(false);
  }
}
