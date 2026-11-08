package io.github.kizio806.spectraevents.platform.bukkit.bossbar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.gameplay.health.Health;
import io.github.kizio806.spectraevents.core.gameplay.hits.HitCounter;
import org.junit.jupiter.api.Test;

class BukkitBossBarProgressCalculatorTest {

  private EventRuntimeState createState(int currentHealth, int maxHealth, HitCounter counter) {
    EventRuntimeState state = new EventRuntimeState(EventInstanceId.generate());
    if (maxHealth > 0) {
      state.setHealth(new Health(currentHealth, maxHealth));
    }
    if (counter != null) {
      state.setHitCounter(counter);
    }
    return state;
  }

  @Test
  void calculatesHealthProgress() {
    EventRuntimeState state = createState(50, 100, null);

    assertEquals(0.5, BukkitBossBarProgressCalculator.calculateDouble(state, null), 1e-6);
    assertEquals(0.5f, BukkitBossBarProgressCalculator.calculateFloat(state, null), 1e-6f);
  }

  @Test
  void calculatesHitCounterProgress() {
    HitCounter counter = new HitCounter(3, 10);
    EventRuntimeState state = createState(100, 100, counter);

    assertEquals(0.3, BukkitBossBarProgressCalculator.calculateDouble(state, "hits"), 1e-6);
    assertEquals(0.3f, BukkitBossBarProgressCalculator.calculateFloat(state, "hits"), 1e-6f);
  }

  @Test
  void parsesNumericProgressString() {
    EventRuntimeState state = createState(100, 100, null);

    assertEquals(0.85, BukkitBossBarProgressCalculator.calculateDouble(state, "0.85"), 1e-6);
    assertEquals(0.85f, BukkitBossBarProgressCalculator.calculateFloat(state, "0.85"), 1e-6f);
  }

  @Test
  void clampsValuesOutsideRange() {
    EventRuntimeState state = createState(100, 100, null);

    assertEquals(1.0, BukkitBossBarProgressCalculator.calculateDouble(state, null), 1e-6);
    assertEquals(0.0, BukkitBossBarProgressCalculator.calculateDouble(state, "-0.5"), 1e-6);
    assertEquals(1.0, BukkitBossBarProgressCalculator.calculateDouble(state, "1.5"), 1e-6);
  }

  @Test
  void fallsBackToHealthWhenNumericParseFails() {
    EventRuntimeState state = createState(75, 100, null);

    assertEquals(
        0.75, BukkitBossBarProgressCalculator.calculateDouble(state, "invalid_number"), 1e-6);
  }

  @Test
  void returnsOneWhenNoHealthConfigured() {
    EventRuntimeState state = createState(0, 0, null);

    assertEquals(1.0, BukkitBossBarProgressCalculator.calculateDouble(state, null), 1e-6);
  }

  @Test
  void handlesSpecialDoubleStringsNaNAndInfinity() {
    EventRuntimeState state = createState(100, 100, null);

    assertEquals(0.0, BukkitBossBarProgressCalculator.calculateDouble(state, "NaN"), 1e-6);
    assertEquals(1.0, BukkitBossBarProgressCalculator.calculateDouble(state, "Infinity"), 1e-6);
    assertEquals(0.0, BukkitBossBarProgressCalculator.calculateDouble(state, "-Infinity"), 1e-6);
  }

  @Test
  void handlesHitCounterBoundaries() {
    EventRuntimeState zeroHitsState = createState(100, 100, new HitCounter(0, 10));
    assertEquals(0.0, BukkitBossBarProgressCalculator.calculateDouble(zeroHitsState, "hits"), 1e-6);

    EventRuntimeState fullHitsState = createState(100, 100, new HitCounter(10, 10));
    assertEquals(1.0, BukkitBossBarProgressCalculator.calculateDouble(fullHitsState, "hits"), 1e-6);

    EventRuntimeState hitsWithoutCounter = createState(50, 100, null);
    assertEquals(
        0.5, BukkitBossBarProgressCalculator.calculateDouble(hitsWithoutCounter, "hits"), 1e-6);
  }

  @Test
  void handlesHealthBoundaries() {
    EventRuntimeState zeroHealth = createState(0, 100, null);
    assertEquals(0.0, BukkitBossBarProgressCalculator.calculateDouble(zeroHealth, null), 1e-6);

    EventRuntimeState fullHealth = createState(100, 100, null);
    assertEquals(1.0, BukkitBossBarProgressCalculator.calculateDouble(fullHealth, null), 1e-6);
  }

  @Test
  void handlesEmptyAndWhitespaceProgressModes() {
    EventRuntimeState state = createState(40, 100, null);

    assertEquals(0.4, BukkitBossBarProgressCalculator.calculateDouble(state, ""), 1e-6);
    assertEquals(0.4, BukkitBossBarProgressCalculator.calculateDouble(state, "   "), 1e-6);
  }

  @Test
  void throwsOnNullState() {
    assertThrows(
        NullPointerException.class,
        () -> BukkitBossBarProgressCalculator.calculateDouble(null, "hits"));
  }
}
