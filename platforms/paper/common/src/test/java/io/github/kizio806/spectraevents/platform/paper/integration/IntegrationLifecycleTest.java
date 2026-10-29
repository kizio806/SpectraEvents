package io.github.kizio806.spectraevents.platform.paper.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.integration.IntegrationRegistry;
import io.github.kizio806.spectraevents.application.integration.IntegrationState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IntegrationLifecycleTest {
  private IntegrationRegistry registry;

  @BeforeEach
  void setUp() {
    registry = new IntegrationRegistry();
  }

  @Test
  void reportsAllDeclaredLifecycleStates() {
    registry.register("Vault", IntegrationState.ABSENT, "Not installed");
    registry.register("LuckPerms", IntegrationState.AVAILABLE, "Detected (v5.4)");
    registry.register("WorldGuard", IntegrationState.ACTIVE, "Hooked");
    registry.register("CustomPlugin", IntegrationState.DEGRADED, "Present but disabled");
    registry.register("PlaceholderAPI", IntegrationState.FAILED, "Registration rejected");

    assertEquals(5, registry.getAll().size());
    assertEquals(IntegrationState.ABSENT, registry.get("Vault").orElseThrow().state());
    assertEquals(IntegrationState.AVAILABLE, registry.get("LuckPerms").orElseThrow().state());
    assertEquals(IntegrationState.ACTIVE, registry.get("WorldGuard").orElseThrow().state());
    assertEquals(IntegrationState.DEGRADED, registry.get("CustomPlugin").orElseThrow().state());
    assertEquals(IntegrationState.FAILED, registry.get("PlaceholderAPI").orElseThrow().state());
  }

  @Test
  void onlyActiveIntegrationsCountTowardActiveTotal() {
    registry.register("Vault", IntegrationState.ABSENT, "Not installed");
    registry.register("LuckPerms", IntegrationState.AVAILABLE, "Detected");
    registry.register("WorldGuard", IntegrationState.ACTIVE, "Hooked");
    registry.register("PlaceholderAPI", IntegrationState.FAILED, "Error");

    assertEquals(1, registry.activeCount());
    assertEquals(1, registry.enabledCount());
  }

  @Test
  void transitionFromAvailableToActiveOrFailedIsTracked() {
    registry.register("PlaceholderAPI", IntegrationState.AVAILABLE, "Detected (v2.11)");
    assertFalse(registry.get("PlaceholderAPI").orElseThrow().state().isActive());

    // Successful initialization promotes to ACTIVE
    registry.register("PlaceholderAPI", IntegrationState.ACTIVE, "Expansion registered");
    assertTrue(registry.get("PlaceholderAPI").orElseThrow().state().isActive());
    assertEquals(1, registry.activeCount());

    // Failed initialization transitions to FAILED
    registry.register("PlaceholderAPI", IntegrationState.FAILED, "Registration rejected");
    assertFalse(registry.get("PlaceholderAPI").orElseThrow().state().isActive());
    assertEquals(0, registry.activeCount());
    assertTrue(registry.get("PlaceholderAPI").orElseThrow().details().contains("rejected"));
  }
}
