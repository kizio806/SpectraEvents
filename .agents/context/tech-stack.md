# Tech Stack Context — SpectraEvents

## Language & JVM
- **Java Version:** Java 25 server runtime (platform-neutral domain and application target Java 21).
- **Compiler Options:** `-Xlint:all -Werror -parameters`.

## Build & Tooling
- **Build System:** Gradle 9.8.2 with Kotlin DSL (`build.gradle.kts`, `settings.gradle.kts`, `build-logic`).
- **Formatting:** Spotless with Google Java Format (2 spaces).
- **Code Analysis:** Checkstyle, PMD, SpotBugs, ArchUnit.
- **Coverage:** JaCoCo with $\ge 90\%$ instruction coverage enforcement per module and root aggregate.
- **Asset Tooling:** Python 3 + Node.js for Blockbench asset validation scripts.

## Server APIs & Libraries
- **Paper API:** `26.1–26.3` (Mojang-mapped).
- **Spigot API:** `26.1–26.3`.
- **Text & Localization:** Adventure MiniMessage (`net.kyori:adventure-text-minimessage`). Relocated on Spigot.
- **Commands:** Paper Brigadier Command API (`io.papermc.paper:paper-brigadier`).
- **Persistence:** SQLite JDBC (`org.xerial:sqlite-jdbc`) in WAL mode.
- **Testing:** JUnit 5, AssertJ, Mockito, ArchUnit.
