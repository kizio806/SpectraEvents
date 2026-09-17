plugins {
    id("spectraevents.java-library")
}

group = "dev.spectraevents.adapter"

dependencies {
    api(project(":spectraevents-application"))
    testImplementation(libs.junit.jupiter)
}
