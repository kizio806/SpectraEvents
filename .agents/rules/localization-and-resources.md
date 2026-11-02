# Localization, Resources, and Text Formatting

## Purpose
SpectraEvents provides internationalized messages and user feedback across supported languages with MiniMessage and legacy chat adapters.

## Standard
- **Supported Locales:** Default locale catalogs are maintained under `spectraevents-application/src/main/resources/locales/` (`en-US.yml`, `pl-PL.yml`, `de-DE.yml`, `es-ES.yml`, `fr-FR.yml`, `pt-BR.yml`).
- **Placeholder Resolution:** Dynamic placeholders (`<event_name>`, `<phase>`, `<health>`, `<progress>`) must resolve consistently across both Paper (MiniMessage) and Spigot (legacy ChatColor/BungeeCord).
- **Template Fallback:** Missing localization keys must fall back gracefully to default templates without crashing or throwing null pointer exceptions.

## Verification:
```bash
./gradlew :spectraevents-application:test
```
