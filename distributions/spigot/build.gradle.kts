import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.GradleException
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import java.nio.charset.StandardCharsets
import java.util.zip.ZipFile

plugins {
    id("spectraevents.java-base")
    alias(libs.plugins.shadow)
}
group = "io.github.kizio806.distribution"

dependencies {
    implementation(project(":spectraevents-core"))
    implementation(project(":spectraevents-application"))
    implementation(project(":adapters:storage-sqlite"))
    implementation(project(":adapters:update-http"))
    implementation(project(":platforms:spigot:common"))
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}

tasks.processResources {
    val pluginVersion = project.version.toString()
    inputs.property("version", pluginVersion)
    filesMatching("plugin.yml") {
        expand("version" to pluginVersion)
    }
}

val shadowJar =
    tasks.named<ShadowJar>("shadowJar") {
        archiveBaseName.set("SpectraEvents")
        archiveVersion.set(project.version.toString())
        archiveClassifier.set("spigot")
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
        manifest {
            attributes(
                "Implementation-Title" to "SpectraEvents-Spigot",
                "Implementation-Version" to project.version.toString(),
                "Implementation-Vendor" to "kizio806",
            )
        }
        relocate("net.kyori", "io.github.kizio806.spectraevents.lib.kyori")
        from(rootProject.file("LICENSE")) {
            into("META-INF")
            rename { "LICENSE.txt" }
        }
    }

tasks.named<Jar>("jar") {
    enabled = false
}

tasks.named<Jar>("sourcesJar") {
    enabled = false
}

val verifyPluginArtifact =
    tasks.register("verifyPluginArtifact") {
        group = "verification"
        description = "Checks the contents of the distributable Spigot plugin JAR."
        dependsOn(shadowJar)
        inputs.file(shadowJar.flatMap { it.archiveFile })

        doLast {
            val archive =
                shadowJar
                    .get()
                    .archiveFile
                    .get()
                    .asFile
            ZipFile(archive).use { zip ->
                val entries =
                    zip
                        .entries()
                        .asSequence()
                        .map { it.name }
                        .toList()
                val required =
                    listOf(
                        "plugin.yml",
                        "io/github/kizio806/spectraevents/platform/spigot/SpectraEventsSpigotPlugin.class",
                        "io/github/kizio806/spectraevents/platform/spigot/SpigotBootstrap.class",
                        "io/github/kizio806/spectraevents/platform/spigot/command/SpigotMainCommand.class",
                        "io/github/kizio806/spectraevents/platform/spigot/interaction/SpigotEventRouter.class",
                        "io/github/kizio806/spectraevents/application/SpectraEventsApplication.class",
                        "io/github/kizio806/spectraevents/adapter/storage/sqlite/SQLiteEventInstanceRepository.class",
                        "org/sqlite/JDBC.class",
                    )
                val missing = required.filterNot(entries::contains)
                val forbiddenEntries =
                    entries.filter {
                        it.startsWith("io/github/kizio806/spectraevents/platform/paper/") ||
                            it.startsWith("io/papermc/") ||
                            it.endsWith("Test.class")
                    }
                val paperReferences =
                    entries
                        .filter { it.endsWith(".class") }
                        .filter { entry ->
                            zip.getInputStream(zip.getEntry(entry)).use { input ->
                                String(input.readAllBytes(), StandardCharsets.ISO_8859_1)
                                    .contains("io/papermc/")
                            }
                        }
                val pluginDescriptor =
                    zip.getInputStream(zip.getEntry("plugin.yml")).use { input ->
                        String(input.readAllBytes(), StandardCharsets.UTF_8)
                    }
                val descriptorValid =
                    pluginDescriptor.contains(
                        "main: io.github.kizio806.spectraevents.platform.spigot.SpectraEventsSpigotPlugin",
                    ) &&
                        pluginDescriptor.contains("api-version: '26.1'") &&
                        pluginDescriptor.contains("commands:") &&
                        pluginDescriptor.contains("  event:")

                if (missing.isNotEmpty() ||
                    forbiddenEntries.isNotEmpty() ||
                    paperReferences.isNotEmpty() ||
                    !descriptorValid
                ) {
                    throw GradleException(
                        "Invalid Spigot artifact. Missing=$missing, forbidden=$forbiddenEntries, paperReferences=$paperReferences, descriptorValid=$descriptorValid",
                    )
                }
            }
        }
    }

tasks.named("assemble") {
    dependsOn(shadowJar)
}

tasks.named("check") {
    dependsOn(verifyPluginArtifact)
}
