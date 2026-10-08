package io.github.kizio806.spectraevents.platform.paper.command;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EventCommandHandlerTest {
  @Test
  void doesNotSuggestRecoveryForACompletelyCleanedTerminalInstance() {
    assertFalse(EventCommandHandler.needsRecoveryGuidance(true, false, 0, 0));
  }

  @Test
  void suggestsRecoveryForMissingRuntimeStateOnAnActiveInstance() {
    assertTrue(EventCommandHandler.needsRecoveryGuidance(false, false, 0, 0));
  }

  @Test
  void suggestsRecoveryForResidualResourcesOnATerminalInstance() {
    assertTrue(EventCommandHandler.needsRecoveryGuidance(true, false, 0, 1));
  }
}
