package io.github.kizio806.spectraevents.adapter.delivery.modrinth;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/** Minimal HTTP client for reading public data from Modrinth API. */
public class ModrinthApiClient {

  private static final String API_BASE = "https://api.modrinth.com/v2";
  private final HttpClient httpClient;
  private final String userAgent;

  public ModrinthApiClient(String pluginVersion) {
    this.userAgent = "SpectraEvents/" + pluginVersion;
    this.httpClient =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
  }

  /** Fetch project versions matching loaders and game_versions. */
  public CompletableFuture<String> getProjectVersions(
      String projectId, String loader, String gameVersion) {
    return CompletableFuture.supplyAsync(
        () -> {
          String encodedLoader =
              java.net.URLEncoder.encode(
                  "[\"" + loader + "\"]", java.nio.charset.StandardCharsets.UTF_8);
          String encodedGameVersion =
              java.net.URLEncoder.encode(
                  "[\"" + gameVersion + "\"]", java.nio.charset.StandardCharsets.UTF_8);
          String url =
              String.format(
                  "%s/project/%s/version?loaders=%s&game_versions=%s",
                  API_BASE,
                  java.net.URLEncoder.encode(projectId, java.nio.charset.StandardCharsets.UTF_8),
                  encodedLoader,
                  encodedGameVersion);

          HttpRequest request =
              HttpRequest.newBuilder()
                  .uri(URI.create(url))
                  .header("User-Agent", userAgent)
                  .timeout(Duration.ofSeconds(10))
                  .GET()
                  .build();

          try {
            HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            final int statusOk = 200;
            final int statusNotFound = 404;
            if (response.statusCode() == statusOk) {
              return response.body();
            } else if (response.statusCode() == statusNotFound) {
              throw new IllegalStateException("Modrinth project not found: " + projectId);
            } else {
              throw new IllegalStateException(
                  "Modrinth API returned status " + response.statusCode());
            }
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while contacting Modrinth API", e);
          } catch (java.io.IOException e) {
            throw new IllegalStateException("Failed to contact Modrinth API", e);
          }
        });
  }
}
