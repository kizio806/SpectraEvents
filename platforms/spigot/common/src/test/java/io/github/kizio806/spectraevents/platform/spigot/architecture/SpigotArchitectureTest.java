package io.github.kizio806.spectraevents.platform.spigot.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

class SpigotArchitectureTest {

  private static final JavaClasses SPIGOT_CLASSES =
      new ClassFileImporter().importPackages("io.github.kizio806.spectraevents.platform.spigot");

  @Test
  void spigotModuleMustNotDependOnPaperApi() {
    ArchRule rule =
        noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("io.papermc.paper..", "com.destroystokyo.paper..")
            .because("Spigot module must maintain pure CraftBukkit and Spigot compatibility");

    rule.check(SPIGOT_CLASSES);
  }

  @Test
  void spigotModuleMustNotDependOnPaperModule() {
    ArchRule rule =
        noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("io.github.kizio806.spectraevents.platform.paper..")
            .because("Spigot module must remain completely isolated from Paper platform module");

    rule.check(SPIGOT_CLASSES);
  }

  @Test
  void spigotModuleMustNotDependOnCraftBukkitNms() {
    ArchRule rule =
        noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("net.minecraft.server..", "org.bukkit.craftbukkit..")
            .because("Spigot adapter must use standard Spigot/Bukkit APIs without NMS hacks");

    rule.check(SPIGOT_CLASSES);
  }
}
