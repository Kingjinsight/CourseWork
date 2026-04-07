package uk.ac.ed.inf.eventsapp.view;

/**
 * Signals that the user requested to exit interactive input immediately.
 */
public class ExitRequestedException extends RuntimeException {
  /** Creates an exception indicating that the user requested immediate application exit. */
  public ExitRequestedException() {
    super("User requested application exit.");
  }
}
