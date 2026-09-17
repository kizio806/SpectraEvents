import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.GradleException

plugins {
    base
    alias(libs.plugins.spotless)
}

group = providers.gradleProperty("spectraevents.group").get()
version = providers.gradleProperty("spectraevents.version").get()

allprojects {
    group = rootProject.group
    version = rootProject.version
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
        project(":spectraevents-api"),
        project(":spectraevents-core"),
        project(":spectraevents-application"),
        project(":adapters:storage-sqlite"),
        project(":adapters:update-http"),
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
            val forbiddenPrefixes = listOf("org.bukkit", "io.papermc.paper", "net.minecraft")
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

tasks.named("check") {
    dependsOn(verifyPlatformBoundaries)
}

tasks.register("runServer") {
    group = "application"
    description = "Runs a local Paper 26.2 development server with the shaded plugin."
    dependsOn(":distributions:paper:v26_2:runServer")
}
