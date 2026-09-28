package io.github.kizio806.spectraevents.core.event.execution.condition;

/** Platform-neutral conditions implemented by optional server integrations. */
public interface IntegrationConditions {

  record HasPermissionCondition(String permission) implements ConditionDefinition {
    @Override
    public String type() {
      return "has_permission";
    }
  }

  record HasGroupCondition(String group) implements ConditionDefinition {
    @Override
    public String type() {
      return "has_group";
    }
  }

  record InRegionCondition(String region) implements ConditionDefinition {
    @Override
    public String type() {
      return "in_region";
    }
  }
}
