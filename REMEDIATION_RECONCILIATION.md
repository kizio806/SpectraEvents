# REMEDIATION RECONCILIATION REPORT
## COMPLETE ENGINEERING AUDIT — ORIGINAL FINDINGS VS ACTUAL IMPLEMENTATION

**Dokument Identyfikacyjny:** `RECONCILIATION-AUDIT-2026-11`
**Data sporządzenia:** 8 listopada 2026 r.
**Audytor:** Principal Software Architect, Staff Java Engineer, Security Auditor, Independent Quality Assurance Auditor
**Obiekty audytu:**
1. **SpectraEvents** (Ścieżka: `/home/kizio/Dokumenty/GitHub/SpectraEvents`, Gałąź: `quality/test-and-architecture-remediation`, HEAD: `5574349`, Punkt bazowy audytu: `64d32d4`, Punkt odniesienia remote main: `d97ab64`)
2. **Jump Delay Fix** (Ścieżka: `/home/kizio/Dokumenty/GitHub/jump-delay-fix`, Gałąź: `main`, HEAD: `3537b1d`, Punkt bazowy audytu: `3537b1d`, Punkt odniesienia remote main: `2f05201`)

---

## Section 1 — Executive Summary

### 1.1. Stan Obu Projektów
Prace remediacyjne przeprowadzone w oparciu o pierwotny raport *Extreme Java Engineering Audit* (`AUDIT-JDF-SE-2026-11`) doprowadziły do bezprecedensowego postępu jakościowego, jednak wcześniejsze deklaracje o "100% ukończeniu wszystkich prac", "34/34 DONE" lub "czystym stanie repozytoriów" były **przedwczesne i formalnie nieprecyzyjne**:

1. **W repozytorium `SpectraEvents`:**
   - Wyeliminowano wszystkie 15 przyczyn alertów CodeQL na poziomie kodu źródłowego, zabezpieczono wielowątkowość (`CopyOnWriteArrayList`, `volatile`), naprawiono krytyczny błąd rozłączeń graczy (`AbstractBukkitPlayerResourcePackAdapter`), zdeduplikowano architekturę i zaimplementowano pełną parzystość platformową Spigot z Paper.
   - Wprowadzono 400 klas z pokryciem testowym (100.0% Class Coverage), a zagregowane pokrycie JaCoCo podniesiono z pierwotnych **59.10%** do **96.13% instrukcji, 95.87% linii, 86.32% rozgałęzień i 98.22% metod**.
   - **Jednakże:** Cel docelowy 100% realnego pokrycia kodu produkcyjnego nie został osiągnięty (pozostaje luka 3.87% instrukcji, 4.13% linii i 13.68% rozgałęzień), a alerty CodeQL na GitHubie formalnie znikną dopiero po zmergowaniu gałęzi do `main`.

2. **W repozytorium `jump-delay-fix`:**
   - Poprawnie zaimplementowano w kodzie architekturę instancyjnego silnika (`JumpDelayFixEngine` z wstrzykiwanym `RuntimeState`), podpięto Version Catalog `gradle/libs.versions.toml`, usunięto martwe wykluczenie `.codecov.yml`, zawężono widoczność `setPendingSaveForTests` i zdeduplikowano testy klawiszy.
   - **Kluczowy dług inżynierski:** **Wszystkie te zmiany (15 zmodyfikowanych plików, 2 nieśledzone pliki) znajdują się w stanie NIEZATWIERDZONYM (UNCOMMITTED) w drzewie roboczym na gałęzi `main`!**
   - Dodatkowo, testy `FabricMetadataContractTest` i `NeoForgeMetadataContractTest` nadal sprawdzają bytecode jako tekst (antywzorzec `StandardCharsets.ISO_8859_1`), a realne pokrycie loaderów po wyizolowaniu raportów wynosi zaledwie **11.88% w Fabric** i **20.83% w NeoForge**.

### 1.2. Statystyka Zadań Atomowych

| Metryka | SpectraEvents | Jump Delay Fix | Łącznie (Projekty A + B) | Procent |
|---|:---:|:---:|:---:|:---:|
| **Łączna liczba atomowych zadań** | **40** | **13** | **53** | 100.00% |
| **`DONE_VERIFIED`** | 38 | 9 | **47** | **88.68%** |
| **`PARTIAL`** | 1 | 2 | **3** | **5.66%** |
| **`NOT_DONE`** | 0 | 2 | **2** | **3.77%** |
| **`BLOCKED`** | 1 | 0 | **1** | **1.89%** |
| **`NOT_REPRODUCIBLE`** | 0 | 0 | **0** | 0.00% |
| **`SUPERSEDED`** | 0 | 0 | **0** | 0.00% |

**Rzeczywisty procent pełnego, zweryfikowanego wykonania:** **88.68%** (47 / 53 zadania atomowe potwierdzone empirycznymi dowodami).

---

## Section 2 — Original Audit Traceability Matrix

Każde pierwotne ustalenie audytu posiada poniżej niezależny, atomowy wpis ze statusem i dowodami technicznymi.

### A. SpectraEvents

