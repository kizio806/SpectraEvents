package org.bukkit.command;

import org.bukkit.plugin.Plugin;

/** Test helper to construct Bukkit {@link PluginCommand} instances without reflection. */
public final class MockPluginCommandFactory {

  private MockPluginCommandFactory() {}

  public static PluginCommand create(String name, Plugin owner) {
    return new PluginCommand(name, owner);
  }
}
