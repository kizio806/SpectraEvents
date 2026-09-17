package io.github.kizio806.spectraevents.core.event.runtime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EventLifecycleStateTest {
  @Test
  void identifiesTerminalStatesCorrectly() {
    assertFalse(EventLifecycleState.CREATED.isTerminal());
    assertFalse(EventLifecycleState.RUNNING.isTerminal());
    assertTrue(EventLifecycleState.COMPLETED.isTerminal());
    assertTrue(EventLifecycleState.CANCELLED.isTerminal());
    assertTrue(EventLifecycleState.FAILED.isTerminal());
  }
}
