package dev.spectraevents.core.gameplay.contribution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DamageContributionTest {

  @Test
  void testAddDamageAccumulates() {
    DamageContribution contrib = DamageContribution.empty();
    UUID player1 = UUID.randomUUID();

    contrib = contrib.addDamage(player1, 10);
    contrib = contrib.addDamage(player1, 15);

    assertEquals(1, contrib.contributions().size());
    assertEquals(25, contrib.contributions().get(player1));
  }

  @Test
  void testGetTopContributors() {
    DamageContribution contrib = DamageContribution.empty();
    UUID p1 = UUID.randomUUID();
    UUID p2 = UUID.randomUUID();
    UUID p3 = UUID.randomUUID();

    contrib = contrib.addDamage(p1, 50);
    contrib = contrib.addDamage(p2, 100);
    contrib = contrib.addDamage(p3, 10);

    List<Map.Entry<UUID, Integer>> top = contrib.getTopContributors(2);

    assertEquals(2, top.size());
    assertEquals(p2, top.get(0).getKey());
    assertEquals(100, top.get(0).getValue());
    assertEquals(p1, top.get(1).getKey());
    assertEquals(50, top.get(1).getValue());
  }

  @Test
  void testImmutability() {
    DamageContribution contrib = DamageContribution.empty();
    UUID p1 = UUID.randomUUID();
    DamageContribution newContrib = contrib.addDamage(p1, 10);

    assertTrue(contrib.contributions().isEmpty());
    assertEquals(1, newContrib.contributions().size());
  }
}
