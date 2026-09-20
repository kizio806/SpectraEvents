# SpectraEvents — Master Plan Projektu

> **Cel tego dokumentu:** jedna, aktualizowana mapa produktu i techniki. Ma wystarczyć osobie, która pobiera repozytorium, aby zrozumieć **czym jest SpectraEvents, jak ma działać, co jest potwierdzone, co jest tylko częściowe i co należy zrobić dalej**.
>
> **Status dokumentu:** stan repozytorium na 2026-09-20. Nie zastępuje testów ani `docs/ai/PROJECT_STATE.md`; przy rozbieżności kod i bieżące wyniki bramki jakości są źródłem prawdy.

## 1. Jednozdaniowy cel

**SpectraEvents to jeden, konfigurowalny silnik eventów 3D dla nowoczesnych serwerów Minecraft, w którym zarówno oficjalne eventy, jak i eventy administratorów używają tych samych modeli, animacji, faz, triggerów, warunków i akcji.**

Nie budujemy oddzielnych pluginów „Meteor”, „Airdrop” czy „Metin”. Budujemy silnik, dzięki któremu te eventy są definicjami danych oraz assetów. Kod Java dopisujemy tylko wtedy, gdy potrzebny jest nowy, rzeczywiście wspólny prymityw.

## 2. Docelowe doświadczenie użytkownika

```text
Model i animacje w Blockbench
             ↓
Bezpieczny import oraz walidacja assetów
             ↓
Zweryfikowany resource-pack ZIP i dostarczenie go graczowi
             ↓
Definicja eventu YAML (model, fazy, trigger, condition, action)
             ↓
Walidacja i kompilacja do niezmiennej definicji
             ↓
/event definition reload  →  /event event start <id>
             ↓
EventInstance wykonuje event, zapisuje stan i sprząta zasoby
```

Administrator powinien móc:

1. stworzyć lub zaimportować model w Blockbench;
2. umieścić asset w katalogu SpectraEvents;
3. podpiąć model i jego animacje do definicji YAML;
4. przetestować i uruchomić event bez pisania Javy;
5. skopiować gotowy Meteor/Airdrop/Metin i zmienić jego zachowanie;
6. napisać rozszerzenie Java tylko wtedy, gdy standardowe elementy silnika nie wystarczą.

Oficjalne eventy nie mogą mieć „ukrytej” ścieżki niedostępnej dla użytkownika. Muszą działać na tym samym publicznie opisanym formacie konfiguracji i tych samych prymitywach co eventy własne.

## 3. Jak event ma być zbudowany

```text
EventDefinition
  └── EventInstance
        └── Phase
              ├── Trigger — kiedy coś się wydarzyło
              ├── Condition — czy można kontynuować
              └── Action — co silnik ma wykonać
```

Przykład logiczny: faza `falling` spawnuje model Meteoru i uruchamia animację spadania; timer przechodzi do `impact`; po uderzeniu aktywna faza przyjmuje obrażenia; `health_threshold_crossed` uruchamia nagrodę i cleanup.

### Granice odpowiedzialności

| Warstwa | Odpowiada za | Nie może robić |
| --- | --- | --- |
| `spectraevents-core` | reguły domenowe, stan eventu, modele danych, matematyka modeli i animacji | zależeć od Bukkit, Paper, Spigot, NMS lub bazy danych |
| `spectraevents-application` | przypadki użycia, kompilację konfiguracji, orkiestrację i porty | importować API Minecrafta |
| `adapters/*` | SQLite, HTTP update i przyszły import/delivery assetów | zawierać logikę platformy Minecraft |
| `platforms/paper` i `platforms/spigot` | encje, komendy, schedulery, renderowanie, integracje platformowe | zawierać reguły biznesowe eventów |
| `distributions/*` | końcowe JAR-y dla właściwych rodzin serwerów | mieszać artefakty Paper i Spigot |

Kierunek zależności jest zawsze: `platform/infrastructure → application → core`.

