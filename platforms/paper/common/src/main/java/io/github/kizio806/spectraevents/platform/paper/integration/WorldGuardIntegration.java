package io.github.kizio806.spectraevents.platform.paper.integration;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.execution.ExecutionContext;
import io.github.kizio806.spectraevents.application.execution.IntegrationConditionResolver;
import io.github.kizio806.spectraevents.application.integration.IntegrationInitializationContext;
import io.github.kizio806.spectraevents.application.integration.PlatformIntegrationModule;
import io.github.kizio806.spectraevents.core.event.execution.condition.ConditionDefinition;
import io.github.kizio806.spectraevents.core.event.execution.condition.IntegrationConditions;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import java.util.UUID;
import org.bukkit.entity.Player;

public class WorldGuardIntegration
    implements IntegrationConditionResolver, PlatformIntegrationModule {

  @Override
  public String requiredPluginName() {
    return "WorldGuard";
  }

  @Override
  public void initialize(IntegrationInitializationContext context) {
    context.application().executionEngine().registerConditionResolver(this);
  }

  @Override
  public boolean supports(String conditionType) {
    return "in_region".equals(conditionType);
  }

  @Override
  public boolean resolve(
      ConditionDefinition condition,
      EventInstance instance,
      EventRuntimeState state,
      ExecutionContext context) {
    if (context == null || context.actor() == null) return false;

    Player player = null;
    if (context.actor() instanceof Player p) {
      player = p;
    } else if (context.actor() instanceof UUID uuid) {
      player = org.bukkit.Bukkit.getPlayer(uuid);
    }
    if (player == null) return false;

    if (!(condition instanceof IntegrationConditions.InRegionCondition regionCondition))
      return false;
    String regionId = regionCondition.region();

    try {
      RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
      RegionManager regions = container.get(BukkitAdapter.adapt(player.getWorld()));
      if (regions != null) {
        ApplicableRegionSet set =
            regions.getApplicableRegions(BukkitAdapter.asBlockVector(player.getLocation()));
        return set.getRegions().stream().anyMatch(r -> r.getId().equalsIgnoreCase(regionId));
      }
    } catch (NoClassDefFoundError expected) {
      // Integration missing
    }
    return false;
  }
}
