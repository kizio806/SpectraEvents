package io.github.kizio806.spectraevents.application.port;

/** Neutral descriptor for the platform environment the engine is currently running on. */
public record PlatformDescriptor(
    String family, String implementation, String minecraftVersion, String apiVersion) {}
