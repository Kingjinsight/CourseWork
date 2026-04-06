package uk.ac.ed.inf.eventsapp.model;

import java.time.LocalDateTime;

/**
 * Booking entity from the UML diagram.
 */
public class Booking {
  private long bookingNumber;
  private int numTickets;
  private double amountPaid;
  private LocalDateTime bookingDateTime;
  private BookingStatus status;
  private Student student;
  private Performance performance;

  /** Creates an empty booking placeholder instance. */
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

  /** Marks the booking as cancelled by the student. */
  public void cancelByStudent() {
    status = BookingStatus.CANCELLEDBYSTUDENT;
  }

  /** Marks the booking as failed because payment did not complete. */
  public void cancelPaymentFailed() {
    status = BookingStatus.PAYMENTFAILED;
  }

  /** Marks the booking as cancelled by the provider due to performance cancellation. */
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
   * @return {@code true} if cancellation is still allowed, otherwise {@code false}
   */
  public boolean checkMoreThan24HoursAway() {
    return performance != null && performance.getStartDateTime() != null
        && performance.getStartDateTime().isAfter(LocalDateTime.now().plusHours(24));
  }

  /** @return the number of tickets in the booking */
  public int getNumTickets() {
    return numTickets;
  }

  /** @return the total amount paid for the booking */
  public double getAmountPaid() {
    return amountPaid;
  }

  /** @return the student's email address */
  public String getStudentEmail() {
    return student == null ? null : student.getEmail();
  }

  /** @return the student's phone number */
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

  /** @return the title of the booked performance's event */
  public String getPerformanceEventTitle() {
    return performance == null ? null : performance.getEventTitle();
  }

  /** @return the organiser email of the booked performance's event */
  public String getPerformanceOrganiserEmail() {
    return performance == null ? null : performance.getOrganiserEmail();
  }

  /** @return the booked performance */
  public Performance getPerformance() {
    return performance;
  }

  String toRefundDetailsString() {
    return numTickets + ";" + amountPaid + ";" + getStudentDetails();
  }

}
