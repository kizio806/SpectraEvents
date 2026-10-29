plugins {
    id("spectraevents.java-library")
}

dependencies {
    implementation(project(":spectraevents-core"))
    implementation(project(":spectraevents-application"))

    // We use Gson for lightweight JSON parsing, available by default in most Minecraft environments
    implementation(libs.gson)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

sourceSets {
    test {
        resources.srcDir(project(":spectraevents-application").file("src/main/resources"))
    }
}

val bundledAssetSourceDirectory =
    project(":spectraevents-application").file("src/main/resources/assets/source")
val releaseResourcePackOutputDirectory =
    providers
        .gradleProperty("releaseResourcePackOutputDir")
        .map { file(it) }
        .orElse(layout.buildDirectory.dir("release-resource-packs").map { it.asFile })

tasks.register<JavaExec>("buildReleaseResourcePacks") {
    group = "distribution"
    description = "Builds one deterministic resource pack for each supported Minecraft release line."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("io.github.kizio806.spectraevents.adapter.blockbench.ResourcePackReleaseBuilder")
    inputs.dir(bundledAssetSourceDirectory)
    outputs.dir(releaseResourcePackOutputDirectory)
    args(bundledAssetSourceDirectory.absolutePath, releaseResourcePackOutputDirectory.get().absolutePath)
}
