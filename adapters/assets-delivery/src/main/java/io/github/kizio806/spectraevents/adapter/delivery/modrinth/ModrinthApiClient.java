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
          String url =
              String.format(
                  "%s/project/%s/version?loaders=[\"%s\"]&game_versions=[\"%s\"]",
                  API_BASE, projectId, loader, gameVersion);

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
            if (response.statusCode() == 200) {
              return response.body();
            } else if (response.statusCode() == 404) {
              throw new RuntimeException("Modrinth project not found: " + projectId);
            } else {
              throw new RuntimeException("Modrinth API returned status " + response.statusCode());
            }
          } catch (Exception e) {
            throw new RuntimeException("Failed to contact Modrinth API", e);
          }
        });
  }
}
