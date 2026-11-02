package io.github.kizio806.spectraevents.application.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.core.event.execution.action.CoreActions;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.gameplay.health.Health;
import io.github.kizio806.spectraevents.core.gameplay.hits.HitCounter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventRuntimeStateTest {

  @Test
  void testHealthAndHitCounter() {
    EventInstanceId id = EventInstanceId.generate();
    EventRuntimeState state = new EventRuntimeState(id);

    assertEquals(id, state.instanceId());
    assertFalse(state.health().isPresent());
    assertEquals(0, state.currentHealth());
    assertEquals(0, state.maxHealth());

    Health health = new Health(100, 100);
    state.setHealth(health);
    assertTrue(state.health().isPresent());
    assertEquals(100, state.currentHealth());
    assertEquals(100, state.maxHealth());

    Health updated = state.updateHealth(h -> h.damage(25));
    assertEquals(75, updated.current());
    assertEquals(75, state.currentHealth());

    assertNull(state.hitCounter());
    HitCounter hitCounter = HitCounter.startingAt(10);
    state.setHitCounter(hitCounter);
    assertEquals(10, state.hitCounter().maximum());

    HitCounter updatedHits = state.updateHitCounter(c -> c.addHits(1));
    assertEquals(1, updatedHits.current());
  }

  @Test
  void testLockAndDeadlines() {
    EventRuntimeState state = new EventRuntimeState(EventInstanceId.generate());

    assertFalse(state.isLocked());
    state.setLockedUntilMillis(System.currentTimeMillis() + 100_000L);
    assertTrue(state.isLocked());
    assertEquals(state.lockedUntilMillis(), state.lockedUntilMillis());

    // Timer deadlines
    assertEquals(0L, state.timerDeadlineMillis());
    state.setTimerDeadlineMillis("timer1", 1000L);
    state.setTimerDeadlineMillis("timer2", 2000L);
    assertEquals(1000L, state.timerDeadlineMillis("timer1"));
    assertEquals(2000L, state.timerDeadlineMillis("timer2"));
    assertEquals(1000L, state.timerDeadlineMillis()); // Earliest
    assertEquals(2, state.timerDeadlines().size());

    state.removeTimerDeadline("timer1");
    assertEquals(2000L, state.timerDeadlineMillis());

    state.clearTimerDeadlines();
    assertEquals(0L, state.timerDeadlineMillis());

    state.setTimerDeadlineMillis(5000L);
    assertEquals(5000L, state.timerDeadlineMillis());

    // Encounter deadline and minimum contribution
    state.setEncounterDeadlineMillis(123456L);
    assertEquals(123456L, state.encounterDeadlineMillis());

    assertEquals(0, state.minimumContribution());
    state.setMinimumContribution(50);
    assertEquals(50, state.minimumContribution());
    assertThrows(IllegalArgumentException.class, () -> state.setMinimumContribution(-1));
  }

  @Test
  void testZoneLocationAndClaiming() {
    EventRuntimeState state = new EventRuntimeState(EventInstanceId.generate());

    assertFalse(state.eventZone().isPresent());
    EventZone zone = new EventZone("world", 10.0, 30.0, 15.0);
    state.setEventZone(zone);
    assertTrue(state.eventZone().isPresent());
    assertEquals(zone, state.eventZone().get());

    assertFalse(state.platformLocation().isPresent());
    Object dummyLoc = new Object();
    state.setPlatformLocation(dummyLoc);
    assertTrue(state.platformLocation().isPresent());
    assertEquals(dummyLoc, state.platformLocation().get());

    assertFalse(state.isClaimed());
    assertNull(state.claimant());
    assertTrue(state.tryClaim("player1"));
    assertTrue(state.isClaimed());
    assertEquals("player1", state.claimant());

    // Same claimant succeeds, different claimant fails
    assertTrue(state.tryClaim("player1"));
    assertFalse(state.tryClaim("player2"));
  }

  @Test
  void testContributionAndParticipants() {
    EventRuntimeState state = new EventRuntimeState(EventInstanceId.generate());
    UUID player1 = UUID.randomUUID();
    UUID player2 = UUID.randomUUID();

    state.recordParticipant(player1);
    assertEquals(Set.of(player1), state.participants());

    state.recordDamage(player1, 50, 100);
    state.recordDamage(player2, 120, 100);

    assertEquals(2, state.participants().size());
    assertEquals(50, state.contribution().contributions().get(player1));
    assertEquals(120, state.contribution().contributions().get(player2));
    assertFalse(state.contributionThresholdMillis().containsKey(player1));
    assertTrue(state.contributionThresholdMillis().containsKey(player2));

    // Anti-spam combat damage cooldown
    assertTrue(state.tryAcceptCombatDamage(player1, 1000L));
    assertFalse(state.tryAcceptCombatDamage(player1, 1000L));
    assertTrue(state.tryAcceptCombatDamage(player2, 0L));
  }

  @Test
  void testWaveTracking() {
    EventRuntimeState state = new EventRuntimeState(EventInstanceId.generate());
    UUID mob1 = UUID.randomUUID();
    UUID mob2 = UUID.randomUUID();

    state.beginWave("wave1");
    assertThrows(IllegalStateException.class, () -> state.beginWave("wave1"));

    state.trackWaveEntity("wave1", mob1);
    state.trackWaveEntity("wave1", mob2);
    assertEquals(2, state.trackedWaveEntityCount());

    assertThrows(
        IllegalStateException.class,
        () -> state.trackWaveEntity("unknown_wave", UUID.randomUUID()));

    assertFalse(state.recordWaveEntityDeath("wave1", mob1)); // 1 remaining
    assertTrue(state.recordWaveEntityDeath("wave1", mob2)); // 0 remaining, wave cleared!
    assertEquals(0, state.trackedWaveEntityCount());
  }

  @Test
  void testSharedLoot() {
    EventRuntimeState state = new EventRuntimeState(EventInstanceId.generate());

    assertFalse(state.sharedLootInitialized());
    assertTrue(state.sharedLootSnapshot().isEmpty());

    CoreActions.LootStack item1 = new CoreActions.LootStack("diamond", 5);
    CoreActions.LootStack item2 = new CoreActions.LootStack("gold_ingot", 10);

    assertTrue(state.initializeSharedLoot(List.of(item1, item2)));
    assertFalse(state.initializeSharedLoot(List.of(item1))); // Can only initialize once
    assertTrue(state.sharedLootInitialized());
    assertEquals(2, state.sharedLootSnapshot().size());
    assertFalse(state.sharedLootEmpty());

    var slot0 = state.takeSharedLootSlot(0);
    assertTrue(slot0.isPresent());
    assertEquals(item1, slot0.get());
    assertEquals(1, state.sharedLootSnapshot().size());

    // Restore slot
    assertTrue(state.restoreSharedLootSlot(0, item1));
    assertEquals(2, state.sharedLootSnapshot().size());

    state.takeSharedLootSlot(0);
    state.takeSharedLootSlot(1);
    assertTrue(state.sharedLootEmpty());

    // Boss entity ID
    assertFalse(state.bossEntityId().isPresent());
    state.setBossEntityId("boss_uuid");
    assertEquals("boss_uuid", state.bossEntityId().get());
  }
}
