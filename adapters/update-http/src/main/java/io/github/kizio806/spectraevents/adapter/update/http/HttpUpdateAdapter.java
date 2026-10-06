package io.github.kizio806.spectraevents.adapter.update.http;

import io.github.kizio806.spectraevents.application.update.SemVer;
import io.github.kizio806.spectraevents.application.update.UpdateCheckStatus;
import io.github.kizio806.spectraevents.application.update.UpdateInfo;
import io.github.kizio806.spectraevents.application.update.UpdatePort;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
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

  private final HttpClient httpClient;

  public HttpUpdateAdapter(Path updateDirectory) {
    Objects.requireNonNull(updateDirectory, "updateDirectory");
    this.httpClient =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
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
            final int statusOk = 200;
            if (response.statusCode() == statusOk) {
              String body = response.body();
              String tag = extractJsonField(body, "tag_name");
              if (tag != null && !tag.isBlank()) {
                String latest = tag.startsWith("v") ? tag.substring(1) : tag;
                boolean available = SemVer.isNewer(latest, currentVersion);
                return new UpdateInfo(
                    currentVersion,
                    latest,
                    available,
                    DEFAULT_UPDATE_URL,
                    "New version " + latest + " available.",
                    Instant.now(),
                    available ? UpdateCheckStatus.UPDATE_AVAILABLE : UpdateCheckStatus.UP_TO_DATE);
              }
            }
            return UpdateInfo.failed(
                currentVersion, "Update endpoint returned no usable release metadata.");
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.fine("Update check interrupted: " + e.getMessage());
            return UpdateInfo.failed(currentVersion, "Update check was interrupted.");
          } catch (java.io.IOException e) {
            LOGGER.fine("Update check unreachable or failed: " + e.getMessage());
            return UpdateInfo.failed(currentVersion, "Update endpoint could not be reached.");
          }
        });
  }

  @Override
  public CompletableFuture<Boolean> downloadUpdate(String downloadUrl, String targetFileName) {
    LOGGER.warning(
        "Automatic update downloads are disabled until signed checksums and bounded, allowlisted redirects are implemented");
    return CompletableFuture.completedFuture(false);
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
