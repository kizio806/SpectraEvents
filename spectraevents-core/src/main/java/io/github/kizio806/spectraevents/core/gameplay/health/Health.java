package io.github.kizio806.spectraevents.core.gameplay.health;

/**
 * Immutable primitive representing health state for combat-oriented events. Ensures invariant rules
 * (e.g., 0 &lt;= current &lt;= max, non-negative damage).
 *
 * @param current the current health amount
 * @param max the maximum health amount
 */
public record Health(int current, int max) {

  public Health {
    if (max <= 0) {
      throw new IllegalArgumentException("Health max must be greater than 0.");
    }
    if (current < 0 || current > max) {
      throw new IllegalArgumentException("Health current must be between 0 and max.");
    }
  }

  /**
   * Applies damage and returns a new Health state clamped at 0.
   *
   * @param amount the damage to apply
   * @return the new Health state
   * @throws IllegalArgumentException if damage amount is negative
   */
  public Health damage(int amount) {
    if (amount < 0) {
      throw new IllegalArgumentException("Damage amount cannot be negative.");
    }
    return new Health(Math.max(0, current - amount), max);
  }

  /**
   * Checks if the health is fully depleted.
   *
   * @return true if current health is 0
   */
  public boolean isDepleted() {
    return current == 0;
  }
}
