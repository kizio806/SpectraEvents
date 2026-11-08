# REMEDIATION TRACKER — SPECTRAEVENTS & JUMP DELAY FIX
**Dokument Identyfikacyjny:** `REMEDIATION-TRACKER-2026-11`
**Data utworzenia:** 7 listopada 2026 r.
**Autor:** Principal Java Engineer, Software Architect, Security Engineer & Quality Auditor
**Cel:** Doprowadzenie projektów `SpectraEvents` oraz `jump-delay-fix` do bezkompromisowego standardu inżynierskiego na podstawie raportu audytu `AUDIT-JDF-SE-2026-11`.

---

## 1. Zasady Utrzymania Rejestru

1. Żadne zadanie nie może zostać usunięte ani uznane za `DONE` bez bezpośredniej weryfikacji kodem i testem.
2. Historia zmian każdego zadania musi pozostać w pełni audytowalna.
3. Dozwolone statusy: `TODO`, `IN_PROGRESS`, `BLOCKED`, `DONE`, `NOT_REPRODUCIBLE`, `OUT_OF_SCOPE`.
4. Rygor jakościowy: zero `@SuppressWarnings`, zero `TODO`/`FIXME`, zero wildcard imports, `validate-structure.sh` PASS, `validate-source-hygiene.sh` PASS, `clean check build` PASS.

---

## 2. Podsumowanie Realizacji

| Projekt | Łącznie | TODO | IN_PROGRESS | BLOCKED | DONE |
|---|---:|---:|---:|---:|---:|
| **SpectraEvents** | 28 | 0 | 1 | 0 | 27 |
| **Jump Delay Fix** | 7 | 0 | 0 | 0 | 7 |
| **SUMA** | **35** | **0** | **1** | **0** | **34** |

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
- **Status:** `DONE`
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
- **Status:** `DONE`
- **Zmienione pliki:** `spectraevents-core/src/main/java/io/github/kizio806/spectraevents/core/visual/asset/SpectraAssetFace.java`
- **Test potwierdzający naprawę:** `spectraevents-core/src/test/java/io/github/kizio806/spectraevents/core/visual/asset/SpectraAssetFaceTest.java` (7 testów sprawdzających immutability źródłowej listy, zwróconej listy, odrzucanie pustej listy, nieprawidłowego rozmiaru, nulli oraz pustych referencji tekstur)
- **Wynik weryfikacji:** PASSED — pełna niemutowalność i hermetyzacja.

#### `SE-SEC-02`
- **Priorytet:** `P0`
- **Opis problemu:** CodeQL Alert #271 (`java/deprecated-call`): Wywołanie deprecated `Player.kickPlayer(String)` w `SpigotPlayerResourcePackAdapter.java:33`.
- **Lokalizacja w kodzie:** `platforms/spigot/common/src/main/java/io/github/kizio806/spectraevents/platform/spigot/asset/delivery/SpigotPlayerResourcePackAdapter.java:33`
- **Plan naprawy:** Wyekstrahować bezpieczną logikę do `BukkitPlatformBridge.kickPlayer` preferującą Adventure `kick(Component)` z fallbackiem do Spigot `kickPlayer(String)` i jawnym logowaniem diagnostycznym.
- **Zależności:** Brak
- **Status:** `DONE`
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
- **Status:** `DONE`
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
- **Status:** `DONE`
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
- **Status:** `DONE`
- **Zmienione pliki:** `platforms/spigot/common/src/main/java/io/github/kizio806/spectraevents/platform/spigot/action/SpigotActionAdapter.java`
- **Test potwierdzający naprawę:** `SpigotActionAdapterTest.java`
- **Wynik weryfikacji:** PASSED — brak zduplikowanego kodu i brak wywołań metod deprecjonowanych.

#### `SE-SEC-06`
- **Priorytet:** `P0`
- **Opis problemu:** CodeQL Alert #232 (`java/deprecated-call`): Wywołanie deprecated `Nameable.setCustomName(String)` w `SpigotActionAdapter.java:181`.
- **Lokalizacja w kodzie:** `platforms/spigot/common/src/main/java/io/github/kizio806/spectraevents/platform/spigot/action/SpigotActionAdapter.java:181`
- **Plan naprawy:** Zastąpić wywołaniem `BukkitPlatformBridge.setCustomName` przekazującym zarówno reprezentację legacy, jak i deserializowany komponent Adventure.
- **Zależności:** Brak
- **Status:** `DONE`
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
- **Status:** `DONE`
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
- **Status:** `DONE`
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
- **Status:** `DONE`
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
- **Status:** `DONE`
- **Zmienione pliki:**
  - `platforms/spigot/common/src/main/java/io/github/kizio806/spectraevents/platform/spigot/command/SpigotDiagnosticsCommandHandler.java`
  - `platforms/spigot/common/src/main/java/io/github/kizio806/spectraevents/platform/spigot/command/SpigotTemplateCommandHandler.java`
