package io.github.kizio806.spectraevents.application.update;

import java.time.Instant;

/** Represents current status of plugin updates. */
public record UpdateInfo(
    String currentVersion,
    String latestVersion,
    boolean updateAvailable,
    String downloadUrl,
    String releaseNotes,
    Instant lastChecked,
    UpdateCheckStatus status) {

  public static UpdateInfo notChecked(String version) {
    return new UpdateInfo(
        version,
        version,
        false,
        "",
        "No update check has run yet.",
        Instant.EPOCH,
        UpdateCheckStatus.NOT_CHECKED);
  }

  public static UpdateInfo upToDate(String version) {
    return new UpdateInfo(
        version,
        version,
        false,
        "",
        "Running latest version.",
        Instant.now(),
        UpdateCheckStatus.UP_TO_DATE);
  }

  public static UpdateInfo failed(String version, String reason) {
    return new UpdateInfo(
        version,
        version,
        false,
        "",
        reason == null || reason.isBlank() ? "Update check failed." : reason,
        Instant.now(),
        UpdateCheckStatus.FAILED);
  }
}
