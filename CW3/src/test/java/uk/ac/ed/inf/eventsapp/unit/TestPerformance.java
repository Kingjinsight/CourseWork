package uk.ac.ed.inf.eventsapp.unit;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uk.ac.ed.inf.eventsapp.model.Booking;
import uk.ac.ed.inf.eventsapp.model.BookingStatus;
import uk.ac.ed.inf.eventsapp.model.EntertainmentProvider;
import uk.ac.ed.inf.eventsapp.model.Event;
import uk.ac.ed.inf.eventsapp.model.EventType;
import uk.ac.ed.inf.eventsapp.model.Performance;
import uk.ac.ed.inf.eventsapp.model.PerformanceStatus;
import uk.ac.ed.inf.eventsapp.model.Student;
import uk.ac.ed.inf.eventsapp.model.StudentPreferences;

/**
 * Unit-test scaffold for the UML Performance class.
 */
public class TestPerformance {
  private static final LocalDateTime FUTURE_START = LocalDateTime.now().plusDays(30);
  private static final LocalDateTime FUTURE_END = FUTURE_START.plusHours(2);
  private static final LocalDateTime PAST_START = LocalDateTime.now().minusDays(1);
  private static final LocalDateTime PAST_END = PAST_START.plusHours(2);

  private EntertainmentProvider provider;
  private Event ticketedEvent;
  private Event nonTicketedEvent;
  private Performance performance;

  @BeforeEach
  @SuppressWarnings("unused")
  void setUp() {
    provider = new EntertainmentProvider("provider@gmail.com", "password", "EooEle", "1234567890",
        "Peter", "This is EooEle");
    ticketedEvent = new Event(1L, "Live Music", EventType.MUSIC, true, provider);
    nonTicketedEvent = new Event(2L, "Free Show", EventType.THEATRE, false, provider);

    performance = new Performance(1L, FUTURE_START, FUTURE_END, List.of("Band"), "Main Hall", 120,
        false, false, 100, 0, 15.0, PerformanceStatus.ACTIVE, ticketedEvent);
  }

  // --- isActive()/cancel() ---
  @Test
  void newPerformanceSIsActive() {
    assertTrue(performance.isActive(), "Newly created perofrmance should be active.");
  }

  @Test
  void cancelMakesPerformanceInactive() {
    performance.cancel();
    assertFalse(performance.isActive(), "Cancelled performance should not be active.");
  }

  // --- checkIfEventIsTicketed() ---
  @Test
  void checkIfEventIsTicketedReturnsTrueForTicketedEvent() {
    assertTrue(performance.checkIfEventIsTicketed(),
        "Should return true when linked event is ticketed.");
  }

  @Test
  void checkIfEventIsTicketedFalseForNonTicketedEvent() {
    Performance nonTicketed = new Performance(2L, FUTURE_START, FUTURE_END, List.of("Actor"),
        "Stage", 50, true, false, 0, 0, 0.0, PerformanceStatus.ACTIVE, nonTicketedEvent);
    assertFalse(nonTicketed.checkIfEventIsTicketed(),
        "Should return false when linked event is not ticketed");
  }

  @Test
  void checkIfEventIsTicketedReturnFalseWhenNoEventLinked() {
    Performance noEvent = new Performance(3L, FUTURE_START, FUTURE_END, List.of("Performer"),
        "Venue", 50, false, false, 10, 0, 5.0, PerformanceStatus.ACTIVE, null);
    assertFalse(noEvent.checkIfEventIsTicketed(), "Should return false when no event is linked.");
  }

  // --- checkIfTicketLeft() ---
  @Test
  void checkIfTicketLeftReturnTrueWhenTicketsAvailable() {
    assertTrue(performance.checkIfTicketsLeft(1), "Should return true when tickets are available.");
  }

  @Test
  void checkIfTicketsLeftReturnsFalseWhenSoldOut() {
    Performance soldOut = new Performance(4L, FUTURE_START, FUTURE_END, List.of("Band"),
        "Main Hall", 100, false, false, 50, 50, 15.0, PerformanceStatus.ACTIVE, ticketedEvent);
    assertFalse(soldOut.checkIfTicketsLeft(1), "Should return false when all tickets are sold.");
  }

