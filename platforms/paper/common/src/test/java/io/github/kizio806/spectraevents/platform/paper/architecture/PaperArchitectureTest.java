package io.github.kizio806.spectraevents.platform.paper.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

class PaperArchitectureTest {

  private static final JavaClasses PAPER_CLASSES =
      new ClassFileImporter().importPackages("io.github.kizio806.spectraevents.platform.paper");

  @Test
  void paperModuleMustNotDependOnSpigotModule() {
    ArchRule rule =
        noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("io.github.kizio806.spectraevents.platform.spigot..")
            .because("Paper module must remain completely isolated from Spigot platform module");

    rule.check(PAPER_CLASSES);
  }

  @Test
  void paperModuleMustNotDependOnCraftBukkitNms() {
    ArchRule rule =
        noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("net.minecraft.server..", "org.bukkit.craftbukkit..")
            .because("Paper adapter must use clean Paper/Bukkit APIs without NMS dependencies");

    rule.check(PAPER_CLASSES);
  }
}
