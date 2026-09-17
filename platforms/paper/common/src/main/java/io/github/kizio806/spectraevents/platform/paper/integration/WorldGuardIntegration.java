package io.github.kizio806.spectraevents.platform.paper.integration;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.execution.ExecutionContext;
import io.github.kizio806.spectraevents.application.execution.IntegrationConditionResolver;
import io.github.kizio806.spectraevents.core.event.execution.condition.ConditionDefinition;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class WorldGuardIntegration implements IntegrationConditionResolver {

  @Override
  public boolean resolve(
      ConditionDefinition condition,
      EventInstance instance,
      EventRuntimeState state,
      ExecutionContext context) {
    if (!"worldguard_region".equalsIgnoreCase(condition.type())) {
      return false;
    }

    Object regionObj = condition.parameters().get("region");
    if (regionObj == null) return false;
    String requiredRegion = String.valueOf(regionObj);

    Location loc = null;
    if (context != null && context.actor() instanceof Player p) {
      loc = p.getLocation();
    } else if (state.platformLocation().isPresent()
        && state.platformLocation().get() instanceof Location l) {
      loc = l;
    }

    if (loc == null) {
      return false;
    }

    try {
      RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
      RegionQuery query = container.createQuery();
      ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(loc));

      for (ProtectedRegion pr : set) {
        if (pr.getId().equalsIgnoreCase(requiredRegion)) {
          return true;
        }
      }
      return false;
    } catch (NoClassDefFoundError | Exception e) {
      return false;
    }
  }

  @Override
  public boolean supports(String conditionType) {
    return "worldguard_region".equalsIgnoreCase(conditionType);
  }
}
