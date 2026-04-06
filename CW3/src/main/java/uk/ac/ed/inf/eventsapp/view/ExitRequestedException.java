package uk.ac.ed.inf.eventsapp.view;

/**
 * Signals that the user requested to exit interactive input immediately.
 */
public class ExitRequestedException extends RuntimeException {
  public ExitRequestedException() {
    super("User requested application exit.");
  }
}
