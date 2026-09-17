package dev.spectraevents.application.update;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/** Manages update checking and status tracking. */
public final class UpdateService {
  private final String currentVersion;
  private final UpdatePort updatePort;
  private final AtomicReference<UpdateInfo> cachedInfo;

  public UpdateService(String currentVersion, UpdatePort updatePort) {
    this.currentVersion = Objects.requireNonNull(currentVersion, "currentVersion");
    this.updatePort = Objects.requireNonNull(updatePort, "updatePort");
    this.cachedInfo = new AtomicReference<>(UpdateInfo.upToDate(currentVersion));
  }

  public CompletableFuture<UpdateInfo> checkNow(String channel) {
    return updatePort
        .checkUpdate(currentVersion, channel)
        .thenApply(
            info -> {
              cachedInfo.set(info);
              return info;
            });
  }

  public UpdateInfo currentInfo() {
    return cachedInfo.get();
  }

  public String currentVersion() {
    return currentVersion;
  }

  public CompletableFuture<Boolean> downloadUpdate() {
    UpdateInfo info = cachedInfo.get();
    if (!info.updateAvailable() || info.downloadUrl().isBlank()) {
      return CompletableFuture.completedFuture(false);
    }
    return updatePort.downloadUpdate(
        info.downloadUrl(), "SpectraEvents-" + info.latestVersion() + ".jar.update");
  }
}
