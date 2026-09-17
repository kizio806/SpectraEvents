import org.gradle.api.tasks.compile.JavaCompile

plugins {
    id("spectraevents.java-library")
}

dependencies {
    implementation(project(":spectraevents-application"))
    implementation(project(":adapters:storage-sqlite"))
    implementation(project(":adapters:update-http"))
    implementation(project(":platforms:paper:common"))
    compileOnly(libs.paper.api)
    testImplementation(libs.paper.api)
    testImplementation(libs.junit.jupiter)
    testImplementation("org.mockito:mockito-core:5.23.0")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}
