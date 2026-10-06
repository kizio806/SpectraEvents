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

tasks
    .matching {
        it.name in
            setOf(
                "generateExternalPluginSpecBuilders",
                "generatePrecompiledScriptPluginAccessors",
                "extractPrecompiledScriptPluginPlugins",
                "generateScriptPluginAdapters",
            )
    }.configureEach {
        mustRunAfter(tasks.named("clean"))
    }
