package uk.ac.ed.inf.eventsapp.system;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import external.MockPaymentSystem;
import external.PaymentSystem;
import uk.ac.ed.inf.eventsapp.controller.BookingController;
import uk.ac.ed.inf.eventsapp.model.Booking;
import uk.ac.ed.inf.eventsapp.model.BookingStatus;
import uk.ac.ed.inf.eventsapp.model.EntertainmentProvider;
import uk.ac.ed.inf.eventsapp.model.Event;
import uk.ac.ed.inf.eventsapp.model.EventType;
import uk.ac.ed.inf.eventsapp.model.Performance;
import uk.ac.ed.inf.eventsapp.model.PerformanceStatus;
import uk.ac.ed.inf.eventsapp.model.Student;
import uk.ac.ed.inf.eventsapp.model.StudentPreferences;

public class CancelBookingSystemTests {
  private EntertainmentProvider provider;
  private Student student;
  private Performance performance;
  private Booking activeBooking;
  private Collection<Booking> bookings;
  private Collection<Performance> performances;

  @BeforeEach
  @SuppressWarnings("unused")
  void setUp() {
    provider = new EntertainmentProvider("provider@gmail.com", "password", "EooEle", "123",
        "Provider", "This is EooEle");
    student =
        new Student("student@ed.ac.uk", "password", "Hagan", 1234567, new StudentPreferences());

    Event event = new Event(1L, "Live Music", EventType.MUSIC, true, provider);
    LocalDateTime start = LocalDateTime.now().plusDays(7);
    performance = new Performance(1L, start, start.plusHours(2), List.of("Band"), "Hall", 100,
        false, false, 100, 0, 15.0, PerformanceStatus.ACTIVE, event);

    activeBooking =
        new Booking(1L, 2, 30.0, LocalDateTime.now(), BookingStatus.ACTIVE, student, performance);

    bookings = new ArrayList<>();
    bookings.add(activeBooking);
    performances = new ArrayList<>();
    performances.add(performance);
  }


  @Test
  void studentCanCancelOwnActiveBooking() {
    ScriptedView view = new ScriptedView("1");
    BookingController controller = new BookingController(view, new MockPaymentSystem(),
        new ArrayList<>(), performances, bookings);
    controller.setCurrentUser(student);

    controller.cancelBooking();

    assertEquals("SUCCESS: Booking cancelled successfully.", view.getLastSuccessMessage(),
        "Student should receive a success message after cancelling.");
  }

  // --- Access control ---

  @Test
  void onlyStudentsCanCancelBookings() {
    ScriptedView view = new ScriptedView();
    BookingController controller = new BookingController(view, new MockPaymentSystem(),
        new ArrayList<>(), performances, bookings);
    controller.setCurrentUser(provider);

    controller.cancelBooking();

    assertEquals("ERROR: Only students can cancel bookings.", view.getLastErrorMessage(),
        "Non-students should be rejected.");
  }

  @Test
  void guestCannotCancelBooking() {
    ScriptedView view = new ScriptedView();
    BookingController controller = new BookingController(view, new MockPaymentSystem(),
        new ArrayList<>(), performances, bookings);

    controller.cancelBooking();

    assertEquals("ERROR: Only students can cancel bookings.", view.getLastErrorMessage(),
        "Guest (no user) should be rejected.");
  }

  // --- Input validation ---

  @Test
  void invalidBookingNumberFormatShowsError() {
    ScriptedView view = new ScriptedView("abc", "1");
    BookingController controller = new BookingController(view, new MockPaymentSystem(),
        new ArrayList<>(), performances, bookings);
    controller.setCurrentUser(student);

    controller.cancelBooking();

    assertTrue(view.getErrorMessages().contains("ERROR: Invalid booking number"),
        "Non-numeric booking number should show an error.");
  }

  // --- Business logic errors ---

  @Test
  void cancellingNonExistentBookingShowsError() {
    ScriptedView view = new ScriptedView("999", "1");
    BookingController controller = new BookingController(view, new MockPaymentSystem(),
        new ArrayList<>(), performances, bookings);
    controller.setCurrentUser(student);

    controller.cancelBooking();

    assertTrue(view.getErrorMessages().contains("ERROR: Booking with given number does not exist"),
        "Non-existent booking number should show an error.");
  }

  @Test
  void studentCannotCancelAnotherStudentsBooking() {
    Student otherStudent =
        new Student("other@ed.ac.uk", "pass", "Bob", 9876543, new StudentPreferences());
    Booking otherBooking = new Booking(2L, 1, 15.0, LocalDateTime.now(), BookingStatus.ACTIVE,
        otherStudent, performance);
    bookings.add(otherBooking);

    ScriptedView view = new ScriptedView("2", "1");
    BookingController controller = new BookingController(view, new MockPaymentSystem(),
        new ArrayList<>(), performances, bookings);
    controller.setCurrentUser(student);

    controller.cancelBooking();

    assertTrue(view.getErrorMessages().contains("ERROR: You can only cancel your own bookings"),
        "Student should not be able to cancel another student's booking.");
    assertEquals("SUCCESS: Booking cancelled successfully.", view.getLastSuccessMessage(),
        "Student should be able to retry with their own booking number.");
  }

