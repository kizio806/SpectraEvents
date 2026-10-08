package io.github.kizio806.spectraevents.application.config.compiler;

import io.github.kizio806.spectraevents.application.config.DurationText;
import io.github.kizio806.spectraevents.application.config.compiled.ConfiguredConditionDefinition;
import io.github.kizio806.spectraevents.application.config.compiled.ConfiguredTriggerDefinition;
import io.github.kizio806.spectraevents.application.config.spec.ActionSpec;
import io.github.kizio806.spectraevents.application.config.spec.ConditionSpec;
import io.github.kizio806.spectraevents.application.config.spec.EventParameterSpec;
import io.github.kizio806.spectraevents.application.config.spec.EventParameterType;
import io.github.kizio806.spectraevents.application.config.spec.EventSpec;
import io.github.kizio806.spectraevents.application.config.spec.PhaseSpec;
import io.github.kizio806.spectraevents.application.config.spec.TransitionSpec;
import io.github.kizio806.spectraevents.application.config.spec.TriggerSpec;
import io.github.kizio806.spectraevents.application.config.validation.ValidationDiagnostic;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinition;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.definition.EventEncounterSettings;
import io.github.kizio806.spectraevents.core.event.execution.TransitionRule;
import io.github.kizio806.spectraevents.core.event.execution.action.ActionDefinition;
import io.github.kizio806.spectraevents.core.event.execution.action.CoreActions;
import io.github.kizio806.spectraevents.core.event.execution.action.IntegrationActions;
import io.github.kizio806.spectraevents.core.event.execution.action.PlatformActions;
import io.github.kizio806.spectraevents.core.event.execution.condition.ConditionDefinition;
import io.github.kizio806.spectraevents.core.event.execution.condition.CoreConditions;
import io.github.kizio806.spectraevents.core.event.execution.condition.IntegrationConditions;
import io.github.kizio806.spectraevents.core.event.execution.trigger.CoreTriggers;
import io.github.kizio806.spectraevents.core.event.execution.trigger.TriggerDefinition;
import io.github.kizio806.spectraevents.core.event.phase.PhaseDefinition;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Compiles a raw {@link EventSpec} into a validated, immutable {@link EventDefinition}. Rejects
 * compilation entirely if any errors are encountered.
 */
public class EventDefinitionCompiler {
  private static final Pattern PARAMETER_REFERENCE = Pattern.compile("\\$\\{([a-z][a-z0-9-]*)}");

  /**
   * Compiles an EventSpec.
   *
   * @param spec raw authoring model
   * @return a compiled EventDefinition
   * @throws EventDefinitionCompilerException if validation errors occur
   */
  public EventDefinition compile(EventSpec spec) {
    return compile(spec, Map.of());
  }

  /**
   * Compiles a definition after validating and applying YAML-declared scalar overrides. Complex
   * YAML structures intentionally cannot be overridden by an operator settings file.
   */
  public EventDefinition compile(EventSpec spec, Map<String, Object> overrides) {
    try {
      return compileResolved(spec, overrides);
    } catch (EventDefinitionCompilerException exception) {
      throw exception;
    } catch (IllegalArgumentException exception) {
      throw new EventDefinitionCompilerException(
          "Failed to compile EventDefinition",
          List.of(
              new ValidationDiagnostic(
                  ValidationDiagnostic.Severity.ERROR,
                  "SE-DEF-VALUE-001",
                  "definition",
                  exception.getMessage())));
    }
  }

