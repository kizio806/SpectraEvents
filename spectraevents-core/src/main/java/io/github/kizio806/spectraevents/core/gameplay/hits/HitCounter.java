package io.github.kizio806.spectraevents.core.gameplay.hits;

/**
 * Immutable counter for interaction-driven events where every accepted interaction has equal value.
 *
 * @param current number of recorded hits
 * @param maximum number of hits required to reach the target
 */
public record HitCounter(int current, int maximum) {

  public HitCounter {
    if (maximum <= 0) {
      throw new IllegalArgumentException("Hit counter maximum must be greater than 0.");
    }
    if (current < 0 || current > maximum) {
      throw new IllegalArgumentException("Hit counter current value must be between 0 and maximum.");
    }
  }

  /** Creates an empty counter with the supplied completion target. */
  public static HitCounter startingAt(int maximum) {
    return new HitCounter(0, maximum);
  }

  /** Records one or more equal-value hits, clamping at the configured maximum. */
  public HitCounter addHits(int amount) {
    if (amount <= 0) {
      throw new IllegalArgumentException("Hit counter amount must be greater than 0.");
    }
    return new HitCounter(Math.min(maximum, Math.addExact(current, amount)), maximum);
  }

  /** Returns whether the configured target has been reached. */
  public boolean isReached() {
    return current == maximum;
  }
}
