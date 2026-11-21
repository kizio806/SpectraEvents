# Architecture Context — SpectraEvents

## Architectural Pattern
SpectraEvents is engineered according to **Hexagonal Architecture** (Ports and Adapters).

```mermaid
flowchart TD
    subgraph Core ["spectraevents-core (Pure Java Domain)"]
        EventModels["EventDefinition, EventInstance, EventPhase"]
        Components["HitboxComponent, DisplayEntityComponent, SoundComponent"]
        StateMachine["EventStateMachine, PhaseTransition, Trigger, Condition"]
    end

    subgraph Application ["spectraevents-application (Use Cases & Ports)"]
        Services["EventLifecycleService, ScheduleService, RewardMailboxService"]
        Ports["EventRepositoryPort, DisplayRenderPort, SchedulerPort, AssetDeliveryPort"]
        Parsers["YamlDefinitionParser, LocaleLoader"]
    end

    subgraph Adapters ["adapters/"]
        Storage["adapters:storage-sqlite (SQLite & Mailbox Recovery)"]
        Blockbench["adapters:assets-blockbench (Model Parser)"]
        Delivery["adapters:assets-delivery (Modrinth CDN & Checksum)"]
        Update["adapters:update-http (Version Checker)"]
    end

    subgraph Platforms ["platforms/"]
        BukkitPlatform["platforms:bukkit (Shared Listeners, Hitbox Interaction)"]
        PaperPlatform["platforms:paper:common (Brigadier Commands, Folia Regional Scheduler)"]
        SpigotPlatform["platforms:spigot:common (Bukkit Commands, Bukkit Scheduler)"]
    end

    subgraph Distributions ["distributions/"]
        DistPaper["distributions:paper (SpectraEvents-<version>-paper.jar)"]
        DistSpigot["distributions:spigot (SpectraEvents-<version>-spigot.jar)"]
    end

    Platforms --> Application
    Application --> Core
    Application --> Ports
    Adapters -. implements .-> Ports
    DistPaper --> PaperPlatform
    DistPaper --> BukkitPlatform
    DistPaper --> Adapters
    DistSpigot --> SpigotPlatform
    DistSpigot --> BukkitPlatform
    DistSpigot --> Adapters
```

## Module Isolation Invariants
1. `spectraevents-core`: Pure domain logic (Java 21), zero Bukkit or Minecraft dependencies.
2. `spectraevents-application`: Use cases, schedulers, parsers, and outbound SPI ports (Java 21).
3. `adapters/`: Infrastructure implementations (SQLite, Blockbench, Modrinth CDN).
4. `platforms/`: Server platform bridges for Paper and Spigot.
5. `distributions/`: Packaged shaded release JARs with relocation and manifest configuration.
