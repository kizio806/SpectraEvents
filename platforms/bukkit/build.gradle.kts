import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification

plugins {
    id("spectraevents.java-library")
}

dependencies {
    api(project(":spectraevents-core"))
    api(project(":spectraevents-application"))
    api(project(":adapters:assets-delivery"))
    compileOnly(libs.spigot.api)
    compileOnly(libs.luckperms.api)
    compileOnly(libs.worldguard.api) {
        exclude(group = "org.spigotmc", module = "spigot-api")
    }
    compileOnly(libs.vault.api) {
        exclude(group = "org.bukkit", module = "bukkit")
    }
    compileOnly(libs.placeholderapi)
    compileOnly(libs.nexo.api) {
        isTransitive = false
    }
    compileOnly(libs.oraxen.api) {
        isTransitive = false
    }
    compileOnly(libs.itemsadder.api)
    compileOnly(libs.adventure.text.minimessage)
    compileOnly(libs.adventure.text.serializer.legacy)
    testImplementation(libs.spigot.api)
    testImplementation(libs.placeholderapi)
    testImplementation(libs.adventure.text.minimessage)
    testImplementation(libs.adventure.text.serializer.legacy)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.archunit.junit5)
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}

val testJar =
    tasks.register<Jar>("testJar") {
        archiveClassifier.set("tests")
        from(sourceSets.test.get().output)
    }

configurations {
    create("testArtifacts") {
        isCanBeConsumed = true
        isCanBeResolved = false
    }
}

artifacts {
    add("testArtifacts", testJar)
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    violationRules {
        rule {
            element = "BUNDLE"
            limit {
                counter = "INSTRUCTION"
                value = "COVEREDRATIO"
                minimum = "0.75".toBigDecimal()
            }
        }
    }
}

tasks.named("check") {
    dependsOn(tasks.named("jacocoTestCoverageVerification"))
}
