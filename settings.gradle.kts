pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/") {
            name = "PaperMC"
            content {
                includeGroup("io.papermc.paper")
                includeGroup("com.mojang")
                includeGroup("net.md-5")
            }
        }
        maven("https://maven.enginehub.org/repo/") { name = "EngineHub" }
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") { name = "PAPI" }
        maven("https://jitpack.io") { name = "JitPack" }
        maven("https://repo.oraxen.com/releases") { name = "Oraxen" }
        maven("https://repo.nexomc.com/releases") { name = "Nexo" }
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") { name = "Spigot" }
    }
}

rootProject.name = "SpectraEvents"

include(
    "spectraevents-api",
    "spectraevents-core",
    "spectraevents-application",
    "adapters:storage-sqlite",
    "adapters:update-http",
    "platforms:paper:common",
    "platforms:spigot:common",
    "platforms:sponge:common",
    "distributions:paper",
    "distributions:spigot",
    "distributions:sponge",
)
