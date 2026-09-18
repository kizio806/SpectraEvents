import org.gradle.api.tasks.compile.JavaCompile

plugins {
    id("spectraevents.java-library")
}

dependencies {
    api(project(":spectraevents-core"))
    api(project(":spectraevents-application"))
    api(project(":adapters:storage-sqlite"))
    api(project(":adapters:update-http"))
    implementation(project(":adapters:assets-blockbench"))
    compileOnly(libs.spigot.api)
    implementation(libs.adventure.platform.bukkit)
    implementation(libs.adventure.text.minimessage)
    testImplementation(libs.spigot.api)
    testImplementation(libs.junit.jupiter)
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}
