package io.github.kizio806.spectraevents.platform.paper.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PaperStartupLoggerTest {
  @Test
  void describesTheRunningServerInsteadOfTheArtifactApiVersion() {
    assertEquals("Purpur 26.3", PaperStartupLogger.platformDescription("Purpur", "26.3"));
  }
}
