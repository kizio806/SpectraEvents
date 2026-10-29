package io.github.kizio806.spectraevents.adapter.delivery.modrinth;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.kizio806.spectraevents.application.asset.AssetTargetProfile;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackDescriptor;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackSourcePort;
import java.util.concurrent.CompletableFuture;

public final class ModrinthResourcePackSource implements ResourcePackSourcePort {

  private final ModrinthApiClient apiClient;
  private final String projectId;
  private final AssetTargetProfile serverProfile;
  private final String versionId;

  public ModrinthResourcePackSource(
      ModrinthApiClient apiClient, String projectId, String gameVersion) {
    this(apiClient, projectId, gameVersion, "");
  }

  /** Resolves the automatic release version, or an explicit configured rollback version ID. */
  public ModrinthResourcePackSource(
      ModrinthApiClient apiClient, String projectId, String gameVersion, String versionId) {
    if (projectId == null || projectId.isBlank() || projectId.startsWith("<")) {
      throw new IllegalArgumentException(
          "Modrinth project-id is missing or unconfigured. Specify the real project ID in resource-pack configuration.");
    }
    this.apiClient = apiClient;
    this.projectId = projectId;
    this.serverProfile = AssetTargetProfile.forMinecraftVersion(gameVersion);
    this.versionId = versionId == null ? "" : versionId.trim();
  }

  @Override
  public CompletableFuture<ResourcePackDescriptor> resolve(
      String pluginVersion, AssetTargetProfile profile) {
    if (profile != serverProfile) {
      return CompletableFuture.failedFuture(
          new IllegalArgumentException(
              "Server resource-pack profile "
                  + serverProfile
                  + " does not match requested profile "
                  + profile));
    }
    String expectedVersion = profile.resourcePackVersion(pluginVersion);
    return apiClient
        .getProjectVersions(projectId, "minecraft", profile.minecraftReleaseLine())
        .thenApply(
            jsonResponse -> {
              JsonArray versions = JsonParser.parseString(jsonResponse).getAsJsonArray();

              JsonObject selectedVersion = null;
              for (JsonElement el : versions) {
                JsonObject v = el.getAsJsonObject();
                if (!versionId.isBlank() && versionId.equals(v.get("id").getAsString())) {
                  selectedVersion = v;
                  break;
                }
                String versionNumber = v.get("version_number").getAsString();
                if (versionId.isBlank() && versionNumber.equals(expectedVersion)) {
                  selectedVersion = v;
                  break;
                }
              }

              if (selectedVersion == null) {
                throw new java.util.concurrent.CompletionException(
                    new IllegalStateException(
                        "Version "
                            + (versionId.isBlank() ? expectedVersion : versionId)
                            + " not found on Modrinth for project "
                            + projectId));
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
                throw new java.util.concurrent.CompletionException(
                    new IllegalStateException(
                        "Could not unambiguously determine primary file for Modrinth version "
                            + expectedVersion));
              }

              String url = primaryFile.get("url").getAsString();
              if (!url.startsWith("https://") || !url.endsWith(".zip")) {
                throw new java.util.concurrent.CompletionException(
                    new IllegalStateException(
                        "Modrinth file URL is invalid (must be HTTPS and .zip): " + url));
              }

              JsonObject hashes = primaryFile.getAsJsonObject("hashes");
              if (!hashes.has("sha1")) {
                throw new java.util.concurrent.CompletionException(
                    new IllegalStateException("Modrinth file is missing sha1 hash"));
              }

              String sha1 = hashes.get("sha1").getAsString();
              String sha512 = hashes.has("sha512") ? hashes.get("sha512").getAsString() : null;
              long size = primaryFile.get("size").getAsLong();

              if (size <= 0) {
                throw new java.util.concurrent.CompletionException(
                    new IllegalStateException("Modrinth file size must be greater than 0"));
              }

              return new ResourcePackDescriptor(
                  selectedVersion.get("id").getAsString(),
                  selectedVersion.get("version_number").getAsString(),
                  url,
                  sha1,
                  sha512,
                  size,
                  profile,
                  "MODRINTH");
            });
  }
}
