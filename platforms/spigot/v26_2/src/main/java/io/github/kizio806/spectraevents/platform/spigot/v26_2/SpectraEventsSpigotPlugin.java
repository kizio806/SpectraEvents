package io.github.kizio806.spectraevents.platform.spigot.v26_2;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.plugin.java.JavaPlugin;

public class SpectraEventsSpigotPlugin extends JavaPlugin {
  private BukkitAudiences adventure;
  private SpigotBootstrap bootstrap;

  @Override
  public void onEnable() {
    this.adventure = BukkitAudiences.create(this);
    this.bootstrap = new SpigotBootstrap(this, adventure);
    this.bootstrap.onEnable();
  }

  @Override
  public void onDisable() {
    if (this.bootstrap != null) {
      this.bootstrap.onDisable();
    }
    if (this.adventure != null) {
      this.adventure.close();
      this.adventure = null;
    }
  }
}