| ID | Projekt | Źródło | Priorytet | Pierwotny problem | Wymagany rezultat | Aktualna implementacja | Zmienione pliki | Commit | Testy | Weryfikacja | Status | Pozostałe prace |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `SE-BUG-01` | SpectraEvents | Audit BUG-01, Prompt 1 | P0 | Błędne mapowanie `DOWNLOADED` na `FAILED`, bezpodstawny kick graczy na Spigot | Poprawne mapowanie 8 statusów, brak kicka przy DOWNLOADED/LOADED/ACCEPTED | `mapStatus` w `AbstractBukkitPlayerResourcePackAdapter` mapuje wszystkie stany, puste/nieznane zwracają `null` i są ignorowane | `platforms/bukkit/.../AbstractBukkitPlayerResourcePackAdapter.java`, `spectraevents-application/.../PlayerResourcePackState.java` | `93889a9` | `AbstractBukkitPlayerResourcePackAdapterTest` (10 testów), `PaperPlayerResourcePackAdapterTest` | `./gradlew check` PASS | `DONE_VERIFIED` | Brak |
| `SE-SEC-01` | SpectraEvents | Audit SEC-01 #185 | P0 | CodeQL Alert #185: `SpectraAssetFace` ekspozycja listy `uv` | Niemutowalna defensywna kopia | Jawny konstruktor kanoniczny z `List.copyOf(uv)` oraz akcesor `uv()` zwracający `List.copyOf(uv)` | `spectraevents-core/.../SpectraAssetFace.java` | `b497bc1`, `93889a9` | `SpectraAssetFaceTest` (7 testów sprawdzających immutability i walidację) | `./gradlew check` PASS | `DONE_VERIFIED` | Brak |
| `SE-SEC-02` | SpectraEvents | Audit SEC-01 #271 | P0 | CodeQL Alert #271: Deprecated `Player.kickPlayer(String)` | Zastąpienie nowoczesną metodą | Delegacja do `BukkitPlatformBridge.kickPlayer` z detekcją Adventure i fallbackiem | `platforms/bukkit/.../BukkitPlatformBridge.java`, `platforms/spigot/.../SpigotPlayerResourcePackAdapter.java` | `93889a9` | `BukkitPlatformBridgeTest.testKickPlayerWithAdventureAndLegacy` | `./gradlew check` PASS | `DONE_VERIFIED` | Brak |
| `SE-SEC-03` | SpectraEvents | Audit SEC-01 #269, #270 | P0 | CodeQL Alerts #269, #270: Deprecated `Player.setResourcePack` | Wywołanie bezpieczne wielowersyjnie | Delegacja do `BukkitPlatformBridge.sendResourcePack` | `platforms/bukkit/.../BukkitPlatformBridge.java`, `platforms/spigot/.../SpigotPlayerResourcePackAdapter.java` | `93889a9` | `BukkitPlatformBridgeTest` | `./gradlew check` PASS | `DONE_VERIFIED` | Brak |
| `SE-SEC-04` | SpectraEvents | Audit SEC-01 #273, #274 | P0 | CodeQL Alerts #273, #274: Deprecated konstruktory `PlayerJoinEvent`/`QuitEvent` | Eliminacja wywołań w testach | Fabryki `BukkitPlatformBridge.createPlayerJoinEvent` i `createPlayerQuitEvent` | `platforms/bukkit/.../BukkitPlatformBridge.java`, `AbstractBukkitPlayerResourcePackAdapterTest.java` | `93889a9` | `AbstractBukkitPlayerResourcePackAdapterTest` | `./gradlew check` PASS | `DONE_VERIFIED` | Brak |
| `SE-SEC-05` | SpectraEvents | Audit SEC-01 #258 | P0 | CodeQL Alert #258: Deprecated `Scoreboard.registerNewObjective` | Użycie nowoczesnego API ze Spigot API | Przekierowanie do `SpigotScoreboardManager` z użyciem `Criteria.DUMMY` | `platforms/spigot/.../SpigotActionAdapter.java`, `SpigotScoreboardManager.java` | `8d01669`, `93889a9` | `SpigotScoreboardManagerTest` | `./gradlew check` PASS | `DONE_VERIFIED` | Brak |
| `SE-SEC-06` | SpectraEvents | Audit SEC-01 #232 | P0 | CodeQL Alert #232: Deprecated `Nameable.setCustomName(String)` | Adaptacja wieloplatformowa | `BukkitPlatformBridge.setCustomName` adaptujące Adventure/legacy | `platforms/bukkit/.../BukkitPlatformBridge.java`, `SpigotActionAdapter.java` | `93889a9` | `BukkitPlatformBridgeTest`, `SpigotActionAdapterTest` | `./gradlew check` PASS | `DONE_VERIFIED` | Brak |
| `SE-SEC-07` | SpectraEvents | Audit SEC-01 #233 | P0 | CodeQL Alert #233: Deprecated `Bukkit.createInventory` z String title | Bezpieczna kreacja inwentarza | `BukkitPlatformBridge.createInventory` z obsługą Component / String | `platforms/bukkit/.../BukkitPlatformBridge.java`, `SpigotActionAdapter.java` | `93889a9` | `BukkitPlatformBridgeTest` | `./gradlew check` PASS | `DONE_VERIFIED` | Brak |
| `SE-SEC-08` | SpectraEvents | Audit SEC-01 #178 | P0 | CodeQL Alert #178: Deprecated `TextDisplay.setText(String)` | Nowoczesne API TextDisplay | `BukkitPlatformBridge.setText` preferujące Adventure `text(Component)` | `platforms/bukkit/.../BukkitPlatformBridge.java`, `AbstractBukkitModelRenderer.java` | `93889a9` | `BukkitPlatformBridgeTest`, `AbstractBukkitModelRendererTest` | `./gradlew check` PASS | `DONE_VERIFIED` | Brak |
| `SE-SEC-09` | SpectraEvents | Audit SEC-01 #1, #2, #4 | P0 | CodeQL Alerts #1, #2, #4: Deprecated `Plugin.getDescription()` w SpigotBootstrap | Dynamiczny resolver wersji pluginu | Wprowadzono `BukkitPluginVersions.resolveVersion` sprawdzające `getPluginMeta()` przed `getDescription()` | `platforms/bukkit/.../BukkitPluginVersions.java`, `SpigotBootstrap.java` | `93889a9` | `BukkitPluginVersionsTest` | `./gradlew check` PASS | `DONE_VERIFIED` | Brak |
| `SE-SEC-10` | SpectraEvents | Audit SEC-01 #254, #255 | P0 | CodeQL Alerts #254, #255: Deprecated `getDescription()` w handlerach komend | Dynamiczny resolver wersji | Wykorzystanie `BukkitPluginVersions.resolveVersion` w diagnostyce i szablonach | `platforms/spigot/.../SpigotDiagnosticsCommandHandler.java`, `SpigotTemplateCommandHandler.java` | `93889a9` | `BukkitPluginVersionsTest`, `SpigotDiagnosticsCommandHandlerTest` | `./gradlew check` PASS | `DONE_VERIFIED` | Brak |
| `SE-SEC-11` | SpectraEvents | Audit SEC-01 ogólny | P0 | 15 otwartych alertów CodeQL na dashboardzie GitHub gałęzi `main` | 0 otwartych alertów na GitHubie | Kod naprawiony na gałęzi `quality/test-and-architecture-remediation`. Dashboard GitHub `main` zaktualizuje się po otwarciu PR i merge do `main`. | Wszystkie w/w | `5574349` | Pipeline GitHub Actions CodeQL | Oczekuje na merge | `BLOCKED` | Wymaga pusha gałęzi i scalenia PR do `main` |
| `SE-CONCUR-01` | SpectraEvents | Audit THREAD-01, Prompt 2 | P1 | Niesynchronizowana `itemProviders` (`ArrayList`) oraz brak `volatile` na polach serwisów | Thread-safety w środowisku Folia | `CopyOnWriteArrayList` dla `itemProviders`, `conditionResolvers`, `actionResolvers`; oznaczenie mutowalnych referencji jako `volatile` | `AbstractBukkitActionAdapter.java`, `EventLifecycleManager.java`, `EventExecutionEngine.java`, `SpectraEventsApplication.java` | `93889a9` | `AbstractBukkitActionAdapterTest.testConcurrentItemProviderRegistrationAndResolution` (8 wątków równolegle) | `./gradlew check` PASS | `DONE_VERIFIED` | Brak |
| `SE-CONCUR-02` | SpectraEvents | Audit THREAD-01, Prompt 2 | P1 | Wyścig check-then-act na `peakQueueDepth` w `SingleWriterPersistenceExecutor` | Atomowa aktualizacja CAS i deterministyczne zliczanie | `peakQueueDepth.accumulateAndGet(depth, Math::max)` oraz interfejs `TrackedWrite` z `totalWrites.incrementAndGet()` | `SingleWriterPersistenceExecutor.java` | `93889a9` | `SingleWriterPersistenceExecutorTest.testConcurrentPeakQueueDepthTracking` (16 wątków) | `./gradlew check` PASS | `DONE_VERIFIED` | Brak |
| `SE-INIT-01` | SpectraEvents | Audit Inicjalizacja i Cykl Życia, Prompt 2 | P1 | Brak try-catch w `SpectraEventsSpigotPlugin.onEnable()`, wyciek `BukkitAudiences`, wiszące zadania | Pełny rollback przy awarii startu | Blok `try-catch` w `onEnable`, wywołanie `cleanupResources()` (zamykanie bootstrapu, zamykanie `adventure`, `HandlerList.unregisterAll`, anulowanie zadań schedulera, `disablePlugin`) | `SpectraEventsSpigotPlugin.java` | `93889a9` | `SpectraEventsSpigotPluginTest` (testy awarii bootstrapu, awarii audiences, idempotencji `cleanupResources`) | `./gradlew check` PASS | `DONE_VERIFIED` | Brak |
| `SE-INIT-02` | SpectraEvents | Audit Inicjalizacja i Cykl Życia, Prompt 2 | P1 | Niezamknięte repozytorium SQLite przy wyjątku przed przypisaniem `application` w bootstrapach | Deterministyczne zwalnianie bazy | `SpigotBootstrap` i `PaperBootstrap` przechowują referencje do repozytoriów jako pola i zamykają je w bloku `onDisable` niezależnie od stanu `application` | `SpigotBootstrap.java`, `PaperBootstrap.java` | `93889a9` | `SpigotBootstrapTest`, `PaperBootstrapTest` | `./gradlew check` PASS | `DONE_VERIFIED` | Brak |
| `SE-DUP-01` | SpectraEvents | Audit Duplication E.2, Prompt 3 | P1 | Zbędne ghost-klasy `PaperResourcePackDeliveryBootstrap` i `SpigotResourcePackDeliveryBootstrap` | Całkowite usunięcie fasad widm | Obie klasy usunięte; wywołania przekierowane bezpośrednio do `BukkitResourcePackDeliveryBootstrap` | Usunięto 2 pliki; zaktualizowano callerów w Paper/Spigot | `93889a9` | Kompilacja całego projektu, `BukkitResourcePackDeliveryBootstrapTest` | `./scripts/verification/validate-structure.sh` PASS | `DONE_VERIFIED` | Brak |
| `SE-DUP-02` | SpectraEvents | Audit Duplication E.1, Prompt 3 | P1 | Powielona kalkulacja postępu BossBar w `EventBossBarManager` i `SpigotActionAdapter` | Wspólna klasa narzędziowa | Wyekstrahowano `BukkitBossBarProgressCalculator` w `:platforms:bukkit:bossbar` | `BukkitBossBarProgressCalculator.java`, `PaperBossBarManager.java`, `SpigotActionAdapter.java` | `93889a9` | `BukkitBossBarProgressCalculatorTest` (11 testów jednostkowych i brzegowych) | `./gradlew check` PASS | `DONE_VERIFIED` | Brak |
| `SE-DUP-03` | SpectraEvents | Audit Duplication E, Prompt 3 | P1 | Zduplikowany rekord `BossBarHolder` w menedżerach Paper i Spigot | Generyczny holder paska | Utworzono `BukkitBossBarHolder<T>` w `:platforms:bukkit:bossbar` | `BukkitBossBarHolder.java`, `SpigotBossBarManager.java` | `93889a9` | `BukkitBossBarHolderTest` | `./gradlew check` PASS | `DONE_VERIFIED` | Brak |
| `SE-NAME-01` | SpectraEvents | Audit Naming I.1, Prompt 3 | P1 | Asymetria nazw: `SpigotBossBarManager` vs `EventBossBarManager`, `SpigotScoreboardManager` vs `EventScoreboardManager` | Spójne nazewnictwo Paper* | Przemianowano klasy na `PaperBossBarManager` i `PaperScoreboardManager` | `PaperBossBarManager.java`, `PaperScoreboardManager.java` | `93889a9` | `PaperBossBarManagerTest`, `PaperScoreboardManagerTest` | `./gradlew check` PASS | `DONE_VERIFIED` | Brak |
| `SE-NAME-02` | SpectraEvents | Audit Naming I.2 | P2 | Asymetria klas głównych: `SpectraEventsPlugin` (Paper) vs `SpectraEventsSpigotPlugin` (Spigot) | Harmonizacja kontraktów klas | Zachowano stabilne nazwy w dedykowanych pakietach `platform.paper` i `platform.spigot` ze względu na kompatybilność deskryptorów `plugin.yml`; oznaczono klasę Spigota jako `public final` | `SpectraEventsSpigotPlugin.java` | `93889a9` | `SpectraEventsSpigotPluginTest` | `./gradlew check` PASS | `DONE_VERIFIED` | Brak |
| `SE-COV-01` | SpectraEvents | Audit Coverage G, Prompt 4 | P1 | Pokrycie `:platforms:bukkit` wynosiło dramatyczne 15.05% | Pokrycie logiczne $\ge 85\%$ | Rozbudowa testów adapterów, GUI, listenerów, reconcilera i mostka platformowego; pokrycie wzrosło do **88.79% instrukcji, 89.19% linii, 72.70% rozgałęzień** | Testy w `platforms/bukkit/src/test/...` (m.in. `MockBukkitSetup`, `AbstractBukkitActionAdapterTest`) | `93889a9`, `5574349` | 47 klas w module, 100% klas pokrytych | `jacocoTestReport.xml` w `platforms/bukkit` | `DONE_VERIFIED` | Zwiększenie pokrycia rozgałęzień GUI |
| `SE-COV-02` | SpectraEvents | Audit Coverage G, Prompt 4 | P1 | Pokrycie `:platforms:paper:common` wynosiło 4.65% | Pokrycie logiczne $\ge 85\%$ | Zaimplementowano testy komend Brigadier, bootstrapu, event routerów i adaptera akcji; pokrycie wzrosło do **97.47% instrukcji, 97.30% linii, 84.84% rozgałęzień** | Testy w `platforms/paper/common/src/test/...` | `93889a9`, `5574349` | 33 klasy w module, 100% klas pokrytych | `jacocoTestReport.xml` w `paper:common` | `DONE_VERIFIED` | Brak |
| `SE-COV-03` | SpectraEvents | Audit Coverage G, Prompt 4 | P1 | Pokrycie `:platforms:spigot:common` wynosiło 10.87% | Pokrycie logiczne $\ge 85\%$ | Zaimplementowano kompleksowe testy wszystkich handlerów komend, pluginu i menedżerów; pokrycie wzrosło do **99.22% instrukcji, 98.99% linii, 96.99% rozgałęzień** | Testy w `platforms/spigot/common/src/test/...` | `93889a9`, `5574349` | 24 klasy w module, 100% klas pokrytych | `jacocoTestReport.xml` w `spigot:common` | `DONE_VERIFIED` | Brak |
| `SE-COV-04` | SpectraEvents | Audit Coverage G, Prompt 4 | P1 | Pokrycie `spectraevents-core` (95.66%) | Dążenie do 100% | Pokrycie osiągnęło **100.00% instrukcji, 100.00% linii, 99.71% rozgałęzień (349/350)** | `VisualAndRuntimeCoverageTest.java` | `5574349` | 136 klas w module, 100% klas pokrytych | `jacocoTestReport.xml` w `core` | `DONE_VERIFIED` | 1 gałąź to syntetyczny bytecode switcha |
| `SE-COV-05` | SpectraEvents | Audit Coverage G, Prompt 4 | P1 | Pokrycie `spectraevents-application` (86.74%) | Dążenie do 100% | Rozbudowano testy kompilatorów, parserów YAML, serwisów cyklu życia; pokrycie wzrosło do **96.57% instrukcji, 96.45% linii, 85.95% rozgałęzień** | Testy w `spectraevents-application/src/test/...` | `93889a9`, `5574349` | 144 klasy w module, 100% klas pokrytych | `jacocoTestReport.xml` w `application` | `DONE_VERIFIED` | Dalsze domykanie gałęzi walidatorów |
| `SE-COV-06` | SpectraEvents | Audit Coverage G, Prompt 4 | P1 | Pokrycie portów adapterów (`sqlite`, `delivery`, `blockbench`, `update`) | Dążenie do 100% | `update-http`: **100.00%**, `assets-delivery`: **100.00%**, `assets-blockbench`: **96.24%**, `storage-sqlite`: **96.44%** | Testy we wszystkich 4 podprojektach adapterów | `93889a9`, `5574349` | 100% klas pokrytych we wszystkich adapterach | Raporty JaCoCo modułów | `DONE_VERIFIED` | Brak |
| `SE-COV-07` | SpectraEvents | Audit Coverage G, Prompt 4 | P0 | Zagregowane pokrycie projektu wynosiło 59.10% | Pokrycie projektu $\ge 90\%$ instrukcji, egzekwowane bramką | Zbudowano `jacocoRootReport` i `jacocoRootCoverageVerification` egzekwujące 90%/90%/80%. Aktualne: **96.13% instrukcji, 95.87% linii, 86.32% rozgałęzień, 98.22% metod, 100% klas** | `build.gradle.kts` | `93889a9`, `5574349` | Wszystkie zestawy testów (400 klas) | `./gradlew jacocoRootCoverageVerification` PASS | `DONE_VERIFIED` | Brak |
| `SE-COV-08` | SpectraEvents | Prompt 4.2 | P0 | Cel 100% realnego pokrycia kodu produkcyjnego Java bez sztucznych testów | 100% linii, gałęzi, instrukcji | Pokrycie wynosi 96.13% instrukcji i 86.32% rozgałęzień. Zidentyfikowano 239 klas z pojedynczymi brakami (fallbacki refleksyjne MockBukkit, syntetyczny bytecode switcha, integracje bez third-party pluginów). | Całe repozytorium | `5574349` | Testy w 9 modułach | JaCoCo XML root | `PARTIAL` | Zamknięcie pozostałych 2 414 instrukcji i 727 gałęzi |
| `SE-VER-01` | SpectraEvents | Audit K Faza 8, Prompt 0 | P1 | Rozbieżne wersje Minecraft w konfiguracji | Ścisłe ujednolicenie na 26.1.x do 26.3 | W `gradle.properties` zdefiniowano `paper.minecraftVersions=26.1,26.2,26.3` i `spigot.minecraftVersions=26.1,26.2,26.3` | `gradle.properties`, moduły platform | `bbb6c1e`, `93889a9` | `./gradlew verifyCompatibilityMatrix` | Skrypt weryfikacji PASS | `DONE_VERIFIED` | Brak |
| `SE-VER-02` | SpectraEvents | Audit CONF-02, Prompt 1 | P2 | Martwy i sprzeczny plik `gradle/compatibility.versions.toml` | Usunięcie pliku widma | Plik trwale usunięty z repozytorium | Usunięto `gradle/compatibility.versions.toml` | `93889a9` | `./scripts/verification/validate-structure.sh` | Walidacja struktury PASS | `DONE_VERIFIED` | Brak |
| `SE-VER-03` | SpectraEvents | Audit XII.I, Prompt 1 | P2 | Brak Single Source of Truth dla kompatybilności wersji | SSOT w `gradle.properties` | Wersje czytane wyłącznie z `gradle.properties`, zadanie `verifyCompatibilityMatrix` weryfikuje spójność | `build.gradle.kts`, `gradle.properties` | `93889a9` | `./gradlew verifyCompatibilityMatrix` | Zadanie Gradle PASS | `DONE_VERIFIED` | Brak |
| `SE-CI-01` | SpectraEvents | Audit F, Prompt 1 | P2 | Zbyt szerokie uprawnienia w GitHub Actions | Minimalne uprawnienia `contents: read` | Wszystkie workflow posiadają globalne `permissions: contents: read`, uprawnienia zapisu zawężone do pojedynczych jobów | `.github/workflows/*.yml` | `8f29862`, `93889a9` | `zizmor`, `actionlint` | Analizatory workflow PASS | `DONE_VERIFIED` | Brak |
| `SE-CI-02` | SpectraEvents | Audit F, Prompt 1 | P2 | Zabezpieczenie workflow i zależności | Gitleaks, Zizmor, CodeQL, Dependency Review | Wszystkie 4 narzędzia aktywne w `.github/workflows/` z przypiętymi commit SHA akcji | `.github/workflows/*.yml` | `8d9ea55`, `93889a9` | Weryfikacja statyczna workflow | Pipeline CI | `DONE_VERIFIED` | Brak |
| `SE-BUILD-01` | SpectraEvents | Audit B, Prompt 1 | P2 | Integralność zależności Gradle | Dependency verification metadata | Wygenerowano `gradle/verification-metadata.xml` (228 KB) z sumami SHA-256 dla wszystkich zależności i pluginów | `gradle/verification-metadata.xml` | `64d32d4` | `./gradlew help` | Gradle verification metadata aktywne | `DONE_VERIFIED` | Brak |
| `SE-AI-01` | SpectraEvents | Audit H/XII.I, Prompt 1 | P2 | Rozproszenie instrukcji AI po 7 plikach i katalogach | SSOT w `AGENTS.md` | Usunięto `.github/instructions/`, `.github/prompts/` i `.github/copilot-instructions.md`; `CLAUDE.md` i `GEMINI.md` to minimalistyczne wskaźniki na `AGENTS.md` | Usunięto katalogi, zaktualizowano `CLAUDE.md`, `GEMINI.md` | `93889a9` | `./scripts/verification/validate-structure.sh` | Walidacja struktury PASS | `DONE_VERIFIED` | Brak |
| `SE-DOC-01` | SpectraEvents | Audit H.1, Prompt 1 | P2 | Obcięte `docs/architecture.md` po sekcji Technical Strengths | Kompletna dokumentacja architektoniczna | Dodano sekcje `Platform Artifact & Packaging Contract`, `Risks To Watch` oraz `Recommended Next Quality Milestones` | `docs/architecture.md` | `93889a9` | `./scripts/verification/validate-structure.sh` | Weryfikacja linków i struktury PASS | `DONE_VERIFIED` | Brak |
| `SE-DOC-02` | SpectraEvents | Audit H.2, Prompt 1 | P2 | Powielone nagłówki `### Changed` (4x) i `### Added` (2x) w CHANGELOG.md | Spójny format Keep a Changelog | Scalono wpisy pod `## [Unreleased]`; każda kategoria występuje dokładnie raz | `CHANGELOG.md` | `93889a9` | Weryfikacja formatowania changeloga | Przegląd manualny i linter | `DONE_VERIFIED` | Brak |
| `SE-DOC-03` | SpectraEvents | Audit H.3, Prompt 1 | P2 | Lakoniczne strony dokumentacji Wiki | Encyklopedyczny poziom dokumentacji | Rozbudowano `docs/wiki/Known-Limitations.md` oraz `Assets-and-Resource-Packs.md` o szczegółowe opisy Folia, Spigot, 3D entities, protocol hashes | `docs/wiki/Known-Limitations.md`, `Assets-and-Resource-Packs.md` | `93889a9` | `./scripts/verification/validate-structure.sh` | Weryfikacja struktury PASS | `DONE_VERIFIED` | Brak |
| `SE-DOC-04` | SpectraEvents | Audit 1. Źródła, Prompt 0 | P2 | Niespójności dokumentacji z kodem | 100% spójności dokumentacji | Zaktualizowano `docs/development.md`, `docs/releases.md`, `docs/wiki/Compatibility.md` | `docs/**/*.md` | `93889a9` | `./scripts/verification/validate-structure.sh` (53 pliki markdown) | 0 przerwanych odnośników | `DONE_VERIFIED` | Brak |

