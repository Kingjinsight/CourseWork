package uk.ac.ed.inf.eventsapp.model;

import java.time.LocalDateTime;

/**
 * Booking entity
 *
 * <p>
 * A booking records the relationship between a {@link Student} and a specific {@link Performance},
 * together with ticket count, payment amount, creation time, and booking status.
 */
public class Booking {
  private long bookingNumber;
  private int numTickets;
  private double amountPaid;
  private LocalDateTime bookingDateTime;
  private BookingStatus status;
  private Student student;
  private Performance performance;

  /**
   * Creates an empty booking placeholder instance.
   *
   * <p>
   * This is kept to support tests that build objects incrementally.
   */
  public Booking() {};

  /**
   * Creates a booking.
   *
   * @param bookingNumber the unique booking number
   * @param numTickets the number of booked tickets
   * @param amountPaid the total amount paid for the booking
   * @param bookingDateTime the date/time when the booking was created
   * @param status the booking status
   * @param student the student who made the booking
   * @param performance the booked performance
   */
  public Booking(long bookingNumber, int numTickets, double amountPaid,
      LocalDateTime bookingDateTime, BookingStatus status, Student student,
      Performance performance) {
    this.bookingNumber = bookingNumber;
    this.numTickets = numTickets;
    this.amountPaid = amountPaid;
    this.bookingDateTime = bookingDateTime;
    this.status = status;
    this.student = student;
    this.performance = performance;
  }

  /**
   * Marks the booking as cancelled by the student.
   *
   * <p>
   * This state transition is used after a successful student-initiated refund.
   */
  public void cancelByStudent() {
    status = BookingStatus.CANCELLEDBYSTUDENT;
  }

  /**
   * Marks the booking as failed because payment did not complete.
   *
   * <p>
   * This status is used when the booking object has been created locally but the external payment
   * step does not succeed.
   */
  public void cancelPaymentFailed() {
    status = BookingStatus.PAYMENTFAILED;
  }

  /**
   * Marks the booking as cancelled by the provider due to performance cancellation.
   *
   * <p>
   * This state transition is used after all required refunds succeed during performance
   * cancellation.
   */
  public void cancelByProvider() {
    status = BookingStatus.CANCELLEDBYPROVIDER;
  }

  /**
   * Checks whether the booking belongs to a given student.
   *
   * @param email the student email to compare against
   * @return {@code true} if the booking belongs to that student, otherwise {@code false}
   */
  public boolean checkBookedByStudent(String email) {
    return student != null && student.getEmail().equals(email);
  }

  /**
   * Returns the serialised student details used by the refund workflow.
   *
   * @return the student details in {@code name|email|phone} format
   */
  public String getStudentDetails() {
    if (student == null) {
      return "";
    }

    return student.getName() + "|" + student.getEmail() + "|" + student.getPhoneNumber();
  }

  /**
   * Produces a user-facing booking record.
   *
   * <p>
   * The record is displayed after a successful booking and includes the student, event,
   * performance, ticket-count, and payment details.
   *
   * @return the formatted booking record text
   */
  public String generateBookingRecord() {
    return "Booking #" + bookingNumber + "\nStudent: " + student.getName() + "\nEmail: "
        + student.getEmail() + "\nPhone: " + student.getPhoneNumber() + "\nEvent: "
        + performance.getEventTitle() + "\nPerformance: " + performance.toString() + "\nTickets: "
        + numTickets + "\nAmount paid: £" + String.format("%.2f", amountPaid);
  }

  /**
   * Checks whether the booking is currently active.
   *
   * @return {@code true} for active bookings, otherwise {@code false}
   */
  public boolean isActive() {
    return status == BookingStatus.ACTIVE;
  }

  /**
   * Checks whether the performance starts more than 24 hours from now.
   *
   * <p>
   * This helper implements the cancellation rule used by the {@code Cancel booking} use case.
   *
   * @return {@code true} if cancellation is still allowed, otherwise {@code false}
   */
  public boolean checkMoreThan24HoursAway() {
    return performance != null && performance.getStartDateTime() != null
        && performance.getStartDateTime().isAfter(LocalDateTime.now().plusHours(24));
  }

  /**
   * Returns the number of tickets recorded on this booking.
   *
   * @return the booked ticket count
   */
  public int getNumTickets() {
    return numTickets;
  }

  /**
   * Returns the amount paid for this booking.
   *
   * @return the total payment amount in GBP
   */
  public double getAmountPaid() {
    return amountPaid;
  }

  /**
   * Returns the booked student's email address.
   *
   * @return the student email, or {@code null} if no student is attached
   */
  public String getStudentEmail() {
    return student == null ? null : student.getEmail();
  }

  /**
   * Returns the booked student's phone number.
   *
   * @return the student phone number, or {@code 0} if no student is attached
   */
  public int getStudentPhone() {
    return student == null ? 0 : student.getPhoneNumber();
  }

  /**
   * Checks whether this booking has the given booking number.
   *
   * @param candidateBookingNumber the booking number to compare against
   * @return {@code true} if the numbers match, otherwise {@code false}
   */
  public boolean hasBookingNumber(long candidateBookingNumber) {
    return bookingNumber == candidateBookingNumber;
  }

  /**
   * Returns the title of the event associated with the booked performance.
   *
   * @return the event title, or {@code null} if no performance is attached
   */
  public String getPerformanceEventTitle() {
    return performance == null ? null : performance.getEventTitle();
  }

  /**
   * Returns the organiser email for the event associated with the booked performance.
   *
   * @return the organiser email, or {@code null} if no performance is attached
   */
  public String getPerformanceOrganiserEmail() {
    return performance == null ? null : performance.getOrganiserEmail();
  }

  /**
   * Returns the booked performance.
   *
   * @return the booked performance, or {@code null} if no performance is attached
   */
  public Performance getPerformance() {
    return performance;
  }

  /**
   * Serialises this booking into a compact refund-data record for the performance-cancellation
   * workflow.
   *
   * @return refund data in {@code numTickets;amountPaid;studentDetails} format
   */
  String toRefundDetailsString() {
    return numTickets + ";" + amountPaid + ";" + getStudentDetails();
  }

}
