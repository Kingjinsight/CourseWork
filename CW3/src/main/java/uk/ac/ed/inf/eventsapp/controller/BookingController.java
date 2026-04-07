package uk.ac.ed.inf.eventsapp.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collection;

import external.PaymentSystem;
import uk.ac.ed.inf.eventsapp.model.Booking;
import uk.ac.ed.inf.eventsapp.model.BookingStatus;
import uk.ac.ed.inf.eventsapp.model.Event;
import uk.ac.ed.inf.eventsapp.model.Performance;
import uk.ac.ed.inf.eventsapp.model.Student;
import uk.ac.ed.inf.eventsapp.util.InputParsers;
import uk.ac.ed.inf.eventsapp.view.View;

/**
 * Handles booking, review, and booking cancellation workflows.
 */
public class BookingController extends Controller {
  private long nextBookingNumber;
  private final PaymentSystem paymentSystem;
  @SuppressWarnings("unused")
  private final Collection<Event> events;
  private final Collection<Performance> performances;
  private final Collection<Booking> bookings;

  /**
   * Creates a booking controller backed by the shared application collections.
   *
   * @param view the text view used for interaction
   * @param paymentSystem the external payment-system adapter
   * @param events the shared event collection
   * @param performances the shared performance collection
   * @param bookings the shared booking collection
   */
  public BookingController(View view, PaymentSystem paymentSystem, Collection<Event> events,
      Collection<Performance> performances, Collection<Booking> bookings) {
    super(view);
    this.paymentSystem = paymentSystem;
    this.events = events;
    this.performances = performances;
    this.bookings = bookings;
    this.nextBookingNumber = 1L;
  }

  /**
   * Books a ticketed performance for the current student.
   *
   * The workflow repeatedly asks for a valid performance ID and, when necessary, a valid ticket
   * quantity before attempting payment.
   */
  public void bookPerformance() {
    if (!checkCurrentUserIsStudent()) {
      view.displayError("Only students can book performances.");
      return;
    }

    Performance performance = null;
    while (performance == null) {
      Long performanceID = InputParsers.parsePositiveLong(view.getInput("Enter performance ID"));
      if (performanceID == null) {
        view.displayError("Invalid performance ID");
        continue;
      }

      performance = getPerformanceByID(performanceID);
      if (performance == null) {
        view.displayError("Performance with given number does not exist.");
      }
    }

    boolean isTicketed = performance.checkIfEventIsTicketed();
    if (!isTicketed) {
      view.displayError(
          "The requested performance's event is not ticketed. There is no need to book it.");
      return;
    }

    int numTicketsRequested = 0;
    boolean bookingPossible = false;
    while (!bookingPossible) {
      Integer parsedTicketCount =
          InputParsers.parsePositiveInteger(view.getInput("Enter number of tickets"));
      if (parsedTicketCount == null) {
        view.displayError("Invalid number of tickets");
        continue;
      }

      numTicketsRequested = parsedTicketCount;
      bookingPossible = checkIfBookingPossible(performance, numTicketsRequested);
    }

    Student student = (Student) getCurrentUser();
    double amountPaid = BigDecimal.valueOf(performance.getFinalTicketPrice())
        .multiply(BigDecimal.valueOf(numTicketsRequested)).setScale(2, RoundingMode.HALF_UP)
        .doubleValue(); // floating point precision
    /*
     * jshell> System.out.println(0.1 * 3); 0.30000000000000004
     */

    Booking booking = new Booking(getNextBookingNumber(), numTicketsRequested, amountPaid,
        LocalDateTime.now(), BookingStatus.ACTIVE, student, performance);
    nextBookingNumber++;

    performance.addBooking(booking);
    student.addBooking(booking);
    addBooking(booking);

    // Booking is created before payment; marked PAYMENTFAILED if charge does not go through.
    boolean paymentSuccessful = paymentSystem.processPayment(numTicketsRequested,
        performance.getEventTitle(), student.getEmail(), student.getPhoneNumber(),
        performance.getOrganiserEmail(), amountPaid);

    if (!paymentSuccessful) {
      view.displayError("There was an issue with payment.");
      booking.cancelPaymentFailed();
    } else {
      performance.addNumTicketsSold(numTicketsRequested);
      view.displaySuccess("Booking successful");
      view.displayBookingRecord(booking.generateBookingRecord());
    }
  }

