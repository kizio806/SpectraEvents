package io.github.kizio806.spectraevents.application.update;

import java.time.Instant;

/** Represents current status of plugin updates. */
public record UpdateInfo(
    String currentVersion,
    String latestVersion,
    boolean updateAvailable,
    String downloadUrl,
    String releaseNotes,
    Instant lastChecked) {

  public static UpdateInfo upToDate(String version) {
    return new UpdateInfo(version, version, false, "", "Running latest version.", Instant.now());
  }
}
