package dev.spectraevents.application.port;

/** Output port used to report the plugin lifecycle without coupling the application to Paper. */
public interface LifecycleReporter {
  /** Reports successful startup. */
  void started();

  /** Reports orderly shutdown. */
  void stopped();
}
