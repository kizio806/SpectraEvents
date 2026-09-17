package dev.spectraevents.adapter.update.http;

import dev.spectraevents.application.update.UpdateInfo;
import dev.spectraevents.application.update.UpdatePort;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/** Platform-neutral HTTP implementation of UpdatePort using Java 11+ HttpClient. */
public final class HttpUpdateAdapter implements UpdatePort {
  private static final Logger LOGGER = Logger.getLogger(HttpUpdateAdapter.class.getName());
  private static final String DEFAULT_UPDATE_URL =
      "https://api.github.com/repos/kizio806/SpectraEvents/releases/latest";

  private final Path updateDirectory;
  private final HttpClient httpClient;

  public HttpUpdateAdapter(Path updateDirectory) {
    this.updateDirectory = Objects.requireNonNull(updateDirectory, "updateDirectory");
    this.httpClient =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
  }

  @Override
  public CompletableFuture<UpdateInfo> checkUpdate(String currentVersion, String channel) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            HttpRequest request =
                HttpRequest.newBuilder()
                    .uri(URI.create(DEFAULT_UPDATE_URL))
                    .header("User-Agent", "SpectraEvents-Plugin")
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
              String body = response.body();
              String tag = extractJsonField(body, "tag_name");
              if (tag != null && !tag.isBlank()) {
                String latest = tag.startsWith("v") ? tag.substring(1) : tag;
                boolean available = !latest.equalsIgnoreCase(currentVersion);
                return new UpdateInfo(
                    currentVersion,
                    latest,
                    available,
                    DEFAULT_UPDATE_URL,
                    "New version " + latest + " available.",
                    Instant.now());
              }
            }
          } catch (Exception e) {
            LOGGER.fine("Update check unreachable or failed: " + e.getMessage());
          }
          return UpdateInfo.upToDate(currentVersion);
        });
  }

  @Override
  public CompletableFuture<Boolean> downloadUpdate(String downloadUrl, String targetFileName) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            Files.createDirectories(updateDirectory);

            Path tempFile = updateDirectory.resolve(targetFileName + ".tmp");
            Path targetFile = updateDirectory.resolve(targetFileName);

            HttpRequest request =
                HttpRequest.newBuilder()
                    .uri(URI.create(downloadUrl))
                    .header("User-Agent", "SpectraEvents-Plugin")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<InputStream> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() == 200) {
              try (InputStream in = response.body()) {
                Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
              }
              Files.move(
                  tempFile,
                  targetFile,
                  StandardCopyOption.REPLACE_EXISTING,
                  StandardCopyOption.ATOMIC_MOVE);
              LOGGER.info(
                  "Update downloaded to " + targetFile + ". It will be applied on next restart.");
              return true;
            }
          } catch (Exception e) {
            LOGGER.warning("Failed to download update: " + e.getMessage());
          }
          return false;
        });
  }

  private String extractJsonField(String json, String field) {
    String search = "\"" + field + "\":\"";
    int idx = json.indexOf(search);
    if (idx != -1) {
      int start = idx + search.length();
      int end = json.indexOf("\"", start);
      if (end != -1) {
        return json.substring(start, end);
      }
    }
    return null;
  }
}
