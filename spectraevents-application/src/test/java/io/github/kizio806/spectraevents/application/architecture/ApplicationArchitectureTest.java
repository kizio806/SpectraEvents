package io.github.kizio806.spectraevents.application.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

class ApplicationArchitectureTest {
  private static final JavaClasses APPLICATION_CLASSES =
      new ClassFileImporter().importPackages("io.github.kizio806.spectraevents.application");

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
                "io.github.kizio806.spectraevents.adapter..",
                "io.github.kizio806.spectraevents.platform..");

    rule.check(APPLICATION_CLASSES);
  }

  @Test
  void portsDoNotDependOnApplicationImplementation() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("io.github.kizio806.spectraevents.application.port..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("io.github.kizio806.spectraevents.application")
            .because("ports define boundaries and must not know their orchestrators");

    rule.check(APPLICATION_CLASSES);
  }
}