## 4. Dwa sposoby tworzenia eventów

| Sposób | Dla kogo | Co powstaje | Kiedy używać |
| --- | --- | --- | --- |
| Definicja YAML + assety | administrator, twórca serwera | model, animacje, fazy, timery, nagrody, gotowe akcje | w zdecydowanej większości eventów |
| Rozszerzenie Java | autor dodatku / programista | nowy `Action`, `Trigger`, `Condition`, integracja lub port | wyłącznie gdy wspólnego zachowania nie da się wyrazić konfiguracją |

Docelowy przykład katalogów użytkownika:

```text
plugins/SpectraEvents/
├── events/
│   ├── meteor.yml                 # gotowa definicja dostarczona z pluginem
│   └── pirate-treasure.yml        # własny event administratora
├── models/
│   └── pirate-chest.yml           # skompilowana/opisana definicja modelu
└── assets/
    └── pirate-chest/              # źródło Blockbench, tekstury i animacje
```

Finalny importer może zmienić dokładną strukturę assetów, ale nie może zmienić zasady: źródło modelu, wynik importu i referencja z YAML muszą być czytelne, walidowalne i powtarzalne.

## 5. Stan produktu — tabela prawdy

Legenda:

- **Zrobione (historycznie zweryfikowane)** — kod i wskazane testy zostały wcześniej potwierdzone.
- **Zaimplementowane, wymagające ponownej bramki** — istnieje w aktualnym checkoutcie, ale pełna obecna bramka nie jest zielona.
- **Częściowe / zablokowane** — część przygotowano, lecz nie wolno składać obietnicy produktu.
- **Do zrobienia** — nie należy tego przedstawiać jako istniejącej funkcji.

| Obszar | Status | Co jest dostępne | Co oznacza „gotowe” |
| --- | --- | --- | --- |
| Architektura modułów | Zrobione historycznie, wymaga ponownej bramki | Core i application są platform-neutral; osobne adaptery, platformy i dystrybucje | test granic architektury oraz pełny build przechodzą na aktualnym checkoutcie |
| Lifecycle i fazy | Zrobione historycznie, wymaga ponownej bramki | `EventInstance`, stany, przejścia, fazy oraz konfiguracja eventów | scenariusze startu, przejść, anulowania, błędu i cleanup są testowane |
| Meteor, Airdrop, Metin | Zaimplementowane, wymagające ponownej bramki | trzy referencyjne eventy uruchamiane przez wspólny silnik YAML | każdy ma real-server workflow, restart/recovery i dokumentację użytkownika |
| YAML: parsing, validation, compile | Zaimplementowane, wymagające ponownej bramki | loader, parser, registry i kompilacja niezmiennych definicji | każdy błąd podaje plik, ścieżkę, przyczynę i naprawę; reload nie psuje działających instancji |
| Modele 3D native | Zaimplementowane, wymagające ponownej bramki | Display i Interaction entities, modele wieloczęściowe, hierarchie, rollback i cleanup | testy oraz realny serwer potwierdzają spawn, interakcję, usunięcie i recovery |
| Animacje | Zaimplementowane, wymagające ponownej bramki | timeline, keyframes, easing, pause/resume/seek/stop i interpolacja klienta | testy matematyki oraz realny przebieg animacji na serwerze |
| Persistence SQLite | Zaimplementowane, wymagające ponownej bramki | WAL, single writer, stan eventu i recovery | crash/restart workflow oraz testy wyścigów i trwałości przechodzą |
| Nagrody i claimy | Zaimplementowane z istotnym ograniczeniem | trwałe przyjęcie claimu, `give_item`, diagnostyka niedostarczonych nagród | jawna procedura operatora dla crasha między zapisem a zmianą ekwipunku; bez fałszywej obietnicy exactly-once |
| Integracje | Zaimplementowane, wymagające ponownej bramki | LuckPerms, WorldGuard, Vault, PlaceholderAPI, MiniPlaceholders, Nexo, Oraxen, ItemsAdder | każda działa albo jawnie raportuje brak/unsupported przez `/event doctor` |
| Paper/Purpur/Folia | Zrobione dla wszystkich dostępnych runtime'ów | Paper artifact; schedulery regionów i jawne ograniczenie scoreboardu Folii | 8/8 dostępnych wierszy przechodzi prawdziwy workflow; Folia 26.3 pozostaje zewnętrznie niedostępna |
| Spigot/CraftBukkit | Zrobione dla wszystkich dostępnych runtime'ów | osobny artifact i adapter Spigot | 6/6 dostępnych wierszy przechodzi prawdziwy workflow; deklarowany zakres jest zweryfikowany |
| Import Blockbench | Częściowe / wyłączone | istnieje kod i dokumentacja eksperymentalna importera | wejście Blockbench przechodzi walidację i buduje prawdziwe, używalne assety |
| Resource-pack ZIP | Do zrobienia | nie ma zweryfikowanego produktu ZIP | prawdziwy ZIP, manifest, SHA-1, item model mapping, walidacja i test klienta |
| Dostarczanie packa graczom | Częściowe / wyłączone fail-closed | interfejsy/adapters mogą istnieć, ale funkcja nie jest obiecana | testy delivery, odrzucenia, ponownego wejścia gracza i awarii sieci |
| Oficjalne eventy | Częściowe | Meteor, Airdrop, Metin są referencjami | kolejne eventy powstają głównie z YAML, bez ukrytej logiki dostępnej tylko twórcom |
| Własne eventy administratora | Częściowe | kopiowanie i modyfikowanie YAML jest zamierzonym workflow | pełne przykłady, walidacja, asset import oraz guide od zera |
| Publiczne API dodatków | Do zrobienia świadomie | nie ma stabilnego publicznego API | dopiero po ustaleniu realnego use case'u, wersjonowania, testów i polityki kompatybilności |
| GUI edytora | Do zrobienia po V1 | istnieje administracyjne GUI/diagnostyka, nie pełny editor | GUI jest tylko frontendem stabilnego YAML, nigdy osobnym źródłem prawdy |

