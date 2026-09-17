package dev.spectraevents.core.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

class CoreArchitectureTest {
  private static final JavaClasses CORE_CLASSES =
      new ClassFileImporter().importPackages("dev.spectraevents.core");

  @Test
  void coreDoesNotDependOnMinecraftOrApplicationOrPlatform() {
    ArchRule rule =
        noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "org.bukkit..",
                "io.papermc.paper..",
                "net.minecraft..",
                "dev.spectraevents.application..",
                "dev.spectraevents.adapter..",
                "dev.spectraevents.platform..")
            .because("Core module must be independent of application, adapters, and platforms");

    rule.check(CORE_CLASSES);
  }
}
