package io.github.kizio806.spectraevents.core.gameplay.hits;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HitCounterTest {

  @Test
  void recordsEqualValueHitsUntilTheTargetIsReached() {
    HitCounter counter = HitCounter.startingAt(3);

    HitCounter afterOneHit = counter.addHits(1);
    assertEquals(1, afterOneHit.current());
    assertFalse(afterOneHit.isReached());

    HitCounter reached = afterOneHit.addHits(2);
    assertEquals(3, reached.current());
    assertTrue(reached.isReached());
    assertEquals(3, reached.addHits(1).current());
  }

  @Test
  void rejectsInvalidCounterValuesAndHitAmounts() {
    assertThrows(IllegalArgumentException.class, () -> new HitCounter(-1, 1));
    assertThrows(IllegalArgumentException.class, () -> HitCounter.startingAt(0));
    assertThrows(IllegalArgumentException.class, () -> HitCounter.startingAt(1).addHits(0));
  }
}