  private EventDefinition compileResolved(EventSpec spec, Map<String, Object> overrides) {
    spec = resolveParameters(spec, overrides == null ? Map.of() : overrides);
    List<ValidationDiagnostic> diagnostics = new ArrayList<>();

    if (spec.id() == null || spec.id().isBlank()) {
      diagnostics.add(
          new ValidationDiagnostic(
              ValidationDiagnostic.Severity.ERROR, "SE-DEF-001", "id", "Event ID cannot be blank"));
    }

    if (spec.initialPhase() == null || spec.initialPhase().isBlank()) {
      diagnostics.add(
          new ValidationDiagnostic(
              ValidationDiagnostic.Severity.ERROR,
              "SE-DEF-002",
              "initial-phase",
              "Initial phase cannot be blank"));
    } else if (spec.phases() != null && !spec.phases().containsKey(spec.initialPhase())) {
      diagnostics.add(
          new ValidationDiagnostic(
              ValidationDiagnostic.Severity.ERROR,
              "SE-DEF-003",
              "initial-phase",
              "Initial phase must exist in phases map: " + spec.initialPhase()));
    }

    if (spec.phases() == null || spec.phases().isEmpty()) {
      diagnostics.add(
          new ValidationDiagnostic(
              ValidationDiagnostic.Severity.ERROR,
              "SE-DEF-004",
              "phases",
              "At least one phase must be defined"));
    }

    EventEncounterSettings encounterSettings =
        compileEncounterSettings(spec.encounter(), spec.limits(), diagnostics);
    if (!diagnostics.isEmpty()) {
      throw new EventDefinitionCompilerException("Failed to compile EventDefinition", diagnostics);
    }

    Map<PhaseId, PhaseDefinition> compiledPhases = new HashMap<>();

    for (Map.Entry<String, PhaseSpec> entry : spec.phases().entrySet()) {
      String phaseName = entry.getKey();
      PhaseSpec phaseSpec = entry.getValue();
      PhaseId phaseId = new PhaseId(phaseName);

      Set<PhaseId> allowedTransitions = Set.of();
      if (phaseSpec.allowedTransitions() != null) {
        allowedTransitions =
            phaseSpec.allowedTransitions().stream().map(PhaseId::new).collect(Collectors.toSet());
        // Validate target exists
        for (String target : phaseSpec.allowedTransitions()) {
          if (!spec.phases().containsKey(target)) {
            diagnostics.add(
                new ValidationDiagnostic(
                    ValidationDiagnostic.Severity.ERROR,
                    "SE-DEF-005",
                    "phases." + phaseName + ".transitions",
                    "Transition target phase does not exist: " + target));
          }
        }
      }

      List<ActionDefinition> onEnter = new ArrayList<>();
      if (phaseSpec.onEnter() != null) {
        onEnter =
            phaseSpec.onEnter().stream().map(this::compileAction).collect(Collectors.toList());
      }

      List<TransitionRule> rules = new ArrayList<>();
      if (phaseSpec.transitions() != null) {
        for (int i = 0; i < phaseSpec.transitions().size(); i++) {
          var tSpec = phaseSpec.transitions().get(i);
          String path = "phases." + phaseName + ".transitions[" + i + "]";

          if (tSpec.trigger() == null) {
            diagnostics.add(
                new ValidationDiagnostic(
                    ValidationDiagnostic.Severity.ERROR,
                    "SE-DEF-006",
                    path + ".trigger",
                    "Trigger is required"));
            continue;
          }

          TriggerDefinition triggerDef = compileTrigger(tSpec.trigger());

          List<ConditionDefinition> conditions = new ArrayList<>();
          if (tSpec.conditions() != null) {
            conditions =
                tSpec.conditions().stream()
                    .map(this::compileCondition)
                    .collect(Collectors.toList());
          }

          List<ActionDefinition> actions = new ArrayList<>();
          if (tSpec.actions() != null) {
            actions =
                tSpec.actions().stream().map(this::compileAction).collect(Collectors.toList());
          }

          Optional<PhaseId> targetPhase = Optional.empty();
          if (tSpec.targetPhase() != null && !tSpec.targetPhase().isBlank()) {
            if (!spec.phases().containsKey(tSpec.targetPhase())) {
              diagnostics.add(
                  new ValidationDiagnostic(
                      ValidationDiagnostic.Severity.ERROR,
                      "SE-DEF-007",
                      path + ".target",
                      "Transition target phase does not exist: " + tSpec.targetPhase()));
            }
            targetPhase = Optional.of(new PhaseId(tSpec.targetPhase()));
          }

          rules.add(new TransitionRule(triggerDef, conditions, targetPhase, actions));
        }
      }

      compiledPhases.put(phaseId, new PhaseDefinition(phaseId, allowedTransitions, rules, onEnter));
    }

    if (!diagnostics.isEmpty()) {
      throw new EventDefinitionCompilerException("Failed to compile EventDefinition", diagnostics);
    }

    return new EventDefinition(
        new EventDefinitionId(spec.id()),
        new PhaseId(spec.initialPhase()),
        compiledPhases,
        encounterSettings);
  }

