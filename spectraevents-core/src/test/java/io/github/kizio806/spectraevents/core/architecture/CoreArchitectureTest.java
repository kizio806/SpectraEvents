package io.github.kizio806.spectraevents.core.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

class CoreArchitectureTest {
  private static final JavaClasses CORE_CLASSES =
      new ClassFileImporter().importPackages("io.github.kizio806.spectraevents.core");

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
                "io.github.kizio806.spectraevents.application..",
                "io.github.kizio806.spectraevents.adapter..",
                "io.github.kizio806.spectraevents.platform..")
            .because("Core module must be independent of application, adapters, and platforms");

    rule.check(CORE_CLASSES);
  }
}
