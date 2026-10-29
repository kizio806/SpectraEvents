package io.github.kizio806.spectraevents.application.validation;

import io.github.kizio806.spectraevents.application.asset.AssetPipelineService;
import io.github.kizio806.spectraevents.application.asset.AssetTargetProfile;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackDeliveryCoordinator;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackDeliverySettings;
import io.github.kizio806.spectraevents.application.config.validation.ValidationDiagnostic;
import io.github.kizio806.spectraevents.application.integration.IntegrationRegistry;
import io.github.kizio806.spectraevents.application.integration.IntegrationState;
import io.github.kizio806.spectraevents.application.model.animation.registry.AnimationDefinitionRegistry;
import io.github.kizio806.spectraevents.application.model.registry.ModelDefinitionRegistry;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinition;
import io.github.kizio806.spectraevents.core.event.execution.action.ActionDefinition;
import io.github.kizio806.spectraevents.core.event.execution.action.PlatformActions;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationId;
import io.github.kizio806.spectraevents.core.visual.model.ModelId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** Evaluates whether configured event systems meet baseline production requirements. */
public final class OperationalReadinessValidator {

  public List<ValidationDiagnostic> validateResourcePack(ResourcePackDeliverySettings settings) {
    return validateResourcePack(settings, null);
  }

  public List<ValidationDiagnostic> validateResourcePack(
      ResourcePackDeliverySettings settings, ResourcePackDeliveryCoordinator coordinator) {
    Objects.requireNonNull(settings, "settings");
    if (!settings.enabled()) {
      if (settings.required()) {
        return List.of(
            error(
                "SE-READY-PACK-003",
                "resource-pack.yml",
                "Resource-pack delivery is marked as required, but delivery is disabled."));
      }
      return List.of(
          warning(
              "SE-READY-PACK-001",
              "resource-pack.yml",
              "Resource-pack delivery is disabled; clients cannot receive custom model assets."));
    }
    List<ValidationDiagnostic> diagnostics = new ArrayList<>();
    if (coordinator != null) {
      switch (coordinator.status()) {
        case FAILED -> {
          String msg = coordinator.lastFailureMessage();
          if (settings.required()) {
            diagnostics.add(
                error(
                    "SE-READY-PACK-004",
                    "resource-pack.yml",
                    "Resource-pack delivery resolution failed: " + msg));
          } else {
            diagnostics.add(
                warning(
                    "SE-READY-PACK-006",
                    "resource-pack.yml",
                    "Optional resource-pack delivery resolution failed: " + msg));
          }
        }
        case RESOLVING ->
            diagnostics.add(
                info(
                    "SE-READY-PACK-005",
                    "resource-pack.yml",
                    "Resource-pack delivery resolution is in progress."));
        case READY ->
            diagnostics.add(
                info(
                    "SE-READY-PACK-007",
                    "resource-pack.yml",
                    "Resource-pack delivery is active and verified."));
        case UNRESOLVED -> {
          if (settings.required()) {
            diagnostics.add(
                warning(
                    "SE-READY-PACK-005",
                    "resource-pack.yml",
                    "Required resource-pack delivery has not yet resolved."));
          }
        }
        default -> {}
      }
    }
    diagnostics.add(
        warning(
            "SE-READY-PACK-002",
            "resource-pack.yml",
            "Resource-pack delivery is configured, but real-client acceptance must be recorded before release."));
    return List.copyOf(diagnostics);
  }

  public List<ValidationDiagnostic> validateAssetPipeline(AssetPipelineService pipeline) {
    if (pipeline == null) {
      return List.of(warning("SE-READY-ASSET-001", "assets", "Asset pipeline is not configured."));
    }
    if (pipeline.isBuilding()) {
      return List.of(
          info("SE-READY-ASSET-002", "assets", "Asset pipeline is currently compiling sources."));
    }
    if (pipeline.lastFailure() != null) {
      return List.of(
          error(
              "SE-READY-ASSET-003",
              "assets",
              "Asset pipeline compilation failed: " + pipeline.lastFailure()));
    }
    return List.of();
  }

  public List<ValidationDiagnostic> validateIntegrations(IntegrationRegistry integrations) {
    Objects.requireNonNull(integrations, "integrations");
    long enabled =
        integrations.getAll().values().stream()
            .filter(i -> i.state() == IntegrationState.ACTIVE)
            .count();
    long unavailable = integrations.getAll().size() - enabled;
    List<ValidationDiagnostic> diagnostics = new ArrayList<>();
    diagnostics.add(
        info(
            "SE-READY-INTEGRATION-001",
            "integrations",
            "Optional integrations: " + enabled + " enabled, " + unavailable + " unavailable."));
    for (var info : integrations.getAll().values()) {
      if (info.state() == IntegrationState.FAILED) {
        diagnostics.add(
            warning(
                "SE-READY-INTEGRATION-002",
                "integrations." + info.name().toLowerCase(Locale.ROOT),
                "Optional integration '" + info.name() + "' failed: " + info.details()));
      }
    }
    return List.copyOf(diagnostics);
  }

