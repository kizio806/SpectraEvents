package io.github.kizio806.spectraevents.core.event.execution.trigger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class CoreTriggersTest {

  @Test
  void testCoreTriggersInstantiationAndTypes() {
    CoreTriggers.LootItemTakenTrigger lootTaken = new CoreTriggers.LootItemTakenTrigger();
    assertEquals("loot_item_taken", lootTaken.type());

    CoreTriggers.LootContainerEmptiedTrigger containerEmptied =
        new CoreTriggers.LootContainerEmptiedTrigger();
    assertEquals("loot_container_emptied", containerEmptied.type());

    CoreTriggers.WaveClearedTrigger waveCleared = new CoreTriggers.WaveClearedTrigger("wave_1");
    assertEquals("wave_cleared", waveCleared.type());
    assertEquals("wave_1", waveCleared.waveId());
    assertThrows(IllegalArgumentException.class, () -> new CoreTriggers.WaveClearedTrigger(""));
    assertThrows(IllegalArgumentException.class, () -> new CoreTriggers.WaveClearedTrigger(null));

    CoreTriggers.TimerElapsedTrigger timer =
        new CoreTriggers.TimerElapsedTrigger(Duration.ofSeconds(30));
    assertEquals("timer_elapsed", timer.type());
    assertEquals(Duration.ofSeconds(30), timer.duration());

    CoreTriggers.HealthThresholdCrossedTrigger healthThreshold =
        new CoreTriggers.HealthThresholdCrossedTrigger(500);
    assertEquals("health_threshold_crossed", healthThreshold.type());
    assertEquals(500, healthThreshold.threshold());

    CoreTriggers.HealthPercentThresholdCrossedTrigger healthPercent =
        new CoreTriggers.HealthPercentThresholdCrossedTrigger(50);
    assertEquals("health_percent_threshold_crossed", healthPercent.type());
    assertEquals(50, healthPercent.percent());
    assertThrows(
        IllegalArgumentException.class,
        () -> new CoreTriggers.HealthPercentThresholdCrossedTrigger(-1));
    assertThrows(
        IllegalArgumentException.class,
        () -> new CoreTriggers.HealthPercentThresholdCrossedTrigger(101));

    CoreTriggers.CombatDamageTrigger combat = new CoreTriggers.CombatDamageTrigger();
    assertEquals("combat_damage", combat.type());

    CoreTriggers.HealthDepletedTrigger healthDepleted = new CoreTriggers.HealthDepletedTrigger();
    assertEquals("health_depleted", healthDepleted.type());

    CoreTriggers.HitsReachedTrigger hitsReached = new CoreTriggers.HitsReachedTrigger();
    assertEquals("hits_reached", hitsReached.type());

    CoreTriggers.HitsPercentThresholdCrossedTrigger hitsPercent =
        new CoreTriggers.HitsPercentThresholdCrossedTrigger(75);
    assertEquals("hits_percent_threshold_crossed", hitsPercent.type());
    assertEquals(75, hitsPercent.percent());
    assertThrows(
        IllegalArgumentException.class,
        () -> new CoreTriggers.HitsPercentThresholdCrossedTrigger(-1));
    assertThrows(
        IllegalArgumentException.class,
        () -> new CoreTriggers.HitsPercentThresholdCrossedTrigger(101));
  }
}
