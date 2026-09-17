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
    "platforms:paper:v26_2",
    "distributions:paper:v26_2",
)
