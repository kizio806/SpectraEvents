plugins {
    id("spectraevents.java-library")
}

group = "dev.spectraevents.adapter"

dependencies {
    api(project(":spectraevents-application"))
    compileOnly("org.xerial:sqlite-jdbc:3.45.1.0")
    testImplementation("org.xerial:sqlite-jdbc:3.45.1.0")
    testImplementation(libs.junit.jupiter)
}
