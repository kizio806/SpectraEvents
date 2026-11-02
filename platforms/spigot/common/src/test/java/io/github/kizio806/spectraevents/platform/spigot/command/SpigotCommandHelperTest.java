package io.github.kizio806.spectraevents.platform.spigot.command;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kizio806.spectraevents.application.config.locale.FileSystemLocaleLoader;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpigotCommandHelperTest {

  @TempDir Path tempDir;
  private LocaleCatalog locales;
  private SpigotCommandHelper helper;

  @BeforeEach
  void setUp() throws IOException {
    locales = new FileSystemLocaleLoader(tempDir).loadCatalog("en-US");
    helper = new SpigotCommandHelper(locales);
  }

  @Test
  void enforcesPermissionsCorrectly() {
    CommandSender permittedSender =
        (CommandSender)
            Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(),
                new Class<?>[] {CommandSender.class},
                (proxy, method, args) -> {
                  if ("hasPermission".equals(method.getName())) {
                    return "perm.test".equals(args[0]);
                  }
                  if ("isOp".equals(method.getName())) {
                    return false;
                  }
                  return null;
                });

    CommandSender unpermittedSender =
        (CommandSender)
            Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(),
                new Class<?>[] {CommandSender.class},
                (proxy, method, args) -> {
                  if ("hasPermission".equals(method.getName())) {
                    return false;
                  }
                  if ("isOp".equals(method.getName())) {
                    return false;
                  }
                  return null;
                });

    assertDoesNotThrow(() -> helper.require(permittedSender, "perm.test"));
    assertThrows(IllegalStateException.class, () -> helper.require(unpermittedSender, "perm.test"));
  }

  @Test
  void enforcesArgumentCount() {
    String[] args = new String[] {"one", "two"};
    assertDoesNotThrow(() -> helper.requireArguments(args, 2, "/cmd <one> <two>"));
    assertThrows(
        IllegalArgumentException.class,
        () -> helper.requireArguments(args, 3, "/cmd <one> <two> <three>"));
  }

  @Test
  void filtersTabCompletionsCaseInsensitivePrefix() {
    List<String> result = helper.matches("ap", "apple", "banana", "apricot", "cherry");
    assertEquals(List.of("apple", "apricot"), result);
  }
}
