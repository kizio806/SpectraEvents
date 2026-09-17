package dev.spectraevents.application.update;

import java.util.concurrent.CompletableFuture;

/** Abstract port for checking and downloading updates. */
public interface UpdatePort {
  CompletableFuture<UpdateInfo> checkUpdate(String currentVersion, String channel);

  CompletableFuture<Boolean> downloadUpdate(String downloadUrl, String targetFileName);
}
