package io.github.kizio806.spectraevents.application.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class UpdateServiceTest {

  @Test
  void testUpdateInfoFactories() {
    UpdateInfo notChecked = UpdateInfo.notChecked("1.0.0");
    assertEquals("1.0.0", notChecked.currentVersion());
    assertEquals("1.0.0", notChecked.latestVersion());
    assertFalse(notChecked.updateAvailable());
    assertEquals("", notChecked.downloadUrl());
    assertEquals(Instant.EPOCH, notChecked.lastChecked());
    assertEquals(UpdateCheckStatus.NOT_CHECKED, notChecked.status());

    UpdateInfo upToDate = UpdateInfo.upToDate("1.0.0");
    assertEquals("1.0.0", upToDate.currentVersion());
    assertFalse(upToDate.updateAvailable());
    assertEquals(UpdateCheckStatus.UP_TO_DATE, upToDate.status());
    assertNotNull(upToDate.lastChecked());

    UpdateInfo failedDefault = UpdateInfo.failed("1.0.0", null);
    assertEquals(UpdateCheckStatus.FAILED, failedDefault.status());
    assertEquals("Update check failed.", failedDefault.releaseNotes());

    UpdateInfo failedBlank = UpdateInfo.failed("1.0.0", "   ");
    assertEquals(UpdateCheckStatus.FAILED, failedBlank.status());
    assertEquals("Update check failed.", failedBlank.releaseNotes());

    UpdateInfo failedCustom = UpdateInfo.failed("1.0.0", "Network timeout");
    assertEquals(UpdateCheckStatus.FAILED, failedCustom.status());
    assertEquals("Network timeout", failedCustom.releaseNotes());
  }

  @Test
  void testUpdateServiceLifecycle() {
    class MockUpdatePort implements UpdatePort {
      UpdateInfo nextCheck;
      boolean downloadResult = true;
      String lastDownloadedUrl;
      String lastDownloadedTarget;

      @Override
      public CompletableFuture<UpdateInfo> checkUpdate(String currentVersion, String channel) {
        return CompletableFuture.completedFuture(nextCheck);
      }

      @Override
      public CompletableFuture<Boolean> downloadUpdate(String downloadUrl, String targetFileName) {
        this.lastDownloadedUrl = downloadUrl;
        this.lastDownloadedTarget = targetFileName;
        return CompletableFuture.completedFuture(downloadResult);
      }
    }

    MockUpdatePort port = new MockUpdatePort();
    assertThrows(NullPointerException.class, () -> new UpdateService(null, port));
    assertThrows(NullPointerException.class, () -> new UpdateService("1.0.0", null));

    UpdateService service = new UpdateService("1.2.3", port);

    assertEquals("1.2.3", service.currentVersion());
    assertEquals(UpdateCheckStatus.NOT_CHECKED, service.currentInfo().status());

    // When update is not available, downloadUpdate returns false
    assertFalse(service.downloadUpdate().join());

    // Check update returns new version
    UpdateInfo available =
        new UpdateInfo(
            "1.2.3",
            "1.3.0",
            true,
            "https://example.com/download.jar",
            "New release",
            Instant.now(),
            UpdateCheckStatus.UPDATE_AVAILABLE);
    port.nextCheck = available;

    UpdateInfo checkResult = service.checkNow("release").join();
    assertEquals(available, checkResult);
    assertEquals(available, service.currentInfo());

    // Now download succeeds
    boolean downloadOk = service.downloadUpdate().join();
    assertTrue(downloadOk);
    assertEquals("https://example.com/download.jar", port.lastDownloadedUrl);
    assertEquals("SpectraEvents-1.3.0.jar.update", port.lastDownloadedTarget);

    // If downloadUrl is blank even if updateAvailable is true
    UpdateInfo blankUrl =
        new UpdateInfo(
            "1.2.3",
            "1.3.0",
            true,
            "",
            "New release",
            Instant.now(),
            UpdateCheckStatus.UPDATE_AVAILABLE);
    port.nextCheck = blankUrl;
    service.checkNow("release").join();
    assertFalse(service.downloadUpdate().join());
  }
}
