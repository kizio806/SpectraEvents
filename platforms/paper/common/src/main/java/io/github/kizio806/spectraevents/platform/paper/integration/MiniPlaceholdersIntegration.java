package io.github.kizio806.spectraevents.platform.paper.integration;

import io.github.miniplaceholders.api.MiniPlaceholders;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;

public class MiniPlaceholdersIntegration {

  public static TagResolver getGlobalResolver() {
    try {
      if (Bukkit.getPluginManager().getPlugin("MiniPlaceholders") != null) {
        return MiniPlaceholders.getGlobalPlaceholders();
      }
    } catch (NoClassDefFoundError | Exception ignored) {
      // Plugin API unavailable or value not applicable — fall through
    }
    return TagResolver.empty();
  }

  private static final class MiniMessageHolder {
    static final MiniMessage INSTANCE = MiniMessage.builder().tags(getGlobalResolver()).build();
  }

  public static MiniMessage getMiniMessage() {
    return MiniMessageHolder.INSTANCE;
  }
}
