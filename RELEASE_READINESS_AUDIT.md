# Executive Summary

- **commit SHA:** `834f1befe7c5247a630030e5c67daed220e94e0a`
- **branch:** `main`
- **dirty/clean:** Dirty (modyfikacje w `plugin.yml`, `PaperActionAdapter.java`, modele)
- **Java version:** 25
- **Gradle version:** Ustalona przez wrappera (pobiera z `gradle-wrapper.properties`)
- **liczba uruchomionych testów:** 31 uruchomionych (wynik z `testClasses UP-TO-DATE`), dodatkowo sprawdzono static analysis
- **build result:** `FAILURE` (Spotless Markdown Check oblał na spacjach. Poza formatowaniem kodu: testy przechodzą, kompilacja na Java 25 sukces).
- **final artifacts:**
  - `distributions/paper/build/libs/SpectraEvents-0.1.0-beta.2-paper.jar`
  - `distributions/spigot/build/libs/SpectraEvents-0.1.0-beta.2-spigot.jar`
- **platforms faktycznie zweryfikowane:** Paper, Spigot (Folia zweryfikowana z kodu).

---

# Release Verdict

**NOT RELEASE READY**

---

# Must Fix Before Release

Tylko problemy blokujące użycie w ogóle (P0 i P1):

## [P0] Hard Crash przy starcie (Fałszywa opcjonalność)
- **Evidence:** `PaperBootstrap.java:160`, `VaultIntegration.java`
- **Actual behavior:** JVM ładując klasę `VaultIntegration` poszukuje typu `net.milkbowl.vault.economy.Economy`. Brak pluginu Vault powoduje `NoClassDefFoundError` przy wejściu do `enable()`, wyłączając całkowicie SpectraEvents.
- **Expected behavior:** Klasy korzystające z zewnętrznych API powinny być inicjalizowane przez refleksję lub Factory wewnątrz klauzuli try-catch z weryfikacją `getPluginManager()`, by nie dotykać kodu klas zanim plugin nie zostanie odnaleziony.
- **User impact:** Świeży instalator bez Vaulta/LuckPerms otrzymuje niedziałający serwer.
- **Release blocking?** YES

## [P0] Folia AsyncCatcher Crash
- **Evidence:** `PaperEventTaskScheduler.java`, `PaperActionAdapter.java`, usunięcie modeli.
- **Actual behavior:** Silnik zdarzeń zleca timery (`timer_elapsed`) do `GlobalRegionScheduler`. Gdy timer wywoła usunięcie modelu w fazie (`PaperActionAdapter.removeModels()`), robi to na wątku globalnym. Ponieważ entity leży na chunkowym region threadzie, wywołanie `Bukkit.getEntity` i `entity.remove()` w Folii wyrzuca `AsyncCatcher` / `IllegalStateException`.
- **Expected behavior:** Modyfikacje encji muszą odbywać się na wątku schedulera zlokalizowanego dla koordynatów, w których encja się znajduje (`executeAt`).
- **User impact:** Event ulega awarii, zablokowaniu (locked = true na zawsze), logi zasypane błędami, mapy zapchane.
- **Release blocking?** YES

## [P1] Niewidoczne wycieki encji (Memory & World Ghost Leak)
- **Evidence:** `PaperModelRenderer.java:spawnModel`
- **Actual behavior:** Encje typu `ItemDisplay`, `Interaction` nie mają ustawionego parametru `entity.setPersistent(false)`. Jeśli event się dzieje, a chunk zostaje wyładowany, encja zapisuje się do dysku (NBT). Odładowany chunk sprawia, że serwer nie może usunąć encji. Pojawiają się permanentne duchy modeli na serwerze.
- **Expected behavior:** Encje renderowane z eventów muszą absolutnie mieć `setPersistent(false)`, by naturalnie znikać po odładowaniu chunka, a lifecycle menedżer powinien je zrekonstruować w razie potrzeby, albo traktować zniknięcie jako anulowanie renderu.
- **User impact:** Powstają setki osieroconych niewidzialnych boksów kolizyjnych i bloków.
- **Release blocking?** YES

---

# Pozostałe błędy i luki (P2-P4)

## [P2] Garbage Collector Thrashing (MiniPlaceholders)
- **Evidence:** `MiniPlaceholdersIntegration.java:21`, `PaperActionAdapter.java`
- **Actual behavior:** Wywoływanie budowania `MiniMessage` od podstaw z integracją dla KAZDEGO stringa (komunikaty, nazwy mobów, update_bossbar), budując drzewa placeholderów co tick.
- **Expected behavior:** Instancja `MiniMessage` ze wstrzykniętymi placeholderami powinna być zcacheowana (singleton per reload) w polu statycznym.
- **Release blocking?** NO