  private EventSpec resolveParameters(EventSpec source, Map<String, Object> overrides) {
    Map<String, Object> values = new LinkedHashMap<>();
    Map<String, EventParameterSpec> declarations =
        source.parameters() == null ? Map.of() : source.parameters();
    for (EventParameterSpec declaration : declarations.values()) {
      values.put(declaration.name(), validateParameter(declaration, declaration.defaultValue()));
    }
    for (Map.Entry<String, Object> override : overrides.entrySet()) {
      EventParameterSpec declaration = declarations.get(override.getKey());
      if (declaration == null) {
        throw new IllegalArgumentException(
            "Unknown event parameter override: " + override.getKey());
      }
      values.put(override.getKey(), validateParameter(declaration, override.getValue()));
    }

    Map<String, PhaseSpec> phases = new LinkedHashMap<>();
    for (Map.Entry<String, PhaseSpec> entry : source.phases().entrySet()) {
      PhaseSpec phase = entry.getValue();
      List<ActionSpec> onEnter =
          phase.onEnter() == null
              ? List.of()
              : phase.onEnter().stream()
                  .map(
                      action ->
                          new ActionSpec(action.type(), resolveMap(action.parameters(), values)))
                  .toList();
      List<TransitionSpec> transitions =
          phase.transitions() == null
              ? List.of()
              : phase.transitions().stream()
                  .map(
                      transition ->
                          new TransitionSpec(
                              new TriggerSpec(
                                  transition.trigger().type(),
                                  resolveMap(transition.trigger().parameters(), values)),
                              transition.conditions() == null
                                  ? List.of()
                                  : transition.conditions().stream()
                                      .map(
                                          condition ->
                                              new ConditionSpec(
                                                  condition.type(),
                                                  resolveMap(condition.parameters(), values)))
                                      .toList(),
                              transition.targetPhase(),
                              transition.actions() == null
                                  ? List.of()
                                  : transition.actions().stream()
                                      .map(
                                          action ->
                                              new ActionSpec(
                                                  action.type(),
                                                  resolveMap(action.parameters(), values)))
                                      .toList()))
                  .toList();
      phases.put(entry.getKey(), new PhaseSpec(phase.allowedTransitions(), transitions, onEnter));
    }
    return new EventSpec(
        source.id(),
        source.schemaVersion(),
        source.initialPhase(),
        phases,
        resolveMap(source.encounter(), values),
        resolveMap(source.limits(), values),
        declarations);
  }

  private Map<String, Object> resolveMap(Map<String, Object> source, Map<String, Object> values) {
    if (source == null || source.isEmpty()) {
      return Map.of();
    }
    Map<String, Object> resolved = new LinkedHashMap<>();
    source.forEach((key, value) -> resolved.put(key, resolveValue(value, values)));
    return resolved;
  }

  @SuppressWarnings("unchecked")
  private Object resolveValue(Object value, Map<String, Object> values) {
    if (value instanceof String text) {
      Matcher matcher = PARAMETER_REFERENCE.matcher(text);
      if (!matcher.find()) {
        return text;
      }
      if (matcher.start() == 0 && matcher.end() == text.length()) {
        String name = matcher.group(1);
        if (!values.containsKey(name)) {
          throw new IllegalArgumentException("YAML references undeclared event parameter: " + name);
        }
        return values.get(name);
      }
      StringBuffer resolved = new StringBuffer();
      do {
        String name = matcher.group(1);
        if (!values.containsKey(name)) {
          throw new IllegalArgumentException("YAML references undeclared event parameter: " + name);
        }
        matcher.appendReplacement(
            resolved, Matcher.quoteReplacement(String.valueOf(values.get(name))));
      } while (matcher.find());
      matcher.appendTail(resolved);
      return resolved.toString();
    }
    if (value instanceof Map<?, ?> map) {
      Map<String, Object> nested = new LinkedHashMap<>();
      map.forEach(
          (key, nestedValue) -> nested.put(String.valueOf(key), resolveValue(nestedValue, values)));
      return nested;
    }
    if (value instanceof List<?> list) {
      return list.stream().map(item -> resolveValue(item, values)).toList();
    }
    return value;
  }

