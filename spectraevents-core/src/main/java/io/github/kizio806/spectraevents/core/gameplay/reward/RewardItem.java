package io.github.kizio806.spectraevents.core.gameplay.reward;

/** Immutable, platform-neutral item stack held by a durable reward claim. */
public record RewardItem(String material, int amount) {
  public RewardItem {
    if (material == null || material.isBlank() || amount < 1) {
      throw new IllegalArgumentException("Reward item requires a material and positive amount");
    }
  }
}