  @Test
  void checkIfTicketsLeftReturnsTrueAtExactBoundary() {
    // 100 total, 99 sold — exactly 1 left
    Performance almostSoldOut = new Performance(5L, FUTURE_START, FUTURE_END, List.of("Band"),
        "Main Hall", 100, false, false, 100, 99, 15.0, PerformanceStatus.ACTIVE, ticketedEvent);
    assertTrue(almostSoldOut.checkIfTicketsLeft(1),
        "Should return true when exactly the requested number of tickets remain.");
  }

  @Test
  void checkIfTicketsLeftReturnsFalseWhenNotEnoughRemain() {
    // 100 total, 99 sold — 1 left, but requesting 2
    Performance almostSoldOut = new Performance(5L, FUTURE_START, FUTURE_END, List.of("Band"),
        "Main Hall", 100, false, false, 100, 99, 15.0, PerformanceStatus.ACTIVE, ticketedEvent);
    assertFalse(almostSoldOut.checkIfTicketsLeft(2),
        "Should return false when fewer tickets remain than requested.");
  }


  // --- addNumTicketSold
  @Test
  void addNumTicketsSoldReducesAvailableTickets() {
    performance.addNumTicketsSold(100);
    assertFalse(performance.checkIfTicketsLeft(1),
        "After selling all tickets, no tickets should remain.");
  }

  @Test
  void addNumTicketsSoldPartialStillHasTicketsLeft() {
    performance.addNumTicketsSold(50);
    assertTrue(performance.checkIfTicketsLeft(1),
        "After selling some tickets, remaining tickets should still be available.");
  }

  @Test
  void removeNumTicketsSoldRestoresTicketAvailability() {
    performance.addNumTicketsSold(100);
    performance.removeNumTicketsSold(1);
    assertTrue(performance.checkIfTicketsLeft(1),
        "Removing sold tickets should restore ticket availability.");
  }

  @Test
  void removeNumTicketsSoldDoesNotGoBelowZero() {
    performance.removeNumTicketsSold(10);
    assertTrue(performance.checkIfTicketsLeft(100),
        "Removing sold tickets from zero sold tickets should not make sold tickets negative.");
  }


  // --- getFinalTicketPrice()
  @Test
  void getFinalTicketPriceReturnsRightPrice() {
    assertEquals(15.0, performance.getFinalTicketPrice(),
        "Should return the ticket price set at right price");
  }

  // --- checkHasNotHappenedYet()
  @Test
  void checkHasNotHappenedYetReturnsTrueForFuturePerformance() {
    assertTrue(performance.checkHasNotHappenedYet(),
        "Should return true for a performance scheduled in the future");
  }

  @Test
  void checkHasNotHappenedYetReturnsFalseForPastPerformance() {
    Performance past = new Performance(6L, PAST_START, PAST_END, List.of("Band"), "Main Hall", 120,
        false, false, 100, 0, 15.0, PerformanceStatus.ACTIVE, ticketedEvent);
    assertFalse(past.checkHasNotHappenedYet(),
        "Should return false for a performance that has already happened.");
  }

  // --- checkCreatedByEP()
  @Test
  void checkCreatedByEPReturnsTrueForMatchingEmail() {
    assertTrue(performance.checkCreatedByEP("provider@gmail.com"),
        "Should return true when the email matches the event organiser.");
  }

  @Test
  void checkCreatedByEPReturnsFalseForDifferentEmail() {
    assertFalse(performance.checkCreatedByEP("hi@gmail.com"),
        "Should return false when the email does not match the event organiser.");
  }

  // --- hasActiveBookings() / getActiveBookings()
  @Test
  void hasActiveBookingReturnsFalseWhenNoBookings() {
    assertFalse(performance.hasActiveBookings(),
        "Should return false when no bookings have been added.");
  }

