package io.github.kizio806.spectraevents.platform.bukkit.bossbar;

import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import java.util.Objects;

/**
 * Platform-independent utility for calculating boss bar progress from event runtime state.
 *
 * <p>Supports percentage strings (e.g. "0.75"), hit counter progress ("hits"), and health ratios.
 * Values are clamped to the inclusive interval [0.0, 1.0].
 */
public final class BukkitBossBarProgressCalculator {

  private BukkitBossBarProgressCalculator() {}

  /**
   * Calculates the boss bar progress as a clamped float [0.0f, 1.0f] for Adventure BossBar.
   *
   * @param state the current event runtime state
   * @param progressMode the progress calculation mode or string
   * @return clamped progress value between 0.0f and 1.0f
   */
  public static float calculateFloat(EventRuntimeState state, String progressMode) {
    return (float) calculateDouble(state, progressMode);
  }

  /**
   * Calculates the boss bar progress as a clamped double [0.0, 1.0] for Bukkit BossBar.
   *
   * @param state the current event runtime state
   * @param progressMode the progress calculation mode or string
   * @return clamped progress value between 0.0 and 1.0
   */
  public static double calculateDouble(EventRuntimeState state, String progressMode) {
    Objects.requireNonNull(state, "state cannot be null");

    if ("hits".equalsIgnoreCase(progressMode) && state.hitCounter() != null) {
      var counter = state.hitCounter();
      if (counter.maximum() > 0) {
        return clamp((double) counter.current() / (double) counter.maximum());
      }
      return 1.0;
    }

    if (progressMode != null && !progressMode.isBlank()) {
      try {
        double parsed = Double.parseDouble(progressMode);
        return clamp(parsed);
      } catch (NumberFormatException ignored) {
        // Fallback to health calculation
      }
    }

    if (state.maxHealth() > 0) {
      return clamp((double) state.currentHealth() / (double) state.maxHealth());
    }

    return 1.0;
  }

  private static double clamp(double value) {
    if (Double.isNaN(value) || value < 0.0) {
      return 0.0;
    }
    if (value > 1.0) {
      return 1.0;
    }
    return value;
  }
}
