package io.github.kizio806.spectraevents.core.gameplay.health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class HealthThresholdsTest {

  @Test
  void testCrossSingleThresholdExactlyOnce() {
    HealthThresholds thresholds = new HealthThresholds(List.of(60));

    Health oldHealth = new Health(100, 100);
    Health newHealth = new Health(60, 100);

    // Drop to 60 triggers enraged
    List<Integer> crossed = thresholds.checkCrossed(oldHealth, newHealth);
    assertEquals(1, crossed.size());
    assertEquals(60, crossed.get(0));

    // Drop from 60 to 50 should NOT trigger again
    Health newerHealth = new Health(50, 100);
    List<Integer> crossedAgain = thresholds.checkCrossed(newHealth, newerHealth);
    assertTrue(crossedAgain.isEmpty());
  }

  @Test
  void testCrossMultipleThresholdsAtOnce() {
    HealthThresholds thresholds = new HealthThresholds(List.of(60, 25));

    Health oldHealth = new Health(100, 100);
    Health newHealth = new Health(10, 100);

    // Massive damage crossing both thresholds
    List<Integer> crossed = thresholds.checkCrossed(oldHealth, newHealth);
    assertEquals(2, crossed.size());
    assertEquals(60, crossed.get(0)); // Should trigger highest first
    assertEquals(25, crossed.get(1));
  }

  @Test
  void testDoesNotTriggerIfNeverAbove() {
    HealthThresholds thresholds = new HealthThresholds(List.of(60));

    // Start at 50, which is already below the 60 threshold
    Health oldHealth = new Health(50, 100);
    Health newHealth = new Health(40, 100);

    List<Integer> crossed = thresholds.checkCrossed(oldHealth, newHealth);
    assertTrue(crossed.isEmpty());
  }
}
