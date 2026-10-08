package io.github.kizio806.spectraevents.adapter.update.http;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HttpUpdateAdapterTest {

  @TempDir Path temporaryDirectory;

  @Test
  void automaticDownloadFailsClosedWithoutWritingAFile() {
    HttpUpdateAdapter adapter = new HttpUpdateAdapter(temporaryDirectory.resolve("updates"));

    boolean downloaded =
        adapter.downloadUpdate("https://example.invalid/plugin.jar", "../outside.jar").join();

    assertFalse(downloaded);
    assertFalse(Files.exists(temporaryDirectory.resolve("outside.jar")));
    assertFalse(Files.exists(temporaryDirectory.resolve("updates")));
  }
}
