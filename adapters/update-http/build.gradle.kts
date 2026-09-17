plugins {
    id("spectraevents.java-library")
}

dependencies {
    api(project(":spectraevents-application"))
    testImplementation(libs.junit.jupiter)
}
