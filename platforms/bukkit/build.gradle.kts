import org.gradle.api.tasks.compile.JavaCompile

plugins {
    id("spectraevents.java-library")
}

dependencies {
    api(project(":spectraevents-core"))
    api(project(":spectraevents-application"))
    api(project(":adapters:assets-delivery"))
    compileOnly(libs.spigot.api)
    compileOnly(libs.adventure.text.minimessage)
    testImplementation(libs.spigot.api)
    testImplementation(libs.adventure.text.minimessage)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.archunit.junit5)
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}
