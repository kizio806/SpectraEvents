import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.GradleException
import org.gradle.api.tasks.compile.JavaCompile

plugins {
    id("spectraevents.java-base")
    alias(libs.plugins.shadow)
}
group = "io.github.kizio806.distribution"

dependencies {
    implementation(project(":spectraevents-api"))
    implementation(project(":spectraevents-core"))
    implementation(project(":spectraevents-application"))
    implementation(project(":adapters:storage-sqlite"))
    implementation(project(":adapters:update-http"))
    implementation(project(":platforms:sponge:common"))
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}

tasks.processResources {
    val pluginVersion = project.version.toString()
    inputs.property("version", pluginVersion)
    filesMatching("sponge_plugins.json") {
        expand("version" to pluginVersion)
    }
}

val shadowJar =
    tasks.named<ShadowJar>("shadowJar") {
        archiveBaseName.set("SpectraEvents")
        archiveVersion.set(project.version.toString())
        archiveClassifier.set("sponge")
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
        manifest {
            attributes(
                "Implementation-Title" to "SpectraEvents-Sponge",
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
        description = "Checks the contents of the distributable Sponge plugin JAR."
        dependsOn(shadowJar)
        inputs.file(shadowJar.flatMap { it.archiveFile })

        doLast {
            // Can add verification here
        }
    }

tasks.named("assemble") {
    dependsOn(shadowJar)
}

tasks.named("check") {
    dependsOn(verifyPluginArtifact)
}