## 6. Aktualny stan jakości i ograniczenia

Pełna wymagana bramka:

```bash
./gradlew clean check build
```

została uruchomiona 2026-09-20 na aktualnym checkoutcie i zakończyła się **`BUILD SUCCESSFUL`**. Przeszły testy, formatowanie, Checkstyle, PMD, SpotBugs, JaCoCo, weryfikacja granic platform oraz budowanie artefaktów Paper i Spigot. Strict dependency verification pozostaje włączone i ma aktualne sumy SHA-256.

Zielona bramka nie oznacza jeszcze gotowego release'u. Osiem lokalnych checkpointów zostało już utworzonych bez pushu. Dostępna real-server matrix jest domknięta: 14/14 wierszy przeszło, a Folia 26.3 pozostaje zewnętrznie niedostępna. Nadal otwarty jest niezweryfikowany end-to-end pipeline Blockbench → resource-pack → klient.

### Kolejność dalszej weryfikacji

1. Zachować wynik zielonej pełnej bramki jako punkt odniesienia.
2. Traktować osiem lokalnych commitów jako spójne checkpointy do przeglądu i ewentualnego odzyskiwania; nie squashować ani nie pushować ich automatycznie.
3. Przy każdej kolejnej zmianie uruchamiać testy zakresowe i pełną bramkę.
4. Utrzymywać dowód 14/14 dostępnych wierszy real-server matrix i dokumentować niedostępne buildy upstream.
5. Dopiero po tych dowodach aktualizować status release'u oraz przechodzić do kolejnych milestone'ów.

## 7. Plan dojścia do najlepszego pluginu

### M0 — uporządkowanie i zielona baza

**Cel:** dokładnie wiedzieć, co działa na obecnym checkoutcie.