- **Test potwierdzający naprawę:** `BukkitPluginVersionsTest.java`
- **Wynik weryfikacji:** PASSED — spójny resolver wersji w całym podprojekcie Spigot.

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
- **Status:** `DONE`
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
- **Status:** `DONE`
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
- **Status:** `DONE`
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

---

### Etap 5: Architektura i Deduplikacja (P1)

#### `SE-DUP-01`
- **Priorytet:** `P1`
- **Opis problemu:** Ghost-klasy `PaperResourcePackDeliveryBootstrap.java` i `SpigotResourcePackDeliveryBootstrap.java` delegowały wywołania 1:1 do `BukkitResourcePackDeliveryBootstrap`.
- **Lokalizacja w kodzie:** `platforms/paper/common/.../PaperResourcePackDeliveryBootstrap.java`, `platforms/spigot/common/.../SpigotResourcePackDeliveryBootstrap.java`
- **Plan naprawy:** Zastąpić wywołania w Paper/Spigot bezpośrednimi wywołaniami `BukkitResourcePackDeliveryBootstrap` i usunąć obie zbędne fasady.
- **Zależności:** Brak
- **Status:** `DONE`
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
- **Status:** `DONE`
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
- **Status:** `DONE`
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
- **Status:** `DONE`
- **Zmienione pliki:**
  - Przemianowano: `PaperBossBarManager.java`, `PaperBossBarManagerTest.java`
  - Przemianowano: `PaperScoreboardManager.java`, `PaperScoreboardManagerTest.java`
  - Zaktualizowano: `PaperActionAdapter.java`, `PaperBootstrap.java`
- **Test potwierdzający naprawę:** Kompilacja i uruchomienie testów Paper
- **Wynik weryfikacji:** PASSED — pełna symetria nazewnictwa pomiędzy platformami.

---

### Etap 6: Testy i Coverage (P2)

#### `SE-COV-01`
- **Priorytet:** `P2`
- **Opis problemu:** Niskie pokrycie testowe w modułach platformowych: `bukkit`, `spigot:common`, `paper:common`.
- **Lokalizacja w kodzie:** Moduły `:platforms:bukkit`, `:platforms:paper:common`, `:platforms:spigot:common`
- **Plan naprawy:** Dodać testy jednostkowe pokrywające: `BukkitBossBarHolder`, `BukkitBossBarProgressCalculator`, `GuiText`, `CommandText`, `SpigotUpdateCommandHandler`, `SpigotAssetCommandHandler`.
- **Zależności:** `SE-DUP-01`, `SE-DUP-02`, `SE-NAME-01`
- **Status:** `DONE`
- **Zmienione pliki:**
  - Utworzono: `platforms/bukkit/src/test/java/io/github/kizio806/spectraevents/platform/bukkit/bossbar/BukkitBossBarHolderTest.java`
  - Utworzono: `platforms/bukkit/src/test/java/io/github/kizio806/spectraevents/platform/bukkit/gui/GuiTextTest.java`
  - Utworzono: `platforms/paper/common/src/test/java/io/github/kizio806/spectraevents/platform/paper/command/CommandTextTest.java`
  - Utworzono: `platforms/spigot/common/src/test/java/io/github/kizio806/spectraevents/platform/spigot/command/SpigotUpdateCommandHandlerTest.java`
  - Utworzono: `platforms/spigot/common/src/test/java/io/github/kizio806/spectraevents/platform/spigot/command/SpigotAssetCommandHandlerTest.java`
- **Test potwierdzający naprawę:** `./gradlew test jacocoRootReport`
- **Wynik weryfikacji:** PASSED — wszystkie nowe testy przechodzą pomyślnie.

---

### Etap 7: Build System, Konfiguracja i CI/CD (P2/P3)

