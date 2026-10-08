package io.github.kizio806.spectraevents.core.event.execution.action;

/** Platform-neutral actions implemented by optional server integrations. */
public interface IntegrationActions {

  record GiveMoneyAction(double amount) implements ActionDefinition {
    @Override
    public String type() {
      return "give_money";
    }
  }

  record TakeMoneyAction(double amount) implements ActionDefinition {
    @Override
    public String type() {
      return "take_money";
    }
  }
}
