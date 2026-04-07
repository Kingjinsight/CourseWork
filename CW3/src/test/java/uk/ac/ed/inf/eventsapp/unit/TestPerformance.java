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
 * Unit-test scaffold for the Performance class.
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
    // Builds one ticketed and one non-ticketed fixture so event-linked behaviour can be reused
    // across tests.
    provider = new EntertainmentProvider("provider@gmail.com", "password", "EooEle", "1234567890",
        "Peter", "This is EooEle");
    ticketedEvent = new Event(1L, "Live Music", EventType.MUSIC, true, provider);
    nonTicketedEvent = new Event(2L, "Free Show", EventType.THEATRE, false, provider);

    performance = new Performance(1L, FUTURE_START, FUTURE_END, List.of("Band"), "Main Hall", 120,
        false, false, 100, 0, 15.0, PerformanceStatus.ACTIVE, ticketedEvent);
  }

  // Verifies that a newly created performance starts in the active state.
  @Test
  void newPerformanceSIsActive() {
    assertTrue(performance.isActive(), "Newly created perofrmance should be active.");
  }

  // Verifies that cancelling a performance changes it to an inactive state.
  @Test
  void cancelMakesPerformanceInactive() {
    performance.cancel();
    assertFalse(performance.isActive(), "Cancelled performance should not be active.");
  }

  // Verifies that ticketed performances report their linked event as ticketed.
  @Test
  void checkIfEventIsTicketedReturnsTrueForTicketedEvent() {
    assertTrue(performance.checkIfEventIsTicketed(),
        "Should return true when linked event is ticketed.");
  }

  // Verifies that non-ticketed performances report their linked event as non-ticketed.
  @Test
  void checkIfEventIsTicketedFalseForNonTicketedEvent() {
    Performance nonTicketed = new Performance(2L, FUTURE_START, FUTURE_END, List.of("Actor"),
        "Stage", 50, true, false, 0, 0, 0.0, PerformanceStatus.ACTIVE, nonTicketedEvent);
    assertFalse(nonTicketed.checkIfEventIsTicketed(),
        "Should return false when linked event is not ticketed");
  }

  // Verifies that performances without a linked event are treated as non-ticketed.
  @Test
  void checkIfEventIsTicketedReturnFalseWhenNoEventLinked() {
    Performance noEvent = new Performance(3L, FUTURE_START, FUTURE_END, List.of("Performer"),
        "Venue", 50, false, false, 10, 0, 5.0, PerformanceStatus.ACTIVE, null);
    assertFalse(noEvent.checkIfEventIsTicketed(), "Should return false when no event is linked.");
  }

  // Verifies that available tickets are reported correctly when stock remains.
  @Test
  void checkIfTicketLeftReturnTrueWhenTicketsAvailable() {
    assertTrue(performance.checkIfTicketsLeft(1), "Should return true when tickets are available.");
  }

  // Verifies that sold-out performances reject further ticket requests.
  @Test
  void checkIfTicketsLeftReturnsFalseWhenSoldOut() {
    Performance soldOut = new Performance(4L, FUTURE_START, FUTURE_END, List.of("Band"),
        "Main Hall", 100, false, false, 50, 50, 15.0, PerformanceStatus.ACTIVE, ticketedEvent);
    assertFalse(soldOut.checkIfTicketsLeft(1), "Should return false when all tickets are sold.");
  }

  // Verifies that requesting exactly the remaining number of tickets still succeeds.
  @Test
  void checkIfTicketsLeftReturnsTrueAtExactBoundary() {
    Performance almostSoldOut = new Performance(5L, FUTURE_START, FUTURE_END, List.of("Band"),
        "Main Hall", 100, false, false, 100, 99, 15.0, PerformanceStatus.ACTIVE, ticketedEvent);
    assertTrue(almostSoldOut.checkIfTicketsLeft(1),
        "Should return true when exactly the requested number of tickets remain.");
  }

  // Verifies that requesting more tickets than remain is rejected.
  @Test
  void checkIfTicketsLeftReturnsFalseWhenNotEnoughRemain() {
    Performance almostSoldOut = new Performance(5L, FUTURE_START, FUTURE_END, List.of("Band"),
        "Main Hall", 100, false, false, 100, 99, 15.0, PerformanceStatus.ACTIVE, ticketedEvent);
    assertFalse(almostSoldOut.checkIfTicketsLeft(2),
        "Should return false when fewer tickets remain than requested.");
  }


  // Verifies that adding sold tickets reduces later availability.
  @Test
  void addNumTicketsSoldReducesAvailableTickets() {
    performance.addNumTicketsSold(100);
    assertFalse(performance.checkIfTicketsLeft(1),
        "After selling all tickets, no tickets should remain.");
  }

  // Verifies that partial sales still leave tickets available.
  @Test
  void addNumTicketsSoldPartialStillHasTicketsLeft() {
    performance.addNumTicketsSold(50);
    assertTrue(performance.checkIfTicketsLeft(1),
        "After selling some tickets, remaining tickets should still be available.");
  }

  // Verifies that removing sold tickets restores future availability.
  @Test
  void removeNumTicketsSoldRestoresTicketAvailability() {
    performance.addNumTicketsSold(100);
    performance.removeNumTicketsSold(1);
    assertTrue(performance.checkIfTicketsLeft(1),
        "Removing sold tickets should restore ticket availability.");
  }

  // Verifies that removing sold tickets cannot drive the sold count below zero.
  @Test
  void removeNumTicketsSoldDoesNotGoBelowZero() {
    performance.removeNumTicketsSold(10);
    assertTrue(performance.checkIfTicketsLeft(100),
        "Removing sold tickets from zero sold tickets should not make sold tickets negative.");
  }


  // Verifies that the final ticket price matches the configured price.
  @Test
  void getFinalTicketPriceReturnsRightPrice() {
    assertEquals(15.0, performance.getFinalTicketPrice(),
        "Should return the ticket price set at right price");
  }

  // Verifies that future performances are reported as not having happened yet.
  @Test
  void checkHasNotHappenedYetReturnsTrueForFuturePerformance() {
    assertTrue(performance.checkHasNotHappenedYet(),
        "Should return true for a performance scheduled in the future");
  }

  // Verifies that past performances are reported as already happened.
  @Test
  void checkHasNotHappenedYetReturnsFalseForPastPerformance() {
    Performance past = new Performance(6L, PAST_START, PAST_END, List.of("Band"), "Main Hall", 120,
        false, false, 100, 0, 15.0, PerformanceStatus.ACTIVE, ticketedEvent);
    assertFalse(past.checkHasNotHappenedYet(),
        "Should return false for a performance that has already happened.");
  }

  // Verifies that organiser-email checks succeed for the owning provider.
  @Test
  void checkCreatedByEPReturnsTrueForMatchingEmail() {
    assertTrue(performance.checkCreatedByEP("provider@gmail.com"),
        "Should return true when the email matches the event organiser.");
  }

  // Verifies that organiser-email checks fail for a different provider email.
  @Test
  void checkCreatedByEPReturnsFalseForDifferentEmail() {
    assertFalse(performance.checkCreatedByEP("hi@gmail.com"),
        "Should return false when the email does not match the event organiser.");
  }

  // Verifies that performances with no bookings report no active bookings.
  @Test
  void hasActiveBookingReturnsFalseWhenNoBookings() {
    assertFalse(performance.hasActiveBookings(),
        "Should return false when no bookings have been added.");
  }

  // Verifies that cancelled bookings do not count as active bookings.
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

  // Verifies that adding an active booking changes the active-booking state.
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


  // Verifies that only active bookings are returned by the active-booking query.
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

  // Verifies that failed and provider-cancelled bookings are excluded from active bookings.
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

  // Verifies that no refund details are produced when no active bookings exist.
  @Test
  void getBookingDetailsForRefundReturnsEmptyStringWhenNoActiveBookings() {
    assertEquals("", performance.getBookingDetailsForRefund(),
        "No active bookings should produce no refund details.");
  }

  // Verifies that refund details are produced only for bookings that are still active.
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

  // Verifies that each active booking becomes a separate refund-detail line for later parsing.
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

  // Verifies that the organiser email is exposed through the performance.
  @Test
  void getOrganiserEmailReturnsProviderEmail() {
    assertEquals("provider@gmail.com", performance.getOrganiserEmail());
  }

  // Verifies that the linked event title is exposed through the performance.
  @Test
  void getEventTitleReturnsEventTitle() {
    assertEquals("Live Music", performance.getEventTitle());
  }

  // Verifies that no organiser email is returned when no event is linked.
  @Test
  void getOrganiserEmailReturnsNullWhenNoEventIsLinked() {
    Performance withoutEvent = new Performance(7L, FUTURE_START, FUTURE_END, List.of("Band"),
        "Venue", 50, false, false, 10, 0, 5.0, PerformanceStatus.ACTIVE, null);
    assertEquals(null, withoutEvent.getOrganiserEmail(),
        "No organiser email should be available when no event is linked.");
  }

  // Verifies that no event title is returned when no event is linked.
  @Test
  void getEventTitleReturnsNullWhenNoEventIsLinked() {
    Performance withoutEvent = new Performance(8L, FUTURE_START, FUTURE_END, List.of("Band"),
        "Venue", 50, false, false, 10, 0, 5.0, PerformanceStatus.ACTIVE, null);
    assertEquals(null, withoutEvent.getEventTitle(),
        "No event title should be available when no event is linked.");
  }

  // Verifies that ID matching succeeds for the performance's own ID.
  @Test
  void hasIDReturnsTrueForMatchingID() {
    assertTrue(performance.hasID(1L));
  }

  // Verifies that ID matching fails for a different performance ID.
  @Test
  void hasIDReturnsFalseForDifferentID() {
    assertFalse(performance.hasID(999L));
  }

  // Verifies that event membership succeeds for the linked event ID.
  @Test
  void belongsToEventReturnsTrueForMatchingEventID() {
    assertTrue(performance.belongsToEvent(1L),
        "The performance should report that it belongs to its linked event.");
  }

  // Verifies that event membership fails for a different event ID.
  @Test
  void belongsToEventReturnsFalseForDifferentEventID() {
    assertFalse(performance.belongsToEvent(999L),
        "The performance should not match a different event ID.");
  }

  // Verifies that summary output contains the key fields needed by search results.
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

  // Verifies that detailed output contains the key fields needed by the view-performance flow.
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

  // Verifies that detailed output distinguishes between ticketed and non-ticketed performances.
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
