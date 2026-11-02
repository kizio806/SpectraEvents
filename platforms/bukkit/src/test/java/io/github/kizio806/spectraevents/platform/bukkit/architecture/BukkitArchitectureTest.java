package io.github.kizio806.spectraevents.platform.bukkit.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

class BukkitArchitectureTest {

  private static final JavaClasses BUKKIT_CLASSES =
      new ClassFileImporter().importPackages("io.github.kizio806.spectraevents.platform.bukkit");

  @Test
  void bukkitSharedLayerMustNotDependOnPaperSpecificApis() {
    ArchRule rule =
        noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("io.papermc.paper..", "com.destroystokyo.paper..")
            .because("Shared Bukkit layer must maintain 100% Spigot and CraftBukkit compatibility");

    rule.check(BUKKIT_CLASSES);
  }

  @Test
  void bukkitSharedLayerMustNotDependOnCraftBukkitNms() {
    ArchRule rule =
        noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("net.minecraft.server..", "org.bukkit.craftbukkit..")
            .because("Platform shared layer must use clean Bukkit abstractions without NMS hacks");

    rule.check(BUKKIT_CLASSES);
  }

  @Test
  void bukkitSharedLayerMustNotDependOnPaperOrSpigotSpecificModules() {
    ArchRule rule =
        noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "io.github.kizio806.spectraevents.platform.paper..",
                "io.github.kizio806.spectraevents.platform.spigot..")
            .because(
                "Bukkit shared layer is a base for Paper and Spigot and cannot depend on them");

    rule.check(BUKKIT_CLASSES);
  }
}