  private Object validateParameter(EventParameterSpec declaration, Object raw) {
    try {
      Object typed =
          switch (declaration.type()) {
            case INTEGER -> Integer.valueOf(String.valueOf(raw));
            case DECIMAL -> Double.valueOf(String.valueOf(raw));
            case DURATION -> {
              Duration duration = parseDuration(raw);
              if (duration.isNegative() || duration.isZero()) {
                throw new IllegalArgumentException("duration must be positive");
              }
              yield formatDuration(duration);
            }
            case BOOLEAN -> {
              String value = String.valueOf(raw);
              if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                throw new IllegalArgumentException("boolean must be true or false");
              }
              yield Boolean.valueOf(value);
            }
          };
      if (declaration.minimum() != null) {
        java.math.BigDecimal numeric = numericValue(declaration.type(), typed);
        if (numeric.compareTo(declaration.minimum()) < 0
            || numeric.compareTo(declaration.maximum()) > 0) {
          throw new IllegalArgumentException(
              "must be between " + declaration.minimum() + " and " + declaration.maximum());
        }
      }
      return typed;
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException(
          "Invalid value for parameter '" + declaration.name() + "': " + exception.getMessage(),
          exception);
    }
  }

  private java.math.BigDecimal numericValue(EventParameterType type, Object value) {
    if (type == EventParameterType.DURATION) {
      return java.math.BigDecimal.valueOf(parseDuration(value).toSeconds());
    }
    return new java.math.BigDecimal(String.valueOf(value));
  }

  private String formatDuration(Duration duration) {
    if (duration.toMillis() % 3_600_000L == 0) return duration.toHours() + "h";
    if (duration.toMillis() % 60_000L == 0) return duration.toMinutes() + "m";
    if (duration.toMillis() % 1_000L == 0) return duration.toSeconds() + "s";
    return duration.toMillis() + "ms";
  }

  private EventEncounterSettings compileEncounterSettings(
      Map<String, Object> encounter,
      Map<String, Object> limits,
      List<ValidationDiagnostic> diagnostics) {
    if ((encounter == null || encounter.isEmpty()) && (limits == null || limits.isEmpty())) {
      return EventEncounterSettings.NONE;
    }
    Set<String> supported = Set.of("zone-radius", "deadline", "minimum-contribution");
    for (String key : encounter.keySet()) {
      if (!supported.contains(key)) {
        diagnostics.add(
            new ValidationDiagnostic(
                ValidationDiagnostic.Severity.ERROR,
                "SE-DEF-ENC-001",
                "encounter." + key,
                "Unknown encounter setting: " + key));
      }
    }
    Set<String> supportedLimits = Set.of("max-active-per-world");
    for (String key : limits.keySet()) {
      if (!supportedLimits.contains(key)) {
        diagnostics.add(
            new ValidationDiagnostic(
                ValidationDiagnostic.Severity.ERROR,
                "SE-DEF-LIMIT-001",
                "limits." + key,
                "Unknown event limit: " + key));
      }
    }
    double radius = getDouble(encounter, "zone-radius", 0.0d);
    int minimumContribution = getInt(encounter, "minimum-contribution", 0);
    int maxActivePerWorld = getInt(limits, "max-active-per-world", radius > 0.0d ? 1 : 0);
    Duration deadline =
        encounter.containsKey("deadline")
            ? parseDuration(encounter.get("deadline"))
            : Duration.ZERO;
    try {
      return new EventEncounterSettings(radius, deadline, minimumContribution, maxActivePerWorld);
    } catch (IllegalArgumentException exception) {
      diagnostics.add(
          new ValidationDiagnostic(
              ValidationDiagnostic.Severity.ERROR,
              "SE-DEF-ENC-002",
              "encounter",
              exception.getMessage()));
      return EventEncounterSettings.NONE;
    }
  }

  private ActionDefinition compileAction(
      io.github.kizio806.spectraevents.application.config.spec.ActionSpec spec) {
    String type = spec.type().toLowerCase(java.util.Locale.ROOT);
    Map<String, Object> params = spec.parameters() != null ? spec.parameters() : Map.of();
    switch (type) {
      case "initialize_health" -> {
        return new CoreActions.InitializeHealthAction(getInt(params, "max", 20));
      }
      case "initialize_hit_counter" -> {
        return new CoreActions.InitializeHitCounterAction(getInt(params, "max", 20));
      }
      case "set_locked" -> {
        return new CoreActions.SetLockedAction(parseDuration(params.get("duration")));
      }
      case "try_claim" -> {
        return new CoreActions.TryClaimAction();
      }
      case "initialize_shared_loot" -> {
        List<CoreActions.LootStack> items = new ArrayList<>();
        if (params.get("items") instanceof List<?> rawItems) {
          for (Object raw : rawItems) {
            if (raw instanceof Map<?, ?> item) {
              Map<String, Object> values = new HashMap<>();
              item.forEach((key, value) -> values.put(String.valueOf(key), value));
              items.add(
                  new CoreActions.LootStack(
                      String.valueOf(values.getOrDefault("material", "minecraft:diamond")),
                      getInt(values, "amount", 1)));
            }
          }
        }
        return new CoreActions.InitializeSharedLootAction(items);
      }
      case "initialize_ground_loot" -> {
        List<CoreActions.GroundLootEntry> entries = new ArrayList<>();
        if (params.get("items") instanceof List<?> rawItems) {
          for (Object raw : rawItems) {
            if (raw instanceof Map<?, ?> item) {
              Map<String, Object> values = new HashMap<>();
              item.forEach((key, value) -> values.put(String.valueOf(key), value));
              entries.add(
                  new CoreActions.GroundLootEntry(
                      String.valueOf(values.getOrDefault("material", "minecraft:diamond")),
                      getInt(values, "amount", 1),
                      getInt(values, "chance", 100)));
            }
          }
        }
        return new CoreActions.InitializeGroundLootAction(entries);
      }
      case "create_participant_reward_claims" -> {
        List<CoreActions.GroundLootEntry> entries = new ArrayList<>();
        if (params.get("items") instanceof List<?> rawItems) {
          for (Object raw : rawItems) {
            if (raw instanceof Map<?, ?> item) {
              Map<String, Object> values = new HashMap<>();
              item.forEach((key, value) -> values.put(String.valueOf(key), value));
              entries.add(
                  new CoreActions.GroundLootEntry(
                      String.valueOf(values.getOrDefault("material", "minecraft:diamond")),
                      getInt(values, "amount", 1),
                      getInt(values, "chance", 100)));
            }
          }
        }
        return new CoreActions.CreateParticipantRewardClaimsAction(entries);
      }
      case "apply_damage" -> {
        return new CoreActions.ApplyDamageAction(getInt(params, "amount", 1));
      }
      case "apply_combat_damage" -> {
        return new CoreActions.ApplyCombatDamageAction(
            getInt(params, "maximum", getInt(params, "max-damage", 1)),
            getInt(params, "health-floor-percent", 0),
            parseDuration(params.getOrDefault("cooldown", "0s")));
      }
      case "increment_hits" -> {
        return new CoreActions.IncrementHitsAction(getInt(params, "amount", 1));
      }
      case "complete_event" -> {
        return new CoreActions.CompleteEventAction();
      }
      case "cancel_event" -> {
        return new CoreActions.CancelEventAction();
      }
      case "fail_event" -> {
        return new CoreActions.FailEventAction();
      }
      case "spawn_model" -> {
        return new PlatformActions.SpawnModelAction(
            requiredString(params, "model"),
            getInt(params, "height-offset", getInt(params, "height_offset", 0)));
      }
      case "move_model" -> {
        return new PlatformActions.MoveModelAction();
      }
      case "remove_model" -> {
        return new PlatformActions.RemoveModelAction();
      }
      case "play_animation", "play-animation" -> {
        return new PlatformActions.PlayAnimationAction(requiredString(params, "animation"));
      }
      case "play_sound" -> {
        return new PlatformActions.PlaySoundAction(
            requiredString(params, "sound"),
            getFloat(params, "volume", 1.0f),
            getFloat(params, "pitch", 1.0f));
      }
      case "spawn_particles" -> {
        return new PlatformActions.SpawnParticlesAction(
            requiredString(params, "particle"), getInt(params, "count", 10));
      }
      case "give_item" -> {
        return new PlatformActions.GiveItemAction(
            requiredString(params, "material"), getInt(params, "amount", 1));
      }
      case "open_shared_loot" -> {
        return new PlatformActions.OpenSharedLootAction(
            String.valueOf(params.getOrDefault("title", "Airdrop Supplies")));
      }
      case "drop_loot" -> {
        double radius = getFloat(params, "radius", 2.0f);
        List<PlatformActions.LootItem> items = new ArrayList<>();
        if (params.get("items") instanceof List<?> list) {
          for (Object obj : list) {
            if (obj instanceof Map<?, ?> m) {
              @SuppressWarnings("unchecked")
              Map<String, Object> map = (Map<String, Object>) m;
              items.add(
                  new PlatformActions.LootItem(
                      String.valueOf(map.getOrDefault("material", "minecraft:diamond")),
                      getInt(map, "amount", 1),
                      getInt(map, "chance", 100)));
            }
          }
        }
        return new PlatformActions.DropLootAction(radius, items);
      }
      case "release_ground_loot" -> {
        return new PlatformActions.ReleaseGroundLootAction(getFloat(params, "radius", 2.0f));
      }
      case "award_podium" -> {
        List<PlatformActions.PodiumPool> pools = new ArrayList<>();
        if (params.get("pools") instanceof List<?> rawPools) {
          for (Object rawPool : rawPools) {
            List<PlatformActions.LootItem> items = new ArrayList<>();
            if (rawPool instanceof Map<?, ?> pool
                && pool.get("items") instanceof List<?> rawItems) {
              for (Object rawItem : rawItems) {
                if (rawItem instanceof Map<?, ?> item) {
                  @SuppressWarnings("unchecked")
                  Map<String, Object> values = (Map<String, Object>) item;
                  items.add(
                      new PlatformActions.LootItem(
                          String.valueOf(values.getOrDefault("material", "minecraft:diamond")),
                          getInt(values, "amount", 1),
                          getInt(values, "chance", 100)));
                }
              }
            }
            pools.add(new PlatformActions.PodiumPool(items));
          }
        }
        return new PlatformActions.AwardPodiumAction(pools);
      }
      case "send_message" -> {
        return new PlatformActions.SendMessageAction(requiredString(params, "message"));
      }
      case "broadcast_message", "broadcast" -> {
        return new PlatformActions.BroadcastMessageAction(requiredString(params, "message"));
      }
      case "show_title" -> {
        return new PlatformActions.ShowTitleAction(
            String.valueOf(params.getOrDefault("title", "")),
            String.valueOf(params.getOrDefault("subtitle", "")),
            getInt(params, "fade-in", 10),
            getInt(params, "stay", 50),
            getInt(params, "fade-out", 10));
      }
      case "spawn_boss", "spawn_entity" -> {
        return new PlatformActions.SpawnBossAction(
            getInt(params, "offset-x", 2),
            getInt(params, "offset-y", 0),
            getInt(params, "offset-z", 0),
            requiredString(params, "entity_type"),
            requiredString(params, "name"));
      }
      case "spawn_mobs", "spawn_wave" -> {
        List<PlatformActions.MobSpawn> mobs = new ArrayList<>();
        if (params.get("mobs") instanceof List<?> list) {
          for (Object obj : list) {
            if (obj instanceof Map<?, ?> m) {
              @SuppressWarnings("unchecked")
              Map<String, Object> map = (Map<String, Object>) m;
              mobs.add(
                  new PlatformActions.MobSpawn(
                      String.valueOf(map.getOrDefault("entity_type", "minecraft:zombie")),
                      String.valueOf(map.getOrDefault("name", "<red>Mob")),
                      getInt(map, "amount", 1),
                      getFloat(map, "radius", 3.0f)));
            }
          }
        }
        return new PlatformActions.SpawnMobsAction(
            String.valueOf(params.getOrDefault("wave-id", params.getOrDefault("wave_id", ""))),
            mobs);
      }
      case "show_bossbar", "create_bossbar" -> {
        return new PlatformActions.ShowBossbarAction(
            String.valueOf(params.getOrDefault("color", "RED")),
            String.valueOf(params.getOrDefault("style", "SOLID")),
            String.valueOf(params.getOrDefault("title", "Boss")),
            String.valueOf(params.getOrDefault("progress", "1.0")));
      }
      case "update_bossbar" -> {
        return new PlatformActions.UpdateBossbarAction(
            String.valueOf(params.getOrDefault("title", "Boss")),
            String.valueOf(params.getOrDefault("progress", "1.0")));
      }
      case "remove_bossbar" -> {
        return new PlatformActions.RemoveBossbarAction();
      }
      case "show_scoreboard", "create_scoreboard" -> {
        List<String> lines = new ArrayList<>();
        if (params.get("lines") instanceof List<?> list) {
          for (Object obj : list) {
            lines.add(String.valueOf(obj));
          }
        }
        return new PlatformActions.ShowScoreboardAction(
            String.valueOf(params.getOrDefault("title", "Event")), lines);
      }
      case "update_scoreboard" -> {
        List<String> lines = new ArrayList<>();
        if (params.get("lines") instanceof List<?> list) {
          for (Object obj : list) {
            lines.add(String.valueOf(obj));
          }
        }
        return new PlatformActions.UpdateScoreboardAction(lines);
      }
      case "remove_scoreboard" -> {
        return new PlatformActions.RemoveScoreboardAction();
      }
      case "give_money" -> {
        return new IntegrationActions.GiveMoneyAction(getDouble(params, "amount", 0.0));
      }
      case "take_money" -> {
        return new IntegrationActions.TakeMoneyAction(getDouble(params, "amount", 0.0));
      }
      default -> {
        throw new IllegalArgumentException("Unknown action type: " + spec.type());
      }
    }
  }

  private TriggerDefinition compileTrigger(
      io.github.kizio806.spectraevents.application.config.spec.TriggerSpec spec) {
    String type = spec.type().toLowerCase(java.util.Locale.ROOT);
    Map<String, Object> params = spec.parameters() != null ? spec.parameters() : Map.of();
    switch (type) {
      case "timer_elapsed" -> {
        return new CoreTriggers.TimerElapsedTrigger(parseDuration(params.get("duration")));
      }
      case "loot_item_taken" -> {
        return new CoreTriggers.LootItemTakenTrigger();
      }
      case "loot_container_emptied" -> {
        return new CoreTriggers.LootContainerEmptiedTrigger();
      }
      case "health_threshold_crossed" -> {
        return new CoreTriggers.HealthThresholdCrossedTrigger(getInt(params, "threshold", -1));
      }
      case "health_percent_threshold_crossed" -> {
        return new CoreTriggers.HealthPercentThresholdCrossedTrigger(getInt(params, "percent", -1));
      }
      case "combat_damage" -> {
        return new CoreTriggers.CombatDamageTrigger();
      }
      case "health_depleted" -> {
        return new CoreTriggers.HealthDepletedTrigger();
      }
      case "wave_cleared" -> {
        String waveId = String.valueOf(params.getOrDefault("wave-id", params.get("wave_id")));
        if (waveId.equals("null") || waveId.isBlank()) {
          throw new IllegalArgumentException("wave_cleared requires wave-id");
        }
        return new CoreTriggers.WaveClearedTrigger(waveId);
      }
      case "hits_reached" -> {
        return new CoreTriggers.HitsReachedTrigger();
      }
      case "hits_percent_threshold_crossed" -> {
        return new CoreTriggers.HitsPercentThresholdCrossedTrigger(getInt(params, "percent", -1));
      }
      default -> {
        return new ConfiguredTriggerDefinition(spec.type());
      }
    }
  }

  private ConditionDefinition compileCondition(
      io.github.kizio806.spectraevents.application.config.spec.ConditionSpec spec) {
    String type = spec.type().toLowerCase(java.util.Locale.ROOT);
    Map<String, Object> params = spec.parameters() != null ? spec.parameters() : Map.of();
    switch (type) {
      case "not_locked" -> {
        return new CoreConditions.NotLockedCondition();
      }
      case "is_locked" -> {
        return new CoreConditions.IsLockedCondition();
      }
      case "has_permission" -> {
        return new IntegrationConditions.HasPermissionCondition(getString(params, "permission"));
      }
      case "has_group" -> {
        return new IntegrationConditions.HasGroupCondition(getString(params, "group"));
      }
      case "in_region" -> {
        return new IntegrationConditions.InRegionCondition(getString(params, "region"));
      }
      default -> {
        return new ConfiguredConditionDefinition(spec.type());
      }
    }
  }

  private int getInt(Map<String, Object> params, String key, int defaultValue) {
    Object val = params.get(key);
    if (val == null) return defaultValue;
    if (val instanceof Number n) {
      try {
        return new java.math.BigDecimal(n.toString()).intValueExact();
      } catch (NumberFormatException | ArithmeticException exception) {
        throw new IllegalArgumentException(key + " must be an integer in range", exception);
      }
    }
    try {
      return Integer.parseInt(String.valueOf(val));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(key + " must be an integer", e);
    }
  }

  private String requiredString(Map<String, Object> params, String key) {
    Object value = params.get(key);
    if (value == null || String.valueOf(value).isBlank()) {
      throw new IllegalArgumentException("action parameter '" + key + "' is required");
    }
    return String.valueOf(value);
  }

  private float getFloat(Map<String, Object> params, String key, float defaultValue) {
    Object val = params.get(key);
    if (val == null) return defaultValue;
    if (val instanceof Number n) {
      float numeric = n.floatValue();
      if (!Float.isFinite(numeric)) {
        throw new IllegalArgumentException(key + " must be a finite number");
      }
      return numeric;
    }
    try {
      float numeric = Float.parseFloat(String.valueOf(val));
      if (!Float.isFinite(numeric)) {
        throw new IllegalArgumentException(key + " must be a finite number");
      }
      return numeric;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(key + " must be a number", e);
    }
  }

  private double getDouble(Map<String, Object> params, String key, double defaultValue) {
    Object value = params.get(key);
    if (value == null) return defaultValue;
    if (value instanceof Number number) {
      double numeric = number.doubleValue();
      if (!Double.isFinite(numeric)) {
        throw new IllegalArgumentException(key + " must be a finite number");
      }
      return numeric;
    }
    try {
      double numeric = Double.parseDouble(String.valueOf(value));
      if (!Double.isFinite(numeric)) {
        throw new IllegalArgumentException(key + " must be a finite number");
      }
      return numeric;
    } catch (NumberFormatException exception) {
      throw new IllegalArgumentException(key + " must be a number", exception);
    }
  }

  private String getString(Map<String, Object> params, String key) {
    Object value = params.get(key);
    if (value == null || String.valueOf(value).isBlank()) {
      throw new IllegalArgumentException(key + " must not be blank");
    }
    return String.valueOf(value);
  }

  private Duration parseDuration(Object obj) {
    if (obj == null) {
      throw new IllegalArgumentException("duration is required and must use ms, s, m, or h");
    }
    try {
      return DurationText.parse(String.valueOf(obj).trim().toLowerCase(java.util.Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException(
          "duration must be an integer followed by ms, s, m, or h", exception);
    }
  }
}
