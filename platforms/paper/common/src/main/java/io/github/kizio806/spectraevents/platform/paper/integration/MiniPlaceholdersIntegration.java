package io.github.kizio806.spectraevents.platform.paper.integration;

import io.github.miniplaceholders.api.MiniPlaceholders;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import org.bukkit.Bukkit;

public final class MiniPlaceholdersIntegration {
  private MiniPlaceholdersIntegration() {}

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
    static final MiniMessage INSTANCE = createMiniMessage(getGlobalResolver());
  }

  public static MiniMessage getMiniMessage() {
    return MiniMessageHolder.INSTANCE;
  }

  static MiniMessage createMiniMessage(TagResolver externalPlaceholders) {
    return MiniMessage.builder()
        .tags(TagResolver.resolver(StandardTags.defaults(), externalPlaceholders))
        .build();
  }
}
