package io.github.kizio806.spectraevents.core.event.execution.action;

import java.time.Duration;
import java.util.List;

public interface CoreActions {

  public record InitializeHealthAction(int max) implements ActionDefinition {
    @Override
    public String type() {
      return "initialize_health";
    }
  }

  public record InitializeHitCounterAction(int max) implements ActionDefinition {
    @Override
    public String type() {
      return "initialize_hit_counter";
    }
  }

  public record SetLockedAction(Duration duration) implements ActionDefinition {
    @Override
    public String type() {
      return "set_locked";
    }
  }

  public record TryClaimAction() implements ActionDefinition {
    @Override
    public String type() {
      return "try_claim";
    }
  }

  /** Initializes a finite public loot snapshot once, before an event container is opened. */
  public record InitializeSharedLootAction(List<LootStack> items) implements ActionDefinition {
    public InitializeSharedLootAction {
      items = List.copyOf(items);
    }

    @Override
    public String type() {
      return "initialize_shared_loot";
    }
  }

  /** Platform-neutral description of an item stored in a shared event container. */
  public record LootStack(String material, int amount) {
    public LootStack {
      if (material == null || material.isBlank() || amount < 1) {
        throw new IllegalArgumentException("Loot stack must have a material and positive amount");
      }
    }
  }

  /** YAML loot entry evaluated once before a public ground-loot release. */
  public record GroundLootEntry(String material, int amount, int chance) {
    public GroundLootEntry {
      if (material == null || material.isBlank() || amount < 1 || chance < 0 || chance > 100) {
        throw new IllegalArgumentException(
            "Ground loot entry must have valid material, amount and chance");
      }
    }
  }

  /** Rolls the YAML pool once and durably stores the resulting public ground-loot slots. */
  public record InitializeGroundLootAction(List<GroundLootEntry> entries)
      implements ActionDefinition {
    public InitializeGroundLootAction {
      entries = List.copyOf(entries);
    }

    @Override
    public String type() {
      return "initialize_ground_loot";
    }
  }

  /** Creates one durable mailbox claim for every recorded event participant. */
  public record CreateParticipantRewardClaimsAction(List<GroundLootEntry> entries)
      implements ActionDefinition {
    public CreateParticipantRewardClaimsAction {
      entries = List.copyOf(entries);
      if (entries.isEmpty()) {
        throw new IllegalArgumentException("participant reward pool must not be empty");
      }
    }

    @Override
    public String type() {
      return "create_participant_reward_claims";
    }
  }

  public record ApplyDamageAction(int amount) implements ActionDefinition {
    @Override
    public String type() {
      return "apply_damage";
    }
  }

  /** Applies a server-calculated combat hit, bounded before it reaches the event health pool. */
  public record ApplyCombatDamageAction(
      int maximumDamage, int healthFloorPercent, Duration cooldown) implements ActionDefinition {
    public ApplyCombatDamageAction {
      if (maximumDamage < 1) {
        throw new IllegalArgumentException("maximumDamage must be positive");
      }
      if (healthFloorPercent < 0 || healthFloorPercent > 100) {
        throw new IllegalArgumentException("healthFloorPercent must be between 0 and 100");
      }
      if (cooldown == null || cooldown.isNegative()) {
        throw new IllegalArgumentException("cooldown must not be negative");
      }
    }

    @Override
    public String type() {
      return "apply_combat_damage";
    }
  }

  public record IncrementHitsAction(int amount) implements ActionDefinition {
    @Override
    public String type() {
      return "increment_hits";
    }
  }

  public record CompleteEventAction() implements ActionDefinition {
    @Override
    public String type() {
      return "complete_event";
    }
  }

  public record CancelEventAction() implements ActionDefinition {
    @Override
    public String type() {
      return "cancel_event";
    }
  }

  /** Ends an encounter as failed; used for hard wave/encounter timeout escalation. */
  public record FailEventAction() implements ActionDefinition {
    @Override
    public String type() {
      return "fail_event";
    }
  }
}
