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
import io.github.kizio806.spectraevents.application.model.animation.compiler.AnimationCompiler;
import io.github.kizio806.spectraevents.application.model.animation.compiler.CompiledAnimation;
import io.github.kizio806.spectraevents.application.model.animation.registry.AnimationDefinitionRegistry;
import io.github.kizio806.spectraevents.application.model.animation.spec.AnimationSpec;
import io.github.kizio806.spectraevents.application.model.registry.ModelDefinitionRegistry;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinition;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.definition.EventEncounterSettings;
import io.github.kizio806.spectraevents.core.event.execution.TransitionRule;
import io.github.kizio806.spectraevents.core.event.execution.action.PlatformActions;
import io.github.kizio806.spectraevents.core.event.phase.PhaseDefinition;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationId;
import io.github.kizio806.spectraevents.core.visual.model.ModelId;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
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
  void testValidateAssetPipeline(@TempDir Path tempDir) throws Exception {
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

    // Failed pipeline with actual failure set
    Path assetSrc = tempDir.resolve("assets_fail");
    Files.createDirectories(assetSrc);
    Files.writeString(assetSrc.resolve("test.bbmodel"), "data");
    AssetPipelineService failingPipeline =
        new AssetPipelineService(
            path -> {
              throw new IOException("Simulated disk error");
            },
            new ResourcePackBuilder(tempDir.resolve("rp_out")),
            assetSrc,
            AssetTargetProfile.PROFILE_26_1);
    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalStateException.class, failingPipeline::buildAssets);
    var diag2 = validator.validateAssetPipeline(failingPipeline);
    assertEquals(1, diag2.size());
    assertEquals("SE-READY-ASSET-003", diag2.getFirst().code());

    // Building pipeline
    CountDownLatch buildingLatch = new CountDownLatch(1);
    CountDownLatch releaseLatch = new CountDownLatch(1);
    Path assetBuilding = tempDir.resolve("assets_building");
    Files.createDirectories(assetBuilding);
    Files.writeString(assetBuilding.resolve("test.bbmodel"), "data");
    AssetPipelineService buildingPipeline =
        new AssetPipelineService(
            path -> {
              buildingLatch.countDown();
              try {
                releaseLatch.await();
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              }
              return null;
            },
            new ResourcePackBuilder(tempDir.resolve("rp_build_out")),
            assetBuilding,
            AssetTargetProfile.PROFILE_26_1);

    Thread buildThread = new Thread(buildingPipeline::buildAssets);
    buildThread.start();
    try {
      buildingLatch.await();
      var diagBuilding = validator.validateAssetPipeline(buildingPipeline);
      assertEquals(1, diagBuilding.size());
      assertEquals("SE-READY-ASSET-002", diagBuilding.getFirst().code());
    } finally {
      releaseLatch.countDown();
      buildThread.join();
    }
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
  void testValidateDefinitionReadiness(@TempDir Path tempDir) throws Exception {
    PhaseId phaseId = new PhaseId("p1");
    TransitionRule ruleWithActions =
        new TransitionRule(
            () -> "custom_trigger",
            List.of(),
            Optional.empty(),
            List.of(
                new PlatformActions.SpawnModelAction("rule_model", 0),
                new PlatformActions.PlayAnimationAction("rule_anim")));

    PhaseDefinition phase =
        new PhaseDefinition(
            phaseId,
            Set.of(),
            List.of(ruleWithActions),
            List.of(
                new PlatformActions.SpawnModelAction("missing_model", 0),
                new PlatformActions.PlayAnimationAction("missing_anim")));

    EventDefinition definition =
        new EventDefinition(
            new EventDefinitionId("test_event"),
            phaseId,
            Map.of(phaseId, phase),
            EventEncounterSettings.NONE);

    ModelDefinitionRegistry modelRegistry =
        new ModelDefinitionRegistry() {
          @Override
          public boolean contains(ModelId id) {
            return "rule_model".equals(id.value());
          }
        };

    AnimationSpec animSpec = new AnimationSpec();
    animSpec.setDuration("1s");
    animSpec.setTracks(Map.of());
    CompiledAnimation compiledAnim =
        new AnimationCompiler().compile(new AnimationId("rule_anim"), animSpec, Set.of());

    AnimationDefinitionRegistry animRegistry =
        new AnimationDefinitionRegistry() {
          @Override
          public Optional<CompiledAnimation> find(ModelId modelId, AnimationId animationId) {
            if ("rule_anim".equals(animationId.value())) {
              return Optional.of(compiledAnim);
            }
            return Optional.empty();
          }
        };

    ResourcePackDeliverySettings settings =
        new ResourcePackDeliverySettings(false, true, "", "", "", "", "");

    // Failing pipeline
    Path assetSrc = tempDir.resolve("assets_src");
    Files.createDirectories(assetSrc);
    Files.writeString(assetSrc.resolve("dummy.bbmodel"), "dummy content");
    AssetPipelineService failingPipeline =
        new AssetPipelineService(
            path -> {
              throw new IOException("Simulated disk error");
            },
            new ResourcePackBuilder(tempDir.resolve("rp_out")),
            assetSrc,
            AssetTargetProfile.PROFILE_26_1);
    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalStateException.class, failingPipeline::buildAssets);

    var diagnostics =
        validator.validateDefinitionReadiness(
            definition,
            modelRegistry,
            animRegistry,
            failingPipeline,
            settings,
            null,
            AssetTargetProfile.PROFILE_26_2); // Mismatch profile

    // missing_model is not registered
    assertTrue(diagnostics.stream().anyMatch(d -> d.code().equals("SE-READY-MODEL-001")));
    // missing_anim is not registered
    assertTrue(diagnostics.stream().anyMatch(d -> d.code().equals("SE-READY-ANIMATION-001")));
    // Pipeline failure blocks event execution
    assertTrue(diagnostics.stream().anyMatch(d -> d.code().equals("SE-READY-ASSET-003")));
    // Server target profile mismatches pipeline build profile
    assertTrue(diagnostics.stream().anyMatch(d -> d.code().equals("SE-READY-PROFILE-001")));
    // Required resource-pack delivery is disabled
    assertTrue(diagnostics.stream().anyMatch(d -> d.code().equals("SE-READY-PACK-003")));

    // Test with failed coordinator
    ResourcePackDeliverySettings settingsEnabledRequired =
        new ResourcePackDeliverySettings(
            true,
            true,
            "https://cdn.example.test/pack.zip",
            "0123456789012345678901234567890123456789",
            "",
            "",
            "");
    ResourcePackDeliveryCoordinator failedCoord =
        ResourcePackDeliveryCoordinator.failed(new RuntimeException("coord fail"));
    var diagWithCoord =
        validator.validateDefinitionReadiness(
            definition,
            modelRegistry,
            animRegistry,
            null,
            settingsEnabledRequired,
            failedCoord,
            AssetTargetProfile.PROFILE_26_1);
    assertTrue(diagWithCoord.stream().anyMatch(d -> d.code().equals("SE-READY-PACK-004")));

    // Test with null registries and settings to cover optional branches
    var diagNulls =
        validator.validateDefinitionReadiness(definition, null, null, null, null, null, null);
    assertTrue(diagNulls.isEmpty());
  }

  @Test
  void testValidateDefinitionReadinessBuildingAndGenericActions(@TempDir Path tempDir)
      throws Exception {
    PhaseId phaseId = new PhaseId("p1");
    PhaseDefinition phase =
        new PhaseDefinition(
            phaseId,
            Set.of(),
            List.of(),
            List.of(
                (io.github.kizio806.spectraevents.core.event.execution.action.ActionDefinition)
                    () -> "generic_action"));

    EventDefinition definition =
        new EventDefinition(
            new EventDefinitionId("generic_event"),
            phaseId,
            Map.of(phaseId, phase),
            EventEncounterSettings.NONE);

    CountDownLatch buildingLatch = new CountDownLatch(1);
    CountDownLatch releaseLatch = new CountDownLatch(1);
    Path assetBuilding = tempDir.resolve("assets_building_def");
    Files.createDirectories(assetBuilding);
    Files.writeString(assetBuilding.resolve("test.bbmodel"), "data");
    AssetPipelineService buildingPipeline =
        new AssetPipelineService(
            path -> {
              buildingLatch.countDown();
              try {
                releaseLatch.await();
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              }
              return null;
            },
            new ResourcePackBuilder(tempDir.resolve("rp_build_def_out")),
            assetBuilding,
            AssetTargetProfile.PROFILE_26_1);

    Thread buildThread = new Thread(buildingPipeline::buildAssets);
    buildThread.start();
    try {
      buildingLatch.await();
      var diag =
          validator.validateDefinitionReadiness(
              definition, null, null, buildingPipeline, null, null, null);
      assertEquals(1, diag.size());
      assertEquals("SE-READY-ASSET-002", diag.getFirst().code());
    } finally {
      releaseLatch.countDown();
      buildThread.join();
    }
  }

  @Test
  void testValidateOverloads() {
    ResourcePackDeliverySettings settings = ResourcePackDeliverySettings.disabled();
    IntegrationRegistry integrations = new IntegrationRegistry();

    var d1 = validator.validate(settings, integrations);
    assertEquals(2, d1.size());

    var d2 = validator.validate(settings, null, integrations);
    assertEquals(2, d2.size());
  }
}
