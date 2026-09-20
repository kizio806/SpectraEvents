package io.github.kizio806.spectraevents.application.execution;

/** Exception thrown when a fatal platform action failure occurs. */
public class FatalActionException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public FatalActionException(String message) {
    super(message);
  }

  public FatalActionException(String message, Throwable cause) {
    super(message, cause);
  }
}