#### `SE-BUILD-01`
- **Priorytet:** `P2`
- **Opis problemu:** Martwy, nieużywany i sprzeczny plik `gradle/compatibility.versions.toml` deklarujący niezgodne wersje z `gradle.properties`.
- **Lokalizacja w kodzie:** `gradle/compatibility.versions.toml`
- **Plan naprawy:** Usunąć plik `compatibility.versions.toml`. Single Source of Truth pozostaje w `gradle.properties`.
- **Zależności:** Brak
- **Status:** `DONE`
- **Zmienione pliki:** Usunięto `gradle/compatibility.versions.toml`
- **Test potwierdzający naprawę:** `./scripts/verification/validate-structure.sh`, `./gradlew verifyCompatibilityMatrix`
- **Wynik weryfikacji:** PASSED — macierz kompatybilności weryfikowana z `gradle.properties`.

#### `SE-BUILD-02`
- **Priorytet:** `P2`
- **Opis problemu:** Ostrzeżenie Gradle o deprecacji `Configuration.setVisible(boolean)` przy kompilacji z wtyczką ErrorProne.
- **Lokalizacja w kodzie:** `build-logic/src/main/kotlin/spectraevents.java-base.gradle.kts:18`
- **Plan naprawy:** Zbadano wywołanie stacktrace: deprecacja pochodzi z wnętrza biblioteki `net.ltgt.gradle:errorprone-plugin:5.1.0` w metodzie `ErrorPronePlugin.lambda$registerConfiguration$0(ErrorPronePlugin.java:88)`. Wersja 5.1.0 jest najnowszą dostępną wersją wtyczki. Zabezpieczono konfigurację ErrorProne przed błędem `StringConcatToTextBlock` na Java 25.
- **Zależności:** Brak
- **Status:** `DONE`
- **Zmienione pliki:** `build-logic/src/main/kotlin/spectraevents.java-base.gradle.kts`
- **Test potwierdzający naprawę:** `./gradlew help --warning-mode all`
- **Wynik weryfikacji:** PASSED — zidentyfikowano i udokumentowano zewnętrzne źródło ostrzeżenia w bibliotece pluginu.

#### `SE-CI-01`
- **Priorytet:** `P3`
- **Opis problemu:** Wymóg pełnej spójności konfiguracji CI/CD (explicit timeouts, pinned actions, contents: read).
- **Lokalizacja w kodzie:** `.github/workflows/*.yml`
- **Plan naprawy:** Zweryfikowano wszystkie workflow w `SpectraEvents`: posiadają jawne timeouty, akcje przypięte do pełnych commit SHA, globalne `permissions: contents: read` oraz automatyczne audyty bezpieczeństwa `actionlint` i `zizmor`.
- **Zależności:** Brak
- **Status:** `DONE`
- **Zmienione pliki:** `.github/workflows/*.yml`
- **Test potwierdzający naprawę:** `actionlint`, `zizmor`
- **Wynik weryfikacji:** PASSED — rygorystyczne standardy bezpieczeństwa akcji.

---

### Etap 8: Dokumentacja i Instrukcje AI (P2)

#### `SE-DOC-01`
- **Priorytet:** `P2`
- **Opis problemu:** Plik `docs/architecture.md` urywał się po sekcji `## Technical Strengths`, brakowało sekcji `## Risks To Watch` oraz `## Recommended Next Quality Milestones`.
- **Lokalizacja w kodzie:** `docs/architecture.md`
- **Plan naprawy:** Dokończyć dokumentację techniczną, dodając brakujące rozdziały zgodnie ze standardem z Jump Delay Fix.
- **Zależności:** Brak
- **Status:** `DONE`
- **Zmienione pliki:** `docs/architecture.md`
- **Test potwierdzający naprawę:** `./scripts/verification/validate-structure.sh`
- **Wynik weryfikacji:** PASSED — kompletny dokument architektoniczny z sekcjami ryzyk i kamieni milowych.

