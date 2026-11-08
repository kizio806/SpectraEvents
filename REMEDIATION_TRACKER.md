# REMEDIATION TRACKER — SPECTRAEVENTS & JUMP DELAY FIX
**Dokument Identyfikacyjny:** `REMEDIATION-TRACKER-2026-11`  
**Data utworzenia:** 7 listopada 2026 r.  
**Ostatnia aktualizacja:** 8 listopada 2026 r. (Reconciliation Audit)  
**Autor:** Principal Java Engineer, Software Architect, Security Engineer & Quality Auditor  
**Cel:** Doprowadzenie projektów `SpectraEvents` oraz `jump-delay-fix` do bezkompromisowego standardu inżynierskiego na podstawie raportu audytu `AUDIT-JDF-SE-2026-11`.

---

## 1. Zasady Utrzymania Rejestru

1. Żadne zadanie nie może zostać usunięte ani uznane za `DONE_VERIFIED` bez bezpośredniej weryfikacji kodem i testem.
2. Historia zmian każdego zadania musi pozostać w pełni audytowalna.
3. Dozwolone statusy: `DONE_VERIFIED`, `PARTIAL`, `NOT_DONE`, `BLOCKED`, `NOT_REPRODUCIBLE`, `SUPERSEDED`.
4. Rygor jakościowy: zero `@SuppressWarnings`, zero `TODO`/`FIXME`, zero wildcard imports, `validate-structure.sh` PASS, `validate-source-hygiene.sh` PASS, `clean check build` PASS.

---

## 2. Podsumowanie Realizacji (Reconciliation Audit)

| Projekt | Łącznie | DONE_VERIFIED | PARTIAL | NOT_DONE | BLOCKED | Procent Ukończenia |
|---|---:|---:|---:|---:|---:|---:|
| **SpectraEvents** | 40 | 38 | 1 | 0 | 1 | 95.00% |
| **Jump Delay Fix** | 13 | 9* | 2 | 2 | 0 | 69.23% |
| **SUMA (ŁĄCZNIE)** | **53** | **47** | **3** | **2** | **1** | **88.68%** |

*\*Uwaga audytowa:* Wszystkie 9 zadań `DONE_VERIFIED` w `jump-delay-fix` znajduje się w stanie **NIEZATWIERDZONYM (UNCOMMITTED)** w drzewie roboczym lokalnej gałęzi `main`. Wymagają natychmiastowego commitu (`JDF-GIT-01`).

---

## 3. Rejestr Zadań — SpectraEvents

### Etap 2: Krytyczne Błędy Działania (P0)

#### `SE-BUG-01`
- **Priorytet:** `P0`
- **Opis problemu:** W `AbstractBukkitPlayerResourcePackAdapter.java:72-80` status `DOWNLOADED` (Minecraft 1.20.3+) wpadał w gałąź `default -> FAILED`. W liniach 57–60 przy wymaganym resource packu serwer Spigot natychmiast wyrzucał (`kick`) każdego gracza po pobraniu paczki zasobów. Brakowało obsługi `INVALID_URL`, `FAILED_RELOAD` i `DISCARDED`, a nieznane przyszłe statusy były bezrefleksyjnie traktowane jako błąd pobierania.
- **Lokalizacja w kodzie:** `platforms/bukkit/src/main/java/io/github/kizio806/spectraevents/platform/bukkit/asset/delivery/AbstractBukkitPlayerResourcePackAdapter.java`
- **Plan naprawy:**
  1. Zaimplementować pełne mapowanie wszystkich statusów (`ACCEPTED`, `DOWNLOADED`, `SUCCESSFULLY_LOADED`, `DECLINED`, `FAILED_DOWNLOAD`, `INVALID_URL`, `FAILED_RELOAD`, `DISCARDED`).
  2. Rozszerzyć domenowy enum `PlayerResourcePackState` o stan `DISCARDED` z poprawną semantyką w `PlayerResourcePackService.getReadiness`.
  3. Nieznane i puste statusy traktować jako `null` w `mapStatus` i bezpiecznie ignorować w `onPackStatus` (nie wyrzucając gracza).
  4. Wyekstrahować współdzieloną platformową logikę wysyłki i kickowania do `BukkitPlatformBridge`.
- **Zależności:** Brak
- **Status:** `DONE_VERIFIED`
- **Zmienione pliki:**
  - `spectraevents-application/src/main/java/io/github/kizio806/spectraevents/application/asset/delivery/PlayerResourcePackState.java`
  - `spectraevents-application/src/main/java/io/github/kizio806/spectraevents/application/asset/delivery/PlayerResourcePackService.java`
  - `platforms/bukkit/src/main/java/io/github/kizio806/spectraevents/platform/bukkit/asset/delivery/AbstractBukkitPlayerResourcePackAdapter.java`
  - `platforms/paper/common/src/main/java/io/github/kizio806/spectraevents/platform/paper/asset/delivery/PaperPlayerResourcePackAdapter.java`
  - `platforms/spigot/common/src/main/java/io/github/kizio806/spectraevents/platform/spigot/asset/delivery/SpigotPlayerResourcePackAdapter.java`
- **Test potwierdzający naprawę:**
  - `platforms/bukkit/src/test/java/io/github/kizio806/spectraevents/platform/bukkit/asset/delivery/AbstractBukkitPlayerResourcePackAdapterTest.java` (testy jednostkowe i sparametryzowane dla wszystkich 8 stanów pod kątem polityki kickowania)
  - `platforms/paper/common/src/test/java/io/github/kizio806/spectraevents/platform/paper/asset/delivery/PaperPlayerResourcePackAdapterTest.java`
  - `spectraevents-application/src/test/java/io/github/kizio806/spectraevents/application/asset/delivery/PlayerResourcePackServiceTest.java`
- **Wynik weryfikacji:** PASSED — 100% poprawności cyklu życia resource packa. Gracz nigdy nie jest wyrzucany po poprawnym pobraniu (`DOWNLOADED`), pomyślnym załadowaniu (`SUCCESSFULLY_LOADED`) ani akceptacji (`ACCEPTED`). Przy wymaganej paczce odmowa (`DECLINED`), odrzucenie (`DISCARDED`) i błędy (`FAILED_DOWNLOAD`, `INVALID_URL`, `FAILED_RELOAD`) skutkują wyrzuceniem gracza. Przy niewymaganej paczce żaden status nie powoduje wyrzucenia.

---

### Etap 3: Security i Alerty CodeQL (P0)

#### `SE-SEC-01`
- **Priorytet:** `P0`
- **Opis problemu:** CodeQL Alert #185 (`java/internal-representation-exposure`): Rekord `SpectraAssetFace` w konstruktorze przypisywał listę `uv` bez jawnego kanonicznego defensywnego przypisania pola, co narażało reprezentację na modyfikację.
- **Lokalizacja w kodzie:** `spectraevents-core/src/main/java/io/github/kizio806/spectraevents/core/visual/asset/SpectraAssetFace.java:8`
- **Plan naprawy:** Zaimplementować jawny konstruktor kanoniczny z `this.uv = List.copyOf(uv)` oraz defensywnym akcesorem `uv()`.
- **Zależności:** Brak
- **Status:** `DONE_VERIFIED`
- **Zmienione pliki:** `spectraevents-core/src/main/java/io/github/kizio806/spectraevents/core/visual/asset/SpectraAssetFace.java`
- **Test potwierdzający naprawę:** `spectraevents-core/src/test/java/io/github/kizio806/spectraevents/core/visual/asset/SpectraAssetFaceTest.java` (7 testów sprawdzających immutability źródłowej listy, zwróconej listy, odrzucanie pustej listy, nieprawidłowego rozmiaru, nulli oraz pustych referencji tekstur)
- **Wynik weryfikacji:** PASSED — pełna niemutowalność i hermetyzacja.

