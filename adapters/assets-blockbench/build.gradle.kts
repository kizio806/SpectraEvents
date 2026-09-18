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
