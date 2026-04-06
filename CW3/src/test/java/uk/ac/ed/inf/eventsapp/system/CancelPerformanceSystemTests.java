package uk.ac.ed.inf.eventsapp.system;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import external.MockPaymentSystem;
import external.PaymentSystem;
import uk.ac.ed.inf.eventsapp.controller.EventPerformanceController;
import uk.ac.ed.inf.eventsapp.model.Booking;
import uk.ac.ed.inf.eventsapp.model.BookingStatus;
import uk.ac.ed.inf.eventsapp.model.EntertainmentProvider;
import uk.ac.ed.inf.eventsapp.model.Event;
import uk.ac.ed.inf.eventsapp.model.EventType;
import uk.ac.ed.inf.eventsapp.model.Performance;
import uk.ac.ed.inf.eventsapp.model.PerformanceStatus;
import uk.ac.ed.inf.eventsapp.model.Student;
import uk.ac.ed.inf.eventsapp.model.StudentPreferences;

public class CancelPerformanceSystemTests {
  private EntertainmentProvider provider;
  private Student student;
  private Event event;
  private Performance futurePerformance;
  private Collection<Performance> performances;

  @BeforeEach
  @SuppressWarnings("unused")
  void setUp() {
    provider = new EntertainmentProvider("provider@gmail.com", "password", "EooEle", "123",
        "Provider", "This is EooEle");
    student =
        new Student("student@ed.ac.uk", "password", "Hagan", 1234567, new StudentPreferences());

    LocalDateTime start = LocalDateTime.now().plusDays(7);
    event = new Event(1L, "Live Music", EventType.MUSIC, true, provider);
    futurePerformance = new Performance(1L, start, start.plusHours(2), List.of("Band"), "Hall", 100,
        false, false, 100, 0, 15.0, PerformanceStatus.ACTIVE, event);

    performances = new ArrayList<>();
    performances.add(futurePerformance);
  }

  @Test
  void providerCanCancelFuturePerformanceWithNoBookings() {
    ScriptedView view = new ScriptedView("1");
    EventPerformanceController controller = new EventPerformanceController(view, new ArrayList<>(),
        performances, new MockPaymentSystem());
    controller.setCurrentUser(provider);

    controller.cancelPerformance();

    assertEquals("SUCCESS: Cancellation Successful!", view.getLastSuccessMessage(),
        "Provider should receive a success message after cancelling.");
  }

  @Test
  void cancellingPerformanceRefundsActiveBookings() {
    Booking activeBooking = new Booking(1L, 2, 30.0, LocalDateTime.now(), BookingStatus.ACTIVE,
        student, futurePerformance);
    futurePerformance.addBooking(activeBooking);

    ScriptedView view = new ScriptedView("1", "Sorry for the inconvenience");
    EventPerformanceController controller = new EventPerformanceController(view, new ArrayList<>(),
        performances, new MockPaymentSystem());
    controller.setCurrentUser(provider);

    controller.cancelPerformance();

    assertEquals("SUCCESS: Cancellation Successful!", view.getLastSuccessMessage(),
        "Cancellation with active bookings should succeed after refunds.");
  }

  @Test
  void emptyCancellationMessageIsRejectedAndRetried() {
    Booking activeBooking = new Booking(1L, 2, 30.0, LocalDateTime.now(), BookingStatus.ACTIVE,
        student, futurePerformance);
    futurePerformance.addBooking(activeBooking);

    ScriptedView view = new ScriptedView("1", "", "Sorry for the inconvenience");
    EventPerformanceController controller = new EventPerformanceController(view, new ArrayList<>(),
        performances, new MockPaymentSystem());
    controller.setCurrentUser(provider);

    controller.cancelPerformance();

    assertTrue(view.getErrorMessages().contains("ERROR: Cancellation message is required."),
        "Empty cancellation message should show an error.");
    assertEquals("SUCCESS: Cancellation Successful!", view.getLastSuccessMessage(),
        "Valid retry should allow performance cancellation to succeed.");
  }

  @Test
  void cancelPerformanceWithMixedBookingsRefundsOnlyActive() {
    Booking activeBooking = new Booking(1L, 2, 30.0, LocalDateTime.now(), BookingStatus.ACTIVE,
        student, futurePerformance);
    Booking cancelledBooking = new Booking(2L, 1, 15.0, LocalDateTime.now(),
        BookingStatus.CANCELLEDBYSTUDENT, student, futurePerformance);
    futurePerformance.addBooking(activeBooking);
    futurePerformance.addBooking(cancelledBooking);

    ScriptedView view = new ScriptedView("1", "Event cancelled");
    EventPerformanceController controller = new EventPerformanceController(view, new ArrayList<>(),
        performances, new MockPaymentSystem());
    controller.setCurrentUser(provider);

    controller.cancelPerformance();

    assertEquals("SUCCESS: Cancellation Successful!", view.getLastSuccessMessage(),
        "Cancellation should succeed even with a mix of active and cancelled bookings.");
  }