#### `SE-SEC-02`
- **Priorytet:** `P0`
- **Opis problemu:** CodeQL Alert #271 (`java/deprecated-call`): Wywołanie deprecated `Player.kickPlayer(String)` w `SpigotPlayerResourcePackAdapter.java:33`.
- **Lokalizacja w kodzie:** `platforms/spigot/common/src/main/java/io/github/kizio806/spectraevents/platform/spigot/asset/delivery/SpigotPlayerResourcePackAdapter.java:33`
- **Plan naprawy:** Wyekstrahować bezpieczną logikę do `BukkitPlatformBridge.kickPlayer` preferującą Adventure `kick(Component)` z fallbackiem do Spigot `kickPlayer(String)` i jawnym logowaniem diagnostycznym.
- **Zależności:** Brak
- **Status:** `DONE_VERIFIED`
- **Zmienione pliki:**
  - `platforms/bukkit/src/main/java/io/github/kizio806/spectraevents/platform/bukkit/compat/BukkitPlatformBridge.java`
  - `platforms/spigot/common/src/main/java/io/github/kizio806/spectraevents/platform/spigot/asset/delivery/SpigotPlayerResourcePackAdapter.java`
- **Test potwierdzający naprawę:** `BukkitPlatformBridgeTest.java`, `SpigotPlayerResourcePackAdapterTest.java`
- **Wynik weryfikacji:** PASSED — bezpieczne wywołanie w dedykowanej warstwie kompatybilności.

#### `SE-SEC-03`
- **Priorytet:** `P0`
- **Opis problemu:** CodeQL Alerts #269, #270 (`java/deprecated-call`): Wywołania deprecated `Player.setResourcePack(url, hash, prompt, force)` oraz `Player.setResourcePack(url, hash)` w `SpigotPlayerResourcePackAdapter.java:25, 27`.
- **Lokalizacja w kodzie:** `platforms/spigot/common/src/main/java/io/github/kizio806/spectraevents/platform/spigot/asset/delivery/SpigotPlayerResourcePackAdapter.java:25, 27`
- **Plan naprawy:** Wyekstrahować wywołanie do `BukkitPlatformBridge.sendResourcePack` z jawną detekcją overloadów 4-argumentowego i 2-argumentowego oraz obsługą błędów.
- **Zależności:** Brak
- **Status:** `DONE_VERIFIED`
- **Zmienione pliki:**
  - `platforms/bukkit/src/main/java/io/github/kizio806/spectraevents/platform/bukkit/compat/BukkitPlatformBridge.java`
  - `platforms/spigot/common/src/main/java/io/github/kizio806/spectraevents/platform/spigot/asset/delivery/SpigotPlayerResourcePackAdapter.java`
- **Test potwierdzający naprawę:** `BukkitPlatformBridgeTest.java`
- **Wynik weryfikacji:** PASSED — poprawna dyspozycja w warstwie kompatybilności.

#### `SE-SEC-04`
- **Priorytet:** `P0`
- **Opis problemu:** CodeQL Alerts #273, #274 (`java/deprecated-call`): Wywołanie deprecated konstruktorów `PlayerJoinEvent(Player, String)` i `PlayerQuitEvent(Player, String)` w teście adaptera packa.
- **Lokalizacja w kodzie:** `platforms/bukkit/src/test/java/io/github/kizio806/spectraevents/platform/bukkit/asset/delivery/AbstractBukkitPlayerResourcePackAdapterTest.java:121, 133`
- **Plan naprawy:** Zastąpić fabrykami `BukkitPlatformBridge.createPlayerJoinEvent` i `createPlayerQuitEvent` preferującymi nowoczesny konstruktor z Componentem.
- **Zależności:** Brak
- **Status:** `DONE_VERIFIED`
- **Zmienione pliki:**
  - `platforms/bukkit/src/main/java/io/github/kizio806/spectraevents/platform/bukkit/compat/BukkitPlatformBridge.java`
  - `platforms/bukkit/src/test/java/io/github/kizio806/spectraevents/platform/bukkit/asset/delivery/AbstractBukkitPlayerResourcePackAdapterTest.java`
- **Test potwierdzający naprawę:** `BukkitPlatformBridgeTest.java`, `AbstractBukkitPlayerResourcePackAdapterTest.java`
- **Wynik weryfikacji:** PASSED — brak bezpośrednich wywołań konstruktorów deprecjonowanych w testach.

#### `SE-SEC-05`
- **Priorytet:** `P0`
- **Opis problemu:** CodeQL Alert #258 (`java/deprecated-call`): Wywołanie deprecated `Scoreboard.registerNewObjective(String, String)` w `SpigotActionAdapter.java:383` na `origin/main`.
- **Lokalizacja w kodzie:** `platforms/spigot/common/src/main/java/io/github/kizio806/spectraevents/platform/spigot/action/SpigotActionAdapter.java:383`
- **Plan naprawy:** Usunięto powieloną logikę scoreboardów ze `SpigotActionAdapter` i przekierowano do `SpigotScoreboardManager.showScoreboard` używającego standardowego `Criteria.DUMMY` ze Spigot API.
- **Zależności:** Brak
- **Status:** `DONE_VERIFIED`
- **Zmienione pliki:** `platforms/spigot/common/src/main/java/io/github/kizio806/spectraevents/platform/spigot/action/SpigotActionAdapter.java`
- **Test potwierdzający naprawę:** `SpigotActionAdapterTest.java`
- **Wynik weryfikacji:** PASSED — brak zduplikowanego kodu i brak wywołań metod deprecjonowanych.

#### `SE-SEC-06`
- **Priorytet:** `P0`
- **Opis problemu:** CodeQL Alert #232 (`java/deprecated-call`): Wywołanie deprecated `Nameable.setCustomName(String)` w `SpigotActionAdapter.java:181`.
- **Lokalizacja w kodzie:** `platforms/spigot/common/src/main/java/io/github/kizio806/spectraevents/platform/spigot/action/SpigotActionAdapter.java:181`
- **Plan naprawy:** Zastąpić wywołaniem `BukkitPlatformBridge.setCustomName` przekazującym zarówno reprezentację legacy, jak i deserializowany komponent Adventure.
- **Zależności:** Brak
- **Status:** `DONE_VERIFIED`
- **Zmienione pliki:**
  - `platforms/bukkit/src/main/java/io/github/kizio806/spectraevents/platform/bukkit/compat/BukkitPlatformBridge.java`
  - `platforms/spigot/common/src/main/java/io/github/kizio806/spectraevents/platform/spigot/action/SpigotActionAdapter.java`
- **Test potwierdzający naprawę:** `BukkitPlatformBridgeTest.java`, `SpigotActionAdapterTest.java`
- **Wynik weryfikacji:** PASSED — bezpieczne wywołanie w warstwie kompatybilności.

#### `SE-SEC-07`
- **Priorytet:** `P0`
- **Opis problemu:** CodeQL Alert #233 (`java/deprecated-call`): Wywołanie deprecated `Bukkit.createInventory(InventoryHolder, int, String)` w `SpigotActionAdapter.java:240`.
- **Lokalizacja w kodzie:** `platforms/spigot/common/src/main/java/io/github/kizio806/spectraevents/platform/spigot/action/SpigotActionAdapter.java:240`
- **Plan naprawy:** Zastąpić wywołaniem `BukkitPlatformBridge.createInventory` obsługującym wersje Adventure oraz Spigot.
- **Zależności:** Brak
- **Status:** `DONE_VERIFIED`
- **Zmienione pliki:**
  - `platforms/bukkit/src/main/java/io/github/kizio806/spectraevents/platform/bukkit/compat/BukkitPlatformBridge.java`
  - `platforms/spigot/common/src/main/java/io/github/kizio806/spectraevents/platform/spigot/action/SpigotActionAdapter.java`
- **Test potwierdzający naprawę:** `BukkitPlatformBridgeTest.java`
- **Wynik weryfikacji:** PASSED — bezpieczne wywołanie w warstwie kompatybilności.

