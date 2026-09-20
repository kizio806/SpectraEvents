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
| Paper/Purpur/Folia | Częściowe: dostępna macierz prawie domknięta | Paper artifact; schedulery regionów i jawne ograniczenie scoreboardu Folii | każda deklarowana wersja ma przejść prawdziwy workflow; Folia 26.3 nadal zależy od wydania upstream |
| Spigot/CraftBukkit | Zaimplementowane, wymagające ponownej bramki | osobny artifact i adapter Spigot | każdy deklarowany wariant ma przejść real-server workflow |
| Import Blockbench | Częściowe / wyłączone | istnieje kod i dokumentacja eksperymentalna importera | wejście Blockbench przechodzi walidację i buduje prawdziwe, używalne assety |
| Resource-pack ZIP | Do zrobienia | nie ma zweryfikowanego produktu ZIP | prawdziwy ZIP, manifest, SHA-1, item model mapping, walidacja i test klienta |
| Dostarczanie packa graczom | Częściowe / wyłączone fail-closed | interfejsy/adapters mogą istnieć, ale funkcja nie jest obiecana | testy delivery, odrzucenia, ponownego wejścia gracza i awarii sieci |
| Oficjalne eventy | Częściowe | Meteor, Airdrop, Metin są referencjami | kolejne eventy powstają głównie z YAML, bez ukrytej logiki dostępnej tylko twórcom |
| Własne eventy administratora | Częściowe | kopiowanie i modyfikowanie YAML jest zamierzonym workflow | pełne przykłady, walidacja, asset import oraz guide od zera |
| Publiczne API dodatków | Do zrobienia świadomie | nie ma stabilnego publicznego API | dopiero po ustaleniu realnego use case'u, wersjonowania, testów i polityki kompatybilności |
| GUI edytora | Do zrobienia po V1 | istnieje administracyjne GUI/diagnostyka, nie pełny editor | GUI jest tylko frontendem stabilnego YAML, nigdy osobnym źródłem prawdy |

## 6. Najważniejszy aktualny blocker jakości

Pełna wymagana bramka:

```bash
./gradlew clean check build
```

została uruchomiona 2026-09-20 na aktualnym checkoutcie i **nie przeszła**. Zablokowała ją Gradle dependency verification: `gradle/verification-metadata.xml` nie zawiera wpisów dla części pobieranych artefaktów (m.in. Adventure, bStats, Flyway i Rhino). Błąd dotknął zadań SpotBugs, PMD, JaCoCo i Spotless.

To nie dowodzi błędu funkcjonalnego w tych modułach, ale oznacza, że aktualnego stanu nie wolno oznaczać jako w pełni zweryfikowanego ani publikować jako release.

### Kolejność naprawy blockera

1. Ustalić źródło i oczekiwane sumy kontrolne każdego nowego artefaktu.
2. Uzupełnić metadata wyłącznie zweryfikowanymi wpisami.
3. Ponownie uruchomić pełną bramkę.
4. Naprawić każdy błąd ujawniony po przejściu dependency verification.
5. Zapisać wynik i datę w tym dokumencie oraz `docs/ai/PROJECT_STATE.md`.

## 7. Plan dojścia do najlepszego pluginu

### M0 — uporządkowanie i zielona baza

**Cel:** dokładnie wiedzieć, co działa na obecnym checkoutcie.

- [x] Naprawić dependency verification bez wyłączania ochrony.
- [ ] Uruchomić `./gradlew clean check build` z wynikiem `BUILD SUCCESSFUL`.
- [ ] Uruchomić testy granic platform i sprawdzić, że core/application nie importują Minecraft API.
- [ ] Przejrzeć duży obecny diff i podzielić go logicznie na spójne zmiany/milestone'y.
- [ ] Ujednolicić `README.md`, roadmapę produktu i `docs/ai/PROJECT_STATE.md` z tym dokumentem.
- [ ] Zapisać dokładne dowody: komenda, data, wynik, wersje JDK/Gradle i wynik testów runtime.

**Nie przechodzimy dalej, dopóki M0 nie jest zielone.**

### M1 — kontrakt autora eventu

**Cel:** osoba bez Javy potrafi stworzyć bezpieczny event YAML.

- [ ] Spisać stabilny schemat definicji eventu: model, animacje, phases, triggers, conditions, actions, rewards, recovery, cleanup.
- [ ] Każdy błąd walidacji wskazuje plik i ścieżkę YAML.
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
| 2026-09-20 | Dependency verification | `./gradlew --write-verification-metadata sha256 :platforms:paper:common:pmdMain` | **PASS** — SHA-256 dodane dla 10 brakujących artefaktów; verification przeszła | Uruchomić pełną bramkę; aktualnie zatrzymuje się na niezależnym PMD `EmptyCatchBlock` w `PaperEntityDeathRouter` |

---

## Zasada końcowa

Najlepszy SpectraEvents nie będzie pluginem z największą liczbą luźnych funkcji. Będzie silnikiem, w którym administrator bezpiecznie przechodzi drogę **Blockbench → asset → YAML → test → działający event**, a programista może dodać nowe możliwości bez łamania architektury, konfiguracji i istniejących serwerów.