  @Test
  void hasActiveBookingsReturnsFalseWhenOnlyCancelledBookings() {
    Student student =
        new Student("student@ed.ac.uk", "password", "Hagan", 1234567, new StudentPreferences());
    Booking booking = new Booking(2L, 2, 30.0, LocalDateTime.now(),
        BookingStatus.CANCELLEDBYSTUDENT, student, performance);
    performance.addBooking(booking);
    assertFalse(performance.hasActiveBookings(),
        "Should return false when all bookings are cancelled.");
  }

  @Test
  void hasActiveBookingsReturnsTrueAfterActiveBookingAdded() {
    Student student =
        new Student("student@ed.ac.uk", "password", "Hagan", 1234567, new StudentPreferences());
    Booking booking =
        new Booking(1L, 2, 30.0, LocalDateTime.now(), BookingStatus.ACTIVE, student, performance);
    performance.addBooking(booking);
    assertTrue(performance.hasActiveBookings(),
        "Should return true after an active booking is added.");
  }


  @Test
  void getActiveBookingsReturnsOnlyActiveBookings() {
    Student student =
        new Student("student@ed.ac.uk", "password", "Hagan", 1234567, new StudentPreferences());
    Booking active =
        new Booking(1L, 1, 15.0, LocalDateTime.now(), BookingStatus.ACTIVE, student, performance);
    Booking cancelled = new Booking(2L, 1, 15.0, LocalDateTime.now(),
        BookingStatus.CANCELLEDBYSTUDENT, student, performance);
    performance.addBooking(active);
    performance.addBooking(cancelled);
    assertEquals(1, performance.getActiveBookings().size(),
        "Should return only the one active booking, not the cancelled one.");
  }

  @Test
  void getActiveBookingsExcludesPaymentFailedAndProviderCancelledBookings() {
    Student student =
        new Student("student@ed.ac.uk", "password", "Hagan", 1234567, new StudentPreferences());
    Booking active =
        new Booking(1L, 1, 15.0, LocalDateTime.now(), BookingStatus.ACTIVE, student, performance);
    Booking paymentFailed = new Booking(2L, 1, 15.0, LocalDateTime.now(),
        BookingStatus.PAYMENTFAILED, student, performance);
    Booking providerCancelled = new Booking(3L, 1, 15.0, LocalDateTime.now(),
        BookingStatus.CANCELLEDBYPROVIDER, student, performance);
    performance.addBooking(active);
    performance.addBooking(paymentFailed);
    performance.addBooking(providerCancelled);

    assertEquals(1, performance.getActiveBookings().size(),
        "Only bookings with ACTIVE status should be returned.");
  }

  @Test
  void getBookingDetailsForRefundReturnsEmptyStringWhenNoActiveBookings() {
    assertEquals("", performance.getBookingDetailsForRefund(),
        "No active bookings should produce no refund details.");
  }

  @Test
  void getBookingDetailsForRefundIncludesOnlyActiveBookings() {
    Student student =
        new Student("student@ed.ac.uk", "password", "Hagan", 1234567, new StudentPreferences());
    Booking active =
        new Booking(1L, 2, 30.0, LocalDateTime.now(), BookingStatus.ACTIVE, student, performance);
    Booking cancelled = new Booking(2L, 1, 15.0, LocalDateTime.now(),
        BookingStatus.CANCELLEDBYSTUDENT, student, performance);
    performance.addBooking(active);
    performance.addBooking(cancelled);

    String refundDetails = performance.getBookingDetailsForRefund();

    assertTrue(refundDetails.contains("2;30.0;Hagan|student@ed.ac.uk|1234567"),
        "Refund details should include the active booking details in the expected serialised format.");
    assertFalse(refundDetails.contains("1;15.0"),
        "Refund details should exclude cancelled bookings.");
  }

