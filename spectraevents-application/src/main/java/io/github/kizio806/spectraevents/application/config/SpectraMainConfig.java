package io.github.kizio806.spectraevents.application.config;

/** Application main configuration settings. */
public record SpectraMainConfig(
    String language,
    boolean updateCheckEnabled,
    boolean updateNotifyConsole,
    boolean updateNotifyAdmins,
    boolean updateAutoDownload,
    String updateChannel,
    String storageType,
    String storageFile,
    boolean luckpermsIntegration,
    boolean placeholderApiIntegration,
    boolean worldGuardIntegration,
    boolean vaultIntegration,
    boolean debugEnabled) {

  public static SpectraMainConfig defaultConfig() {
    return new SpectraMainConfig(
        "en",
        true,
        true,
        true,
        false,
        "stable",
        "sqlite",
        "spectraevents.db",
        true,
        true,
        true,
        true,
        false);
  }
}