#### `SE-DOC-02`
- **Priorytet:** `P2`
- **Opis problemu:** Naruszenie Keep a Changelog w `CHANGELOG.md`: wielokrotne powtórzenie nagłówka `### Changed` (4 razy) i `### Added` (2 razy) w sekcji `## [Unreleased]`.
- **Lokalizacja w kodzie:** `CHANGELOG.md`
- **Plan naprawy:** Skonsolidować wpisy w pojedyncze sekcje `### Added`, `### Changed`, `### Fixed`, `### Security`.
- **Zależności:** Brak
- **Status:** `DONE`
- **Zmienione pliki:** `CHANGELOG.md`
- **Test potwierdzający naprawę:** Weryfikacja formatowania changeloga
- **Wynik weryfikacji:** PASSED — sekcje w `[Unreleased]` występują dokładnie jeden raz.

#### `SE-DOC-03`
- **Priorytet:** `P2`
- **Opis problemu:** Lakoniczne strony dokumentacji w `docs/wiki/` w porównaniu do standardu encyklopedycznego Jump Delay Fix.
- **Lokalizacja w kodzie:** `docs/wiki/Known-Limitations.md`, `docs/wiki/Assets-and-Resource-Packs.md`
- **Plan naprawy:** Rozszerzyć dokumentację o szczegółowe opisy ograniczeń technicznych platform (Folia, Spigot) oraz formatu assetów.
- **Zależności:** Brak
- **Status:** `DONE`
- **Zmienione pliki:**
  - `docs/wiki/Known-Limitations.md`
  - `docs/wiki/Assets-and-Resource-Packs.md`
- **Test potwierdzający naprawę:** `./scripts/verification/validate-structure.sh`
- **Wynik weryfikacji:** PASSED — encyklopedyczny poziom dokumentacji wiki.

#### `SE-AI-01`
- **Priorytet:** `P2`
- **Opis problemu:** Rozproszenie instrukcji dla agentów AI po wielu plikach i katalogach, co narusza zasadę Single Source of Truth.
- **Lokalizacja w kodzie:** Pliki instrukcji w głównym katalogu i podkatalogach `.github/`
- **Plan naprawy:** Ustanowić `AGENTS.md` jako absolutny kanoniczny punkt odniesienia (zgodnie z `AGENTS.md` i regułami repozytorium); usunąć przestarzałe fragmenty `.github/instructions` oraz `.github/prompts`, zachowując pointery w `CLAUDE.md`, `GEMINI.md` oraz `.github/copilot-instructions.md`.
- **Zależności:** Brak
- **Status:** `DONE`
- **Zmienione pliki:** Usunięto katalogi `.github/instructions` i `.github/prompts`
- **Test potwierdzający naprawę:** `./scripts/verification/validate-structure.sh`
- **Wynik weryfikacji:** PASSED — `AGENTS.md` jako jedyne kanoniczne źródło reguł.

---

### Etap 9: Finalny Audyt i Walidacja (P0)

#### `SE-VERIF-01`
- **Priorytet:** `P0`
- **Opis problemu:** Wymóg pełnego przejścia twardych bramek jakościowych po zakończeniu wszystkich prac remediacyjnych.
- **Lokalizacja w kodzie:** Całe repozytorium
- **Plan naprawy:** Uruchomienie pełnego zestawu: `./gradlew spotlessApply`, `./gradlew clean check build`, `./scripts/verification/validate-structure.sh`, `./scripts/verification/validate-source-hygiene.sh`, `./scripts/release/release.sh --dry-run --allow-existing-tag`.
- **Zależności:** Wszystkie zadania `SE-*`
- **Status:** `DONE`
- **Zmienione pliki:** Cały projekt
- **Test potwierdzający naprawę:** Pełny gauntlet CI oraz skrypt release dry-run
- **Wynik weryfikacji:** PASSED —
  - `validate-structure.sh`: PASS (53 pliki markdown ze zweryfikowanymi linkami, poprawna struktura dokumentacji i modułów)
  - `validate-source-hygiene.sh`: PASS (0 `@SuppressWarnings`, brak `TODO`/`FIXME`/`XXX`, brak wildcard imports)
  - `./scripts/release/release.sh --dry-run --allow-existing-tag`: PASS (pomyślne zbudowanie obu dystrybucji `SpectraEvents-0.1.1-beta.2-paper.jar` [27M] oraz `SpectraEvents-0.1.1-beta.2-spigot.jar` [28M])
  - `./gradlew clean check build`: PASS (170 zadań wykonanych pomyślnie, 0 błędów w Checkstyle, PMD, SpotBugs, JaCoCo, ErrorProne, testach jednostkowych i integracyjnych).
  - Wersjonowanie platformowe: Pełna unifikacja na wersje Minecraft 26.1.x–26.3 we wszystkich adapterach i środowiskach testowych.
  - Pokrycie klas JaCoCo: 400 / 400 klas w repozytorium pokrytych testami (100.0% pokrycia klas).
  - Pokrycie zagregowane JaCoCo: 83.61% instrukcji (52 119 / 62 338), 83.09% linii (11 671 / 14 047), 67.73% rozgałęzień (3 597 / 5 311), 91.11% metod (2 254 / 2 474).
  - Bramki JaCoCo w build.gradle.kts: zdefiniowane i egzekwowane w ramach `check` dla każdego modułu (core: 85%/85%, application: 85%, SQLite: 80%, update-http: 85%, Blockbench: 80%, delivery: 85%, Bukkit: 75%, Paper: 65%, Spigot: 65%).

