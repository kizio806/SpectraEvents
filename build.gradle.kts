import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.GradleException
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    base
    jacoco
    alias(libs.plugins.spotless)
    alias(libs.plugins.sonarqube)
}

group = providers.gradleProperty("spectraevents.group").get()
version = providers.gradleProperty("spectraevents.version").get()

allprojects {
    group = rootProject.group
    version = rootProject.version

    dependencyLocking {
        lockAllConfigurations()
    }
}

configure<SpotlessExtension> {
    java {
        target("**/src/**/*.java")
        targetExclude("**/build/**")
        googleJavaFormat(libs.versions.googlejavaformat.get())
    }
    kotlinGradle {
        target("*.gradle.kts", "**/*.gradle.kts")
        targetExclude("**/build/**", "**/.gradle/**")
        ktlint(libs.versions.ktlint.get())
    }
    format("markdown") {
        target("**/*.md")
        targetExclude("**/build/**", "**/.gradle/**")
        trimTrailingWhitespace()
        endWithNewline()
    }
    format("yaml") {
        target("**/*.yml", "**/*.yaml")
        targetExclude("**/build/**", "**/.gradle/**", "server/**")
        trimTrailingWhitespace()
        endWithNewline()
    }
    format("json") {
        target("**/*.json")
        targetExclude("**/build/**", "**/.gradle/**", "server/**")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

val platformIndependentProjects =
    listOf(
        project(":spectraevents-core"),
        project(":spectraevents-application"),
        project(":adapters:storage-sqlite"),
        project(":adapters:update-http"),
        project(":adapters:assets-blockbench"),
    )

val verifyPlatformBoundaries =
    tasks.register("verifyPlatformBoundaries") {
        group = "verification"
        description = "Rejects Minecraft platform dependencies and references outside adapters."

        val sourceTrees =
            platformIndependentProjects.map { independentProject ->
                independentProject.fileTree("src/main") { include("**/*.java", "**/*.kt") }
            }
        inputs.files(sourceTrees)

        doLast {
            val forbiddenPrefixes = listOf("org.bukkit", "io.papermc.paper", "net.minecraft", "org.spongepowered")
            val violations = mutableListOf<String>()

            platformIndependentProjects.forEach { independentProject ->
                independentProject.configurations.forEach { configuration ->
                    configuration.dependencies.forEach { dependency ->
                        val coordinates = listOfNotNull(dependency.group, dependency.name).joinToString(":")
                        if (forbiddenPrefixes.any(coordinates::startsWith)) {
                            violations +=
                                "${independentProject.path}:${configuration.name} declares $coordinates"
                        }
                    }
                }

                independentProject
                    .fileTree("src/main") {
                        include("**/*.java", "**/*.kt")
                    }.forEach { sourceFile ->
                        val contents = sourceFile.readText()
                        forbiddenPrefixes.forEach { forbiddenPrefix ->
                            if (contents.contains(forbiddenPrefix)) {
                                violations +=
                                    "${sourceFile.relativeTo(rootDir)} references $forbiddenPrefix"
                            }
                        }
                    }
            }

            if (violations.isNotEmpty()) {
                throw GradleException(
                    "Platform boundary violations:\n${violations.joinToString(separator = "\n")}",
                )
            }
        }
    }

val printCompatibilityMatrix =
    tasks.register("printCompatibilityMatrix") {
        group = "help"
        description = "Prints the configured Minecraft version compatibility matrix."
        doLast {
            println("=== SpectraEvents Compatibility Matrix ===")
            println("Product Version: $version")
            println("Paper Family: ${providers.gradleProperty("paper.minecraftVersions").get()}")
            println("Spigot Family: ${providers.gradleProperty("spigot.minecraftVersions").get()}")
        }
    }

val verifyCompatibilityMatrix =
    tasks.register("verifyCompatibilityMatrix") {
        group = "verification"
        description = "Validates the Minecraft compatibility matrix configuration."
        doLast {
            val paperVersions =
                providers
                    .gradleProperty(
                        "paper.minecraftVersions",
                    ).get()
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            val spigotVersions =
                providers
                    .gradleProperty(
                        "spigot.minecraftVersions",
                    ).get()
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

            if (paperVersions.isEmpty()) {
                throw GradleException("Compatibility matrix for Paper must not be empty!")
            }
            if (spigotVersions.isEmpty()) {
                throw GradleException("Compatibility matrix for Spigot must not be empty!")
            }
            if (paperVersions.toSet().size != paperVersions.size) {
                throw GradleException("Compatibility matrix for Paper contains duplicates: $paperVersions")
            }
            if (spigotVersions.toSet().size != spigotVersions.size) {
                throw GradleException("Compatibility matrix for Spigot contains duplicates: $spigotVersions")
            }
            println("Compatibility matrix verification PASSED.")
        }
    }

val paperDistributionDirectory = project(":distributions:paper").layout.buildDirectory.dir("libs")
val spigotDistributionDirectory = project(":distributions:spigot").layout.buildDirectory.dir("libs")
val distributionVersion = version.toString()

val verifyDistributionArtifacts =
    tasks.register("verifyDistributionArtifacts") {
        group = "verification"
        description = "Validates that exactly 2 public distribution artifacts (paper, spigot) exist."
        dependsOn(
            ":distributions:paper:verifyPluginArtifact",
            ":distributions:spigot:verifyPluginArtifact",
        )
        doLast {
            val expected =
                setOf(
                    "SpectraEvents-$distributionVersion-paper.jar",
                    "SpectraEvents-$distributionVersion-spigot.jar",
                )
            val artifacts =
                listOf(
                    paperDistributionDirectory.get().asFile,
                    spigotDistributionDirectory.get().asFile,
                ).flatMap { directory ->
                    directory
                        .listFiles { file ->
                            file.name.startsWith("SpectraEvents-") && file.extension == "jar"
                        }?.toList()
                        .orEmpty()
                }
            val actual = artifacts.map { it.name }.toSet()
            if (actual != expected || artifacts.size != 2) {
                throw GradleException("Expected public artifacts $expected, found ${artifacts.map { it.name }}")
            }
            println("Distribution artifact verification PASSED: ${actual.sorted()}")
        }
    }

val validateStructure =
    tasks.register<Exec>("validateStructure") {
        group = "verification"
        description = "Validates the source layout, package structure, and documentation expectations."
        commandLine("bash", "scripts/verification/validate-structure.sh")
    }

val validateSourceHygiene =
    tasks.register<Exec>("validateSourceHygiene") {
        group = "verification"
        description = "Fails build on source hygiene violations in Java sources."
        commandLine("bash", "scripts/verification/validate-source-hygiene.sh")
    }

val jacocoRootReport =
    tasks.register<JacocoReport>("jacocoRootReport") {
        group = "verification"
        description = "Generates aggregated JaCoCo coverage report across all tested subprojects."
        val testedProjects = subprojects.filter { it.pluginManager.hasPlugin("jacoco") }
        dependsOn(testedProjects.map { it.tasks.named("jacocoTestReport") })

        val allExecutionData =
            files(
                testedProjects.map { testedProject ->
                    testedProject.fileTree(testedProject.layout.buildDirectory.dir("jacoco")) {
                        include("*.exec")
                    }
                },
            )
        executionData.setFrom(allExecutionData)

        val allSourceSets =
            testedProjects.mapNotNull { testedProject ->
                testedProject.extensions.findByType<SourceSetContainer>()?.findByName("main")
            }
        sourceDirectories.setFrom(allSourceSets.map { it.allSource.srcDirs })
        classDirectories.setFrom(allSourceSets.map { it.output })

        reports {
            xml.required.set(true)
            html.required.set(true)
            xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/jacocoRootReport/jacocoRootReport.xml"))
            html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/jacocoRootReport/html"))
        }
    }

sonar {
    properties {
        property("sonar.projectKey", "kizio806_SpectraEvents")
        property("sonar.organization", "kizio806")
        property("sonar.host.url", "https://sonarcloud.io")
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            listOf(
                layout.buildDirectory
                    .file("reports/jacoco/jacocoRootReport/jacocoRootReport.xml")
                    .get()
                    .asFile.absolutePath,
                "$projectDir/build/reports/jacoco/jacocoRootReport/jacocoRootReport.xml",
            ).joinToString(","),
        )
        property("sonar.exclusions", "distributions/**,server/**")
        property("sonar.coverage.exclusions", "distributions/**,server/**,build-logic/**")
        property("sonar.cpd.exclusions", "distributions/**,server/**,build-logic/**")
    }
}

subprojects {
    sonar {
        properties {
            val subprojectReport =
                layout.buildDirectory
                    .file("reports/jacoco/test/jacocoTestReport.xml")
                    .get()
                    .asFile
            val rootReport =
                rootProject.layout.buildDirectory
                    .file("reports/jacoco/jacocoRootReport/jacocoRootReport.xml")
                    .get()
                    .asFile
            property(
                "sonar.coverage.jacoco.xmlReportPaths",
                listOf(subprojectReport.absolutePath, rootReport.absolutePath).joinToString(","),
            )
        }
    }
}

tasks.named("check") {
    dependsOn(
        verifyPlatformBoundaries,
        verifyCompatibilityMatrix,
        verifyDistributionArtifacts,
        validateStructure,
        validateSourceHygiene,
        jacocoRootReport,
    )
}

tasks.register("runServer") {
    group = "application"
    description = "Runs a local Paper development server with the shaded plugin."
    dependsOn(":distributions:paper:runServer")
}
