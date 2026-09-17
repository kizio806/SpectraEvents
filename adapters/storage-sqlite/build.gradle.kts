plugins {
    id("spectraevents.java-library")
}

dependencies {
    api(project(":spectraevents-application"))
    compileOnly("org.xerial:sqlite-jdbc:3.45.1.0")
    testImplementation("org.xerial:sqlite-jdbc:3.45.1.0")
    testImplementation(libs.junit.jupiter)
}