#### `SE-SEC-08`
- **Priorytet:** `P0`
- **Opis problemu:** CodeQL Alert #178 (`java/deprecated-call`): Wywołanie deprecated `TextDisplay.setText(String)` w rendererze modeli `SpigotModelRenderer.java:219`.
- **Lokalizacja w kodzie:** `platforms/bukkit/src/main/java/io/github/kizio806/spectraevents/platform/bukkit/render/AbstractBukkitModelRenderer.java:250`
- **Plan naprawy:** Zastąpić wywołaniem `BukkitPlatformBridge.setText` adaptującym wywołanie do `text(Component)` na Paper lub `setText(String)` na Spigocie.
- **Zależności:** Brak
- **Status:** `DONE_VERIFIED`
- **Zmienione pliki:**
  - `platforms/bukkit/src/main/java/io/github/kizio806/spectraevents/platform/bukkit/compat/BukkitPlatformBridge.java`
  - `platforms/bukkit/src/main/java/io/github/kizio806/spectraevents/platform/bukkit/render/AbstractBukkitModelRenderer.java`
- **Test potwierdzający naprawę:** `BukkitPlatformBridgeTest.java`
- **Wynik weryfikacji:** PASSED — czyste wywołanie bez ostrzeżeń kompilatora.

#### `SE-SEC-09`
- **Priorytet:** `P0`
- **Opis problemu:** CodeQL Alerts #1, #2, #4 (`java/deprecated-call`): Wywołania deprecated `JavaPlugin.getDescription()` i `Plugin.getDescription()` w `SpigotBootstrap.java:42, 98, 238`.
- **Lokalizacja w kodzie:** `platforms/spigot/common/src/main/java/io/github/kizio806/spectraevents/platform/spigot/SpigotBootstrap.java`
- **Plan naprawy:** Zastosować `BukkitPluginVersions.resolveVersion(plugin)` sprawdzający najpierw `getPluginMeta()` (Paper), a następnie `getDescription()` (Spigot).
- **Zależności:** Brak
- **Status:** `DONE_VERIFIED`
- **Zmienione pliki:**
  - `platforms/bukkit/src/main/java/io/github/kizio806/spectraevents/platform/bukkit/util/BukkitPluginVersions.java`
  - `platforms/spigot/common/src/main/java/io/github/kizio806/spectraevents/platform/spigot/SpigotBootstrap.java`
- **Test potwierdzający naprawę:** `BukkitPluginVersionsTest.java`
- **Wynik weryfikacji:** PASSED — brak statycznych wywołań `getDescription()`.

#### `SE-SEC-10`
- **Priorytet:** `P0`
- **Opis problemu:** CodeQL Alerts #254, #255 (`java/deprecated-call`): Wywołania deprecated `Plugin.getDescription()` w `SpigotDiagnosticsCommandHandler.java:78` i `SpigotTemplateCommandHandler.java:51`.
- **Lokalizacja w kodzie:** `platforms/spigot/common/src/main/java/io/github/kizio806/spectraevents/platform/spigot/command/SpigotDiagnosticsCommandHandler.java:78`
- **Plan naprawy:** Zastąpić przez `BukkitPluginVersions.resolveVersion(plugin)`.
- **Zależności:** Brak
- **Status:** `DONE_VERIFIED`
- **Zmienione pliki:**
  - `platforms/spigot/common/src/main/java/io/github/kizio806/spectraevents/platform/spigot/command/SpigotDiagnosticsCommandHandler.java`
  - `platforms/spigot/common/src/main/java/io/github/kizio806/spectraevents/platform/spigot/command/SpigotTemplateCommandHandler.java`
- **Test potwierdzający naprawę:** `BukkitPluginVersionsTest.java`
- **Wynik weryfikacji:** PASSED — spójny resolver wersji w całym podprojekcie Spigot.

