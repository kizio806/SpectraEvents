package dev.spectraevents.application.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

class ApplicationArchitectureTest {
  private static final JavaClasses APPLICATION_CLASSES =
      new ClassFileImporter().importPackages("dev.spectraevents.application");

  @Test
  void applicationDoesNotDependOnMinecraftPlatforms() {
    ArchRule rule =
        noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "org.bukkit..",
                "io.papermc.paper..",
                "net.minecraft..",
                "dev.spectraevents.adapter..",
                "dev.spectraevents.platform..");

    rule.check(APPLICATION_CLASSES);
  }

  @Test
  void portsDoNotDependOnApplicationImplementation() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("dev.spectraevents.application.port..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("dev.spectraevents.application")
            .because("ports define boundaries and must not know their orchestrators");

    rule.check(APPLICATION_CLASSES);
  }
}
