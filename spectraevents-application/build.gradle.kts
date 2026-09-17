plugins {
    id("spectraevents.java-library")
}

dependencies {
    api(project(":spectraevents-core"))
    implementation(libs.snakeyaml)

    testImplementation(libs.archunit.junit5)
}