  @Test
  void studentCannotCancelAlreadyCancelledBooking() {
    Booking cancelledBooking = new Booking(2L, 1, 15.0, LocalDateTime.now(),
        BookingStatus.CANCELLEDBYSTUDENT, student, performance);
    bookings.add(cancelledBooking);

    ScriptedView view = new ScriptedView("2", "1");
    BookingController controller = new BookingController(view, new MockPaymentSystem(),
        new ArrayList<>(), performances, bookings);
    controller.setCurrentUser(student);

    controller.cancelBooking();

    assertTrue(
        view.getErrorMessages().contains("ERROR: Booking is not active and cannot be cancelled"),
        "Already cancelled booking should not be cancellable.");
  }

  @Test
  void bookingCancelledByProviderCannotBeCancelledByStudent() {
    Booking providerCancelled = new Booking(3L, 1, 15.0, LocalDateTime.now(),
        BookingStatus.CANCELLEDBYPROVIDER, student, performance);
    bookings.add(providerCancelled);

    ScriptedView view = new ScriptedView("3", "1");
    BookingController controller = new BookingController(view, new MockPaymentSystem(),
        new ArrayList<>(), performances, bookings);
    controller.setCurrentUser(student);

    controller.cancelBooking();

    assertTrue(
        view.getErrorMessages().contains("ERROR: Booking is not active and cannot be cancelled"),
        "Provider-cancelled booking should not be cancellable by student.");
  }

  @Test
  void paymentFailedBookingCannotBeCancelled() {
    Booking failedBooking = new Booking(4L, 1, 15.0, LocalDateTime.now(),
        BookingStatus.PAYMENTFAILED, student, performance);
    bookings.add(failedBooking);

    ScriptedView view = new ScriptedView("4", "1");
    BookingController controller = new BookingController(view, new MockPaymentSystem(),
        new ArrayList<>(), performances, bookings);
    controller.setCurrentUser(student);

    controller.cancelBooking();

    assertTrue(
        view.getErrorMessages().contains("ERROR: Booking is not active and cannot be cancelled"),
        "Payment-failed booking should not be cancellable.");
  }

  @Test
  void bookingLessThan24HoursAwayCannotBeCancelled() {
    LocalDateTime soonStart = LocalDateTime.now().plusHours(12);
    Performance soonPerformance = new Performance(2L, soonStart, soonStart.plusHours(2),
        List.of("Band"), "Hall", 100, false, false, 100, 2, 15.0, PerformanceStatus.ACTIVE,
        new Event(2L, "Soon Show", EventType.MUSIC, true, provider));
    Booking soonBooking = new Booking(2L, 2, 30.0, LocalDateTime.now(), BookingStatus.ACTIVE,
        student, soonPerformance);
    bookings.add(soonBooking);

    ScriptedView view = new ScriptedView("2", "1");
    BookingController controller = new BookingController(view, new MockPaymentSystem(),
        new ArrayList<>(), performances, bookings);
    controller.setCurrentUser(student);

    controller.cancelBooking();

    assertTrue(
        view.getErrorMessages().contains(
            "ERROR: Booking cannot be cancelled less than 24 hours before the performance"),
        "Bookings less than 24 hours away should not be cancellable.");
    assertTrue(soonBooking.isActive(),
        "Booking should remain active when cancellation is blocked.");
  }

  // --- Refund failure ---

  @Test
  void refundFailurePreventsBookingCancellation() {
    PaymentSystem failingPayment = new PaymentSystem() {
      @Override
      public boolean processPayment(int n, String t, String se, int sp, String ep, double a) {
        return false;
      }

      @Override
      public boolean processRefund(int n, String t, String se, int sp, String ep, double a,
          String m) {
        return false;
      }
    };

    ScriptedView view = new ScriptedView("1");
    BookingController controller =
        new BookingController(view, failingPayment, new ArrayList<>(), performances, bookings);
    controller.setCurrentUser(student);

    controller.cancelBooking();

    assertTrue(
        view.getErrorMessages().stream().anyMatch(e -> e.contains("issue processing the refund")),
        "Failed refund should prevent booking cancellation.");
    assertTrue(activeBooking.isActive(), "Booking should remain active when refund fails.");
  }

  // --- State verification ---

  @Test
  void cancelledBookingStatusIsCancelledByStudent() {
    ScriptedView view = new ScriptedView("1");
    BookingController controller = new BookingController(view, new MockPaymentSystem(),
        new ArrayList<>(), performances, bookings);
    controller.setCurrentUser(student);

    controller.cancelBooking();

    assertFalse(activeBooking.isActive(), "Booking should no longer be active after cancellation.");
  }

  @Test
  void cancelledBookingReturnsTicketsToPerformance() {
    performance.addNumTicketsSold(activeBooking.getNumTickets());

    ScriptedView view = new ScriptedView("1");
    BookingController controller = new BookingController(view, new MockPaymentSystem(),
        new ArrayList<>(), performances, bookings);
    controller.setCurrentUser(student);

    controller.cancelBooking();

    assertTrue(performance.checkIfTicketsLeft(100),
        "Cancelled booking should return tickets to the performance.");
  }
}
