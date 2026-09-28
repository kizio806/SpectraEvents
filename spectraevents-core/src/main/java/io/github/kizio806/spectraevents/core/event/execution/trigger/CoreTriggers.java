package io.github.kizio806.spectraevents.core.event.execution.trigger;

import java.time.Duration;

public interface CoreTriggers {

  /** Emitted after one durable, atomic removal from a public event container. */
  public record LootItemTakenTrigger() implements TriggerDefinition {
    @Override
    public String type() {
      return "loot_item_taken";
    }
  }

  /** Emitted exactly once after the final public container slot has been removed. */
  public record LootContainerEmptiedTrigger() implements TriggerDefinition {
    @Override
    public String type() {
      return "loot_container_emptied";
    }
  }

  /** Fired once all entities owned by a named bounded wave have died. */
  public record WaveClearedTrigger(String waveId) implements TriggerDefinition {
    public WaveClearedTrigger {
      if (waveId == null || waveId.isBlank()) {
        throw new IllegalArgumentException("waveId must not be blank");
      }
    }

    @Override
    public String type() {
      return "wave_cleared";
    }
  }

  public record TimerElapsedTrigger(Duration duration) implements TriggerDefinition {
    @Override
    public String type() {
      return "timer_elapsed";
    }
  }

  public record HealthThresholdCrossedTrigger(int threshold) implements TriggerDefinition {
    @Override
    public String type() {
      return "health_threshold_crossed";
    }
  }

  /** Fired once a health pool crosses a relative percentage threshold. */
  public record HealthPercentThresholdCrossedTrigger(int percent) implements TriggerDefinition {
    public HealthPercentThresholdCrossedTrigger {
      if (percent < 0 || percent > 100) {
        throw new IllegalArgumentException("percent must be between 0 and 100");
      }
    }

    @Override
    public String type() {
      return "health_percent_threshold_crossed";
    }
  }

  /** A direct player melee hit whose damage is carried by the execution context. */
  public record CombatDamageTrigger() implements TriggerDefinition {
    @Override
    public String type() {
      return "combat_damage";
    }
  }

  public record HealthDepletedTrigger() implements TriggerDefinition {
    @Override
    public String type() {
      return "health_depleted";
    }
  }

  public record HitsReachedTrigger() implements TriggerDefinition {
    @Override
    public String type() {
      return "hits_reached";
    }
  }

  /** Fired exactly once when an interaction counter crosses a relative completion percentage. */
  public record HitsPercentThresholdCrossedTrigger(int percent) implements TriggerDefinition {
    public HitsPercentThresholdCrossedTrigger {
      if (percent < 0 || percent > 100) {
        throw new IllegalArgumentException("percent must be between 0 and 100");
      }
    }

    @Override
    public String type() {
      return "hits_percent_threshold_crossed";
    }
  }
}
