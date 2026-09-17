package io.github.kizio806.spectraevents.core.gameplay.health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HealthTest {

  @Test
  void shouldInitializeCorrectly() {
    Health health = new Health(20, 20);
    assertEquals(20, health.current());
    assertEquals(20, health.max());
    assertFalse(health.isDepleted());
  }

  @Test
  void shouldRejectInvalidMax() {
    assertThrows(IllegalArgumentException.class, () -> new Health(0, 0));
    assertThrows(IllegalArgumentException.class, () -> new Health(-5, -5));
  }

  @Test
  void shouldRejectInvalidCurrent() {
    assertThrows(IllegalArgumentException.class, () -> new Health(-1, 20));
    assertThrows(IllegalArgumentException.class, () -> new Health(21, 20));
  }

  @Test
  void shouldApplyDamage() {
    Health health = new Health(20, 20);
    Health damaged = health.damage(5);

    assertEquals(15, damaged.current());
    assertEquals(20, damaged.max());
  }

  @Test
  void shouldClampDamageAtZero() {
    Health health = new Health(5, 20);
    Health damaged = health.damage(10);

    assertEquals(0, damaged.current());
    assertTrue(damaged.isDepleted());
  }

  @Test
  void shouldRejectNegativeDamage() {
    Health health = new Health(20, 20);
    assertThrows(IllegalArgumentException.class, () -> health.damage(-5));
  }
}
