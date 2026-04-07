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
import uk.ac.ed.inf.eventsapp.controller.EventPerformanceController;
import uk.ac.ed.inf.eventsapp.model.EntertainmentProvider;
import uk.ac.ed.inf.eventsapp.model.Event;
import uk.ac.ed.inf.eventsapp.model.EventType;
import uk.ac.ed.inf.eventsapp.model.Performance;
import uk.ac.ed.inf.eventsapp.model.Student;
import uk.ac.ed.inf.eventsapp.model.StudentPreferences;

/**
 * System tests for the view-performance use case.
 */
public class ViewPerformanceSystemTests {
  private Collection<Event> events;
  private Collection<Performance> performances;
  private EntertainmentProvider provider;

  @BeforeEach
  @SuppressWarnings("unused")
  void setUp() {
    // Resets the shared event and performance collections before each view-performance scenario.
    events = new ArrayList<>();
    performances = new ArrayList<>();
    provider = new EntertainmentProvider("provider@example.com", "encrypted_password",
        "Festival Org", "BN-1234567", "Provider Rep", "Runs live events");
  }

  // Verifies that a logged-in user can view the detailed information for a valid performance ID.
  @Test
  void loggedInUserCanViewDetailedPerformanceInformation() {
    events.add(createEventWithPerformance(1L, "Spring Concert", EventType.MUSIC,
        LocalDateTime.of(2026, 5, 10, 19, 0), LocalDateTime.of(2026, 5, 10, 21, 0), "McEwan Hall"));

    ScriptedView view = new ScriptedView("1");
    EventPerformanceController controller =
        new EventPerformanceController(view, events, performances, new MockPaymentSystem());
    controller.setCurrentUser(
        new Student("student@ed.ac.uk", "password", "Hagan", 123456789, new StudentPreferences()));

    controller.viewPerformance();

    String displayedPerformance = view.getLastDisplayedPerformance();
    assertNotNull(displayedPerformance);
    assertTrue(displayedPerformance.contains("Performance ID: 1"));
    assertTrue(displayedPerformance.contains("Event: Spring Concert"));
    assertTrue(displayedPerformance.contains("Time: 2026-05-10 19:00 to 2026-05-10 21:00"));
    assertTrue(displayedPerformance.contains("Venue: McEwan Hall"));
    assertTrue(displayedPerformance.contains("Venue details: capacity 300, Indoors, Non-smoking"));
    assertTrue(displayedPerformance.contains("Performers: Performer"));
    assertTrue(displayedPerformance.contains("Provider: Festival Org"));
    assertTrue(displayedPerformance.contains("Ticketing: Ticketed"));
    assertTrue(
        displayedPerformance.contains("Ticket details: Price: 12.50 | Tickets remaining: 150"));
    assertTrue(displayedPerformance.contains("Status: ACTIVE"));
    assertTrue(view.getErrorMessages().isEmpty());
  }

  // Verifies that unauthenticated users cannot view performance details.
  @Test
  void guestCannotViewPerformanceDetails() {
    events.add(createEventWithPerformance(1L, "Spring Concert", EventType.MUSIC,
        LocalDateTime.of(2026, 5, 10, 19, 0), LocalDateTime.of(2026, 5, 10, 21, 0), "McEwan Hall"));

    ScriptedView view = new ScriptedView("1");
    EventPerformanceController controller =
        new EventPerformanceController(view, events, performances, new MockPaymentSystem());

    controller.viewPerformance();

    assertEquals("ERROR: Only logged-in users can view performances.", view.getLastErrorMessage());
    assertEquals(null, view.getLastDisplayedPerformance());
  }

  // Verifies that invalid performance-ID input shows an error before a successful retry.
  @Test
  void invalidPerformanceIdShowsAnErrorMessage() {
    events.add(createEventWithPerformance(1L, "Spring Concert", EventType.MUSIC,
        LocalDateTime.of(2026, 5, 10, 19, 0), LocalDateTime.of(2026, 5, 10, 21, 0), "McEwan Hall"));

    ScriptedView view = new ScriptedView("abc", "1");
    EventPerformanceController controller =
        new EventPerformanceController(view, events, performances, new MockPaymentSystem());
    controller.setCurrentUser(
        new Student("student@ed.ac.uk", "password", "Hagan", 123456789, new StudentPreferences()));

    controller.viewPerformance();

    assertEquals("ERROR: Performance ID must be a valid positive whole number.",
        view.getLastErrorMessage());
    assertNotNull(view.getLastDisplayedPerformance());
  }

  // Verifies that an unknown performance ID shows an error before a successful retry.
  @Test
  void unknownPerformanceIdShowsAnErrorMessage() {
    events.add(createEventWithPerformance(1L, "Spring Concert", EventType.MUSIC,
        LocalDateTime.of(2026, 5, 10, 19, 0), LocalDateTime.of(2026, 5, 10, 21, 0), "McEwan Hall"));

    ScriptedView view = new ScriptedView("99", "1");
    EventPerformanceController controller =
        new EventPerformanceController(view, events, performances, new MockPaymentSystem());
    controller.setCurrentUser(
        new Student("student@ed.ac.uk", "password", "Hagan", 123456789, new StudentPreferences()));

    controller.viewPerformance();

    assertEquals("ERROR: Performance not found.", view.getLastErrorMessage());
    assertNotNull(view.getLastDisplayedPerformance());
  }

  private Event createEventWithPerformance(long eventId, String title, EventType type,
      LocalDateTime startDateTime, LocalDateTime endDateTime, String venue) {
    Event event = new Event(eventId, title, type, true, provider);
    event.createPerformance(eventId, startDateTime, endDateTime, List.of("Performer"), venue, 300,
        false, false, 150, 12.50);
    performances.add(event.getPerformanceByID(eventId));
    return event;
  }
}
