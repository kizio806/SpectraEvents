package io.github.kizio806.spectraevents.adapter.delivery.modrinth;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.kizio806.spectraevents.application.asset.AssetTargetProfile;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackDescriptor;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackSourcePort;
import java.util.concurrent.CompletableFuture;

public class ModrinthResourcePackSource implements ResourcePackSourcePort {

  private final ModrinthApiClient apiClient;
  private final String projectId;
  private final String gameVersion; // Resolved from server env, e.g. "26.3"

  public ModrinthResourcePackSource(
      ModrinthApiClient apiClient, String projectId, String gameVersion) {
    this.apiClient = apiClient;
    this.projectId = projectId;
    this.gameVersion = gameVersion;
  }

  @Override
  public CompletableFuture<ResourcePackDescriptor> resolve(
      String pluginVersion, AssetTargetProfile profile) {
    return apiClient
        .getProjectVersions(projectId, "minecraft", gameVersion)
        .thenApply(
            jsonResponse -> {
              JsonArray versions = JsonParser.parseString(jsonResponse).getAsJsonArray();

              String expectedVersion =
                  pluginVersion + "+" + profile.name().replace("PROFILE_", "").replace("_", ".");

              JsonObject selectedVersion = null;
              for (JsonElement el : versions) {
                JsonObject v = el.getAsJsonObject();
                String versionNumber = v.get("version_number").getAsString();
                if (versionNumber.equals(expectedVersion)) {
                  selectedVersion = v;
                  break;
                }
              }

              if (selectedVersion == null) {
                throw new RuntimeException(
                    "Version "
                        + expectedVersion
                        + " not found on Modrinth for project "
                        + projectId);
              }

              JsonArray files = selectedVersion.getAsJsonArray("files");
              JsonObject primaryFile = null;

              for (JsonElement el : files) {
                JsonObject file = el.getAsJsonObject();
                if (file.has("primary") && file.get("primary").getAsBoolean()) {
                  primaryFile = file;
                  break;
                }
              }

              if (primaryFile == null && files.size() == 1) {
                primaryFile = files.get(0).getAsJsonObject();
              }

              if (primaryFile == null) {
                throw new RuntimeException(
                    "Could not unambiguously determine primary file for Modrinth version "
                        + expectedVersion);
              }

              String url = primaryFile.get("url").getAsString();
              if (!url.startsWith("https://") || !url.endsWith(".zip")) {
                throw new RuntimeException(
                    "Modrinth file URL is invalid (must be HTTPS and .zip): " + url);
              }

              JsonObject hashes = primaryFile.getAsJsonObject("hashes");
              if (!hashes.has("sha1")) {
                throw new RuntimeException("Modrinth file is missing sha1 hash");
              }

              String sha1 = hashes.get("sha1").getAsString();
              String sha512 = hashes.has("sha512") ? hashes.get("sha512").getAsString() : null;
              long size = primaryFile.get("size").getAsLong();

              if (size <= 0) {
                throw new RuntimeException("Modrinth file size must be greater than 0");
              }

              return new ResourcePackDescriptor(
                  selectedVersion.get("id").getAsString(),
                  expectedVersion,
                  url,
                  sha1,
                  sha512,
                  size,
                  profile,
                  "MODRINTH");
            });
  }
}