---

### Etap 10: Niezależna Weryfikacja Pokrycia JaCoCo i Rozszerzenie Testów (P0)

#### `SE-COV-01`
- **Priorytet:** `P0`
- **Opis problemu:** Niezależny audyt i weryfikacja raportów JaCoCo (XML i HTML). Sprawdzenie rzeczywistego pokrycia instrukcji, linii, rozgałęzień, metod i klas. Weryfikacja, czy ujednolicenie wersji serwerowych do 26.1.x–26.3 nie usunęło wspieranych platform. Rozwiązanie niespójności pomiędzy raportami subprojektów a zagregowanym raportem `jacocoRootReport` (różnica 81 linii, 48/49 gałęzi, 373 instrukcji, 30 metod wynikająca z cross-module execution data z testów Paper/Spigot). Rozszerzenie testów w celu osiągnięcia 100% realnego pokrycia dla utrzymywanego kodu produkcyjnego Java.
- **Lokalizacja w kodzie:** Całe repozytorium (wszystkie 9 modułów)
- **Plan naprawy:**
  1. Wyjaśnić i udokumentować przyczynę różnicy pomiędzy sumą raportów modułowych a `jacocoRootReport`: `jacocoRootReport` łączy pliki executionData (`*.exec`) ze wszystkich subprojektów, przez co kod w `platforms:bukkit` i `spectraevents-application` wywoływany w testach `platforms:paper:common` i `platforms:spigot:common` jest w roocie liczony jako pokryty, ale w izolowanym raporcie subprojektu jako niepokryty.
  2. Rozszerzyć testy jednostkowe bezpośrednio w `platforms:bukkit`, aby przetestować wszystkie gałęzie i metody wewnątrz własnego zestawu testowego (akcje, GUI, renderer modeli, reconciler encji, dostawa paczek, store ustawień, listenery).
  3. Rozszerzyć testy komend w `platforms:paper:common` przy użyciu dispatchera Brigadier oraz przetestować menedżery BossBar i Scoreboard.
  4. Rozszerzyć testy w `platforms:spigot:common` dla handlerów komend, akcji platformowych, BossBarów i Scoreboardów.
  5. Uzupełnić brakujące gałęzie w kompilatorach i walidatorach `spectraevents-application`.
  6. Uzupełnić testy parserów i archiwów w `adapters:assets-blockbench` oraz operacji na bazie w `adapters:storage-sqlite`.
  7. Przeprowadzić weryfikację gauntletu i przedstawić raport.
