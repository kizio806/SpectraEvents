package io.github.kizio806.spectraevents.core.event.execution.condition;

public interface CoreConditions {

  public record NotLockedCondition() implements ConditionDefinition {
    @Override
    public String type() {
      return "not_locked";
    }
  }

  public record IsLockedCondition() implements ConditionDefinition {
    @Override
    public String type() {
      return "is_locked";
    }
  }
}