  public List<ValidationDiagnostic> validateDefinitionReadiness(
      EventDefinition definition,
      ModelDefinitionRegistry modelRegistry,
      AnimationDefinitionRegistry animRegistry,
      AssetPipelineService pipeline,
      ResourcePackDeliverySettings settings,
      ResourcePackDeliveryCoordinator coordinator,
      AssetTargetProfile targetProfile) {
    Objects.requireNonNull(definition, "definition");
    List<ValidationDiagnostic> diagnostics = new ArrayList<>();
    String path = "events." + definition.id().value();

    if (pipeline != null) {
      if (pipeline.isBuilding()) {
        diagnostics.add(
            error(
                "SE-READY-ASSET-002",
                path,
                "Assets are still compiling; event activation is temporarily unavailable."));
      } else if (pipeline.lastFailure() != null) {
        diagnostics.add(
            error(
                "SE-READY-ASSET-003",
                path,
                "Asset pipeline failure blocks event execution: " + pipeline.lastFailure()));
      }
      if (targetProfile != null && pipeline.targetProfile() != targetProfile) {
        diagnostics.add(
            error(
                "SE-READY-PROFILE-001",
                path,
                "Server target profile "
                    + targetProfile
                    + " mismatches pipeline build profile "
                    + pipeline.targetProfile()));
      }
    }

    Set<String> modelRefs = new LinkedHashSet<>();
    Set<String> animRefs = new LinkedHashSet<>();
    for (PhaseId phaseId : definition.phaseIds()) {
      var phaseOpt = definition.phase(phaseId);
      if (phaseOpt.isEmpty()) {
        continue;
      }
      var phase = phaseOpt.get();
      for (ActionDefinition action : phase.onEnterActions()) {
        collectReferences(action, modelRefs, animRefs);
      }
      for (var rule : phase.rules()) {
        for (ActionDefinition action : rule.actions()) {
          collectReferences(action, modelRefs, animRefs);
        }
      }
    }

    if (modelRegistry != null) {
      for (String model : modelRefs) {
        if (!modelRegistry.contains(new ModelId(model))) {
          diagnostics.add(
              error(
                  "SE-READY-MODEL-001",
                  path + ".models." + model,
                  "Configured model '" + model + "' is not registered in the model catalog"));
        }
      }
    }

    if (animRegistry != null && modelRegistry != null) {
      for (String anim : animRefs) {
        boolean found = false;
        for (String model : modelRefs) {
          if (animRegistry.find(new ModelId(model), new AnimationId(anim)).isPresent()) {
            found = true;
            break;
          }
        }
        if (!found && !modelRefs.isEmpty()) {
          diagnostics.add(
              error(
                  "SE-READY-ANIMATION-001",
                  path + ".animations." + anim,
                  "Configured animation '" + anim + "' is not registered for referenced models"));
        }
      }
    }

    if (!modelRefs.isEmpty() && settings != null && settings.required()) {
      if (!settings.enabled()) {
        diagnostics.add(
            error(
                "SE-READY-PACK-003",
                path,
                "Event requires custom model assets, but required resource-pack delivery is disabled."));
      } else if (coordinator != null
          && coordinator.status() == ResourcePackDeliveryCoordinator.Status.FAILED) {
        diagnostics.add(
            error(
                "SE-READY-PACK-004",
                path,
                "Required resource-pack delivery failed: " + coordinator.lastFailureMessage()));
      }
    }

    return List.copyOf(diagnostics);
  }

  public List<ValidationDiagnostic> validate(
      ResourcePackDeliverySettings settings, IntegrationRegistry integrations) {
    return validate(settings, null, integrations);
  }

  public List<ValidationDiagnostic> validate(
      ResourcePackDeliverySettings settings,
      ResourcePackDeliveryCoordinator coordinator,
      IntegrationRegistry integrations) {
    List<ValidationDiagnostic> diagnostics =
        new ArrayList<>(validateResourcePack(settings, coordinator));
    diagnostics.addAll(validateIntegrations(integrations));
    return List.copyOf(diagnostics);
  }

  private static void collectReferences(
      ActionDefinition action, Set<String> models, Set<String> anims) {
    if (action instanceof PlatformActions.SpawnModelAction spawnModel) {
      models.add(spawnModel.model());
    } else if (action instanceof PlatformActions.PlayAnimationAction playAnim) {
      anims.add(playAnim.animation());
    }
  }

  private static ValidationDiagnostic error(String code, String path, String message) {
    return new ValidationDiagnostic(ValidationDiagnostic.Severity.ERROR, code, path, message);
  }

  private static ValidationDiagnostic warning(String code, String path, String message) {
    return new ValidationDiagnostic(ValidationDiagnostic.Severity.WARNING, code, path, message);
  }

  private static ValidationDiagnostic info(String code, String path, String message) {
    return new ValidationDiagnostic(ValidationDiagnostic.Severity.INFO, code, path, message);
  }
}