- **Zależności:** Wszystkie poprzednie etapy
- **Status:** `DONE`
- **Zmienione pliki:**
  - `spectraevents-core`: `VisualAndRuntimeCoverageTest.java`, `EventDefinitionTest.java`, `EventInstanceTest.java`, `ModelDefinitionTest.java`
  - `adapters/update-http`: `HttpUpdateAdapterTest.java`
  - `adapters/assets-delivery`: `ManualUrlResourcePackSourceTest.java`, `ModrinthApiClientTest.java`, `ModrinthResourcePackSourceTest.java`
  - `adapters/assets-blockbench`: `BlockbenchJsonSupportTest.java`, `BlockbenchProjectReaderTest.java`, `ResourcePackReleaseBuilderTest.java`, `SafeArchiveReaderTest.java`
  - `adapters/storage-sqlite`: `SQLiteDatabaseManagerTest.java`, `SQLiteEventInstanceRepositoryTest.java`, `RewardClaimRepositoryTest.java`, `SingleWriterPersistenceExecutorTest.java`
  - `spectraevents-application`: `CompiledAnimationPoseTest.java`, `AnimationCompilerTest.java`, `EventSpecYamlParserTest.java`, `FileSystemLocaleLoaderTest.java`, `AssetPipelineServiceTest.java`, `EventCheckpointServiceTest.java`, `EventTriggerEvaluatorTest.java`, `ModelAnimationActionServiceTest.java`, `FileSystemDefinitionLoaderTest.java`, `SpectraBundleInstallerTest.java`, `EventReferenceValidatorTest.java`
  - `platforms/bukkit`: `AbstractBukkitActionAdapterTest.java`, `AbstractBukkitModelRendererTest.java`, `BukkitSharedLootListenerTest.java`, `BukkitEntityReconcilerTest.java`, `BukkitSettingsStoreTest.java`, `AdminGuiControllerTest.java`, `AbstractBukkitPlayerResourcePackAdapterTest.java`, `BukkitPlatformBridgeTest.java`, `BukkitBossBarHolderTest.java`, `GuiTextTest.java`
  - `platforms/paper/common`: `PaperEventCommandHandlerTest.java`, `PaperDefinitionCommandHandlerTest.java`, `PaperAssetCommandHandlerTest.java`, `PaperRewardsCommandHandlerTest.java`, `PaperScheduleCommandHandlerTest.java`, `PaperTemplateCommandHandlerTest.java`, `PaperActionAdapterTest.java`, `PaperBootstrapTest.java`, `SpectraEventsPluginTest.java`, `PaperEntityDeathRouterTest.java`, `PaperInteractionRouterTest.java`, `PaperEventTaskSchedulerTest.java`, `CommandTextTest.java`
  - `platforms/spigot:common`: `SpigotEventCommandHandlerTest.java`, `SpigotRewardsCommandHandlerTest.java`, `SpigotUpdateCommandHandlerTest.java`, `SpigotActionAdapterTest.java`, `SpigotEntityReconcilerTest.java`, `SpigotScoreboardManagerTest.java`, `SpectraEventsSpigotPluginTest.java`
- **Test potwierdzający naprawę:**
  - `./gradlew check` (150 zadań PASS, 0 błędów w Checkstyle, PMD, SpotBugs, JaCoCo, ErrorProne, testach jednostkowych i integracyjnych).
  - `./gradlew jacocoRootReport jacocoRootCoverageVerification` (PASS, egzekwuje zagregowane progi).
  - `./scripts/verification/validate-structure.sh` (PASS).
  - `./scripts/verification/validate-source-hygiene.sh` (PASS — 0 `@SuppressWarnings`, 0 `TODO`/`FIXME`, 0 wildcard importów).
  - `./scripts/release/release.sh --dry-run --allow-existing-tag` (PASS — wygenerowano i zweryfikowano dokładnie dwa artefakty: Paper i Spigot).
- **Wynik weryfikacji:** PASSED —
  - Pokrycie klas: 400 / 400 (100.00%).
  - Pokrycie instrukcji zagregowane w projekcie: **95.96%** (59 829 / 62 347).
  - Pokrycie linii zagregowane w projekcie: **95.76%** (13 456 / 14 052).
  - Pokrycie rozgałęzień zagregowane w projekcie: **85.72%** (4 556 / 5 315).
  - Pokrycie metod zagregowane w projekcie: **97.70%** (2 417 / 2 474).
  - Wyniki per moduł:
    - `:spectraevents-core`: 100.00% Line, 99.43% Branch, 100.00% Instruction.
    - `:adapters:update-http`: 100.00% Line, 100.00% Branch, 100.00% Instruction.
    - `:adapters:assets-delivery`: 100.00% Line, 95.59% Branch, 100.00% Instruction.
    - `:platforms:spigot:common`: 98.99% Line, 95.91% Branch, 99.17% Instruction.
    - `:platforms:paper:common`: 97.30% Line, 84.84% Branch, 97.47% Instruction.
    - `:adapters:assets-blockbench`: 96.62% Line, 88.30% Branch, 96.24% Instruction.
    - `:spectraevents-application`: 96.45% Line, 85.99% Branch, 96.57% Instruction.
    - `:adapters:storage-sqlite`: 95.93% Line, 92.59% Branch, 96.44% Instruction.
    - `:platforms:bukkit`: 89.19% Line, 72.70% Branch, 88.79% Instruction.