#### `SE-SEC-11`
- **Priorytet:** `P0`
- **Opis problemu:** 15 otwartych alertów CodeQL na dashboardzie GitHub Code Scanning gałęzi `main`.
- **Lokalizacja w kodzie:** Całe repozytorium (alerty #1, #2, #4, #178, #185, #232, #233, #254, #255, #258, #269, #270, #271, #273, #274)
- **Plan naprawy:** Wszystkie 15 przyczyn alertów zostało usuniętych i zweryfikowanych testami jednostkowymi na gałęzi `quality/test-and-architecture-remediation` (`SE-SEC-01` do `SE-SEC-10`). Zamknięcie alertów na zdalnym dashboardzie GitHub wymaga zmergowania gałęzi do `main` i wykonania workflow CodeQL.
- **Zależności:** `SE-SEC-01` do `SE-SEC-10`
- **Status:** `BLOCKED` (Oczekuje na otwarcie i scalenie Pull Request do `main`)
- **Zmienione pliki:** Zgodnie z `SE-SEC-01` do `SE-SEC-10`
- **Test potwierdzający naprawę:** Pipeline GitHub Actions CodeQL
- **Wynik weryfikacji:** BLOCKED — kod lokalny w 100% czysty, dashboard zdalny zaktualizuje się po merge'u.

---

### Etap 4: Wielowątkowość i Cykl Życia (P1)

#### `SE-CONCUR-01`
- **Priorytet:** `P1`
- **Opis problemu:** W `AbstractBukkitActionAdapter.java:64` pole `itemProviders` to zwykła `ArrayList` bez synchronizacji. W środowisku Folia lub przy asynchronicznych triggerach groziło to błędem `ConcurrentModificationException` podczas współbieżnej iteracji i rejestracji. Ponadto referencje serwisów i handlerów w `AbstractBukkitActionAdapter`, `EventLifecycleManager` i `SpectraEventsApplication` nie posiadały gwarancji bezpiecznej publikacji pamięci między wątkami.
- **Lokalizacja w kodzie:**
  - `platforms/bukkit/src/main/java/io/github/kizio806/spectraevents/platform/bukkit/action/AbstractBukkitActionAdapter.java`
  - `spectraevents-application/src/main/java/io/github/kizio806/spectraevents/application/execution/EventLifecycleManager.java`
  - `spectraevents-application/src/main/java/io/github/kizio806/spectraevents/application/execution/EventExecutionEngine.java`
  - `spectraevents-application/src/main/java/io/github/kizio806/spectraevents/application/SpectraEventsApplication.java`
- **Plan naprawy:**
  1. Analiza profilu obciążenia `itemProviders`: odczyt jest ultra-częsty (każda akcja resolvera na dowolnym ticku/regionie), a modyfikacja rzadka (rejestracja przy uruchomieniu lub przez zewnętrzne pluginy). Wybrano `CopyOnWriteArrayList<CustomItemProvider>` zapewniający bezblokadową iterację (lock-free snapshot) bez ryzyka `ConcurrentModificationException`. Dodano metodę `unregisterItemProvider` oraz unmodifiable getter `itemProviders()`.
  2. Zabezpieczono widoczność pamięci (safe publication):
     - `AbstractBukkitActionAdapter`: `volatile ModelRuntimeService`, `volatile ModelAnimationActionService`, `volatile FatalActionHandler`.
     - `EventLifecycleManager`: `volatile PhaseExecutionController phaseController`.
     - `EventExecutionEngine`: zamieniono `ArrayList` na `CopyOnWriteArrayList` dla `conditionResolvers` i `actionResolvers`.
     - `SpectraEventsApplication`: oznaczono mutowalne referencje post-konstrukcyjne jako `volatile` (`resourcePackCoordinator`, `resourcePackSettings`, `assetTargetProfile`, `assetPipelineService`, `lastReconciliationReport`).
- **Zależności:** Brak
- **Status:** `DONE_VERIFIED`
- **Zmienione pliki:**
  - `platforms/bukkit/src/main/java/io/github/kizio806/spectraevents/platform/bukkit/action/AbstractBukkitActionAdapter.java`
  - `platforms/bukkit/src/test/java/io/github/kizio806/spectraevents/platform/bukkit/action/AbstractBukkitActionAdapterTest.java`
  - `spectraevents-application/src/main/java/io/github/kizio806/spectraevents/application/execution/EventLifecycleManager.java`
  - `spectraevents-application/src/main/java/io/github/kizio806/spectraevents/application/execution/EventExecutionEngine.java`
  - `spectraevents-application/src/main/java/io/github/kizio806/spectraevents/application/SpectraEventsApplication.java`
- **Test potwierdzający naprawę:**
  - `platforms/bukkit/src/test/java/io/github/kizio806/spectraevents/platform/bukkit/action/AbstractBukkitActionAdapterTest.java`: test współbieżności `testConcurrentItemProviderRegistrationAndResolution()` (8 wątków równolegle rejestrujących i pobierających przedmioty bez CME i bez utraty spójności), testy rejestracji i wyrejestrowania `testItemProviderRegistrationAndResolution()`, `testUnregisterItemProvider()`.
- **Wynik weryfikacji:** PASSED — pełne bezpieczeństwo wielowątkowe i bezpieczna widoczność referencji między wątkami.

#### `SE-CONCUR-02`
- **Priorytet:** `P1`
- **Opis problemu:** W `SingleWriterPersistenceExecutor.java:50-53` aktualizacja metryki `peakQueueDepth` zawierała wyścig check-then-act (`if (depth > peakQueueDepth.get()) peakQueueDepth.set(depth)`), co przy współbieżnych zapytaniach gubiło wartość szczytową. Ponadto asynchroniczne zadania zwracające `CompletableFuture` mogły kończyć operację zanim licznik `totalWrites` został zaktualizowany na wątku wykonawczym.
- **Lokalizacja w kodzie:** `adapters/storage-sqlite/src/main/java/io/github/kizio806/spectraevents/adapter/storage/sqlite/SingleWriterPersistenceExecutor.java`
- **Plan naprawy:**
  1. Zastąpić check-then-act atomową operacją CAS: `peakQueueDepth.accumulateAndGet(depth, Math::max)`.
  2. Wprowadzić interfejs `TrackedWrite` oraz opakowanie `RawTrackedWrite` dla zadań dodawanych przez `enqueue(Runnable)`, gwarantujące deterministyczne zliczanie `totalWrites` wewnątrz wątku zapisu oraz bezpieczną współpracę z `executeAsync` i `supplyAsync`.
  3. Dodać wielowątkowy test weryfikujący poprawność metryk pod obciążeniem.
- **Zależności:** Brak
- **Status:** `DONE_VERIFIED`
- **Zmienione pliki:**
  - `adapters/storage-sqlite/src/main/java/io/github/kizio806/spectraevents/adapter/storage/sqlite/SingleWriterPersistenceExecutor.java`
  - `adapters/storage-sqlite/src/test/java/io/github/kizio806/spectraevents/adapter/storage/sqlite/SingleWriterPersistenceExecutorTest.java`
- **Test potwierdzający naprawę:** `SingleWriterPersistenceExecutorTest.java`: test `testConcurrentPeakQueueDepthTracking()` (16 wątków emitujących po 50 operacji zapisu równolegle, weryfikacja atomowej aktualizacji `peakQueueDepth > 0` oraz dokładnej równości `totalWrites == executedWrites`).
- **Wynik weryfikacji:** PASSED — brak wyścigów w metrykach głębokości kolejki i licznikach zapisu.

#### `SE-INIT-01`
- **Priorytet:** `P1`
- **Opis problemu:** `SpectraEventsSpigotPlugin.java` nie posiadał odpowiedniej obsługi błędów w `onEnable()`, co przy niepowodzeniu bootstrapu powodowało wyciek instancji `BukkitAudiences`, brak wyrejestrowania listenerów, wiszące zadania schedulerowe oraz pozostawienie pluginu w stanie częściowej inicjalizacji. Podobne ryzyko istniało w Paper (`SpectraEventsPlugin`) oraz w bootstrapach, gdzie nieudana inicjalizacja pozostawiała otwarte połączenie bazy SQLite.
- **Lokalizacja w kodzie:**
  - `platforms/spigot/common/src/main/java/io/github/kizio806/spectraevents/platform/spigot/SpectraEventsSpigotPlugin.java`
  - `platforms/spigot/common/src/main/java/io/github/kizio806/spectraevents/platform/spigot/SpigotBootstrap.java`
  - `platforms/paper/common/src/main/java/io/github/kizio806/spectraevents/platform/paper/SpectraEventsPlugin.java`
  - `platforms/paper/common/src/main/java/io/github/kizio806/spectraevents/platform/paper/PaperBootstrap.java`
- **Plan naprawy:**
  1. Utworzyć interfejs cyklu życia `BukkitBootstrapLifecycle` w `:platforms:bukkit`.
  2. Zabezpieczyć `SpectraEventsSpigotPlugin`:
     - Cały blok `onEnable()` w `try ... catch (Exception ex)`.
     - Metoda `cleanupResources()`: zamyka `SpigotBootstrap`, zamyka `BukkitAudiences`, wyrejestrowuje wszystkie listenery pluginu (`HandlerList.unregisterAll(this)`), anuluje wszystkie zadania schedulerowe (`getServer().getScheduler().cancelTasks(this)`), a w bloku `finally` zeruje referencje (`bootstrap = null`, `adventure = null`).
     - W razie błędu wywołuje `getServer().getPluginManager().disablePlugin(this)`.
  3. Zabezpieczyć `SpigotBootstrap`:
     - Przechowywać referencje do `repository` i `actionAdapter` jako pola klasy.
     - W `onDisable()` zamknąć `repository` i wyczyścić `actionAdapter` nawet w przypadku, gdy wyjątek nastąpił przed przypisaniem pola `application`.
  4. Analogicznie zabezpieczyć `SpectraEventsPlugin` oraz `PaperBootstrap` (zamknięcie `sqliteRepository` przy braku `application`).
- **Zależności:** Brak
- **Status:** `DONE_VERIFIED`
- **Zmienione pliki:**
  - `platforms/bukkit/src/main/java/io/github/kizio806/spectraevents/platform/bukkit/lifecycle/BukkitBootstrapLifecycle.java`
  - `platforms/spigot/common/src/main/java/io/github/kizio806/spectraevents/platform/spigot/SpectraEventsSpigotPlugin.java`
  - `platforms/spigot/common/src/main/java/io/github/kizio806/spectraevents/platform/spigot/SpigotBootstrap.java`
  - `platforms/paper/common/src/main/java/io/github/kizio806/spectraevents/platform/paper/SpectraEventsPlugin.java`
  - `platforms/paper/common/src/main/java/io/github/kizio806/spectraevents/platform/paper/PaperBootstrap.java`
  - `platforms/spigot/common/src/test/java/io/github/kizio806/spectraevents/platform/spigot/SpectraEventsSpigotPluginTest.java`
- **Test potwierdzający naprawę:**
  - `platforms/spigot/common/src/test/java/io/github/kizio806/spectraevents/platform/spigot/SpectraEventsSpigotPluginTest.java`:
    - `testOnEnableAndOnDisableHappyPath()`
    - `testOnEnableHandlesAdventureCreationFailure()`
    - `testOnEnableHandlesBootstrapFailure()`
    - `testOnDisableHandlesNullGracefully()`
    - `testCleanupResourcesSuppressesAndLogsExceptions()`
- **Wynik weryfikacji:** PASSED — pełne zabezpieczenie cyklu życia pluginu, brak wycieków zasobów i bezpieczna obsługa błędów startowych.

#### `SE-INIT-02`
- **Priorytet:** `P1`
- **Opis problemu:** Brak gwarancji zwalniania zasobów bazy SQLite i unregisterowania handlerów w `PaperBootstrap` oraz `SpigotBootstrap`, gdy wyjątek wystąpił w trakcie rejestracji serwisów.
- **Lokalizacja w kodzie:** `platforms/paper/common/.../PaperBootstrap.java`, `platforms/spigot/common/.../SpigotBootstrap.java`
- **Plan naprawy:** Przechowywać referencje do repozytoriów jako pola i zamykać w bloku `onDisable`.
- **Zależności:** Brak
- **Status:** `DONE_VERIFIED`
- **Zmienione pliki:** `PaperBootstrap.java`, `SpigotBootstrap.java`
- **Test potwierdzający naprawę:** `PaperBootstrapTest`, `SpigotBootstrapTest`
- **Wynik weryfikacji:** PASSED — deterministyczne zamykanie zasobów w bootstrapach.

---

### Etap 5: Architektura i Deduplikacja (P1)

#### `SE-DUP-01`
- **Priorytet:** `P1`
- **Opis problemu:** Ghost-klasy `PaperResourcePackDeliveryBootstrap.java` i `SpigotResourcePackDeliveryBootstrap.java` delegowały wywołania 1:1 do `BukkitResourcePackDeliveryBootstrap`.
- **Lokalizacja w kodzie:** `platforms/paper/common/.../PaperResourcePackDeliveryBootstrap.java`, `platforms/spigot/common/.../SpigotResourcePackDeliveryBootstrap.java`
- **Plan naprawy:** Zastąpić wywołania w Paper/Spigot bezpośrednimi wywołaniami `BukkitResourcePackDeliveryBootstrap` i usunąć obie zbędne fasady.
- **Zależności:** Brak
- **Status:** `DONE_VERIFIED`
- **Zmienione pliki:**
  - Usunięto: `platforms/paper/common/src/main/java/io/github/kizio806/spectraevents/platform/paper/asset/delivery/PaperResourcePackDeliveryBootstrap.java`
  - Usunięto: `platforms/spigot/common/src/main/java/io/github/kizio806/spectraevents/platform/spigot/asset/delivery/SpigotResourcePackDeliveryBootstrap.java`
  - Zaktualizowano callerów: `PaperBootstrap.java`, `DefinitionCommandHandler.java`, `SpigotBootstrap.java`, `SpigotDefinitionCommandHandler.java`
- **Test potwierdzający naprawę:** Kompilacja całego projektu i uruchomienie bootstrapów
- **Wynik weryfikacji:** PASSED — usunięto zbędne fasady, bezpośrednie odwołanie do `BukkitResourcePackDeliveryBootstrap`.

#### `SE-DUP-02`
- **Priorytet:** `P1`
- **Opis problemu:** Identyczny algorytm wyliczania postępu BossBar w `EventBossBarManager.java:162-178` oraz `SpigotActionAdapter.java:360-374`.
- **Lokalizacja w kodzie:** `platforms/paper/common/.../EventBossBarManager.java:162-178`, `platforms/spigot/common/.../SpigotActionAdapter.java:360-374`
- **Plan naprawy:** Wydzielić wspólną klasę narzędziową w `:platforms:bukkit`: `BukkitBossBarProgressCalculator.calculateFloat(EventRuntimeState, String)` i `calculateDouble(EventRuntimeState, String)` i użyć jej w Paper oraz Spigot.
- **Zależności:** Brak
- **Status:** `DONE_VERIFIED`
- **Zmienione pliki:**
  - Utworzono: `platforms/bukkit/src/main/java/io/github/kizio806/spectraevents/platform/bukkit/bossbar/BukkitBossBarProgressCalculator.java`
  - Utworzono: `platforms/bukkit/src/test/java/io/github/kizio806/spectraevents/platform/bukkit/bossbar/BukkitBossBarProgressCalculatorTest.java`
  - Zaktualizowano: `PaperBossBarManager.java`, `SpigotActionAdapter.java`
- **Test potwierdzający naprawę:** `BukkitBossBarProgressCalculatorTest.java` (11 testów jednostkowych i brzegowych: kalkulacja zdrowia, licznik uderzeń, parsowanie ciągów liczbowych, clamping wartości poza zakresem, fallback do zdrowia, brak zdrowia w stanie, obsługa `NaN`, `+Infinity`, `-Infinity`, wartości brzegowe licznika uderzeń 0/max/brak, wartości brzegowe zdrowia 0/max, puste i białe znaki w trybie, weryfikacja NPE przy pustym stanie).
- **Wynik weryfikacji:** PASSED — pełna deduplikacja kalkulatora paska postępu.

#### `SE-DUP-03`
- **Priorytet:** `P1`
- **Opis problemu:** Duplikacja prywatnego rekordu `BossBarHolder` w `EventBossBarManager` i `SpigotBossBarManager`.
- **Lokalizacja w kodzie:** `platforms/paper/common/.../EventBossBarManager.java`, `platforms/spigot/common/.../SpigotBossBarManager.java`
- **Plan naprawy:** Przenieść generyczny holder paska do `:platforms:bukkit:bossbar:BukkitBossBarHolder<T>` i użyć w menedżerach platformowych.
- **Zależności:** `SE-NAME-01`
- **Status:** `DONE_VERIFIED`
- **Zmienione pliki:**
  - Utworzono: `platforms/bukkit/src/main/java/io/github/kizio806/spectraevents/platform/bukkit/bossbar/BukkitBossBarHolder.java`
  - Utworzono: `platforms/bukkit/src/test/java/io/github/kizio806/spectraevents/platform/bukkit/bossbar/BukkitBossBarHolderTest.java`
  - Zaktualizowano: `SpigotBossBarManager.java`
- **Test potwierdzający naprawę:** `BukkitBossBarHolderTest.java`, `SpigotBossBarManagerTest.java`
- **Wynik weryfikacji:** PASSED — generyczna thread-safe abstrakcja `BukkitBossBarHolder<T>`.

#### `SE-NAME-01`
- **Priorytet:** `P1`
- **Opis problemu:** Asymetria nazewnictwa pomiędzy platformami: w Spigot `SpigotBossBarManager`, `SpigotScoreboardManager`, a w Paper `EventBossBarManager`, `EventScoreboardManager`.
- **Lokalizacja w kodzie:** `platforms/paper/common/src/main/java/io/github/kizio806/spectraevents/platform/paper/bossbar/EventBossBarManager.java`, `platforms/paper/common/src/main/java/io/github/kizio806/spectraevents/platform/paper/scoreboard/EventScoreboardManager.java`
- **Plan naprawy:** Przemianować klasy i powiązane testy na `PaperBossBarManager` i `PaperScoreboardManager`.
- **Zależności:** Brak
- **Status:** `DONE_VERIFIED`
- **Zmienione pliki:**
  - Przemianowano: `PaperBossBarManager.java`, `PaperBossBarManagerTest.java`
  - Przemianowano: `PaperScoreboardManager.java`, `PaperScoreboardManagerTest.java`
  - Zaktualizowano: `PaperActionAdapter.java`, `PaperBootstrap.java`
- **Test potwierdzający naprawę:** Kompilacja i uruchomienie testów Paper
- **Wynik weryfikacji:** PASSED — pełna symetria nazewnictwa pomiędzy platformami.

#### `SE-NAME-02`
- **Priorytet:** `P2`
- **Opis problemu:** Asymetria klas głównych pluginu (`SpectraEventsPlugin` na Paper vs `SpectraEventsSpigotPlugin` na Spigot).
- **Lokalizacja w kodzie:** `platforms/paper/common/.../SpectraEventsPlugin.java`, `platforms/spigot/common/.../SpectraEventsSpigotPlugin.java`
- **Plan naprawy:** Utrzymać klasy w dedykowanych pakietach platformowych ze względu na zgodność `plugin.yml`, oznaczyć klasę Spigota jako `public final`.
- **Zależności:** Brak
- **Status:** `DONE_VERIFIED`
- **Zmienione pliki:** `SpectraEventsSpigotPlugin.java`
- **Test potwierdzający naprawę:** `SpectraEventsSpigotPluginTest`
- **Wynik weryfikacji:** PASSED — stabilne kontrakty klas platformowych.

---

### Etap 6: Testy i Pokrycie JaCoCo (P2)

#### `SE-COV-01`
- **Priorytet:** `P1`
- **Opis problemu:** Pokrycie `:platforms:bukkit` wynosiło dramatyczne 15.05%.
- **Lokalizacja w kodzie:** Moduł `:platforms:bukkit`
- **Plan naprawy:** Zaimplementować testy jednostkowe oparte o MockBukkit (GUI, adapter akcji, model renderer, loot listener, store ustawień, reconciler).
- **Zależności:** `SE-DUP-01`, `SE-DUP-02`
- **Status:** `DONE_VERIFIED`
- **Zmienione pliki:** Testy w `platforms/bukkit/src/test/...`
- **Test potwierdzający naprawę:** `./gradlew :platforms:bukkit:jacocoTestReport`
- **Wynik weryfikacji:** PASSED — pokrycie wzrosło do **88.79% instrukcji, 89.19% linii, 72.70% rozgałęzień, 100% klas (47/47)**.

#### `SE-COV-02`
- **Priorytet:** `P1`
- **Opis problemu:** Pokrycie `:platforms:paper:common` wynosiło zaledwie 4.65%.
- **Lokalizacja w kodzie:** Moduł `:platforms:paper:common`
- **Plan naprawy:** Testy komend Brigadier, bootstrapu, event routerów i adaptera akcji.
- **Zależności:** `SE-NAME-01`
- **Status:** `DONE_VERIFIED`
- **Zmienione pliki:** Testy w `platforms/paper/common/src/test/...`
- **Test potwierdzający naprawę:** `./gradlew :platforms:paper:common:jacocoTestReport`
- **Wynik weryfikacji:** PASSED — pokrycie wzrosło do **97.47% instrukcji, 97.30% linii, 84.84% rozgałęzień, 100% klas (33/33)**.

#### `SE-COV-03`
- **Priorytet:** `P1`
- **Opis problemu:** Pokrycie `:platforms:spigot:common` wynosiło zaledwie 10.87%.
- **Lokalizacja w kodzie:** Moduł `:platforms:spigot:common`
- **Plan naprawy:** Testy wszystkich handlerów komend, pluginu i menedżerów.
- **Zależności:** Brak
- **Status:** `DONE_VERIFIED`
- **Zmienione pliki:** Testy w `platforms/spigot/common/src/test/...`
- **Test potwierdzający naprawę:** `./gradlew :platforms:spigot:common:jacocoTestReport`
- **Wynik weryfikacji:** PASSED — pokrycie wzrosło do **99.22% instrukcji, 98.99% linii, 96.99% rozgałęzień, 100% klas (24/24)**.

#### `SE-COV-04`
- **Priorytet:** `P1`
- **Opis problemu:** Pokrycie `spectraevents-core` (95.66%).
- **Lokalizacja w kodzie:** Moduł `:spectraevents-core`
- **Plan naprawy:** Rozbudowa testów wizualnych i runtime.
- **Zależności:** Brak
- **Status:** `DONE_VERIFIED`
- **Zmienione pliki:** `VisualAndRuntimeCoverageTest.java`
- **Test potwierdzający naprawę:** `./gradlew :spectraevents-core:jacocoTestReport`
- **Wynik weryfikacji:** PASSED — **100.00% instrukcji, 100.00% linii, 99.71% rozgałęzień (349/350), 100% klas (136/136)**.

#### `SE-COV-05`
- **Priorytet:** `P1`
- **Opis problemu:** Pokrycie `spectraevents-application` (86.74%).
- **Lokalizacja w kodzie:** Moduł `:spectraevents-application`
- **Plan naprawy:** Testy kompilatorów, parserów YAML, serwisów cyklu życia.
- **Zależności:** Brak
- **Status:** `DONE_VERIFIED`
- **Zmienione pliki:** Testy w `spectraevents-application/src/test/...`
- **Test potwierdzający naprawę:** `./gradlew :spectraevents-application:jacocoTestReport`
- **Wynik weryfikacji:** PASSED — **96.57% instrukcji, 96.45% linii, 85.95% rozgałęzień, 100% klas (144/144)**.

#### `SE-COV-06`
- **Priorytet:** `P1`
- **Opis problemu:** Pokrycie modułów adapterów.
- **Lokalizacja w kodzie:** Podprojekty w `:adapters:*`
- **Plan naprawy:** Testy klientów HTTP, archiwów Blockbench, operacji SQLite WAL.
- **Zależności:** Brak
- **Status:** `DONE_VERIFIED`
- **Zmienione pliki:** Testy w adapterach
- **Test potwierdzający naprawę:** Raporty JaCoCo modułów
- **Wynik weryfikacji:** PASSED — `update-http`: **100.00%**, `assets-delivery`: **100.00%**, `assets-blockbench`: **96.24%**, `storage-sqlite`: **96.44%**.

#### `SE-COV-07`
- **Priorytet:** `P0`
- **Opis problemu:** Zagregowane pokrycie projektu wynosiło 59.10%.
- **Lokalizacja w kodzie:** Całe repozytorium
- **Plan naprawy:** Utworzenie zadania `jacocoRootReport` i bramki `jacocoRootCoverageVerification` egzekwującej 90%/90%/80%.
- **Zależności:** `SE-COV-01` do `SE-COV-06`
- **Status:** `DONE_VERIFIED`
- **Zmienione pliki:** `build.gradle.kts`
- **Test potwierdzający naprawę:** `./gradlew jacocoRootCoverageVerification`
- **Wynik weryfikacji:** PASSED — wynik zagregowany: **96.13% instrukcji, 95.87% linii, 86.32% rozgałęzień, 98.22% metod, 100.00% klas (400/400)**.

#### `SE-COV-08`
- **Priorytet:** `P0`
- **Opis problemu:** Wymaganie docelowe PROMPT 4.2: Osiągnięcie 100% realnego pokrycia kodu produkcyjnego Java bez sztucznych testów, z jawnym udokumentowaniem wszystkich wyjątków i syntetycznych instrukcji JVM.
- **Lokalizacja w kodzie:** Wszystkie 9 modułów
- **Plan naprawy:** Domykanie luk w 239 klasach (alternatywne ścieżki refleksji, fallbacki MockBukkit, obsługa zewnętrznych pluginów).
- **Zależności:** `SE-COV-07`
- **Status:** `PARTIAL`
- **Zmienione pliki:** Wszystkie moduły
- **Test potwierdzający naprawę:** Raport JaCoCo root XML
- **Wynik weryfikacji:** PARTIAL — 96.13% instrukcji, 95.87% linii, 86.32% rozgałęzień. Pozostaje 2 414 instrukcji i 727 gałęzi do domknięcia.

---

### Etap 7: Build System, Konfiguracja i CI/CD (P2/P3)

#### `SE-VER-01`
- **Priorytet:** `P1`
- **Opis problemu:** Niespójne deklaracje wersji Minecraft w repozytorium.
- **Lokalizacja w kodzie:** `gradle.properties`, moduły platform
- **Plan naprawy:** Ujednolicić wersje serwerowe na 26.1.x–26.3.
- **Zależności:** Brak
- **Status:** `DONE_VERIFIED`
- **Zmienione pliki:** `gradle.properties`
- **Test potwierdzający naprawę:** `./gradlew verifyCompatibilityMatrix`
- **Wynik weryfikacji:** PASSED — pełna spójność macierzy kompatybilności.

#### `SE-VER-02`
- **Priorytet:** `P2`
- **Opis problemu:** Martwy, nieużywany i sprzeczny plik `gradle/compatibility.versions.toml`.
- **Lokalizacja w kodzie:** `gradle/compatibility.versions.toml`
- **Plan naprawy:** Usunąć plik `compatibility.versions.toml`. Single Source of Truth pozostaje w `gradle.properties`.
- **Zależności:** Brak
- **Status:** `DONE_VERIFIED`
- **Zmienione pliki:** Usunięto `gradle/compatibility.versions.toml`
- **Test potwierdzający naprawę:** `./scripts/verification/validate-structure.sh`
- **Wynik weryfikacji:** PASSED — brak nieużywanego pliku wersji.

#### `SE-VER-03`
- **Priorytet:** `P2`
- **Opis problemu:** Brak zautomatyzowanej weryfikacji spójności wersji w buildzie.
- **Lokalizacja w kodzie:** `build.gradle.kts`
- **Plan naprawy:** Zadanie `verifyCompatibilityMatrix` czytające właściwości z `gradle.properties`.
- **Zależności:** `SE-VER-01`
- **Status:** `DONE_VERIFIED`
- **Zmienione pliki:** `build.gradle.kts`
- **Test potwierdzający naprawę:** `./gradlew verifyCompatibilityMatrix`
- **Wynik weryfikacji:** PASSED — automatyczna bramka weryfikacji wersji.

#### `SE-CI-01`
- **Priorytet:** `P3`
- **Opis problemu:** Wymóg minimalnych uprawnień w GitHub Actions (`permissions: contents: read`).
- **Lokalizacja w kodzie:** `.github/workflows/*.yml`
- **Plan naprawy:** Wszystkie workflow posiadają jawne timeouty, akcje przypięte do pełnych commit SHA, globalne `permissions: contents: read`.
- **Zależności:** Brak
- **Status:** `DONE_VERIFIED`
- **Zmienione pliki:** `.github/workflows/*.yml`
- **Test potwierdzający naprawę:** `actionlint`, `zizmor`
- **Wynik weryfikacji:** PASSED — bezpieczne workflow CI/CD.

#### `SE-CI-02`
- **Priorytet:** `P2`
- **Opis problemu:** Wdrożenie pipeline'u bezpieczeństwa CI/CD.
- **Lokalizacja w kodzie:** `.github/workflows/`
- **Plan naprawy:** Wdrożenie Gitleaks, Zizmor, CodeQL, Dependency Review.
- **Zależności:** Brak
- **Status:** `DONE_VERIFIED`
- **Zmienione pliki:** Pliki workflow
- **Test potwierdzający naprawę:** GitHub Actions
- **Wynik weryfikacji:** PASSED — aktywny zestaw analizatorów bezpieczeństwa.

#### `SE-BUILD-01`
- **Priorytet:** `P2`
- **Opis problemu:** Integralność zależności i ochrona przed atakami supply chain.
- **Lokalizacja w kodzie:** `gradle/verification-metadata.xml`
- **Plan naprawy:** Wygenerować metadane weryfikacji z sumami SHA-256 dla wszystkich zależności i pluginów Gradle.
- **Zależności:** Brak
- **Status:** `DONE_VERIFIED`
- **Zmienione pliki:** `gradle/verification-metadata.xml` (228 KB)
- **Test potwierdzający naprawę:** `./gradlew help`
- **Wynik weryfikacji:** PASSED — weryfikacja sum kontrolnych włączona.

#### `SE-BUILD-02`
- **Priorytet:** `P2`
- **Opis problemu:** Ostrzeżenie Gradle o deprecacji `Configuration.setVisible(boolean)` przy kompilacji z wtyczką ErrorProne.
- **Lokalizacja w kodzie:** `build-logic/src/main/kotlin/spectraevents.java-base.gradle.kts:18`
- **Plan naprawy:** Zbadano wywołanie stacktrace: deprecacja pochodzi z wnętrza biblioteki `net.ltgt.gradle:errorprone-plugin:5.1.0` w metodzie `ErrorPronePlugin.lambda$registerConfiguration$0(ErrorPronePlugin.java:88)`. Wersja 5.1.0 jest najnowszą dostępną wersją wtyczki. Zabezpieczono konfigurację ErrorProne przed błędem `StringConcatToTextBlock` na Java 25.
- **Zależności:** Brak
- **Status:** `DONE_VERIFIED`
- **Zmienione pliki:** `build-logic/src/main/kotlin/spectraevents.java-base.gradle.kts`
- **Test potwierdzający naprawę:** `./gradlew help --warning-mode all`
- **Wynik weryfikacji:** PASSED — udokumentowane zewnętrzne źródło ostrzeżenia w pluginie.

---

### Etap 8: Dokumentacja i Instrukcje AI (P2)

#### `SE-AI-01`
- **Priorytet:** `P2`
- **Opis problemu:** Rozproszenie instrukcji dla agentów AI po wielu plikach i katalogach, co narusza zasadę Single Source of Truth.
- **Lokalizacja w kodzie:** Pliki instrukcji w głównym katalogu i podkatalogach `.github/`
- **Plan naprawy:** Ustanowić `AGENTS.md` jako absolutny kanoniczny punkt odniesienia; usunąć przestarzałe fragmenty `.github/instructions` oraz `.github/prompts`, zachowując pointery w `CLAUDE.md`, `GEMINI.md`.
- **Zależności:** Brak
- **Status:** `DONE_VERIFIED`
- **Zmienione pliki:** Usunięto katalogi `.github/instructions`, `.github/prompts`, `.github/copilot-instructions.md`
- **Test potwierdzający naprawę:** `./scripts/verification/validate-structure.sh`
- **Wynik weryfikacji:** PASSED — `AGENTS.md` jako jedyne kanoniczne źródło reguł.

#### `SE-DOC-01`
- **Priorytet:** `P2`
- **Opis problemu:** Plik `docs/architecture.md` urywał się po sekcji `## Technical Strengths`.
- **Lokalizacja w kodzie:** `docs/architecture.md`
- **Plan naprawy:** Dokończyć dokumentację techniczną, dodając sekcje `Platform Artifact & Packaging Contract`, `Risks To Watch` oraz `Recommended Next Quality Milestones`.
- **Zależności:** Brak
- **Status:** `DONE_VERIFIED`
- **Zmienione pliki:** `docs/architecture.md`
- **Test potwierdzający naprawę:** `./scripts/verification/validate-structure.sh`
- **Wynik weryfikacji:** PASSED — kompletny dokument architektoniczny.

#### `SE-DOC-02`
- **Priorytet:** `P2`
- **Opis problemu:** Naruszenie Keep a Changelog w `CHANGELOG.md`: wielokrotne powtórzenie nagłówka `### Changed` (4 razy) i `### Added` (2 razy) w sekcji `## [Unreleased]`.
- **Lokalizacja w kodzie:** `CHANGELOG.md`
- **Plan naprawy:** Skonsolidować wpisy w pojedyncze sekcje `### Added`, `### Changed`, `### Fixed`, `### Security`.
- **Zależności:** Brak
- **Status:** `DONE_VERIFIED`
- **Zmienione pliki:** `CHANGELOG.md`
- **Test potwierdzający naprawę:** Weryfikacja formatowania changeloga
- **Wynik weryfikacji:** PASSED — sekcje w `[Unreleased]` występują dokładnie jeden raz.

#### `SE-DOC-03`
- **Priorytet:** `P2`
- **Opis problemu:** Lakoniczne strony dokumentacji w `docs/wiki/` w porównaniu do standardu encyklopedycznego Jump Delay Fix.
- **Lokalizacja w kodzie:** `docs/wiki/Known-Limitations.md`, `docs/wiki/Assets-and-Resource-Packs.md`
- **Plan naprawy:** Rozszerzyć dokumentację o szczegółowe opisy ograniczeń technicznych platform (Folia, Spigot) oraz formatu assetów.
- **Zależności:** Brak
- **Status:** `DONE_VERIFIED`
- **Zmienione pliki:**
  - `docs/wiki/Known-Limitations.md`
  - `docs/wiki/Assets-and-Resource-Packs.md`
- **Test potwierdzający naprawę:** `./scripts/verification/validate-structure.sh`
- **Wynik weryfikacji:** PASSED — encyklopedyczny poziom dokumentacji wiki.

#### `SE-DOC-04`
- **Priorytet:** `P2`
- **Opis problemu:** Spójność dokumentacji technicznej z rzeczywistą implementacją kodu.
- **Lokalizacja w kodzie:** Cały katalog `docs/`
- **Plan naprawy:** Zaktualizować odnośniki w `docs/development.md`, `docs/releases.md`, `docs/wiki/Compatibility.md`.
- **Zależności:** Brak
- **Status:** `DONE_VERIFIED`
- **Zmienione pliki:** `docs/**/*.md`
- **Test potwierdzający naprawę:** `./scripts/verification/validate-structure.sh` (53 pliki markdown bez przerwanych linków)
- **Wynik weryfikacji:** PASSED — 100% spójności dokumentacji.

---

### Etap 9: Finalny Audyt i Walidacja (P0)

#### `SE-VERIF-01`
- **Priorytet:** `P0`
- **Opis problemu:** Wymóg pełnego przejścia twardych bramek jakościowych po zakończeniu wszystkich prac remediacyjnych.
- **Lokalizacja w kodzie:** Całe repozytorium
- **Plan naprawy:** Uruchomienie pełnego zestawu: `./gradlew spotlessApply`, `./gradlew clean check build`, `./scripts/verification/validate-structure.sh`, `./scripts/verification/validate-source-hygiene.sh`, `./scripts/release/release.sh --dry-run --allow-existing-tag`.
- **Zależności:** Wszystkie zadania `SE-*`
- **Status:** `DONE_VERIFIED`
- **Zmienione pliki:** Cały projekt
- **Test potwierdzający naprawę:** Pełny gauntlet CI oraz skrypt release dry-run
- **Wynik weryfikacji:** PASSED —
  - `validate-structure.sh`: PASS (53 pliki markdown ze zweryfikowanymi linkami).
  - `validate-source-hygiene.sh`: PASS (0 `@SuppressWarnings`, brak `TODO`/`FIXME`/`XXX`, brak wildcard imports).
  - `./scripts/release/release.sh --dry-run --allow-existing-tag`: PASS (pomyślne zbudowanie obu dystrybucji `SpectraEvents-0.1.1-beta.2-paper.jar` [27M] oraz `SpectraEvents-0.1.1-beta.2-spigot.jar` [28M]).
  - `./gradlew clean check build`: PASS (wszystkie zadania wykonane pomyślnie, 0 błędów w Checkstyle, PMD, SpotBugs, JaCoCo, ErrorProne, testach jednostkowych i integracyjnych).

---

## 4. Rejestr Zadań — Jump Delay Fix

*(Pełna synchronizacja z `jump-delay-fix/REMEDIATION_TRACKER.md`)*

| ID | Priorytet | Nazwa Zadania | Status | Uwagi Audytowe |
|---|:---:|---|:---:|---|
| `JDF-ARCH-01` | P1 | Instancyjny `RuntimeState` wstrzykiwany do `JumpDelayFixEngine` | `DONE_VERIFIED`* | Zaimplementowane w kodzie; oczekuje na commit w git |
| `JDF-ARCH-02` | P1 | Izolacja równoległych instancji silnika w testach | `DONE_VERIFIED`* | Test `shouldSupportIsolatedRuntimeStateInjection`; oczekuje na commit |
| `JDF-BUILD-01` | P2 | Podpięcie Version Catalog `gradle/libs.versions.toml` | `DONE_VERIFIED`* | Zintegrowane we wszystkich modułach; oczekuje na commit |
| `JDF-CONF-01` | P2 | Usunięcie martwego `build-logic/**` z `.codecov.yml` | `DONE_VERIFIED`* | Usunięto; oczekuje na commit |
| `JDF-CONF-02` | P2 | Eliminacja wykluczeń SonarCloud w loaderach | `DONE_VERIFIED`* | Klasy przeniesione do `common/src/client`; oczekuje na commit |
| `JDF-HYGIENE-01` | P2 | Hermetyzacja `setPendingSaveForTests` w `ConfigManager` | `DONE_VERIFIED`* | Widoczność pakietowa; oczekuje na commit |
| `JDF-DUP-01` | P2 | Deduplikacja testów klawiszy przez `KeyMappingAssertions` | `DONE_VERIFIED`* | Dodano helper asercji; oczekuje na commit |
| `JDF-TEST-01` | P2 | Precyzyjne filtrowanie klas loadera w JaCoCo | `DONE_VERIFIED`* | Filtry `include: '.../fabric/**'`; oczekuje na commit |
| `JDF-TEST-02` | P3 | Eliminacja antywzorca inspekcji bytecode'u jako tekstu | `NOT_DONE` | Nadal występuje czytanie pliku `.class` jako String ISO-8859-1 |
| `JDF-COV-01` | P1 | Realne pokrycie klas adapterów loaderów Fabric/NeoForge | `PARTIAL` | Pokrycie wynosi 11.88% (Fabric) i 20.83% (NeoForge) |
| `JDF-CI-01` | P3 | Minimalne uprawnienia `contents: read` w workflow | `DONE_VERIFIED`* | Ograniczono uprawnienia; oczekuje na commit |
| `JDF-DOC-01` | P2 | Przywrócenie `CODE_OF_CONDUCT.md` na lokalnej gałęzi `main` | `PARTIAL` | Plik istnieje na zdalnym repozytorium (`2f05201`), brak lokalnie |
| `JDF-GIT-01` | P0 | Zatwierdzenie 15 zmodyfikowanych i 2 nieśledzonych plików | `NOT_DONE` | Drzewo robocze wisi ze zmianami niezłożonymi w git |

*\*Wszystkie oznaczone pozycje wymagają wykonania `git commit` w repozytorium `jump-delay-fix`.*

---

## 5. Rejestr Dodatkowych Zmian Architektonicznych (`UNP-01` do `UNP-15`)

*(Zgodnie z Section 3 raportu `REMEDIATION_RECONCILIATION.md`)*

1. `UNP-01`: Utworzenie centralnego mostka kompatybilności `BukkitPlatformBridge` (305 LOC) z bezpieczną refleksją.
2. `UNP-02`: Przeniesienie 17 ekranów Admin GUI do `:platforms:bukkit:gui` (pełna parzystość GUI na Spigot).
3. `UNP-03`: Implementacja modułowych podkomend administracyjnych w Spigot (`location`, `model`, `asset`, `integration`, `update`, `admin`).
4. `UNP-04`: Przeniesienie integracji (Vault, LuckPerms, WorldGuard, PlaceholderAPI) do `:platforms:bukkit:integration`.
5. `UNP-05`: Przeniesienie dostawców Custom Items (Nexo, Oraxen, ItemsAdder) do `:platforms:bukkit:integration:item`.
6. `UNP-06`: Wydzielenie `AbstractBukkitEntityReconciler` i `BukkitSharedLootListener` do `:platforms:bukkit`.
7. `UNP-07`: Wydzielenie `SpecMathParser` w `spectraevents-application` (deduplikacja matematyki 3D).
8. `UNP-08`: Uogólnienie transformacji encji Display (`applyTransformTeleports`) w `AbstractBukkitModelRenderer`.
9. `UNP-09`: Dekompozycja `SpectraMainCommand` na Paper (`ScheduleCommandHandler`, `RewardsCommandHandler`).
10. `UNP-10`: Defensywne sprawdzenia null-safety w `EventDisplayPlaceholders`, `ModelCompiler`, `EventDefinitionCompiler`.
11. `UNP-11`: Obsługa `EventDefinitionCompilerException` w `EventOrchestrationService.startEventAsync`.
12. `UNP-12`: Dodanie metody `executeAndWait(Runnable)` w `SingleWriterPersistenceExecutor`.
13. `UNP-13`: Wyłączenie reguły `StringConcatToTextBlock` w konfiguracji ErrorProne dla Java 25.
14. `UNP-14`: Wykluczenie reguły `AvoidAccessibilityAlteration` w PMD dla testów MockBukkit.
15. `UNP-15`: Utworzenie zaawansowanego zestawu mockującego `MockBukkitSetup.java` (1,237 LOC) w `:platforms:bukkit`.