- [x] Naprawić dependency verification bez wyłączania ochrony.
- [x] Uruchomić `./gradlew clean check build` z wynikiem `BUILD SUCCESSFUL`.
- [x] Uruchomić testy granic platform i sprawdzić, że core/application nie importują Minecraft API.
- [x] Przejrzeć duży obecny diff i podzielić go logicznie na spójne zmiany/milestone'y.
- [x] Ujednolicić `README.md`, roadmapę produktu i `docs/ai/PROJECT_STATE.md` z tym dokumentem.
- [x] Zapisać dokładne dowody: komenda, data, wynik, wersje JDK/Gradle i wynik testów runtime.

**M0 jest zielone dla wszystkich dostępnych runtime'ów. Publikacja nadal pozostaje zablokowana przez brak Folia 26.3 upstream.**

### Inwentaryzacja bieżącego dużego diffu

Stan po przeglądzie worktree: **147 zmienionych ścieżek tracked oraz dodatkowe pliki untracked**. To nie jest jeden bezpieczny commit funkcjonalny. Zmiany mieszają kilka niezależnych tematów:

| Proponowany pakiet | Główne ścieżki | Zakres | Status organizacyjny |
| --- | --- | --- | --- |
| Q0 — build i jakość | `build.gradle.kts`, `build-logic/`, `config/`, `gradle/`, `settings.gradle.kts`, `.github/workflows/` | Gradle, dependency verification, Checkstyle/PMD/SpotBugs, CI i release checks | checkpoint `3e954a1`; dependency verification ma dodatkowy commit `7846e28` |
| E1 — wspólny runtime eventów | `spectraevents-core/`, `spectraevents-application/`, `examples/events/` | lifecycle, YAML, execution engine, conditions/actions, lokacje, testy i definicje referencyjne | checkpoint `abb41d4`; runtime platform-neutral jest rozdzielony od platform |
| E2 — platformy i dystrybucje | `platforms/paper/`, `platforms/spigot/`, `distributions/` | adaptery Paper/Spigot, schedulery, komendy, GUI, renderery, integracje, JAR-y | checkpoint `f9abc19`; Paper-family 8/8 i Spigot-family 6/6 dostępnych PASS |
| E3 — trwałość i adaptery | `adapters/storage-sqlite/`, `adapters/update-http/` | SQLite single-writer, recovery, update HTTP, testy obciążeniowe | checkpoint `fa5fb28`; recovery jest pokryte pełną bramką i smoke workflow |
| E4 — Blockbench i resource-pack | `adapters/assets-*`, `spectraevents-application/src/main/.../asset/`, `tools/blockbench/`, `docs/authoring/`, `docs/config/` | importer, budowanie ZIP, delivery, Modrinth, testy bezpieczeństwa | checkpoint `facbea6`; zakres nadal częściowy, pełny pipeline nie jest obiecany |
| E5 — runtime smoke i kompatybilność | `scripts/runtime-smoke/`, manifesty pluginów, dokumentacja platform | Paper/Purpur/Folia/Spigot/CraftBukkit i macierz wersji | checkpoint `acf86aa`; 14/14 dostępnych PASS, Folia 26.3 zależy od buildu upstream |
| D1 — dokumentacja produktu | `README.md`, `docs/ai/`, `docs/architecture/`, `docs/product/` | kontrakt produktu, workflow administratora, architektura, roadmapa i ograniczenia | checkpoint `d61bf7a`; dokumentacja zsynchronizowana z aktualnym stanem |
| A0 — odroczenie public API | `spectraevents-api/`, ADR 0005 | usunięcie przedwczesnego modułu public API | checkpoint `c38387d`; nie przywracać bez udowodnionego use case'u |

### Bezpieczna kolejność dalszego porządkowania

1. Zachować bieżący zielony punkt kontrolny `./gradlew clean check build`.
2. Traktować osiem lokalnych commitów jako checkpointy do przeglądu; nie wykonywać resetu, checkoutu ani automatycznego squashowania.
3. Przy kolejnych zmianach dotykać tylko jednego pakietu na raz.
4. Dla każdego pakietu uruchamiać jego testy, potem pełną bramkę i dopisywać dowód do dziennika.
5. Przejść do M1 i M2; nie dodawać kolejnych eventów tylko po to, aby zwiększać liczbę funkcji.