  /**
   * Placeholder for the 4-person-group {@code Review performance} use case shown in the UML
   * diagram.
   *
   * @throws UnsupportedOperationException because this use case is out of scope for
   *         three-person-group submissions
   */
  @SuppressWarnings("unused")
  public void reviewPerformance() {
    // Review performance is a 4-person-group-only use case and is intentionally left
    // unimplemented in the current 3-person-group submission.
    throw new UnsupportedOperationException("reviewPerformance is not implemented yet.");
  }


  /**
   * Cancels one of the current student's active bookings.
   *
   * Invalid booking selections are retried, while refund failure or the 24-hour cancellation rule
   * terminate the use case after displaying an error.
   */
  public void cancelBooking() {
    if (!checkCurrentUserIsStudent()) {
      view.displayError("Only students can cancel bookings.");
      return;
    }
    Student student = (Student) getCurrentUser();

    Booking booking = null;
    while (booking == null) {
      Long bookingNumber =
          InputParsers.parsePositiveLong(view.getInput("Enter booking number to cancel"));
      if (bookingNumber == null) {
        view.displayError("Invalid booking number");
        continue;
      }
      booking = getBookingByNumber(bookingNumber);
      if (booking == null) {
        view.displayError("Booking with given number does not exist");
      } else if (!booking.checkBookedByStudent(student.getEmail())) {
        view.displayError("You can only cancel your own bookings");
        booking = null;
      } else if (!booking.isActive()) {
        view.displayError("Booking is not active and cannot be cancelled");
        booking = null;
      } else if (!booking.checkMoreThan24HoursAway()) {
        view.displayError("Booking cannot be cancelled less than 24 hours before the performance");
        return; // terminate
      }
    }

    boolean refundSuccessful = paymentSystem.processRefund(booking.getNumTickets(),
        booking.getPerformanceEventTitle(), student.getEmail(), student.getPhoneNumber(),
        booking.getPerformanceOrganiserEmail(), booking.getAmountPaid(), "");

    if (!refundSuccessful) {
      view.displayError("There was an issue processing the refund.");
      return;
    }

    booking.cancelByStudent();
    booking.getPerformance().removeNumTicketsSold(booking.getNumTickets());
    view.displaySuccess("Booking cancelled successfully.");
  }


  /**
   * Adds a booking to the shared application booking collection.
   *
   * @param booking the booking to store
   */
  private void addBooking(Booking booking) {
    bookings.add(booking);
  }

  /**
   * Finds a performance by its identifier.
   *
   * @param performanceID the performance identifier to look up
   * @return the matching performance, or {@code null} if none exists
   */
  private Performance getPerformanceByID(long performanceID) {
    for (Performance performance : performances) {
      if (performance.hasID(performanceID)) {
        return performance;
      }
    }
    return null;
  }

  /**
   * Checks whether a requested booking quantity can still be satisfied for a performance.
   *
   * @param performance the performance being booked
   * @param numTickets the requested number of tickets
   * @return {@code true} if enough tickets remain, otherwise {@code false}
   */
  private boolean checkIfBookingPossible(Performance performance, int numTickets) {
    if (!performance.checkIfTicketsLeft(numTickets)) {
      view.displayError("Requested performance has no tickets left");
      return false;
    }
    return true;
  }

  /**
   * Finds bookings whose performances belong to the supplied event identifier.
   *
   * <p>
   * This helper is retained to match the UML API surface even though the current three-person-group
   * flows do not depend on it directly.
   *
   * @param eventID the event identifier to match against
   * @return the bookings associated with performances belonging to that event
   */
  @SuppressWarnings("unused")
  private Collection<Booking> findBookingsByEventID(long eventID) {
    Collection<Booking> matchingBookings = new java.util.ArrayList<>();
    for (Booking booking : bookings) {
      Performance performance = booking.getPerformance();
      if (performance != null && performance.belongsToEvent(eventID)) {
        matchingBookings.add(booking);
      }
    }
    return matchingBookings;
  }

  /**
   * Finds a booking by its booking number.
   *
   * @param bookingNumber the booking number to look up
   * @return the matching booking, or {@code null} if none exists
   */
  private Booking getBookingByNumber(long bookingNumber) {
    for (Booking booking : bookings) {
      if (booking.hasBookingNumber(bookingNumber)) {
        return booking;
      }
    }
    return null;
  }

  /**
   * Returns the next booking number that will be assigned.
   *
   * @return the next booking number
   */
  private long getNextBookingNumber() {
    return nextBookingNumber;
  }
}