  // --- Access control ---

  @Test
  void onlyProvidersCanCancelPerformances() {
    ScriptedView view = new ScriptedView();
    EventPerformanceController controller = new EventPerformanceController(view, new ArrayList<>(),
        performances, new MockPaymentSystem());
    controller.setCurrentUser(student);

    controller.cancelPerformance();

    assertEquals("ERROR: Only entertainment providers can cancel performance.",
        view.getLastErrorMessage(), "Non-providers should be rejected.");
  }

  @Test
  void guestCannotCancelPerformance() {
    ScriptedView view = new ScriptedView();
    EventPerformanceController controller = new EventPerformanceController(view, new ArrayList<>(),
        performances, new MockPaymentSystem());

    controller.cancelPerformance();

    assertEquals("ERROR: Only entertainment providers can cancel performance.",
        view.getLastErrorMessage(), "Guest (no user) should be rejected.");
  }

  // --- Input validation ---

  @Test
  void invalidPerformanceIdFormatShowsError() {
    ScriptedView view = new ScriptedView("abc", "1");
    EventPerformanceController controller = new EventPerformanceController(view, new ArrayList<>(),
        performances, new MockPaymentSystem());
    controller.setCurrentUser(provider);

    controller.cancelPerformance();

    assertTrue(
        view.getErrorMessages().contains("ERROR: Performance ID must be a valid positive number."),
        "Non-numeric performance ID should show an error.");
  }

  // --- Business logic errors ---

  @Test
  void cancellingNonExistentPerformanceShowsError() {
    ScriptedView view = new ScriptedView("999", "1");
    EventPerformanceController controller = new EventPerformanceController(view, new ArrayList<>(),
        performances, new MockPaymentSystem());
    controller.setCurrentUser(provider);

    controller.cancelPerformance();

    assertTrue(view.getErrorMessages().contains("ERROR: Performance with given ID does not exist."),
        "Non-existent performance ID should show an error.");
  }

  @Test
  void providerCannotCancelAnotherProvidersPerformance() {
    EntertainmentProvider otherProvider =
        new EntertainmentProvider("other@example.com", "pass", "Other Org", "999", "Other", "Desc");
    Event otherEvent = new Event(2L, "Other Show", EventType.DANCE, true, otherProvider);
    LocalDateTime start = LocalDateTime.now().plusDays(7);
    Performance otherPerformance = new Performance(2L, start, start.plusHours(2), List.of("Dancer"),
        "Stage", 50, false, false, 50, 0, 10.0, PerformanceStatus.ACTIVE, otherEvent);
    performances.add(otherPerformance);

    ScriptedView view = new ScriptedView("2", "1");
    EventPerformanceController controller = new EventPerformanceController(view, new ArrayList<>(),
        performances, new MockPaymentSystem());
    controller.setCurrentUser(provider);

    controller.cancelPerformance();

    assertTrue(view.getErrorMessages().contains("ERROR: You can only cancel your own performance."),
        "Provider should not be able to cancel another provider's performance.");
  }

  @Test
  void providerCannotCancelPastPerformance() {
    LocalDateTime past = LocalDateTime.now().minusDays(1);
    Performance pastPerformance = new Performance(3L, past, past.plusHours(2), List.of("Band"),
        "Hall", 100, false, false, 100, 0, 15.0, PerformanceStatus.ACTIVE, event);
    performances.add(pastPerformance);

    ScriptedView view = new ScriptedView("3", "1");
    EventPerformanceController controller = new EventPerformanceController(view, new ArrayList<>(),
        performances, new MockPaymentSystem());
    controller.setCurrentUser(provider);

    controller.cancelPerformance();

    assertTrue(view.getErrorMessages().contains("ERROR: Performance has already happened"),
        "Provider should not be able to cancel a past performance.");
  }

  @Test
  void negativePerformanceIdShowsError() {
    ScriptedView view = new ScriptedView("-1", "1");
    EventPerformanceController controller = new EventPerformanceController(view, new ArrayList<>(),
        performances, new MockPaymentSystem());
    controller.setCurrentUser(provider);

    controller.cancelPerformance();

    assertTrue(
        view.getErrorMessages().contains("ERROR: Performance ID must be a valid positive number."),
        "Negative performance ID should show an error.");
  }

  @Test
  void refundFailurePreventsPerformanceCancellation() {
    Booking activeBooking = new Booking(1L, 2, 30.0, LocalDateTime.now(), BookingStatus.ACTIVE,
        student, futurePerformance);
    futurePerformance.addBooking(activeBooking);

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

    ScriptedView view = new ScriptedView("1", "Sorry");
    EventPerformanceController controller =
        new EventPerformanceController(view, new ArrayList<>(), performances, failingPayment);
    controller.setCurrentUser(provider);

    controller.cancelPerformance();

    assertTrue(view.getErrorMessages().stream().anyMatch(e -> e.contains("issue with a refund")),
        "Failed refund should prevent performance cancellation.");
    assertNull(view.getLastSuccessMessage(),
        "No success message should be shown when refund fails.");
  }

