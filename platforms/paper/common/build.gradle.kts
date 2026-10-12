import org.gradle.api.tasks.compile.JavaCompile

plugins {
    id("spectraevents.java-library")
}

configurations.configureEach {
    resolutionStrategy {
        // Paper API brings these Maven libraries only for compilation, but their versions still
        // must meet the project's vulnerability-policy patch floor.
        force(
            "org.apache.commons:commons-lang3:3.21.0",
            "org.apache.logging.log4j:log4j-api:2.26.1",
            "org.codehaus.plexus:plexus-utils:4.1.0",
        )
    }
}

dependencies {
    api(project(":spectraevents-application"))
    api(project(":adapters:storage-sqlite"))
    api(project(":adapters:update-http"))
    implementation(project(":adapters:assets-blockbench"))
    implementation(project(":adapters:assets-delivery"))
    compileOnly(libs.paper.api)
    compileOnly(libs.luckperms.api)
    compileOnly(libs.worldguard.api) {
        exclude(group = "org.spigotmc", module = "spigot-api")
    }
    compileOnly(libs.vault.api) {
        exclude(group = "org.bukkit", module = "bukkit")
    }
    compileOnly(libs.placeholderapi)
    compileOnly(libs.miniplaceholders.api)
    compileOnly(libs.nexo.api) {
        isTransitive = false
    }
    compileOnly(libs.oraxen.api) {
        isTransitive = false
    }
    compileOnly(libs.itemsadder.api)
    testImplementation(libs.paper.api)
    testImplementation(libs.junit.jupiter)
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}
