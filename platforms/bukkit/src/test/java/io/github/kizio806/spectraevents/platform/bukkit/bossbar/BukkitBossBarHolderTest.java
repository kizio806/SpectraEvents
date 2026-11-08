package io.github.kizio806.spectraevents.platform.bukkit.bossbar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BukkitBossBarHolderTest {

  @Test
  void constructsAndGetsProperties() {
    String dummyBar = "bar1";
    BukkitBossBarHolder<String> holder =
        new BukkitBossBarHolder<>(dummyBar, "Title <event>", "health");

    assertEquals("bar1", holder.bossBar());
    assertEquals("Title <event>", holder.titleTemplate());
    assertEquals("health", holder.progressMode());
  }

  @Test
  void mutatesTitleAndProgressMode() {
    String dummyBar = "bar2";
    BukkitBossBarHolder<String> holder = new BukkitBossBarHolder<>(dummyBar, "Title A", "health");

    holder.setTitleTemplate("Title B");
    assertEquals("Title B", holder.titleTemplate());

    holder.setProgressMode("hits");
    assertEquals("hits", holder.progressMode());
  }

  @Test
  void enforcesNonNullInvariants() {
    assertThrows(
        NullPointerException.class, () -> new BukkitBossBarHolder<>(null, "title", "health"));
    assertThrows(
        NullPointerException.class, () -> new BukkitBossBarHolder<>("bar", null, "health"));
    assertThrows(NullPointerException.class, () -> new BukkitBossBarHolder<>("bar", "title", null));

    BukkitBossBarHolder<String> holder = new BukkitBossBarHolder<>("bar", "title", "health");

    assertThrows(NullPointerException.class, () -> holder.setTitleTemplate(null));
    assertThrows(NullPointerException.class, () -> holder.setProgressMode(null));
  }
}
