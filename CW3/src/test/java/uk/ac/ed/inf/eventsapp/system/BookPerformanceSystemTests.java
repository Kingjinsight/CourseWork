package uk.ac.ed.inf.eventsapp.system;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import external.MockPaymentSystem;
import external.PaymentSystem;
import uk.ac.ed.inf.eventsapp.controller.BookingController;
import uk.ac.ed.inf.eventsapp.model.Booking;
import uk.ac.ed.inf.eventsapp.model.EntertainmentProvider;
import uk.ac.ed.inf.eventsapp.model.Event;
import uk.ac.ed.inf.eventsapp.model.EventType;
import uk.ac.ed.inf.eventsapp.model.Performance;
import uk.ac.ed.inf.eventsapp.model.PerformanceStatus;
import uk.ac.ed.inf.eventsapp.model.Student;
import uk.ac.ed.inf.eventsapp.model.StudentPreferences;

public class BookPerformanceSystemTests {
  private EntertainmentProvider provider;
  private Student student;
  private Event ticketedEvent;
  private Event nonTicketedEvent;
  private Performance futurePerformance;
  private Performance nonTicketedPerformance;
  private Collection<Performance> performances;
  private Collection<Booking> bookings;

  @BeforeEach
  @SuppressWarnings("unused")
  void setUp() {
    // Builds one ticketed and one non-ticketed performance for the booking scenarios.
    provider = new EntertainmentProvider("provider@gmail.com", "password", "EooEle", "123",
        "Provider", "This is EooEle");
    student =
        new Student("student@ed.ac.uk", "password", "Hagan", 1234567, new StudentPreferences());

    LocalDateTime start = LocalDateTime.now().plusDays(7);
    ticketedEvent = new Event(1L, "Live Music", EventType.MUSIC, true, provider);
    nonTicketedEvent = new Event(2L, "Free Show", EventType.THEATRE, false, provider);

    futurePerformance = new Performance(1L, start, start.plusHours(2), List.of("Band"), "Hall", 100,
        false, false, 100, 0, 15.0, PerformanceStatus.ACTIVE, ticketedEvent);
    nonTicketedPerformance = new Performance(2L, start, start.plusHours(2), List.of("Actor"),
        "Stage", 50, false, false, 0, 0, 0.0, PerformanceStatus.ACTIVE, nonTicketedEvent);

    performances = new ArrayList<>();
    performances.add(futurePerformance);
    performances.add(nonTicketedPerformance);
    bookings = new ArrayList<>();
  }

  // Verifies that a student can book an available ticketed performance.
  @Test
  void studentCanBookAvailablePerformance() {
    ScriptedView view = new ScriptedView("1", "2");
    BookingController controller = new BookingController(view, new MockPaymentSystem(),
        new ArrayList<>(), performances, bookings);
    controller.setCurrentUser(student);

    controller.bookPerformance();

    assertEquals("SUCCESS: Booking successful", view.getLastSuccessMessage(),
        "Student should receive a success message after booking.");
    assertNotNull(view.getLastDisplayedBookingRecord(),
        "A booking record should be displayed after successful booking.");
  }

  // Verifies that booking exactly the remaining ticket count succeeds.
  @Test
  void bookingExactlyAllRemainingTickets() {
    ScriptedView view = new ScriptedView("1", "100");
    BookingController controller = new BookingController(view, new MockPaymentSystem(),
        new ArrayList<>(), performances, bookings);
    controller.setCurrentUser(student);

    controller.bookPerformance();

    assertEquals("SUCCESS: Booking successful", view.getLastSuccessMessage(),
        "Booking exactly all remaining tickets should succeed.");
  }

  // Verifies that authenticated non-students cannot book performances.
  @Test
  void onlyStudentsCanBookPerformances() {
    ScriptedView view = new ScriptedView();
    BookingController controller = new BookingController(view, new MockPaymentSystem(),
        new ArrayList<>(), performances, bookings);
    controller.setCurrentUser(provider);

    controller.bookPerformance();

    assertEquals("ERROR: Only students can book performances.", view.getLastErrorMessage(),
        "Non-students should be rejected.");
  }

  // Verifies that unauthenticated users cannot book performances.
  @Test
  void guestCannotBookPerformance() {
    ScriptedView view = new ScriptedView();
    BookingController controller = new BookingController(view, new MockPaymentSystem(),
        new ArrayList<>(), performances, bookings);

    controller.bookPerformance();

    assertEquals("ERROR: Only students can book performances.", view.getLastErrorMessage(),
        "Guest (no user) should be rejected.");
  }

  // Verifies that invalid performance-ID input shows an error before a successful retry.
  @Test
  void invalidPerformanceIdFormatShowsError() {
    ScriptedView view = new ScriptedView("abc", "1", "1");
    BookingController controller = new BookingController(view, new MockPaymentSystem(),
        new ArrayList<>(), performances, bookings);
    controller.setCurrentUser(student);

    controller.bookPerformance();

    assertTrue(view.getErrorMessages().contains("ERROR: Invalid performance ID"),
        "Non-numeric performance ID should show an error.");
  }

  // Verifies that invalid ticket-count input shows an error before a successful retry.
  @Test
  void invalidTicketCountFormatShowsError() {
    ScriptedView view = new ScriptedView("1", "abc", "1");
    BookingController controller = new BookingController(view, new MockPaymentSystem(),
        new ArrayList<>(), performances, bookings);
    controller.setCurrentUser(student);

    controller.bookPerformance();

    assertTrue(view.getErrorMessages().contains("ERROR: Invalid number of tickets"),
        "Non-numeric ticket count should show an error.");
  }

