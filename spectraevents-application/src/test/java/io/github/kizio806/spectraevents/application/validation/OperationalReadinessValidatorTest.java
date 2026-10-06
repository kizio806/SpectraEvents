package io.github.kizio806.spectraevents.application.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackDeliverySettings;
import io.github.kizio806.spectraevents.application.config.validation.ValidationDiagnostic;
import io.github.kizio806.spectraevents.application.integration.IntegrationRegistry;
import io.github.kizio806.spectraevents.application.integration.IntegrationState;
import org.junit.jupiter.api.Test;

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
  void reportsOptionalIntegrationStateWithoutFailingValidation() {
    IntegrationRegistry integrations = new IntegrationRegistry();
    integrations.register("Vault", IntegrationState.MISSING, "Not installed");
    integrations.register("WorldGuard", IntegrationState.ENABLED, "Available");

    var diagnostics = validator.validateIntegrations(integrations);

    assertEquals(ValidationDiagnostic.Severity.INFO, diagnostics.getFirst().severity());
    assertTrue(diagnostics.getFirst().message().contains("1 enabled, 1 unavailable"));
  }
}
