package dev.spectraevents.platform.paper.integration;

import dev.spectraevents.application.execution.EventRuntimeState;
import dev.spectraevents.application.execution.ExecutionContext;
import dev.spectraevents.application.execution.FatalActionException;
import dev.spectraevents.application.execution.IntegrationActionResolver;
import dev.spectraevents.core.event.execution.action.ActionDefinition;
import dev.spectraevents.core.event.runtime.EventInstance;
import java.util.UUID;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultIntegration implements IntegrationActionResolver {

  private Economy econ = null;

  public VaultIntegration() {
    if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
      RegisteredServiceProvider<Economy> rsp =
          Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
      if (rsp != null) {
        econ = rsp.getProvider();
      }
    }
  }

  @Override
  public boolean execute(
      ActionDefinition action,
      EventInstance instance,
      EventRuntimeState state,
      ExecutionContext context)
      throws FatalActionException {
    if (!"give_money".equalsIgnoreCase(action.type())) {
      return false;
    }

    if (econ == null) {
      return false; // Can't execute
    }

    if (context == null || context.actor() == null) {
      return false;
    }

    Player player = null;
    if (context.actor() instanceof Player p) {
      player = p;
    } else if (context.actor() instanceof UUID uuid) {
      player = Bukkit.getPlayer(uuid);
    }

    if (player == null) {
      return false; // Player not online
    }

    Object amountObj = action.parameters().get("amount");
    if (amountObj == null) return false;

    double amount;
    if (amountObj instanceof Number n) {
      amount = n.doubleValue();
    } else {
      try {
        amount = Double.parseDouble(String.valueOf(amountObj));
      } catch (NumberFormatException e) {
        return false;
      }
    }

    try {
      // Need to run on main thread? Vault economy might not be async safe.
      // But we might be in Folia where we need RegionScheduler or GlobalRegionScheduler.
      // Actually, vault economy is typically not async safe for some plugins.
      // We use Bukkit.getScheduler() or global region scheduler.
      Player finalPlayer = player;
      player
          .getScheduler()
          .execute(
              Bukkit.getPluginManager().getPlugin("SpectraEvents"),
              () -> {
                econ.depositPlayer(finalPlayer, amount);
              },
              null,
              1);
      return true;
    } catch (NoClassDefFoundError | Exception e) {
      return false;
    }
  }

  @Override
  public boolean supports(String actionType) {
    return "give_money".equalsIgnoreCase(actionType);
  }
}
