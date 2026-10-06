package io.github.kizio806.spectraevents.application.validation;

import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackDeliverySettings;
import io.github.kizio806.spectraevents.application.config.validation.ValidationDiagnostic;
import io.github.kizio806.spectraevents.application.integration.IntegrationRegistry;
import io.github.kizio806.spectraevents.application.integration.IntegrationState;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Reports operational prerequisites that do not make an otherwise valid YAML definition invalid.
 */
public final class OperationalReadinessValidator {
  public List<ValidationDiagnostic> validateResourcePack(ResourcePackDeliverySettings settings) {
    Objects.requireNonNull(settings, "settings");
    if (!settings.enabled()) {
      return List.of(
          warning(
              "SE-READY-PACK-001",
              "resource-pack.yml",
              "Resource-pack delivery is disabled; clients cannot receive custom model assets."));
    }
    return List.of(
        warning(
            "SE-READY-PACK-002",
            "resource-pack.yml",
            "Resource-pack delivery is configured, but real-client acceptance must be recorded before release."));
  }

  public List<ValidationDiagnostic> validateIntegrations(IntegrationRegistry integrations) {
    Objects.requireNonNull(integrations, "integrations");
    long enabled =
        integrations.getAll().values().stream()
            .filter(info -> info.state() == IntegrationState.ENABLED)
            .count();
    long unavailable = integrations.getAll().size() - enabled;
    return List.of(
        info(
            "SE-READY-INTEGRATION-001",
            "integrations",
            "Optional integrations: "
                + enabled
                + " enabled, "
                + unavailable
                + " unavailable. No optional integration is required by a generic Metin template."));
  }

  public List<ValidationDiagnostic> validate(
      ResourcePackDeliverySettings settings, IntegrationRegistry integrations) {
    List<ValidationDiagnostic> diagnostics = new ArrayList<>(validateResourcePack(settings));
    diagnostics.addAll(validateIntegrations(integrations));
    return List.copyOf(diagnostics);
  }

  private static ValidationDiagnostic warning(String code, String path, String message) {
    return new ValidationDiagnostic(ValidationDiagnostic.Severity.WARNING, code, path, message);
  }

  private static ValidationDiagnostic info(String code, String path, String message) {
    return new ValidationDiagnostic(ValidationDiagnostic.Severity.INFO, code, path, message);
  }
}