Rozdzielenie zmian zostało wykonane z zachowaniem ich kontekstu w ośmiu lokalnych commitach. Nie wykonujemy automatycznego `reset`, `checkout`, squashowania ani pushowania, ponieważ każdy checkpoint ma pozostać czytelnym punktem przeglądu i odzyskiwania.

### M1 — kontrakt autora eventu

**Cel:** osoba bez Javy potrafi stworzyć bezpieczny event YAML.

- [ ] Spisać stabilny schemat definicji eventu: model, animacje, phases, triggers, conditions, actions, rewards, recovery, cleanup.
- [x] Każdy błąd walidacji wskazuje plik i ścieżkę YAML, także dla błędów parsera, kompilatora i duplikatów ID.
- [ ] Dodać minimum trzy kompletne, działające przykłady o różnych mechanikach.
- [ ] Dodać guide: „skopiuj event → zmień model → validate → reload → start → diagnose”.
- [ ] Zagwarantować, że running instance zachowuje stary snapshot definicji po reloadzie.

### M2 — profesjonalny pipeline Blockbench i assetów

**Cel:** model z Blockbench trafia łatwo i bezpiecznie do eventu.

- [ ] Zdefiniować jeden wspierany format wejściowy oraz stabilną strukturę katalogów.
- [ ] Importować geometrię, pivoty, hierarchię, tekstury i nazwane animacje.
- [ ] Wprowadzić limity: rozmiar pliku, liczba elementów, głębokość modelu, rozmiar tekstur oraz zakazane ścieżki.
- [ ] Zbudować realny resource-pack ZIP z poprawnym manifestem i hashami.
- [ ] Dodać item predicate/custom model mapping wyłącznie po przetestowaniu na kliencie.
- [ ] Dostarczyć pack graczowi i obsłużyć odrzucenie, reconnect oraz awarię.
- [ ] Dodać testy z dobrymi, uszkodzonymi i złośliwymi plikami.

**Kryterium ukończenia:** twórca importuje model, wskazuje go w YAML, odpala animację i widzi go na prawdziwym serwerze bez ręcznego składania resource packa.

### M3 — referencyjne eventy jako produkt

**Cel:** gotowe eventy są przykładami jakości, a nie specjalnymi wyjątkami.

- [ ] Doprowadzić Meteor, Airdrop i Metin do pełnego standardu M1/M2.
- [ ] Dodać Pinata jako test hit-counter/interakcji oraz jeden event z mob waves lub boss portalem.
- [ ] Każdy oficjalny event ma: definicję YAML, assety, dokumentację, testy i workflow restart/recovery.
- [ ] Zero osobnych „managerów Meteoru” lub hardcoded coordinators.
- [ ] Każdy brakujący mechanizm najpierw ocenić jako potencjalny wspólny primitive.

### M4 — obsługa produkcyjna i kompatybilność

**Cel:** operator serwera wie, co jest bezpieczne, co nie działa i jak to naprawić.

- [ ] Domknąć real-server matrix Paper, Purpur, Folia, Spigot i CraftBukkit dla każdej deklarowanej wersji.
- [ ] Nie publikować wydania, gdy wymagany wiersz macierzy nie przeszedł; zewnętrzną niedostępność dokumentować jawnie.
- [ ] Rozszerzyć `/event doctor`, inspect i logi o instrukcje naprawcze.
- [ ] Udokumentować monitoring, backup SQLite, recovery oraz ręczne uzgadnianie claimów.
- [ ] Wprowadzić release checklist: JAR, checksum, test runtime, upgrade, rollback i dokumentacja.

### M5 — rozszerzalność i GUI po stabilizacji

**Cel:** rozszerzać silnik bez łamania użytkowników.