  // Verifies that an unknown performance ID shows an error before a successful retry.
  @Test
  void bookingWithNonExistentPerformanceIdShowsError() {
    ScriptedView view = new ScriptedView("999", "1", "1");
    BookingController controller = new BookingController(view, new MockPaymentSystem(),
        new ArrayList<>(), performances, bookings);
    controller.setCurrentUser(student);

    controller.bookPerformance();

    assertTrue(
        view.getErrorMessages().contains("ERROR: Performance with given number does not exist."),
        "Non-existent performance ID should show an error.");
  }

  // Verifies that non-ticketed performances cannot be booked.
  @Test
  void bookingNonTicketedPerformanceShowsError() {
    ScriptedView view = new ScriptedView("2");
    BookingController controller = new BookingController(view, new MockPaymentSystem(),
        new ArrayList<>(), performances, bookings);
    controller.setCurrentUser(student);

    controller.bookPerformance();

    assertEquals(
        "ERROR: The requested performance's event is not ticketed. There is no need to book it.",
        view.getLastErrorMessage(), "Booking a non-ticketed performance should show an error.");
  }

  // Verifies that requesting too many tickets for a nearly sold-out performance shows an error
  // before retrying.
  @Test
  void bookingSoldOutPerformanceShowsError() {
    LocalDateTime start = LocalDateTime.now().plusDays(7);
    Performance soldOut = new Performance(3L, start, start.plusHours(2), List.of("Band"), "Hall",
        50, false, false, 50, 49, 15.0, PerformanceStatus.ACTIVE, ticketedEvent);
    performances.add(soldOut);

    ScriptedView view = new ScriptedView("3", "2", "1");
    BookingController controller = new BookingController(view, new MockPaymentSystem(),
        new ArrayList<>(), performances, bookings);
    controller.setCurrentUser(student);

    controller.bookPerformance();

    assertTrue(view.getErrorMessages().contains("ERROR: Requested performance has no tickets left"),
        "Requesting more tickets than remain should show the no-tickets-left error.");
    assertEquals("SUCCESS: Booking successful", view.getLastSuccessMessage(),
        "Student should be able to retry with a smaller valid ticket count.");
  }

  // Verifies that requesting more tickets than remain produces the expected error.
  @Test
  void bookingMoreTicketsThanAvailableShowsError() {
    LocalDateTime start = LocalDateTime.now().plusDays(7);
    Performance fewTickets = new Performance(4L, start, start.plusHours(2), List.of("Band"), "Hall",
        50, false, false, 10, 0, 15.0, PerformanceStatus.ACTIVE, ticketedEvent);
    performances.add(fewTickets);

    ScriptedView view = new ScriptedView("4", "20", "1");
    BookingController controller = new BookingController(view, new MockPaymentSystem(),
        new ArrayList<>(), performances, bookings);
    controller.setCurrentUser(student);

    controller.bookPerformance();

    assertTrue(view.getErrorMessages().contains("ERROR: Requested performance has no tickets left"),
        "Requesting more tickets than available should show an error.");
  }

  // Verifies that a payment failure leaves the booking flow in an error state.
  @Test
  void paymentFailureCancelsBooking() {
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

    ScriptedView view = new ScriptedView("1", "1");
    BookingController controller =
        new BookingController(view, failingPayment, new ArrayList<>(), performances, bookings);
    controller.setCurrentUser(student);

    controller.bookPerformance();

    assertEquals("ERROR: There was an issue with payment.", view.getLastErrorMessage(),
        "Payment failure should display an error.");
  }

  // Verifies that a successful booking is stored in the shared bookings collection.
  @Test
  void successfulBookingAddsToBookingsCollection() {
    ScriptedView view = new ScriptedView("1", "2");
    BookingController controller = new BookingController(view, new MockPaymentSystem(),
        new ArrayList<>(), performances, bookings);
    controller.setCurrentUser(student);

    controller.bookPerformance();

    assertEquals(1, bookings.size(),
        "Booking should be added to the bookings collection after success.");
  }

  // Verifies that the displayed booking record contains the expected student and event details.
  @Test
  void bookingRecordContainsStudentAndEventDetails() {
    ScriptedView view = new ScriptedView("1", "2");
    BookingController controller = new BookingController(view, new MockPaymentSystem(),
        new ArrayList<>(), performances, bookings);
    controller.setCurrentUser(student);

    controller.bookPerformance();

    String record = view.getLastDisplayedBookingRecord();
    assertNotNull(record, "Booking record should be displayed.");
    assertTrue(record.contains("Hagan"), "Booking record should contain student name.");
    assertTrue(record.contains("Live Music"), "Booking record should contain event title.");
  }

  // Verifies that booking a single ticket succeeds for an available performance.
  @Test
  void bookingSingleTicketSucceeds() {
    ScriptedView view = new ScriptedView("1", "1");
    BookingController controller = new BookingController(view, new MockPaymentSystem(),
        new ArrayList<>(), performances, bookings);
    controller.setCurrentUser(student);

    controller.bookPerformance();

    assertEquals("SUCCESS: Booking successful", view.getLastSuccessMessage(),
        "Booking a single ticket should succeed.");
  }
}
