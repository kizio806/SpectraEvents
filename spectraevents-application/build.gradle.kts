plugins {
    id("spectraevents.java-library")
}

dependencies {
    api(project(":spectraevents-core"))
    implementation(libs.snakeyaml)

    testImplementation(project(":adapters:assets-blockbench"))
    testImplementation(libs.archunit.junit5)
}

val bundleMetinTemplate =
    tasks.register<Zip>("bundleMetinTemplate") {
        group = "build"
        description = "Builds the bundled Spectra Bundle v1 Metin template."
        archiveFileName.set("metin.spectra.zip")
        destinationDirectory.set(layout.buildDirectory.dir("generated/templates"))
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
        from("src/main/spectra-bundles/metin")
        from("src/main/resources/events") {
            include("metin.yml")
            rename("metin.yml", "event.yml")
        }
        from("src/main/resources/assets/source") {
            include("metin_stone.bbmodel")
            into("models")
        }
    }

tasks.named<ProcessResources>("processResources") {
    dependsOn(bundleMetinTemplate)
    from(bundleMetinTemplate) {
        into("templates")
    }
}