- [ ] Zidentyfikować prawdziwy use case dla publicznego API.
- [ ] Zaprojektować wersjonowane API dla custom `Action`, `Trigger`, `Condition` i integracji.
- [ ] Dodać compatibility policy oraz testowy addon.
- [ ] Zbudować GUI jako edytor/preview YAML, nie jako alternatywne źródło konfiguracji.

## 8. Definition of Done — zasady bez wyjątków

### Nowy primitive silnika

- [ ] Ma uzasadniony, wspólny przypadek użycia dla więcej niż jednego eventu.
- [ ] Jest w core/application, nie w listenerze platformowym.
- [ ] Ma testy jednostkowe i architektoniczne, gdy dotyczy granic modułów.
- [ ] Jest udokumentowany dla autora YAML.
- [ ] Jest użyty lub sprawdzony przez co najmniej jeden realny event.

### Nowy event

- [ ] Jest definicją i assetami, a nie osobnym subsystemem bez uzasadnienia.
- [ ] Przechodzi validate, reload, start, runtime, cleanup i recovery.
- [ ] Ma zachowanie przy błędnym modelu, brakującej integracji i anulowaniu.
- [ ] Ma dokumentację użytkownika oraz przykładowe ustawienia.
- [ ] Działa w wymaganej macierzy platform.

### Nowy importer lub delivery assetów

- [ ] Nie pozwala na zip-slip, path traversal ani nieograniczone zużycie pamięci/dysku.
- [ ] Weryfikuje format, rozmiar, liczbę plików oraz zawartość.
- [ ] Ma pozytywne i negatywne testy na prawdziwych plikach.
- [ ] Nie twierdzi, że asset jest gotowy, dopóki ZIP i klient nie zostały zweryfikowane.

### Wydanie

- [ ] `./gradlew clean check build` przechodzi.
- [ ] Finalne JAR-y są sprawdzone i mają checksumy.
- [ ] Real-server matrix przechodzi dla deklarowanego zakresu.
- [ ] README, dokumentacja, compatibility table i changelog mówią prawdę.
- [ ] Znane ograniczenia są jawne, w szczególności external-side-effect delivery i brak upstream buildów.

## 9. Czego świadomie nie budujemy teraz

- osobnego pluginu dla każdego eventu;
- NMS, reflection hacks oraz globalnych singletonów;
- pełnego systemu mobów, ekonomii, questów lub skryptów;
- publicznego API bez kontraktu kompatybilności;
- GUI przed stabilizacją formatu YAML;
- resource-pack delivery przed zweryfikowanym ZIP-em;
- deklaracji wsparcia platformy, której nie sprawdziliśmy na prawdziwym serwerze.

## 10. Jak aktualizować ten dokument

Przy każdej znaczącej zmianie:

1. Przed implementacją zaznacz właściwy punkt M0–M5 jako „w toku”.
2. Po implementacji uzupełnij tabelę w rozdziale 5, bez zmieniania statusu na „zrobione” tylko dlatego, że kod się kompiluje.
3. Dopisz dowód: test, workflow serwera, komendę lub ADR.
4. Gdy decyzja zmienia architekturę, dodaj ADR, a potem zsynchronizuj `docs/ai/ARCHITECTURE.md` i `docs/ai/PROJECT_STATE.md`.
5. Po ukończeniu milestone'u zapisz datę, zakres, wynik pełnej bramki i pozostające ograniczenia.

### Dziennik weryfikacji

