import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification

plugins {
    id("spectraevents.java-library")
}

dependencies {
    api(project(":platforms:bukkit"))
    api(project(":spectraevents-core"))
    api(project(":spectraevents-application"))
    api(project(":adapters:storage-sqlite"))
    api(project(":adapters:update-http"))
    implementation(project(":adapters:assets-blockbench"))
    implementation(project(":adapters:assets-delivery"))
    compileOnly(libs.spigot.api)
    implementation(libs.adventure.platform.bukkit)
    implementation(libs.adventure.text.minimessage)
    testImplementation(project(path = ":platforms:bukkit", configuration = "testArtifacts"))
    testImplementation(libs.spigot.api)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.archunit.junit5)
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    violationRules {
        rule {
            element = "BUNDLE"
            limit {
                counter = "INSTRUCTION"
                value = "COVEREDRATIO"
                minimum = "0.65".toBigDecimal()
            }
        }
    }
}

tasks.named("check") {
    dependsOn(tasks.named("jacocoTestCoverageVerification"))
}
