package uk.ac.ed.inf.eventsapp.model;

/**
 * {@code BookingStatus} values
 *
 * <p>
 * These values describe whether a booking is still active or why it became inactive.
 */
public enum BookingStatus {
  ACTIVE, CANCELLEDBYSTUDENT, CANCELLEDBYPROVIDER, PAYMENTFAILED
}