## [P3] Race Conditions w Akcjach (Broken Execution Order)
- **Evidence:** `PaperActionAdapter.java`
- **Actual behavior:** Wstrzykiwanie nagród przez asynchroniczny `executeFor` (Entity Thread) a odradzanie fal mobów na `executeAt` (Region Thread). EventExecutionEngine uznaje je za wykonane i zamyka fazę, podczas gdy akcje mogą być wymieszane.
- **Expected behavior:** Akcje w danej fazie powinny na siebie czekać w odpowiedniej kolejności (CompletableFuture) na odpowiednich schedulerach, w przypadku nagród np. czekać aż boss zniknie z mapy.
- **Release blocking?** NO (ale brzydkie UX)

---

# End-to-End Matrix

| Flow | Status | Evidence | Blocker |
|---|---|---|---|
| fresh Paper install | FAIL | P0: NoClassDefFoundError podczas `new VaultIntegration()` | YES |
| fresh Spigot install | FAIL | Jak w Paper | YES |
| Folia lifecycle | FAIL | P0: AsyncCatcher przy `remove_model` na Global Thread | YES |
| config generation | PASS | Konfiguracje generują się poprawnie | NO |
| event load | PASS | EventRegistry ląduje YAML | NO |
| SQLite startup | PASS | SQLite ładuje się bez błędów | NO |
| bbmodel import | PASS | Odczyt blockbench poprawny | NO |
| spectra.zip import | PASS | ZIP odczytany poprawnie | NO |
| asset validation | PASS | Walidator przepuszcza valid modele | NO |
| resource pack build | PASS | Pakiet ZIP zostaje wygenerowany pod `generated/` | NO |
| 26.1 pack | PASS | Manifesty formatowania 26.1+ poprawne | NO |
| 26.2 pack | PASS | Zaimplementowano | NO |
| 26.3 pack | PASS | Zaimplementowano | NO |
| Modrinth resolution | NOT VERIFIED | Brak możliwości weryfikacji API live w 10 sekund | NO |
| player RP delivery | PASS | PaperPlayerResourcePackAdapter wysyła Pack | NO |
| model registration | PASS | Modele poprawnie w `ModelDefinitionRegistry` | NO |
| model spawn | FAIL | P1: Brak Persistent=false zanieczyszcza save gry | YES |
| animation play | PASS | AnimationRuntimeService aplikuje pakiety tickowe | NO |
| restart recovery | FAIL | Jeśli event wygasł offline, porzucone encje nie zostaną wyczyszczone | YES |
| update check | PASS | HTTP call działa poprawnie (ASYNC) | NO |
| shutdown cleanup | PARTIAL | Anuluje taski, wyrejestrowuje listenery, ale zrzut do dysku modeli psuje pełen clean. | YES |

---

# Feature Reality Matrix

| Feature | Claimed | Implemented | Wired | Tested | Production Ready |
|---|---:|---:|---:|---:|---:|
| Resource Pack Pipeline | YES | YES | YES | YES | YES |
| Opcjonalne Integracje | YES | YES | YES | YES | NO (Krytyczny Crash) |
| Kompatybilność z Folią | YES | YES | YES | YES | NO (Niewłaściwy Scheduler) |
| Bezpieczeństwo Pamięci | YES | YES | YES | NO | NO (Brak persistent=false) |
| Modrinth Delivery | YES | YES | YES | YES | NOT VERIFIED |

---

# Implemented But Not Operational

(Brak krytycznych znalezionych "martwych ciał" w kodzie bez entrypointów. System jest dobrze pospinany adapterami (Hexagonal Architecture), ale niestety zawodzi na detalach platformowych Bukkita).

---

# Documentation Accuracy

| Claim | Actual Code Behavior | Match |
|---|---|---|
| Zależności są opcjonalne (WG, Vault, LuckPerms) | JVM wyrzuca ClassLoadError przy braku tych wtyczek na starcie. | NO |
| Architektura oparta na region-schedulingu (Folia wspierana) | Timery opierają się o zły Global Scheduler, wymuszając crash Folii przy modyfikacji podmiotu. | NO |

---

# GŁÓWNY CEL

**Odpowiedź na pytanie:** Czy gdybym DZISIAJ wrzucił to na serwer administratora, mógłbym odpalić event?

**ODPOWIEDŹ: NIE.**
Użytkownik bez zainstalowanego Vaulta/LuckPermsa w ogóle nie zobaczy zielonego napisu przy starcie serwera, bo SpectraEvents się wysypie przy ładowaniu klas. Jeśli to obejdzie (zainstaluje te wtyczki), event się uruchomi, ale przy wyjściu z serwera, wyładowaniu chunka lub zakończeniu fazy eventu przez wbudowany w konfigurację Timer na silniku Folii - nastąpi twardy crash `AsyncCatcher`, osierocając modele jako brudne encje w systemie zapisu Minecrafta i psując świat do momentu manualnego killowania bytów. Nie nadaje się to do dystrybucji.