| Data | Zakres | Dowód | Wynik | Następny krok |
| --- | --- | --- | --- | --- |
| 2026-09-20 | Dependency verification | `./gradlew --write-verification-metadata sha256 :platforms:paper:common:pmdMain` | **PASS** — SHA-256 dodane dla brakujących artefaktów; strict verification pozostaje włączone | Utrzymywać metadata przy kolejnych zmianach zależności |
| 2026-09-20 | Naprawy jakości po pełnej bramce | `./gradlew :platforms:paper:common:check spotlessJavaCheck` | **PASS** — PMD, SpotBugs, Checkstyle i formatowanie przeszły po poprawkach | Uruchomić pełną bramkę na aktualnym checkoutcie |
| 2026-09-20 | Pełna bramka aktualnego checkoutu | `./gradlew clean check build` | **PASS** — testy, format, analiza statyczna, granice platform i oba artefakty przeszły; 124 zadania actionable | M0: bezpieczne checkpointy oraz real-server matrix |
| 2026-09-20 | Paper 26.2 runtime smoke | `./gradlew :distributions:paper:build && python3 scripts/runtime-smoke/runtime_workflow.py --server paper --version 26.2 --artifact distributions/paper/build/libs/SpectraEvents-*-paper.jar` | **PASS** — start, przejście fazy, cleanup, restart/recovery, ponowny cleanup i clean shutdown | Powtórzyć dla pozostałych dostępnych wierszy macierzy |
| 2026-09-20 | Spigot 26.2 runtime smoke | `./gradlew :distributions:spigot:build && python3 scripts/runtime-smoke/runtime_workflow.py --server spigot --version 26.2 --artifact distributions/spigot/build/libs/SpectraEvents-*-spigot.jar` | **PASS** — start, przejście fazy, cleanup, restart/recovery, ponowny cleanup i clean shutdown | Powtórzyć dla pozostałych dostępnych wierszy macierzy |
| 2026-09-20 | Poprawka runtime smoke i recovery | `./gradlew clean check build` po poprawkach `READY`, shutdown writer'a, workflow stop oraz kolejności ładowania Paper | **PASS** — 124 zadania actionable; Paper 26.2 przechodzi również restart/recovery bez błędu rejestracji definicji | Zachować jako punkt odniesienia przed kolejnym checkpointem |
| 2026-09-20 | Synchronizacja dokumentacji | `./gradlew spotlessMarkdownCheck` oraz `git diff --check` | **PASS** — plan, roadmapa i status projektu opisują ten sam kierunek; brak błędów whitespace | M0: zamknąć checkpointy bez naruszania cudzych zmian |
| 2026-09-20 | Lokalne checkpointy M0 | `git log --oneline -8` oraz `git status --short` | **PASS** — osiem spójnych commitów zapisanych lokalnie, worktree czysty, bez pushu | Dokończyć pozostałe dostępne wiersze real-server matrix |
| 2026-09-20 | Paper-family runtime matrix | sekwencyjne uruchomienie `scripts/runtime-smoke/runtime_workflow.py` dla Paper 26.1–26.3, Purpur 26.1–26.3 i Folia 26.1–26.2 | **PASS** — 8/8 dostępnych wierszy: start, faza, cleanup, restart/recovery i clean shutdown; Folia 26.3 bez dostępnego buildu upstream | Przejść do Spigot/CraftBukkit 26.1–26.3 |
| 2026-09-20 | Spigot-family runtime matrix | sekwencyjne uruchomienie `scripts/runtime-smoke/runtime_workflow.py` dla Spigot 26.1–26.3 i CraftBukkit 26.1–26.3 | **PASS** — 6/6 dostępnych wierszy: start, faza, cleanup, restart/recovery i clean shutdown | M0 zielone dla dostępnej macierzy; rozpocząć M1 |
| 2026-09-20 | M1 diagnostic contract | targeted parser/loader tests oraz `./gradlew clean check build` | **PASS** — canonical YAML paths, source-file prefixes, wrong-type diagnostics and bounded parameter validation; 124 zadania actionable | Stabilizować pełny author-facing YAML schema |

---

## Zasada końcowa

Najlepszy SpectraEvents nie będzie pluginem z największą liczbą luźnych funkcji. Będzie silnikiem, w którym administrator bezpiecznie przechodzi drogę **Blockbench → asset → YAML → test → działający event**, a programista może dodać nowe możliwości bez łamania architektury, konfiguracji i istniejących serwerów.
