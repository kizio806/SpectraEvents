package io.github.kizio806.spectraevents.adapter.update.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import io.github.kizio806.spectraevents.application.update.UpdateCheckStatus;
import io.github.kizio806.spectraevents.application.update.UpdateInfo;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HttpUpdateAdapterTest {

  @TempDir Path temporaryDirectory;

  @Test
  void constructorAndMethodsValidateNullArguments() {
    assertThrows(NullPointerException.class, () -> new HttpUpdateAdapter(null));

    HttpUpdateAdapter adapter = new HttpUpdateAdapter(temporaryDirectory.resolve("updates"));
    assertThrows(NullPointerException.class, () -> adapter.checkUpdate(null, "stable"));
    assertThrows(NullPointerException.class, () -> adapter.checkUpdate("1.0.0", null));
    assertThrows(NullPointerException.class, () -> adapter.downloadUpdate(null, "plugin.jar"));
    assertThrows(
        NullPointerException.class, () -> adapter.downloadUpdate("https://example.invalid", null));
  }

  @Test
  void automaticDownloadFailsClosedWithoutWritingAFile() {
    HttpUpdateAdapter adapter = new HttpUpdateAdapter(temporaryDirectory.resolve("updates"));

    boolean downloaded =
        adapter.downloadUpdate("https://example.invalid/plugin.jar", "../outside.jar").join();

    assertFalse(downloaded);
    assertFalse(Files.exists(temporaryDirectory.resolve("outside.jar")));
    assertFalse(Files.exists(temporaryDirectory.resolve("updates")));
  }

  @Test
  void checkUpdateDetectsNewerVersion() throws Exception {
    HttpServer server =
        HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    server.createContext(
        "/release",
        exchange -> {
          byte[] response = "{\"tag_name\":\"v2.1.0\"}".getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, response.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
          }
        });
    server.start();

    try {
      String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/release";
      HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
      HttpUpdateAdapter adapter =
          new HttpUpdateAdapter(temporaryDirectory.resolve("updates"), url, client);

      UpdateInfo info = adapter.checkUpdate("1.0.0", "stable").join();

      assertTrue(info.updateAvailable());
      assertEquals("2.1.0", info.latestVersion());
      assertEquals(UpdateCheckStatus.UPDATE_AVAILABLE, info.status());
    } finally {
      server.stop(0);
    }
  }

  @Test
  void checkUpdateReportsUpToDateWhenVersionIsEqual() throws Exception {
    HttpServer server =
        HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    server.createContext(
        "/release",
        exchange -> {
          byte[] response = "{\"tag_name\":\"v1.0.0\"}".getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, response.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
          }
        });
    server.start();

    try {
      String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/release";
      HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
      HttpUpdateAdapter adapter =
          new HttpUpdateAdapter(temporaryDirectory.resolve("updates"), url, client);

      UpdateInfo info = adapter.checkUpdate("1.0.0", "stable").join();

      assertFalse(info.updateAvailable());
      assertEquals("1.0.0", info.latestVersion());
      assertEquals(UpdateCheckStatus.UP_TO_DATE, info.status());
    } finally {
      server.stop(0);
    }
  }

  @Test
  void checkUpdateFailsGracefullyOnHttpError() throws Exception {
    HttpServer server =
        HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    server.createContext(
        "/release",
        exchange -> {
          exchange.sendResponseHeaders(404, -1);
        });
    server.start();

    try {
      String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/release";
      HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
      HttpUpdateAdapter adapter =
          new HttpUpdateAdapter(temporaryDirectory.resolve("updates"), url, client);

      UpdateInfo info = adapter.checkUpdate("1.0.0", "stable").join();

      assertFalse(info.updateAvailable());
      assertEquals(UpdateCheckStatus.FAILED, info.status());
      assertTrue(info.releaseNotes().contains("no usable release metadata"));
    } finally {
      server.stop(0);
    }
  }

  @Test
  void checkUpdateFailsGracefullyOnInvalidJsonPayload() throws Exception {
    HttpServer server =
        HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    server.createContext(
        "/release",
        exchange -> {
          byte[] response = "{\"message\":\"not found\"}".getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, response.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
          }
        });
    server.start();

    try {
      String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/release";
      HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
      HttpUpdateAdapter adapter =
          new HttpUpdateAdapter(temporaryDirectory.resolve("updates"), url, client);

      UpdateInfo info = adapter.checkUpdate("1.0.0", "stable").join();

      assertFalse(info.updateAvailable());
      assertEquals(UpdateCheckStatus.FAILED, info.status());
    } finally {
      server.stop(0);
    }
  }

  @Test
  void checkUpdateFailsGracefullyOnUnreachableServer() {
    String url = "http://127.0.0.1:59999/release";
    HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(200)).build();
    HttpUpdateAdapter adapter =
        new HttpUpdateAdapter(temporaryDirectory.resolve("updates"), url, client);

    UpdateInfo info = adapter.checkUpdate("1.0.0", "stable").join();

    assertFalse(info.updateAvailable());
    assertEquals(UpdateCheckStatus.FAILED, info.status());
    assertTrue(info.releaseNotes().contains("could not be reached"));
  }
}
