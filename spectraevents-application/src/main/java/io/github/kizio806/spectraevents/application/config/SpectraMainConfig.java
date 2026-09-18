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
    boolean debugEnabled,
    ResourcePackConfig resourcePack) {

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
        false,
        ResourcePackConfig.defaultConfig());
  }

  public record ResourcePackConfig(
      boolean enabled,
      boolean required,
      String sourceType,
      String modrinthProjectId,
      String manualUrl,
      String manualSha1,
      String prompt,
      String failurePolicy) {

    public static ResourcePackConfig defaultConfig() {
      return new ResourcePackConfig(
          true,
          true,
          "modrinth",
          "",
          "",
          "",
          "<gold>This server uses SpectraEvents assets.",
          "deny-assets");
    }
  }
}
