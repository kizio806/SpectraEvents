import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.GradleException
import org.gradle.api.tasks.compile.JavaCompile
import xyz.jpenilla.runpaper.task.RunServer
import java.util.zip.ZipFile

plugins {
    id("spectraevents.java-base")
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
}
group = "io.github.kizio806.distribution"

dependencies {
    implementation(project(":spectraevents-api"))
    implementation(project(":spectraevents-core"))
    implementation(project(":spectraevents-application"))
    implementation(project(":adapters:storage-sqlite"))
    implementation(project(":adapters:update-http"))
    implementation(project(":platforms:paper:common"))
    implementation(project(":platforms:paper:v26_2"))
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
        archiveClassifier.set("")
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
        manifest {
            attributes(
                "Implementation-Title" to "SpectraEvents",
                "Implementation-Version" to project.version.toString(),
                "Implementation-Vendor" to "kizio806",
            )
        }
        from(rootProject.file("LICENSE")) {
            into("META-INF")
            rename { "LICENSE.txt" }
        }
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
                    "io/github/kizio806/spectraevents/platform/paper/v26_2/SpectraEventsPlugin.class",
                    "io/github/kizio806/spectraevents/platform/paper/common/PaperLifecycleReporter.class",
                    "io/github/kizio806/spectraevents/application/SpectraEventsApplication.class",
                    "io/github/kizio806/spectraevents/adapter/storage/sqlite/SQLiteEventInstanceRepository.class",
                    "io/github/kizio806/spectraevents/adapter/update/http/HttpUpdateAdapter.class",
                )
            val missing = requiredSuffixes.filter { required -> entries.none { it.endsWith(required) } }
            val forbidden =
                entries.filter { entry ->
                    entry.contains("/src/test/") ||
                        entry.endsWith("Test.class") ||
                        entry.contains(".idea/") ||
                        entry.contains(".gradle/") ||
                        entry.startsWith("server/") ||
                        entry.startsWith("dev/spectraevents/")
                }

            if (missing.isNotEmpty() || forbidden.isNotEmpty()) {
                throw GradleException(
                    "Invalid plugin artifact. Missing=$missing, forbidden=$forbidden",
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
