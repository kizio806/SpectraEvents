import org.gradle.api.tasks.compile.JavaCompile

plugins {
    id("spectraevents.java-library")
}

dependencies {
    api(project(":spectraevents-application"))
    api(project(":adapters:storage-sqlite"))
    api(project(":adapters:update-http"))
    compileOnly(libs.paper.api)
    compileOnly(libs.luckperms.api)
    compileOnly(libs.worldguard.api)
    compileOnly(libs.vault.api) {
        exclude(group = "org.bukkit", module = "bukkit")
    }
    compileOnly(libs.placeholderapi)
    compileOnly(libs.miniplaceholders.api)
    compileOnly(libs.nexo.api)
    compileOnly(libs.oraxen.api)
    compileOnly(libs.itemsadder.api)
    testImplementation(libs.paper.api)
    testImplementation(libs.junit.jupiter)
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}
