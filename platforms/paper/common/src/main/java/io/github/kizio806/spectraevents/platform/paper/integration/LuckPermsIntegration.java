package io.github.kizio806.spectraevents.platform.paper.integration;

import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.execution.ExecutionContext;
import io.github.kizio806.spectraevents.application.execution.IntegrationConditionResolver;
import io.github.kizio806.spectraevents.application.integration.IntegrationInitializationContext;
import io.github.kizio806.spectraevents.application.integration.PlatformIntegrationModule;
import io.github.kizio806.spectraevents.core.event.execution.condition.ConditionDefinition;
import io.github.kizio806.spectraevents.core.event.execution.condition.IntegrationConditions;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import java.util.UUID;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class LuckPermsIntegration
    implements IntegrationConditionResolver, PlatformIntegrationModule {

  @Override
  public String requiredPluginName() {
    return "LuckPerms";
  }

  @Override
  public void initialize(IntegrationInitializationContext context) {
    context.application().executionEngine().registerConditionResolver(this);
  }

  @Override
  public boolean supports(String conditionType) {
    return "has_permission".equals(conditionType) || "has_group".equals(conditionType);
  }

  @Override
  public boolean resolve(
      ConditionDefinition condition,
      EventInstance instance,
      EventRuntimeState state,
      ExecutionContext context) {
    if (context == null || context.actor() == null) return false;
    try {
      LuckPerms api = LuckPermsProvider.get();
      User user = null;
      if (context.actor() instanceof UUID uuid) {
        user = api.getUserManager().getUser(uuid);
        if (user == null) {
          OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(uuid);
          if (op.isOnline()) {
            user = api.getUserManager().getUser(op.getName());
          }
        }
      } else if (context.actor() instanceof Player p) {
        user = api.getUserManager().getUser(p.getUniqueId());
      }

      if (user == null) return false;

      if (condition instanceof IntegrationConditions.HasPermissionCondition permissionCondition) {
        String perm = permissionCondition.permission();
        return user.getCachedData().getPermissionData().checkPermission(perm).asBoolean();
      }
      if (condition instanceof IntegrationConditions.HasGroupCondition groupCondition) {
        String group = groupCondition.group();
        return user.getPrimaryGroup().equalsIgnoreCase(group)
            || user.getNodes().stream()
                .anyMatch(
                    node ->
                        node instanceof net.luckperms.api.node.types.InheritanceNode inheritanceNode
                            && inheritanceNode.getGroupName().equalsIgnoreCase(group));
      }
    } catch (NoClassDefFoundError | IllegalStateException expected) {
      // Integration missing
    }
    return false;
  }
}
