plugins {
    id("spectraevents.java-library")
}

dependencies {
    implementation(project(":spectraevents-application"))
    implementation(libs.gson)

    // Testing
    testImplementation(libs.junit.jupiter)
}
