package dev.spectraevents.platform.paper.integration;

import dev.spectraevents.application.execution.EventRuntimeState;
import dev.spectraevents.application.execution.ExecutionContext;
import dev.spectraevents.application.execution.IntegrationConditionResolver;
import dev.spectraevents.core.event.execution.condition.ConditionDefinition;
import dev.spectraevents.core.event.runtime.EventInstance;
import java.util.UUID;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.entity.Player;

public class LuckPermsIntegration implements IntegrationConditionResolver {

  @Override
  public boolean resolve(
      ConditionDefinition condition,
      EventInstance instance,
      EventRuntimeState state,
      ExecutionContext context) {
    if (!"luckperms_group".equalsIgnoreCase(condition.type())) {
      return false;
    }

    if (context == null || context.actor() == null) {
      return false; // Group requires an actor
    }

    UUID uuid = null;
    if (context.actor() instanceof Player p) {
      uuid = p.getUniqueId();
    } else if (context.actor() instanceof UUID id) {
      uuid = id;
    } else {
      return false;
    }

    Object groupObj = condition.parameters().get("group");
    if (groupObj == null) return false;
    String requiredGroup = String.valueOf(groupObj);

    try {
      LuckPerms api = LuckPermsProvider.get();
      User user = api.getUserManager().getUser(uuid);
      if (user == null) {
        // If user is offline, we could try loadUser, but for events we only care about online
        // players usually.
        return false;
      }
      return user.getPrimaryGroup().equalsIgnoreCase(requiredGroup)
          || user.getNodes().stream()
              .anyMatch(n -> n.getKey().equalsIgnoreCase("group." + requiredGroup));
    } catch (NoClassDefFoundError | Exception e) {
      return false;
    }
  }

  @Override
  public boolean supports(String conditionType) {
    return "luckperms_group".equalsIgnoreCase(conditionType);
  }
}