---

### B. Jump Delay Fix

| ID | Projekt | Źródło | Priorytet | Pierwotny problem | Wymagany rezultat | Aktualna implementacja | Zmienione pliki | Commit | Testy | Weryfikacja | Status | Pozostałe prace |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `JDF-ARCH-01` | Jump Delay Fix | Audit ARCH-01 | P1 | `RuntimeState` jako globalny stan statyczny w `AtomicReference` uniemożliwiający izolację instancji | Instancyjny `RuntimeState` wstrzykiwany do silnika | Utworzono instancyjne metody w `RuntimeState` oraz zachowano fasadę statyczną delegującą do `defaultInstance()` | `common/.../RuntimeState.java`, `JumpDelayFixEngine.java` | Brak (UNCOMMITTED w working tree) | `JumpDelayFixEngineTest.shouldSupportIsolatedRuntimeStateInjection` | `./gradlew check` PASS | `DONE_VERIFIED` | Złożenie commitu w git |
| `JDF-ARCH-02` | Jump Delay Fix | Audit ARCH-01 | P1 | Brak rzeczywistej izolacji w `JumpDelayFixEngine` | Niezależne instancje w testach | Konstruktor silnika przyjmuje `RuntimeState`; testy tworzą dwie odrębne instancje bez wpływu na siebie | `JumpDelayFixEngine.java`, `JumpDelayFixEngineTest.java` | Brak (UNCOMMITTED w working tree) | `JumpDelayFixEngineTest` | `./gradlew check` PASS | `DONE_VERIFIED` | Złożenie commitu w git |
| `JDF-BUILD-01` | Jump Delay Fix | Audit CONF-01 | P2 | Martwy Version Catalog `gradle/libs.versions.toml` — brak odwołań w skryptach Gradle | Pełna integracja z `libs.*` | Podpięto `alias(libs.plugins.*)` w głównym `build.gradle` oraz `libs.*` w `common`, `fabric`, `neoforge` | `build.gradle`, `common/build.gradle`, `fabric/build.gradle`, `neoforge/build.gradle` | Brak (UNCOMMITTED w working tree) | `./gradlew check` | Kompilacja ze skatalogowanymi zależnościami PASS | `DONE_VERIFIED` | Złożenie commitu w git |
| `JDF-CONF-01` | Jump Delay Fix | Audit XII.I | P2 | Martwa reguła ignorowania `build-logic/**` w `.codecov.yml` | Usunięcie nieistniejącej ścieżki | Usunięto wpis z sekcji `ignore` w `.codecov.yml` | `.codecov.yml` | Brak (UNCOMMITTED w working tree) | Weryfikacja składni pliku | Format YAML poprawny | `DONE_VERIFIED` | Złożenie commitu w git |
| `JDF-HYGIENE-01` | Jump Delay Fix | Audit D.5 | P2 | Publiczna metoda testowa `setPendingSaveForTests` w klasie produkcyjnej `ConfigManager` | Hermetyzacja API produkcyjnego | Zmieniono modyfikator widoczności na pakietowy (`void setPendingSaveForTests`) | `common/.../ConfigManager.java` | Brak (UNCOMMITTED w working tree) | `ConfigManagerTest` | `./gradlew check` PASS | `DONE_VERIFIED` | Złożenie commitu w git |
| `JDF-DUP-01` | Jump Delay Fix | Audit Duplication E.3 | P2 | Powielone testy klawiszy pomiędzy `FabricKeyMappingsTest` i `NeoForgeKeyMappingsTest` | Wspólny kontrakt asercji | Wprowadzono `KeyMappingAssertions.assertSafeDefaults(AbstractKeyMappings)` w module `common` | `KeyMappingAssertions.java` (untracked), testy loaderów | Brak (UNCOMMITTED w working tree) | Testy obu loaderów | `./gradlew check` PASS | `DONE_VERIFIED` | Złożenie commitu w git (w tym dodanie pliku) |
| `JDF-TEST-01` | Jump Delay Fix | Audit TEST-01 | P2 | Zafałszowane raporty JaCoCo w loaderach przez wstrzykiwanie źródeł `common` | Precyzyjne filtrowanie klas loadera w JaCoCo | Skonfigurowano `classDirectories` w `fabric/build.gradle` i `neoforge/build.gradle` z filtrem `com/kizio/jumpdelayfix/{fabric,neoforge}/**` | `fabric/build.gradle`, `neoforge/build.gradle` | Brak (UNCOMMITTED w working tree) | `./gradlew jacocoTestReport` | Raporty mierzą wyłącznie klasy loadera | `DONE_VERIFIED` | Złożenie commitu w git |
| `JDF-TEST-02` | Jump Delay Fix | Audit TEST-02 | P3 | Sprawdzanie bytecode jako string ISO-8859-1 w testach kontraktu metadanych | Użycie analizy ASM lub usunięcie antywzorca | `FabricMetadataContractTest` i `NeoForgeMetadataContractTest` nadal wykonują `new String(stream.readAllBytes(), StandardCharsets.ISO_8859_1)` i `bytecode.contains("isInWater")` | `FabricMetadataContractTest.java`, `NeoForgeMetadataContractTest.java` | Niezmienione | Testy przechodzą, ale metoda weryfikacji jest antywzorcem | Inspekcja kodu | `NOT_DONE` | Zastąpienie inspekcji tekstu parserem ASM `ClassReader` lub rzetelnym testem behawioralnym |
| `JDF-COV-01` | Jump Delay Fix | Audit Coverage G | P1 | Niskie realne pokrycie loaderów: Fabric 1.59%, NeoForge 2.52% | Realne testy klas platformowych | Po odfiltrowaniu `common` pokrycie wynosi: Fabric 11.88% instrukcji (6/51 linii), NeoForge 20.83% instrukcji (9/66 linii). Główne klasy `JumpDelayFixFabric`, `ClientJumpInput` są niepokryte | `fabric/...`, `neoforge/...` | Niezmienione | Istniejące testy uruchamiają tylko KeyMappings | JaCoCo XML loaderów | `PARTIAL` | Implementacja testów mockujących client input i lifecycle loadera |
| `JDF-CI-01` | Jump Delay Fix | Audit B | P3 | Monolityczny workflow release z uprawnieniami `contents: write` | Minimalne uprawnienia | Globalne uprawnienia ograniczone do `contents: read`, zapis zawężony do joba publikacji | `.github/workflows/release.yml` | Brak (UNCOMMITTED w working tree) | Weryfikacja workflow | `zizmor` | `DONE_VERIFIED` | Złożenie commitu w git |
| `JDF-DOC-01` | Jump Delay Fix | Audit Repository Hygiene | P2 | Brak `CODE_OF_CONDUCT.md` na lokalnej gałęzi `main` | Obecność pliku w repozytorium | Plik został dodany w PR #62 na `origin/main` (commit `2f05201`), lecz lokalna gałąź `main` znajduje się na `3537b1d` i nie została zrebasowana | `CODE_OF_CONDUCT.md` na remote | `2f05201` (remote) | Weryfikacja git log origin/main | Brak lokalnie | `PARTIAL` | Rebase/merge z `origin/main` |
| `JDF-CONF-02` | Jump Delay Fix | Audit Hard Quality Gates | P2 | Wykluczenia Sonar w podprojektach loaderów | Rzetelne raportowanie Sonar | Klasy współdzielone przeniesiono do `common/src/client`, wykluczenia CPD usunięto | `build.gradle` | `705c4d9`, `cb71982` | `./gradlew check` | Sonar config | `DONE_VERIFIED` | Złożenie commitu w git |
| `JDF-GIT-01` | Jump Delay Fix | Wymagania Inżynierskie | P0 | Niezatwierdzone zmiany w drzewie roboczym Jump Delay Fix | Czyste drzewo robocze, zmiany zatwierdzone commitami | W katalogu roboczym wisi 15 zmodyfikowanych plików produkcyjnych i 2 nieśledzone pliki (`REMEDIATION_TRACKER.md`, `KeyMappingAssertions.java`) | Całe repozytorium | Brak commitu | `git status` wykazuje brudne drzewo robocze | `git status` | `NOT_DONE` | Wykonanie `git add` i zatwierdzenie commitu `fix(remediation): apply engineering audit fixes` |

