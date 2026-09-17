package io.github.kizio806.spectraevents.application.execution;

/** Exception thrown when a fatal platform action failure occurs. */
public class FatalActionException extends RuntimeException {
  public FatalActionException(String message) {
    super(message);
  }

  public FatalActionException(String message, Throwable cause) {
    super(message, cause);
  }
}