  @Test
  void missingBookingDetailsForRefundShowsError() {
    Performance performanceWithMissingRefundDetails = new Performance(4L,
        LocalDateTime.now().plusDays(7), LocalDateTime.now().plusDays(7).plusHours(2),
        List.of("Band"), "Hall", 100, false, false, 100, 0, 15.0, PerformanceStatus.ACTIVE, event) {
      @Override
      public boolean hasActiveBookings() {
        return true;
      }

      @Override
      public Collection<Booking> getActiveBookings() {
        return List.of(
            new Booking(10L, 1, 15.0, LocalDateTime.now(), BookingStatus.ACTIVE, student, this));
      }

      @Override
      public String getBookingDetailsForRefund() {
        return "";
      }
    };
    performances.add(performanceWithMissingRefundDetails);

    ScriptedView view = new ScriptedView("4", "Event cancelled");
    EventPerformanceController controller = new EventPerformanceController(view, new ArrayList<>(),
        performances, new MockPaymentSystem());
    controller.setCurrentUser(provider);

    controller.cancelPerformance();

    assertEquals("ERROR: There are no active booking details available for refund.",
        view.getLastErrorMessage(),
        "Missing refund details should block performance cancellation.");
  }

  @Test
  void inconsistentBookingDetailsForRefundShowError() {
    Performance performanceWithBadRefundDetails = new Performance(5L,
        LocalDateTime.now().plusDays(7), LocalDateTime.now().plusDays(7).plusHours(2),
        List.of("Band"), "Hall", 100, false, false, 100, 0, 15.0, PerformanceStatus.ACTIVE, event) {
      @Override
      public boolean hasActiveBookings() {
        return true;
      }

      @Override
      public Collection<Booking> getActiveBookings() {
        return List.of(
            new Booking(11L, 1, 15.0, LocalDateTime.now(), BookingStatus.ACTIVE, student, this));
      }

      @Override
      public String getBookingDetailsForRefund() {
        return "bad-refund-details";
      }
    };
    performances.add(performanceWithBadRefundDetails);

    ScriptedView view = new ScriptedView("5", "Event cancelled");
    EventPerformanceController controller = new EventPerformanceController(view, new ArrayList<>(),
        performances, new MockPaymentSystem());
    controller.setCurrentUser(provider);

    controller.cancelPerformance();

    assertEquals("ERROR: Active booking refund details are inconsistent.",
        view.getLastErrorMessage(),
        "Malformed refund details should block performance cancellation.");
  }

  @Test
  void performanceIdZeroShowsError() {
    ScriptedView view = new ScriptedView("0", "1");
    EventPerformanceController controller = new EventPerformanceController(view, new ArrayList<>(),
        performances, new MockPaymentSystem());
    controller.setCurrentUser(provider);

    controller.cancelPerformance();

    assertTrue(
        view.getErrorMessages().contains("ERROR: Performance ID must be a valid positive number."),
        "Performance ID 0 should show an error.");
  }

  // --- State verification ---

  @Test
  void cancelledPerformanceIsNoLongerActive() {
    ScriptedView view = new ScriptedView("1");
    EventPerformanceController controller = new EventPerformanceController(view, new ArrayList<>(),
        performances, new MockPaymentSystem());
    controller.setCurrentUser(provider);

    controller.cancelPerformance();

    assertFalse(futurePerformance.isActive(),
        "Performance should not be active after cancellation.");
  }

  @Test
  void allActiveBookingsAreCancelledByProviderAfterCancellation() {
    Student student2 =
        new Student("bob@ed.ac.uk", "pass", "Bob", 7654321, new StudentPreferences());
    Booking booking1 = new Booking(1L, 2, 30.0, LocalDateTime.now(), BookingStatus.ACTIVE, student,
        futurePerformance);
    Booking booking2 = new Booking(2L, 1, 15.0, LocalDateTime.now(), BookingStatus.ACTIVE, student2,
        futurePerformance);
    futurePerformance.addBooking(booking1);
    futurePerformance.addBooking(booking2);

    ScriptedView view = new ScriptedView("1", "Event cancelled");
    EventPerformanceController controller = new EventPerformanceController(view, new ArrayList<>(),
        performances, new MockPaymentSystem());
    controller.setCurrentUser(provider);

    controller.cancelPerformance();

    assertFalse(booking1.isActive(), "First booking should no longer be active.");
    assertFalse(booking2.isActive(), "Second booking should no longer be active.");
  }
}
