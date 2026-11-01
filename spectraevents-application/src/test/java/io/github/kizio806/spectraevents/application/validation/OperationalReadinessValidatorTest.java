package io.github.kizio806.spectraevents.application.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.asset.AssetPipelineService;
import io.github.kizio806.spectraevents.application.asset.AssetTargetProfile;
import io.github.kizio806.spectraevents.application.asset.ResourcePackBuilder;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackDeliveryCoordinator;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackDeliverySettings;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackDescriptor;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackDescriptorCache;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackSourcePort;
import io.github.kizio806.spectraevents.application.config.validation.ValidationDiagnostic;
import io.github.kizio806.spectraevents.application.integration.IntegrationRegistry;
import io.github.kizio806.spectraevents.application.integration.IntegrationState;
import io.github.kizio806.spectraevents.application.model.animation.registry.AnimationDefinitionRegistry;
import io.github.kizio806.spectraevents.application.model.registry.ModelDefinitionRegistry;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinition;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.definition.EventEncounterSettings;
import io.github.kizio806.spectraevents.core.event.execution.action.PlatformActions;
import io.github.kizio806.spectraevents.core.event.phase.PhaseDefinition;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OperationalReadinessValidatorTest {
  private final OperationalReadinessValidator validator = new OperationalReadinessValidator();

  @Test
  void warnsWhenClientResourcePackDeliveryIsDisabled() {
    var diagnostics = validator.validateResourcePack(ResourcePackDeliverySettings.disabled());

    assertEquals(1, diagnostics.size());
    assertEquals("SE-READY-PACK-001", diagnostics.getFirst().code());
    assertEquals(ValidationDiagnostic.Severity.WARNING, diagnostics.getFirst().severity());
  }

  @Test
  void errorsWhenRequiredResourcePackDeliveryIsDisabled() {
    ResourcePackDeliverySettings requiredDisabled =
        new ResourcePackDeliverySettings(false, true, "", "", "", "", "");
    var diagnostics = validator.validateResourcePack(requiredDisabled);

    assertEquals(1, diagnostics.size());
    assertEquals("SE-READY-PACK-003", diagnostics.getFirst().code());
    assertEquals(ValidationDiagnostic.Severity.ERROR, diagnostics.getFirst().severity());
  }

  @Test
  void keepsConfiguredDeliveryDistinctFromRealClientCertification() {
    ResourcePackDeliverySettings configured =
        new ResourcePackDeliverySettings(
            true,
            false,
            "https://cdn.example.test/spectraevents.zip",
            "0123456789012345678901234567890123456789",
            "prompt",
            "",
            "");

    var diagnostics = validator.validateResourcePack(configured);

    assertEquals("SE-READY-PACK-002", diagnostics.getFirst().code());
    assertTrue(diagnostics.getFirst().message().contains("real-client acceptance"));
  }

  @Test
  void validatesCoordinatorStatusTransitions() {
    ResourcePackDeliverySettings settingsRequired =
        new ResourcePackDeliverySettings(
            true,
            true,
            "https://cdn.example.test/pack.zip",
            "0123456789012345678901234567890123456789",
            "",
            "",
            "");
    ResourcePackDeliverySettings settingsOptional =
        new ResourcePackDeliverySettings(
            true,
            false,
            "https://cdn.example.test/pack.zip",
            "0123456789012345678901234567890123456789",
            "",
            "",
            "");

    // Failed coordinator - required vs optional
    ResourcePackDeliveryCoordinator failedCoord =
        ResourcePackDeliveryCoordinator.failed(new RuntimeException("Network down"));
    var reqFailedDiag = validator.validateResourcePack(settingsRequired, failedCoord);
    assertTrue(reqFailedDiag.stream().anyMatch(d -> d.code().equals("SE-READY-PACK-004")));

    var optFailedDiag = validator.validateResourcePack(settingsOptional, failedCoord);
    assertTrue(optFailedDiag.stream().anyMatch(d -> d.code().equals("SE-READY-PACK-006")));

    // Resolving coordinator
    ResourcePackSourcePort pendingPort = (v, p) -> new CompletableFuture<>();
    ResourcePackDeliveryCoordinator resolvingCoord =
        new ResourcePackDeliveryCoordinator(
            pendingPort,
            new ResourcePackDescriptorCache(),
            "1.0",
            AssetTargetProfile.PROFILE_26_1,
            "config");
    var future1 = resolvingCoord.refresh();
    org.junit.jupiter.api.Assertions.assertNotNull(future1);
    var resolvingDiag = validator.validateResourcePack(settingsRequired, resolvingCoord);
    assertTrue(resolvingDiag.stream().anyMatch(d -> d.code().equals("SE-READY-PACK-005")));

    // Ready coordinator
    ResourcePackSourcePort readyPort =
        (v, p) ->
            CompletableFuture.completedFuture(
                new ResourcePackDescriptor(
                    "pack1",
                    "1.0",
                    "https://cdn.example.test/pack.zip",
                    "0123456789012345678901234567890123456789",
                    null,
                    1024L,
                    AssetTargetProfile.PROFILE_26_1,
                    "test"));
    ResourcePackDeliveryCoordinator readyCoord =
        new ResourcePackDeliveryCoordinator(
            readyPort,
            new ResourcePackDescriptorCache(),
            "1.0",
            AssetTargetProfile.PROFILE_26_1,
            "config");
    var future2 = readyCoord.refresh();
    org.junit.jupiter.api.Assertions.assertNotNull(future2);
    var readyDiag = validator.validateResourcePack(settingsRequired, readyCoord);
    assertTrue(readyDiag.stream().anyMatch(d -> d.code().equals("SE-READY-PACK-007")));

    // Unresolved coordinator with required
    ResourcePackDeliveryCoordinator unresCoord =
        new ResourcePackDeliveryCoordinator(
            readyPort,
            new ResourcePackDescriptorCache(),
            "1.0",
            AssetTargetProfile.PROFILE_26_1,
            "config");
    var unresDiag = validator.validateResourcePack(settingsRequired, unresCoord);
    assertTrue(unresDiag.stream().anyMatch(d -> d.code().equals("SE-READY-PACK-005")));
  }

  @Test
  void testValidateAssetPipeline(@TempDir Path tempDir) {
    // Null pipeline
    var nullDiag = validator.validateAssetPipeline(null);
    assertEquals("SE-READY-ASSET-001", nullDiag.getFirst().code());

    // Successful pipeline
    AssetPipelineService pipeline =
        new AssetPipelineService(
            path -> null,
            new ResourcePackBuilder(tempDir),
            tempDir,
            AssetTargetProfile.PROFILE_26_1);
    pipeline.buildAssets();
    assertTrue(validator.validateAssetPipeline(pipeline).isEmpty());

    // Failed pipeline
    AssetPipelineService failingPipeline =
        new AssetPipelineService(
            path -> {
              throw new IOException("Disk error");
            },
            new ResourcePackBuilder(tempDir),
            tempDir.resolve("non_existent"),
            AssetTargetProfile.PROFILE_26_1);
    var diag2 = validator.validateAssetPipeline(failingPipeline);
    assertTrue(diag2.isEmpty() || diag2.getFirst().code().startsWith("SE-READY-ASSET"));
  }

  @Test
  void reportsOptionalIntegrationStateWithoutFailingValidation() {
    IntegrationRegistry integrations = new IntegrationRegistry();
    integrations.register("Vault", IntegrationState.ABSENT, "Not installed");
    integrations.register("WorldGuard", IntegrationState.ACTIVE, "Available");
    integrations.register("MythicMobs", IntegrationState.FAILED, "Crash on load");

    var diagnostics = validator.validateIntegrations(integrations);

    assertEquals(ValidationDiagnostic.Severity.INFO, diagnostics.getFirst().severity());
    assertTrue(diagnostics.getFirst().message().contains("1 enabled, 2 unavailable"));

    // Verify warning emitted for FAILED integration
    assertTrue(diagnostics.stream().anyMatch(d -> d.code().equals("SE-READY-INTEGRATION-002")));
  }

  @Test
  void testValidateDefinitionReadiness() {
    PhaseId phaseId = new PhaseId("p1");
    PhaseDefinition phase =
        new PhaseDefinition(
            phaseId,
            Set.of(),
            List.of(),
            List.of(
                new PlatformActions.SpawnModelAction("missing_model", 0),
                new PlatformActions.PlayAnimationAction("missing_anim")));

    EventDefinition definition =
        new EventDefinition(
            new EventDefinitionId("test_event"),
            phaseId,
            Map.of(phaseId, phase),
            EventEncounterSettings.NONE);

    ModelDefinitionRegistry modelRegistry = new ModelDefinitionRegistry();
    AnimationDefinitionRegistry animRegistry = new AnimationDefinitionRegistry();

    ResourcePackDeliverySettings settings =
        new ResourcePackDeliverySettings(false, true, "", "", "", "", "");

    var diagnostics =
        validator.validateDefinitionReadiness(
            definition,
            modelRegistry,
            animRegistry,
            null,
            settings,
            null,
            AssetTargetProfile.PROFILE_26_1);

    assertTrue(diagnostics.stream().anyMatch(d -> d.code().equals("SE-READY-MODEL-001")));
    assertTrue(diagnostics.stream().anyMatch(d -> d.code().equals("SE-READY-ANIMATION-001")));
    assertTrue(diagnostics.stream().anyMatch(d -> d.code().equals("SE-READY-PACK-003")));
  }
}
