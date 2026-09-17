package io.github.kizio806.spectraevents.application.port;

/** Port for querying the current server platform's capabilities. */
public interface PlatformCapabilityQuery {

  /**
   * Checks if the platform supports the given capability.
   *
   * @param capability the capability to check
   * @return true if supported, false otherwise
   */
  boolean hasCapability(PlatformCapability capability);

  /**
   * Retrieves a descriptor of the platform environment.
   *
   * @return the platform descriptor
   */
  PlatformDescriptor platformDescriptor();
}
