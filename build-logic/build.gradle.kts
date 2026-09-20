plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.errorprone.gradle.plugin)
    implementation(libs.spotbugs.gradle.plugin)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}
