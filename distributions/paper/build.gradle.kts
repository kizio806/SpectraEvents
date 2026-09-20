import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.GradleException
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import xyz.jpenilla.runpaper.task.RunServer
import java.util.zip.ZipFile

plugins {
    id("spectraevents.java-base")
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
}
group = "io.github.kizio806.distribution"

dependencies {
    implementation(project(":spectraevents-core"))
    implementation(project(":spectraevents-application"))
    implementation(project(":adapters:storage-sqlite"))
    implementation(project(":adapters:update-http"))
    implementation(project(":platforms:paper:common"))
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
        archiveClassifier.set("paper")
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
        manifest {
            attributes(
                "Implementation-Title" to "SpectraEvents-Paper",
                "Implementation-Version" to project.version.toString(),
                "Implementation-Vendor" to "kizio806",
            )
        }
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
        description = "Checks the contents of the distributable Paper plugin JAR."
        dependsOn(shadowJar)
        inputs.file(shadowJar.flatMap { it.archiveFile })

        doLast {
            val archive =
                shadowJar
                    .get()
                    .archiveFile
                    .get()
                    .asFile
            val entries =
                ZipFile(archive).use { zip ->
                    zip
                        .entries()
                        .asSequence()
                        .map { it.name }
                        .toList()
                }
            val requiredSuffixes =
                listOf(
                    "plugin.yml",
                    "io/github/kizio806/spectraevents/platform/paper/SpectraEventsPlugin.class",
                    "io/github/kizio806/spectraevents/platform/paper/PaperBootstrap.class",
                    "io/github/kizio806/spectraevents/platform/paper/command/SpectraMainCommand.class",
                    "io/github/kizio806/spectraevents/platform/paper/interaction/PaperInteractionRouter.class",
                    "io/github/kizio806/spectraevents/platform/paper/common/PaperLifecycleReporter.class",
                    "io/github/kizio806/spectraevents/application/SpectraEventsApplication.class",
                    "io/github/kizio806/spectraevents/adapter/storage/sqlite/SQLiteEventInstanceRepository.class",
                    "io/github/kizio806/spectraevents/adapter/update/http/HttpUpdateAdapter.class",
                    "org/sqlite/JDBC.class",
                )
            val missing = requiredSuffixes.filter { required -> entries.none { it.endsWith(required) } }
            val forbidden =
                entries.filter { entry ->
                    entry.contains("/src/test/") ||
                        entry.endsWith("Test.class") ||
                        entry.contains(".idea/") ||
                        entry.contains(".gradle/") ||
                        entry.startsWith("server/") ||
                        entry.startsWith("dev/spectraevents/") ||
                        entry.startsWith("io/github/kizio806/spectraevents/platform/spigot/")
                }

            val pluginDescriptor =
                ZipFile(archive).use { zip ->
                    zip.getInputStream(zip.getEntry("plugin.yml")).use { input ->
                        String(input.readAllBytes(), Charsets.UTF_8)
                    }
                }
            val descriptorValid =
                pluginDescriptor.contains(
                    "main: io.github.kizio806.spectraevents.platform.paper.SpectraEventsPlugin",
                ) &&
                    pluginDescriptor.contains("api-version: '26.1'") &&
                    pluginDescriptor.contains("folia-supported: true")

            if (missing.isNotEmpty() || forbidden.isNotEmpty() || !descriptorValid) {
                throw GradleException(
                    "Invalid Paper artifact. Missing=$missing, forbidden=$forbidden, descriptorValid=$descriptorValid",
                )
            }
        }
    }

tasks.named("assemble") {
    dependsOn(shadowJar)
}

tasks.named("check") {
    dependsOn(verifyPluginArtifact)
}

tasks.named<RunServer>("runServer") {
    minecraftVersion("26.2")
    runDirectory(rootProject.file("server"))
}
