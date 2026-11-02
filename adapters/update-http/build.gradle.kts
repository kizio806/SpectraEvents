import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification

plugins {
    id("spectraevents.java-library")
}

dependencies {
    api(project(":spectraevents-application"))
    testImplementation(libs.junit.jupiter)
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    violationRules {
        rule {
            element = "BUNDLE"
            limit {
                counter = "INSTRUCTION"
                value = "COVEREDRATIO"
                minimum = "0.85".toBigDecimal()
            }
        }
    }
}

tasks.named("check") {
    dependsOn(tasks.named("jacocoTestCoverageVerification"))
}
