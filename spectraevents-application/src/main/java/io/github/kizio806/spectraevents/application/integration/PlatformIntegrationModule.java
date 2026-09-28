package io.github.kizio806.spectraevents.application.integration;

public interface PlatformIntegrationModule {
  String requiredPluginName();

  void initialize(IntegrationInitializationContext context);
}
