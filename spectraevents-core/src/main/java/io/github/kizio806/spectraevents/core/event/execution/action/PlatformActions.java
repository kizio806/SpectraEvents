package io.github.kizio806.spectraevents.core.event.execution.action;

import java.util.List;

public interface PlatformActions {

  public record SpawnModelAction(String model, int heightOffset) implements ActionDefinition {
    @Override
    public String type() {
      return "spawn_model";
    }
  }

  public record MoveModelAction() implements ActionDefinition {
    @Override
    public String type() {
      return "move_model";
    }
  }

  public record RemoveModelAction() implements ActionDefinition {
    @Override
    public String type() {
      return "remove_model";
    }
  }

  public record PlayAnimationAction(String animation) implements ActionDefinition {
    @Override
    public String type() {
      return "play_animation";
    }
  }

  public record PlaySoundAction(String sound, float volume, float pitch)
      implements ActionDefinition {
    @Override
    public String type() {
      return "play_sound";
    }
  }

  public record SpawnParticlesAction(String particle, int count) implements ActionDefinition {
    @Override
    public String type() {
      return "spawn_particles";
    }
  }

  public record GiveItemAction(String material, int amount) implements ActionDefinition {
    @Override
    public String type() {
      return "give_item";
    }
  }

  /** Opens the event's public, server-authoritative shared loot inventory for the actor. */
  public record OpenSharedLootAction(String title) implements ActionDefinition {
    @Override
    public String type() {
      return "open_shared_loot";
    }
  }

  public record DropLootAction(double radius, List<LootItem> items) implements ActionDefinition {
    public DropLootAction {
      items = List.copyOf(items);
    }

    @Override
    public String type() {
      return "drop_loot";
    }
  }

  /** Releases the already-durable ground loot snapshot as public, tagged world items. */
  public record ReleaseGroundLootAction(double radius) implements ActionDefinition {
    @Override
    public String type() {
      return "release_ground_loot";
    }
  }

  /** Three direct-delivery pools for deterministic first/second/third encounter podium rewards. */
  public record AwardPodiumAction(List<PodiumPool> pools) implements ActionDefinition {
    public AwardPodiumAction {
      pools = List.copyOf(pools);
      if (pools.size() != 3) {
        throw new IllegalArgumentException("podium rewards require exactly three pools");
      }
    }

    @Override
    public String type() {
      return "award_podium";
    }
  }

  public record PodiumPool(List<LootItem> items) {
    public PodiumPool {
      items = List.copyOf(items);
    }
  }

  public record LootItem(String material, int amount, int chance) {}

  public record SendMessageAction(String message) implements ActionDefinition {
    @Override
    public String type() {
      return "send_message";
    }
  }

  public record BroadcastMessageAction(String message) implements ActionDefinition {
    @Override
    public String type() {
      return "broadcast_message";
    }
  }

  public record ShowTitleAction(String title, String subtitle, int fadeIn, int stay, int fadeOut)
      implements ActionDefinition {
    @Override
    public String type() {
      return "show_title";
    }
  }

  public record SpawnBossAction(
      int offsetX, int offsetY, int offsetZ, String entityType, String name)
      implements ActionDefinition {
    @Override
    public String type() {
      return "spawn_boss";
    }
  }

  public record SpawnMobsAction(String waveId, List<MobSpawn> mobs) implements ActionDefinition {
    public SpawnMobsAction {
      if (waveId == null) {
        waveId = "";
      }
      mobs = List.copyOf(mobs);
    }

    public SpawnMobsAction(List<MobSpawn> mobs) {
      this("", mobs);
    }

    @Override
    public String type() {
      return "spawn_mobs";
    }
  }

  public record MobSpawn(String entityType, String name, int amount, double radius) {}

  public record ShowBossbarAction(String color, String style, String title, String progress)
      implements ActionDefinition {
    @Override
    public String type() {
      return "show_bossbar";
    }
  }

  public record UpdateBossbarAction(String title, String progress) implements ActionDefinition {
    @Override
    public String type() {
      return "update_bossbar";
    }
  }

  public record RemoveBossbarAction() implements ActionDefinition {
    @Override
    public String type() {
      return "remove_bossbar";
    }
  }

  public record ShowScoreboardAction(String title, List<String> lines) implements ActionDefinition {
    public ShowScoreboardAction {
      lines = List.copyOf(lines);
    }

    @Override
    public String type() {
      return "show_scoreboard";
    }
  }

  public record UpdateScoreboardAction(List<String> lines) implements ActionDefinition {
    public UpdateScoreboardAction {
      lines = List.copyOf(lines);
    }

    @Override
    public String type() {
      return "update_scoreboard";
    }
  }

  public record RemoveScoreboardAction() implements ActionDefinition {
    @Override
    public String type() {
      return "remove_scoreboard";
    }
  }
}
