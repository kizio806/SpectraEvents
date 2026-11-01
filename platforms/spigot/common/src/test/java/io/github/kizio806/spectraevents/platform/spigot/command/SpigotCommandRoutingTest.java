package io.github.kizio806.spectraevents.platform.spigot.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SpigotCommandRoutingTest {

  private SpigotCommandHelper helper;
  private List<String> sentMessages;

  static class DummySender implements CommandSender {
    final List<String> messages;
    boolean op;
    boolean adminPerm;
    boolean specificPerm;

    DummySender(List<String> messages) {
      this.messages = messages;
    }

    @Override
    public void sendMessage(String message) {
      messages.add(message);
    }

    @Override
    public void sendMessage(String... msgs) {
      messages.addAll(List.of(msgs));
    }

    @Override
    public void sendMessage(UUID sender, String message) {
      messages.add(message);
    }

    @Override
    public void sendMessage(UUID sender, String... msgs) {
      messages.addAll(List.of(msgs));
    }

    @Override
    public boolean isOp() {
      return op;
    }

    @Override
    public void setOp(boolean value) {
      this.op = value;
    }

    @Override
    public boolean hasPermission(String name) {
      if (op) {
        return true;
      }
      if ("spectraevents.admin".equals(name)) {
        return adminPerm;
      }
      return specificPerm;
    }

    @Override
    public boolean hasPermission(org.bukkit.permissions.Permission perm) {
      return hasPermission(perm.getName());
    }

    @Override
    public org.bukkit.Server getServer() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getName() {
      return "Console";
    }

    @Override
    public CommandSender.Spigot spigot() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isPermissionSet(String name) {
      return true;
    }

    @Override
    public boolean isPermissionSet(org.bukkit.permissions.Permission perm) {
      return true;
    }

    @Override
    public org.bukkit.permissions.PermissionAttachment addAttachment(
        org.bukkit.plugin.Plugin plugin, String name, boolean value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public org.bukkit.permissions.PermissionAttachment addAttachment(
        org.bukkit.plugin.Plugin plugin) {
      throw new UnsupportedOperationException();
    }

    @Override
    public org.bukkit.permissions.PermissionAttachment addAttachment(
        org.bukkit.plugin.Plugin plugin, String name, boolean value, int ticks) {
      throw new UnsupportedOperationException();
    }

    @Override
    public org.bukkit.permissions.PermissionAttachment addAttachment(
        org.bukkit.plugin.Plugin plugin, int ticks) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void removeAttachment(org.bukkit.permissions.PermissionAttachment attachment) {}

    @Override
    public void recalculatePermissions() {}

    @Override
    public java.util.Set<org.bukkit.permissions.PermissionAttachmentInfo>
        getEffectivePermissions() {
      return java.util.Set.of();
    }
  }

  @BeforeEach
  void setUp() {
    sentMessages = new ArrayList<>();
    LocaleCatalog locales =
        LocaleCatalog.of(
            Map.of(
                "command.spigot.help-general", "General Help",
                "command.spigot.help-events", "Events Help",
                "command.spigot.help-rewards", "Rewards Help",
                "command.spigot.help-definitions", "Definitions Help",
                "command.spigot.help-schedules", "Schedules Help",
                "command.spigot.failed", "Failed: %reason%"));
    helper = new SpigotCommandHelper(locales);
  }

  @Test
  void shouldFilterMatchesCorrectly() {
    List<String> results = helper.matches("st", "start", "stop", "status", "list", "doctor");
    assertEquals(List.of("start", "stop", "status"), results);

    List<String> empty = helper.matches("xyz", "start", "stop");
    assertTrue(empty.isEmpty());
  }

  @Test
  void shouldEnforcePermissions() {
    DummySender sender = new DummySender(sentMessages);
    sender.op = false;
    sender.adminPerm = false;
    sender.specificPerm = false;

    assertThrows(
        IllegalStateException.class, () -> helper.require(sender, "spectraevents.event.start"));

    sender.specificPerm = true;
    helper.require(sender, "spectraevents.event.start");

    sender.specificPerm = false;
    sender.adminPerm = true;
    helper.require(sender, "spectraevents.event.start");

    sender.adminPerm = false;
    sender.op = true;
    helper.require(sender, "spectraevents.event.start");
  }

  @Test
  void shouldRequireArguments() {
    helper.requireArguments(new String[] {"arg1", "arg2"}, 2, "/cmd <a1> <a2>");
    assertThrows(
        IllegalArgumentException.class,
        () -> helper.requireArguments(new String[] {"arg1"}, 2, "/cmd <a1> <a2>"));
  }

  @Test
  void shouldSendLocalizedMessages() {
    DummySender sender = new DummySender(sentMessages);
    helper.send(sender, "command.spigot.help-general");
    assertEquals(1, sentMessages.size());
    assertEquals("General Help", sentMessages.getFirst());
  }

  @Test
  void shouldFormatThrowableMessage() {
    RuntimeException ex = new RuntimeException("Custom error");
    assertEquals("Custom error", helper.messageFor(ex));

    RuntimeException wrapped = new RuntimeException(new IllegalStateException("Root cause"));
    assertEquals("Root cause", helper.messageFor(wrapped));
  }
}
