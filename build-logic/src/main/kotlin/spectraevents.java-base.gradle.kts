import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort
import com.github.spotbugs.snom.SpotBugsExtension
import com.github.spotbugs.snom.SpotBugsTask
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.quality.Pmd
import org.gradle.api.plugins.quality.PmdExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.jvm.tasks.Jar

plugins {
    java
    checkstyle
    pmd
    id("com.github.spotbugs")
    id("net.ltgt.errorprone")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    add("errorprone", libs.findLibrary("errorprone-core").get())
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-classfile", "-Werror"))
    options.errorprone {
        allErrorsAsWarnings.set(false)
        disableAllChecks.set(false)
        disableAllWarnings.set(false)
        ignoreSuppressionAnnotations.set(false)
    }
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
}

tasks.withType<Jar>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

checkstyle {
    toolVersion = libs.findVersion("checkstyle").get().requiredVersion
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    isIgnoreFailures = false
    maxErrors = 0
    maxWarnings = 0
}

pmd {
    toolVersion = libs.findVersion("pmd").get().requiredVersion
    isConsoleOutput = true
    isIgnoreFailures = false
    rulesMinimumPriority.set(3)
    ruleSetFiles = files(rootProject.file("config/pmd/pmd.xml"))
    ruleSets = listOf()
}

tasks.withType<Pmd>().configureEach {
    maxFailures.set(0)
    reports {
        html.required.set(true)
        xml.required.set(true)
    }
}

configure<SpotBugsExtension> {
    ignoreFailures.set(false)
    effort.set(Effort.MAX)
    reportLevel.set(Confidence.LOW)
    excludeFilter.set(rootProject.file("config/spotbugs/spotbugs-exclude.xml"))
}

tasks.withType<SpotBugsTask>().configureEach {
    reports {
        create("html") {
            required.set(true)
        }
        create("xml") {
            required.set(true)
        }
    }
}