  @Test
  void getBookingDetailsForRefundSeparatesMultipleActiveBookingsByLine() {
    Student firstStudent =
        new Student("first@ed.ac.uk", "password", "First", 1111111, new StudentPreferences());
    Student secondStudent =
        new Student("second@ed.ac.uk", "password", "Second", 2222222, new StudentPreferences());
    Booking first = new Booking(1L, 1, 15.0, LocalDateTime.now(), BookingStatus.ACTIVE,
        firstStudent, performance);
    Booking second = new Booking(2L, 2, 30.0, LocalDateTime.now(), BookingStatus.ACTIVE,
        secondStudent, performance);
    performance.addBooking(first);
    performance.addBooking(second);

    String refundDetails = performance.getBookingDetailsForRefund();

    assertTrue(refundDetails.contains(System.lineSeparator()),
        "Multiple active bookings should be serialised onto separate lines.");
  }

  @Test
  void getOrganiserEmailReturnsProviderEmail() {
    assertEquals("provider@gmail.com", performance.getOrganiserEmail());
  }

  @Test
  void getEventTitleReturnsEventTitle() {
    assertEquals("Live Music", performance.getEventTitle());
  }

  @Test
  void getOrganiserEmailReturnsNullWhenNoEventIsLinked() {
    Performance withoutEvent = new Performance(7L, FUTURE_START, FUTURE_END, List.of("Band"),
        "Venue", 50, false, false, 10, 0, 5.0, PerformanceStatus.ACTIVE, null);
    assertEquals(null, withoutEvent.getOrganiserEmail(),
        "No organiser email should be available when no event is linked.");
  }

  @Test
  void getEventTitleReturnsNullWhenNoEventIsLinked() {
    Performance withoutEvent = new Performance(8L, FUTURE_START, FUTURE_END, List.of("Band"),
        "Venue", 50, false, false, 10, 0, 5.0, PerformanceStatus.ACTIVE, null);
    assertEquals(null, withoutEvent.getEventTitle(),
        "No event title should be available when no event is linked.");
  }

  @Test
  void hasIDReturnsTrueForMatchingID() {
    assertTrue(performance.hasID(1L));
  }

  @Test
  void hasIDReturnsFalseForDifferentID() {
    assertFalse(performance.hasID(999L));
  }

  @Test
  void belongsToEventReturnsTrueForMatchingEventID() {
    assertTrue(performance.belongsToEvent(1L),
        "The performance should report that it belongs to its linked event.");
  }

  @Test
  void belongsToEventReturnsFalseForDifferentEventID() {
    assertFalse(performance.belongsToEvent(999L),
        "The performance should not match a different event ID.");
  }

  @Test
  void summaryToStringContainsKeySearchFields() {
    String summary = performance.toString(false);

    assertTrue(summary.contains("Performance ID: 1"),
        "Summary output should include the performance ID.");
    assertTrue(summary.contains("Event: Live Music"),
        "Summary output should include the event title.");
    assertTrue(summary.contains("Venue: Main Hall"), "Summary output should include the venue.");
    assertTrue(summary.contains("Provider: EooEle"),
        "Summary output should include the provider display name.");
  }

  @Test
  void detailedToStringContainsKeyDetailFields() {
    String details = performance.toString(true);

    assertTrue(details.contains("Venue details: capacity 120, Indoors, Non-smoking"),
        "Detailed output should include venue details.");
    assertTrue(details.contains("Ticket details: Price: 15.00 | Tickets remaining: 100"),
        "Detailed output should include ticket details.");
    assertTrue(details.contains("Status: ACTIVE"),
        "Detailed output should include the performance status.");
  }

  @Test
  void detailedToStringForNonTicketedPerformanceShowsNoTicketsRequired() {
    Performance nonTicketed = new Performance(9L, FUTURE_START, FUTURE_END, List.of("Actor"),
        "Stage", 50, true, false, 0, 0, 0.0, PerformanceStatus.ACTIVE, nonTicketedEvent);

    String details = nonTicketed.toString(true);

    assertTrue(details.contains("Ticketing: Non-ticketed"),
        "Detailed output should identify a non-ticketed performance.");
    assertTrue(details.contains("Ticket details: No tickets required"),
        "Detailed output should state that no tickets are required for non-ticketed performances.");
  }

}