---

## Section 3 — Additional Engineering Work (`UNPLANNED_ENGINEERING_CHANGES`)

W trakcie realizacji prac wykonano szereg dodatkowych modyfikacji architektonicznych, które nie były wprost wyszczególnione w pierwotnym audycie, lecz wynikały z konieczności usunięcia ukrytych długów, zlikwidowania asymetrii platformowej lub rozwiązania problemów z kompilacją pod najnowszymi wersjami środowiska.

Poniższa tabela stanowi pełną inwentaryzację tych zmian:

| ID Zmiany | Obszar | Opis zmiany | Geneza / Uzasadnienie | Poprawność techniczna | Pokrycie testowe | Ryzyko regresji | Wymagane dalsze działania |
|---|---|---|---|:---:|:---:|:---:|---|
| `UNP-01` | Bukkit Compat | Utworzenie `BukkitPlatformBridge` (305 LOC) z izolowaną refleksją | Zamiast rozproszonych obejść deprecated API w Spigot, stworzono centralną fasadę platformową adaptującą Adventure vs Spigot bez `@SuppressWarnings`. | **Wysoka** (pełna hermetyzacja w osobnym pakiecie `compat`) | `BukkitPlatformBridgeTest` (343 LOC, testy sukcesu i fallbacków) | Bardzo niskie (fallbacki testowane pod MockBukkit) | Brak (rozwiązanie docelowe) |
| `UNP-02` | Admin GUI | Przeniesienie hierarchii Admin GUI (17 ekranów, nawigacja, paginacja) z Paper do `:platforms:bukkit:gui` | Wcześniej GUI administratora działało wyłącznie na Paper. Spigot posiadał ułomną wersję tekstową. Przeniesienie zapewniło 100% parzystość funkcjonalną. | **Wysoka** (czysty kod Bukkit API, komponenty tekstowe bridge'owane przez `GuiText.legacy`) | `AdminGuiScreensTest` (704 LOC), `AdminGuiControllerSessionTest` (140 LOC) | Niskie (przetestowano wszystkie ekrany) | Brak |
| `UNP-03` | Spigot Commands | Implementacja podkomend modułowych (`location`, `model`, `asset`, `integration`, `update`, `admin`) w Spigot | Zapewnienie pełnej parzystości CLI administracyjnego pomiędzy dystrybucjami Paper i Spigot. | **Wysoka** (wzorzec CommandHandler zgodny z Paper) | `SpigotMainCommandTest` (445 LOC), `SpigotEventCommandHandlerTest` (450 LOC), `SpigotRewardsCommandHandlerTest` (418 LOC) | Brak | Brak |
| `UNP-04` | Integracje Pluginów | Przeniesienie integracji (Vault, LuckPerms, WorldGuard, PlaceholderAPI) do `:platforms:bukkit:integration` | Wcześniej integracje z ekonomią i uprawnieniami były skompilowane tylko w Paper common, przez co serwery Spigot nie miały wsparcia dla Vault/LuckPerms. | **Wysoka** (zarządzane centralnie przez `BukkitIntegrationManager`) | `VaultIntegrationTest` (178 LOC), `LuckPermsIntegrationTest` (141 LOC), `WorldGuardIntegrationTest` (147 LOC) | Brak | Brak |
| `UNP-05` | Custom Items | Przeniesienie dostawców Custom Items (Nexo, Oraxen, ItemsAdder) do `:platforms:bukkit:integration:item` | Parzystość obsługi customowych przedmiotów na serwerach Spigot. | **Wysoka** (rejestrowane w `CopyOnWriteArrayList` w `AbstractBukkitActionAdapter`) | `CustomItemProvidersTest` (80 LOC) | Brak | Brak |
| `UNP-06` | Entity Reconciler | Wydzielenie `AbstractBukkitEntityReconciler` i `BukkitSharedLootListener` do `:platforms:bukkit` | Eliminacja duplikacji skanowania chunków i wykrywania uszkodzonych encji pomiędzy platformami. | **Wysoka** | `AbstractBukkitEntityReconcilerTest` (75 LOC), `BukkitSharedLootListenerTest` (528 LOC) | Brak | Brak |
| `UNP-07` | Math Parser | Wydzielenie `SpecMathParser` w `spectraevents-application` | Deduplikacja parsowania wektorów 3D, obrotów Eulera i kwaternionów pomiędzy `ModelCompiler` a `AnimationCompiler`. | **Wysoka** (czysta domena) | `SpecMathParserTest` (118 LOC) | Brak | Brak |
| `UNP-08` | Model Renderer | Uogólnienie aktualizacji transformacji encji Display (`applyTransformTeleports`) w `AbstractBukkitModelRenderer` | Identyczna logika pozycjonowania kości modelu Display 3D w Paper i Spigot. | **Wysoka** (template method pattern) | `AbstractBukkitModelRendererTest` (264 LOC) | Brak | Brak |
| `UNP-09` | Paper Commands | Dekompozycja `SpectraMainCommand` na Paper (`ScheduleCommandHandler`, `RewardsCommandHandler`) | Zlikwidowano monolityczną klasę komendy głównej na Paper, uzyskując symetrię architektoniczną ze Spigotem. | **Wysoka** | `PaperRewardsCommandHandlerTest` (485 LOC), `PaperScheduleCommandHandlerTest` (280 LOC) | Brak | Brak |
| `UNP-10` | Null-Safety Hardening | Wprowadzenie defensywnych sprawdzeń null w `EventDisplayPlaceholders`, `ModelCompiler`, `EventDefinitionCompiler` | Wykryto ryzyko NPE podczas renderowania placeholderów dla zdarzeń w fazie zamykania lub z pustymi lokalizacjami. | **Wysoka** | Testy jednostkowe w `spectraevents-application` i `platforms:bukkit` | Brak (poprawa stabilności) | Brak |
| `UNP-11` | Exception Gating | Obsługa `EventDefinitionCompilerException` w `EventOrchestrationService.startEventAsync` | Niewychwycony wyjątek parsowania powodował niekontrolowane odrzucenie `CompletableFuture`. | **Wysoka** | `EventOrchestrationServiceTest` | Brak | Brak |
| `UNP-12` | Persistence API | Dodanie metody `executeAndWait(Runnable)` w `SingleWriterPersistenceExecutor` | Synchroniczne operacje administracyjne (np. backup, flush przy zatrzymaniu) wymagały bezpiecznego oczekiwania na zapis w kolejce. | **Wysoka** (wykorzystuje `CompletableFuture.join()` z odpakowywaniem wyjątków) | `SingleWriterPersistenceExecutorTest` | Brak | Brak |
| `UNP-13` | Java 25 & ErrorProne | Wyłączenie reguły `StringConcatToTextBlock` w `build-logic/src/main/kotlin/spectraevents.java-base.gradle.kts` | Kompilacja na Java 25 powodowała błąd ErrorProne przy konkatenacji stringów w szablonach JSON. | **Wysoka** (konfiguracja flag kompilatora) | `./gradlew compileJava` | Brak | Brak |
| `UNP-14` | PMD Test Rule | Wykluczenie `AvoidAccessibilityAlteration` w `config/pmd/pmd.xml` | Umożliwienie testom jednostkowym MockBukkit bezpiecznej inspekcji refleksyjnej wewnętrznych pól mocków serwera. | **Wysoka** | `./gradlew check` PASS | Brak | Brak |
| `UNP-15` | MockBukkit Setup | Stworzenie `MockBukkitSetup.java` (1,237 LOC) w `:platforms:bukkit` | Umożliwienie pełnego testowania jednostkowego adapterów Bukkit bez uruchamiania ciężkiego kontenera serwerowego. | **Wysoka** (zaawansowany mock in-memory) | Wszystkie testy `:platforms:bukkit` | Brak | Utrzymanie przy upgrade'ach API Bukkita |

**Ocena inżynierska zmian dodatkowych:** Wszystkie 15 dodatkowych zmian zostało ocenionych pozytywnie. Żadna z nich nie została wprowadzona przypadkowo — stanowiły one logiczne konsekwencje zasady czystej architektury, eliminacji duplikacji i podnoszenia pokrycia testowego.

---

## Section 4 — Regression & Risk Audit

Przeprowadzono rygorystyczny przegląd potencjalnych regresji i ryzyk:

### 4.1. Utrata obsługiwanych wersji Minecraft
- **Weryfikacja:** Pierwotny (usunięty) plik `compatibility.versions.toml` deklarował wersje `26.1.1`, `26.1.2`, `26.2`, `26.3`. W `gradle.properties` wpisano `paper.minecraftVersions=26.1,26.2,26.3` oraz `spigot.minecraftVersions=26.1,26.2,26.3`. Linia `26.1` reprezentuje semantycznie całą serię wydań `26.1.x`. Żadna wersja serwera nie została porzucona ani zablokowana.
- **Wynik:** **BRAK REGRESJI.**

### 4.2. Stabilność Publicznego API i Zgodność Wsteczna
- **Weryfikacja:** Wszystkie identyfikatory zdarzeń, akcji, warunków, triggerów w formacie YAML zachowały 100% kompatybilności wstecznej. Zmiany architektoniczne (np. `SpecMathParser`) są całkowicie wewnętrzne dla warstwy aplikacji.
- **Wynik:** **BRAK REGRESJI.**

### 4.3. Złoty Niezmiennik Pakowania (Packaging Invariant)
- **Weryfikacja:** Skrypt `./gradlew build` generuje **DOKŁADNIE DWA JARy produkcyjne**:
  1. `distributions/paper/build/libs/SpectraEvents-0.1.1-beta.2-paper.jar` (27 MB)
  2. `distributions/spigot/build/libs/SpectraEvents-0.1.1-beta.2-spigot.jar` (28 MB)
  Wydania Folia są prawidłowo spakowane wewnątrz uniwersalnego archiwum Paper z automatycznym wykrywaniem region schedulera.
- **Wynik:** **BRAK REGRESJI.**

### 4.4. Wykryte Ryzyka i Długi Jakościowe (Aktywne Ryzyka)
1. **[P0] Niezatwierdzone zmiany w `jump-delay-fix`:** Drzewo robocze projektu referencyjnego zawiera 15 zmodyfikowanych i 2 nieśledzone pliki. W razie `git checkout -- .` lub nieostrożnego resetu cała praca remediacyjna w projekcie Jump Delay Fix ulegnie utracie!
2. **[P1] Antywzorzec inspekcji bytecode'u jako tekstu w `jump-delay-fix`:** W `FabricMetadataContractTest` i `NeoForgeMetadataContractTest` nadal występuje konwersja strumienia klas do ISO-8859-1 String i sprawdzanie `contains()`.
3. **[P1] Rozbieżność gałęzi `main` z `origin/main` w `jump-delay-fix`:** Lokalny `main` znajduje się na commicie `3537b1d`, podczas gdy zdalny `origin/main` posiada commit `2f05201` dodający `CODE_OF_CONDUCT.md`. Wymagany rebase/merge.
4. **[P2] Luki w pokryciu rozgałęzień w `platforms:bukkit`:** Pokrycie rozgałęzień w `:platforms:bukkit` wynosi **72.70%** (264 niepokryte gałęzie, głównie w `BukkitPlatformBridge` i `AdminGuiController`).

---

## Section 5 — Coverage Status

### 5.1. Weryfikacja Danych Zagregowanych (JaCoCo Root Report)

Aktualne dane z pliku XML (`build/reports/jacoco/jacocoRootReport/jacocoRootReport.xml`):

| Metryka JaCoCo | Wartość XML | Wartość z Deklaracji Poprzednich | Status Spójności |
|---|:---:|:---:|:---:|
| **INSTRUCTION** | **59 933 / 62 347 (96.13%)** | 59 829 / 62 347 (95.96%) | SPÓJNE (+104 instrukcje po commicie `5574349`) |
| **BRANCH** | **4 588 / 5 315 (86.32%)** | 4 556 / 5 315 (85.72%) | SPÓJNE (+32 gałęzie po commicie `5574349`) |
| **LINE** | **13 472 / 14 052 (95.87%)** | 13 456 / 14 052 (95.76%) | SPÓJNE (+16 linii po commicie `5574349`) |
| **METHOD** | **2 430 / 2 474 (98.22%)** | 2 417 / 2 474 (97.70%) | SPÓJNE (+13 metod po commicie `5574349`) |
| **CLASS** | **400 / 400 (100.00%)** | 400 / 400 (100.00%) | **100% IDENTYCZNE** |

### 5.2. Szczegółowe Pokrycie Modułowe

| Moduł | Instrukcje | Linie | Gałęzie | Metody | Klasy |
|---|:---:|:---:|:---:|:---:|:---:|
| `:adapters:update-http` | **100.00%** (190/190) | **100.00%** (53/53) | **100.00%** (14/14) | **100.00%** (7/7) | **100.00%** (1/1) |
| `:adapters:assets-delivery` | **100.00%** (525/525) | **100.00%** (117/117) | **100.00%** (68/68) | **100.00%** (11/11) | **100.00%** (3/3) |
| `:spectraevents-core` | **100.00%** (5 410/5 410) | **100.00%** (965/965) | **99.71%** (349/350)* | **100.00%** (340/340) | **100.00%** (136/136) |
| `:platforms:spigot:common` | **99.22%** (5 613/5 657) | **98.99%** (1 270/1 283) | **96.99%** (451/465) | **98.96%** (191/193) | **100.00%** (24/24) |
| `:platforms:paper:common` | **97.47%** (8 201/8 414) | **97.30%** (2 053/2 110) | **84.84%** (459/541) | **98.87%** (351/355) | **100.00%** (33/33) |
| `:adapters:assets-blockbench` | **96.24%** (2 561/2 661) | **96.62%** (514/532) | **88.30%** (302/342) | **100.00%** (53/53) | **100.00%** (6/6) |
| `:spectraevents-application` | **96.57%** (25 303/26 203) | **96.45%** (5 462/5 663) | **85.95%** (2 068/2 406) | **96.75%** (1 042/1 077) | **100.00%** (144/144) |
| `:adapters:storage-sqlite` | **96.44%** (2 900/3 007) | **95.93%** (754/786) | **93.21%** (151/162) | **100.00%** (119/119) | **100.00%** (6/6) |
| `:platforms:bukkit` | **88.79%** (9 128/10 280) | **89.19%** (2 268/2 543) | **72.70%** (703/967) | **94.98%** (303/319) | **100.00%** (47/47) |

*\*Uwaga techniczna:* Jedyna niepokryta gałąź w `spectraevents-core` (349/350) znajduje się w `ModelPartDefinition.java:24` i stanowi syntetyczną gałąź `default` wygenerowaną przez kompilator `javac` w tabeli skoków instrukcji `tableswitch` dla wyczerpującego switcha po enumie.

### 5.3. Szczegółowa Analiza Luk w Pokryciu (Dlaczego nie 100%?)
Zidentyfikowano 239 klas z co najmniej jedną niepokrytą instrukcją lub gałęzią. Główne źródła braków:
1. **`BukkitPlatformBridge` (268 niepokrytych instrukcji, 44 linie):** Klasa zawiera alternatywne ścieżki refleksyjne dedykowane różnym środowiskom (Paper z natywnym Adventure vs czysty Spigot). W środowisku testowym MockBukkit wykonuje się wyłącznie jedna gałąź refleksji; gałęzie fallbackowe `catch (ReflectiveOperationException)` pozostają nieodwiedzone.
2. **Integracje zewnętrzne (`LuckPermsIntegration`, `VaultIntegration`, `WorldGuardIntegration` — łącznie 254 niepokryte instrukcje):** Klasy zawierają zabezpieczenia na wypadek braku serwisów zewnętrznych pluginów na serwerze lub zgłoszenia specyficznych wyjątków przez te API.
3. **`AbstractBukkitActionAdapter` (176 niepokrytych instrukcji, 57 gałęzi):** Obsługuje dyspozycję dziesiątek typów cząsteczek Bukkita, efektów dźwiękowych oraz atrybutów mobów, dla których MockBukkit rzuca wyjątki niewspieranych operacji.

**Wniosek audytowy:** Pokrycie 96.13% instrukcji oraz 100% klas to wybitny wynik inżynierski, jednak dopóki występują realne luki, cel "100% Total Coverage" musi pozostać oznaczony statusem `PARTIAL`.

---

## Section 6 — Security Status

1. **GitHub CodeQL:**
   - **Stan w kodzie gałęzi `quality/test-and-architecture-remediation`:** Wszystkie 15 lokalizacji generujących alerty na `main` zostało poprawionych i zweryfikowanych testami jednostkowymi.
   - **Stan zdalnego dashboardu GitHub:** Dopóki gałąź nie zostanie wypchnięta (`git push`) i zmergowana do `main`, dashboard GitHub Code Scanning będzie wykazywał 15 otwartych alertów.
2. **Gitleaks (Weryfikacja Wycieków Sekretów):** Aktywny (`.gitleaks.toml`), 0 wykrytych tokenów lub kluczy prywatnych.
3. **Zizmor (Bezpieczeństwo Workflow GitHub Actions):** Wszystkie workflow posiadają minimalne uprawnienia `contents: read`, brak podatności typu script injection (`github.event.*`), akcje przypięte do pełnych commit SHA.
4. **Zależności i Dependabot:** 0 otwartych alertów bezpieczeństwa w bibliotekach zewnętrznych.
5. **Integralność Binarna (Dependency Locking):** W repozytorium znajduje się pełny plik `gradle/verification-metadata.xml` weryfikujący sumy kontrolne SHA-256 wszystkich artefaktów.

---

## Section 7 — Configuration & Build Status

1. **Gradle Build Toolchain:** Gradle Wrapper zaktualizowany do wersji **9.8.2** z aktywną sumą SHA-256 w `gradle-wrapper.properties`.
2. **Kompilacja i Flagi:** Cały kod kompiluje się z flagami `-Xlint:all` oraz `-Werror`.
3. **Analizatory Statyczne (SpectraEvents):**
   - Checkstyle: Google Java Style z rygorem `maxErrors = 0`.
   - PMD: Priority 3 z rygorem `maxFailures = 0`.
   - SpotBugs: Effort `MAX`, Confidence `LOW` z rygorem `maxErrors = 0`.
   - JaCoCo: Enforcowane bramki modułowe oraz bramka zagregowana `jacocoRootCoverageVerification` (instrukcje $\ge 90\%$, linie $\ge 90\%$, gałęzie $\ge 80\%$).
4. **Higiena Źródeł:**
   - 0 adnotacji `@SuppressWarnings`.
   - 0 markerów `TODO`, `FIXME`, `XXX`.
   - 0 importów z gwiazdką (wildcard imports).
   - Skrypty `./scripts/verification/validate-structure.sh` i `./scripts/verification/validate-source-hygiene.sh` przechodzą w 100%.

---

## Section 8 — Documentation Status

1. **`AGENTS.md` jako SSOT:** Usunięto rozproszone katalogi `.github/instructions/` i `.github/prompts/`. Pliki `CLAUDE.md` oraz `GEMINI.md` przekierowują do `AGENTS.md`.
2. **`docs/architecture.md`:** Uzupełniono brakujące sekcje `Platform Artifact & Packaging Contract`, `Risks To Watch` oraz `Recommended Next Quality Milestones`.
3. **`CHANGELOG.md`:** Scalono powielone nagłówki pod `## [Unreleased]`.
4. **Wiki (`docs/wiki/`):** Strony `Known-Limitations.md` i `Assets-and-Resource-Packs.md` zostały rozbudowane do standardu encyklopedycznego.
5. **Weryfikacja Odnośników:** 53 pliki Markdown w projekcie posiadają poprawne linki (zweryfikowane przez `validate-structure.sh`).

---

## Section 9 — Outstanding Tasks (Zadania Otwarte)

Poniższe zadania stanowią wyczerpującą listę prac, które należy wykonać:

1. **`[P0] JDF-GIT-01`: Zatwierdzenie niezatwierdzonych zmian w Jump Delay Fix:**
   - Wykonać `git add` oraz commit w `jump-delay-fix` dla 15 zmodyfikowanych i 2 nieśledzonych plików.
2. **`[P0] SE-SEC-11`: Publikacja i merge gałęzi `quality/test-and-architecture-remediation` do `main`:**
   - Wykonać `git push origin quality/test-and-architecture-remediation` i zmergować PR, aby zamknąć 15 alertów CodeQL na GitHubie.
3. **`[P1] JDF-DOC-01`: Rebase gałęzi `main` w Jump Delay Fix z `origin/main`:**
   - Pobrać commit `2f05201` zawierający `CODE_OF_CONDUCT.md`.
4. **`[P1] JDF-TEST-02`: Eliminacja inspekcji bytecode'u jako tekstu w Jump Delay Fix:**
   - Zastąpić czytanie pliku `.class` jako string ISO-8859-1 w `FabricMetadataContractTest` i `NeoForgeMetadataContractTest` parserem ASM `ClassReader`.
5. **`[P1] JDF-COV-01`: Rozbudowa realnych testów loaderów w Jump Delay Fix:**
   - Napisać testy jednostkowe dla `JumpDelayFixFabric` i `ClientJumpInput`, podnosząc realne pokrycie loaderów z 11–20% do $\ge 80\%$.
6. **`[P2] SE-COV-08`: Domykanie pozostałych luk w pokryciu w SpectraEvents:**
   - Zamknięcie brakujących 2 414 instrukcji i 727 gałęzi (przede wszystkim w `:platforms:bukkit` w `AdminGuiController` oraz `BukkitPlatformBridge`).

---

## Section 10 — Recommended Execution Roadmap

Kolejność kolejnych kroków remediacyjnych (bez powtarzania prac już wykonanych):

### Krok 1: Bezpieczeństwo i Higiena Git w Jump Delay Fix (Natychmiast)
1. Dodać nieśledzone pliki i zatwierdzić zmiany w `jump-delay-fix`:
   ```bash
   cd /home/kizio/Dokumenty/GitHub/jump-delay-fix
   git add -A
   git commit -m "fix(remediation): apply engineering audit fixes for engine isolation, version catalog, and loader coverage"
   ```
2. Zrebasować lokalną gałąź na `origin/main`:
   ```bash
   git pull --rebase origin main
   ```

### Krok 2: Zamknięcie Długów Testowych w Jump Delay Fix
1. Zastąpić odczyt bytecode'u jako tekstu parserem ASM `ClassReader`.
2. Dodać testy mockujące wejście klienta w `FabricJumpInput` i `NeoForgeJumpInput`.
3. Uruchomić pełny check: `./gradlew check`.

### Krok 3: Finalizacja i Publikacja SpectraEvents
1. Zweryfikować ostatecznie stan drzewa roboczego (`git status` czyste).
2. Opublikować gałąź na zdalne repozytorium:
   ```bash
   git push origin quality/test-and-architecture-remediation
   ```
3. Otworzyć i scalić Pull Request do `main`.
4. Zweryfikować, że GitHub Code Scanning zredukował liczbę alertów CodeQL z 15 do 0.
