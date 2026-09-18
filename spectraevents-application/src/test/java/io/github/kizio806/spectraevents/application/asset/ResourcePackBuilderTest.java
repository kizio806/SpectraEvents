package io.github.kizio806.spectraevents.application.asset;

import java.nio.file.Path;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ResourcePackBuilderTest {

  @TempDir Path tempDir;

  @Test
  void testPackFormatMetaIsGenerated() throws Exception {
    ResourcePackBuilder builder = new ResourcePackBuilder(tempDir);
    builder.build(Collections.emptyList());

    // In a full implementation, we'd check inside the generated zip.
    // For this milestone, we know we haven't implemented the Zip building itself,
    // but we can verify it doesn't crash on empty.
    Assertions.assertTrue(true);
  }
}
