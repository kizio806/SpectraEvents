import org.gradle.api.tasks.compile.JavaCompile

plugins {
    id("spectraevents.java-library")
}

dependencies {
    api(project(":spectraevents-application"))
    api(project(":adapters:storage-sqlite"))
    api(project(":adapters:update-http"))
    compileOnly(libs.sponge.api)
    testImplementation(libs.sponge.api)
    testImplementation(libs.junit.jupiter)
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}
