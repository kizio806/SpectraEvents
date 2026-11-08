package io.github.kizio806.spectraevents.platform.bukkit.bossbar;

import java.util.Objects;

/**
 * Thread-safe holder for an active BossBar instance and its formatting templates.
 *
 * @param <T> platform-specific boss bar representation (Adventure BossBar or Bukkit BossBar)
 */
public final class BukkitBossBarHolder<T> {

  private final T bossBar;
  private volatile String titleTemplate;
  private volatile String progressMode;

  public BukkitBossBarHolder(T bossBar, String titleTemplate, String progressMode) {
    this.bossBar = Objects.requireNonNull(bossBar, "bossBar cannot be null");
    this.titleTemplate = Objects.requireNonNull(titleTemplate, "titleTemplate cannot be null");
    this.progressMode = Objects.requireNonNull(progressMode, "progressMode cannot be null");
  }

  public T bossBar() {
    return bossBar;
  }

  public String titleTemplate() {
    return titleTemplate;
  }

  public void setTitleTemplate(String titleTemplate) {
    this.titleTemplate = Objects.requireNonNull(titleTemplate, "titleTemplate cannot be null");
  }

  public String progressMode() {
    return progressMode;
  }

  public void setProgressMode(String progressMode) {
    this.progressMode = Objects.requireNonNull(progressMode, "progressMode cannot be null");
  }
}
