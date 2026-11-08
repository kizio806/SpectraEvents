package io.github.kizio806.spectraevents.platform.paper.command;

import com.mojang.brigadier.CommandDispatcher;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.lang.reflect.Proxy;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

final class PaperCommandTestSupport {
  private PaperCommandTestSupport() {}

  static CommandSender createRecordingSender(List<Component> recordedMessages) {
    return (CommandSender)
        Proxy.newProxyInstance(
            CommandSender.class.getClassLoader(),
            new Class<?>[] {CommandSender.class},
            (p, m, a) -> {
              if ("sendMessage".equals(m.getName())) {
                if (a != null && a.length > 0 && a[0] instanceof Component c) {
                  recordedMessages.add(c);
                }
                return null;
              }
              if ("hasPermission".equals(m.getName())) {
                return true;
              }
              if ("getName".equals(m.getName())) {
                return "MockSender";
              }
              if ("isOp".equals(m.getName())) {
                return true;
              }
              return null;
            });
  }

  static Player createRecordingPlayer(List<Component> recordedMessages, Location location) {
    return (Player)
        Proxy.newProxyInstance(
            Player.class.getClassLoader(),
            new Class<?>[] {Player.class},
            (p, m, a) -> {
              if ("sendMessage".equals(m.getName())) {
                if (a != null && a.length > 0 && a[0] instanceof Component c) {
                  recordedMessages.add(c);
                }
                return null;
              }
              if ("hasPermission".equals(m.getName())) {
                return true;
              }
              if ("getName".equals(m.getName())) {
                return "MockPlayer";
              }
              if ("isOp".equals(m.getName())) {
                return true;
              }
              if ("getLocation".equals(m.getName())) {
                return location;
              }
              if ("getUniqueId".equals(m.getName())) {
                return java.util.UUID.randomUUID();
              }
              return null;
            });
  }

  static CommandSourceStack createMockSource(CommandSender sender, Location location) {
    return (CommandSourceStack)
        Proxy.newProxyInstance(
            CommandSourceStack.class.getClassLoader(),
            new Class<?>[] {CommandSourceStack.class},
            (proxy, method, args) -> {
              if ("getSender".equals(method.getName())) return sender;
              if ("getLocation".equals(method.getName())) return location;
              if ("getExecutor".equals(method.getName())) {
                return sender instanceof Entity e ? e : null;
              }
              return null;
            });
  }

  static CommandDispatcher<CommandSourceStack> createDispatcher() {
    return new CommandDispatcher<>();
  }
}
