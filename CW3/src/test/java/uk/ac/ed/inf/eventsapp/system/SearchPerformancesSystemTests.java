package uk.ac.ed.inf.eventsapp.system;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * System tests for the search-performances use case.
 */
public class SearchPerformancesSystemTests {
  private Collection<Event> events;
  private Collection<Performance> performances;
  private EntertainmentProvider provider;

  @BeforeEach
  @SuppressWarnings("unused")
  void setUp() {
    events = new ArrayList<>();
    performances = new ArrayList<>();
    provider = new EntertainmentProvider("provider@example.com", "encrypted_password",
        "Festival Org", "BN-1234567", "Provider Rep", "Runs live events");
  }

  @Test
  void loggedInUserCanSeePerformancesOnTheRequestedDate() {
    Event musicEvent = createEventWithPerformance(1L, "Spring Concert", EventType.MUSIC,
        LocalDateTime.of(2026, 5, 10, 19, 0), LocalDateTime.of(2026, 5, 10, 21, 0), "McEwan Hall");
    Event theatreEvent = createEventWithPerformance(2L, "Drama", EventType.THEATRE,
        LocalDateTime.of(2026, 5, 10, 18, 30), LocalDateTime.of(2026, 5, 10, 20, 0), "Assembly");
    Event theatreEvent2 = createEventWithPerformance(3L, "ABC", EventType.THEATRE,
        LocalDateTime.of(2026, 5, 11, 18, 30), LocalDateTime.of(2026, 5, 11, 20, 0), "Assembly");
    events.add(musicEvent);
    events.add(theatreEvent);
    events.add(theatreEvent2);

    ScriptedView view = new ScriptedView("2026-05-10");
    EventPerformanceController controller =
        new EventPerformanceController(view, events, performances, new MockPaymentSystem());
    controller.setCurrentUser(
        new Student("student@ed.ac.uk", "secret", "XXXXRT", 123456789, new StudentPreferences()));

    controller.searchforPerformances();

    List<String> displayed = new ArrayList<>(view.getLastDisplayedPerformanceList());
    assertEquals(2, displayed.size());
    assertTrue(
        displayed.get(0).contains("Spring Concert") || displayed.get(1).contains("Spring Concert"));
    assertTrue(displayed.get(0).contains("Drama") || displayed.get(1).contains("Drama"));
    assertTrue(displayed.get(0).contains("Performance ID:") && displayed.get(0).contains("Event:")
        && displayed.get(0).contains("Time:") && displayed.get(0).contains("Venue:")
        && displayed.get(0).contains("Provider:")
        && displayed.get(0).contains("Event review average:"));
    assertTrue(displayed.get(1).contains("Performance ID:") && displayed.get(1).contains("Event:")
        && displayed.get(1).contains("Time:") && displayed.get(1).contains("Venue:")
        && displayed.get(1).contains("Provider:")
        && displayed.get(1).contains("Event review average:"));
    assertTrue(view.getErrorMessages().isEmpty());
  }

  @Test
  void guestCannotSearchForPerformances() {
    events.add(createEventWithPerformance(1L, "Spring Concert", EventType.MUSIC,
        LocalDateTime.of(2026, 5, 10, 19, 0), LocalDateTime.of(2026, 5, 10, 21, 0), "McEwan Hall"));

    ScriptedView view = new ScriptedView("2026-05-10");
    EventPerformanceController controller =
        new EventPerformanceController(view, events, performances, new MockPaymentSystem());

    controller.searchforPerformances();

    assertEquals("ERROR: Only logged-in users can search for performances.",
        view.getLastErrorMessage());
    assertTrue(view.getLastDisplayedPerformanceList().isEmpty());
  }

  @Test
  void studentPreferencesMoveMatchingEventsToTheFront() {
    Event sportsEvent = createEventWithPerformance(1L, "Varsity Match", EventType.SPORTS,
        LocalDateTime.of(2026, 5, 10, 13, 0), LocalDateTime.of(2026, 5, 10, 15, 0), "Sports Hall");
    Event musicEvent = createEventWithPerformance(2L, "Spring Concert", EventType.MUSIC,
        LocalDateTime.of(2026, 5, 10, 19, 0), LocalDateTime.of(2026, 5, 10, 21, 0), "McEwan Hall");
    events.add(sportsEvent);
    events.add(musicEvent);

    ScriptedView view = new ScriptedView("2026-05-10");
    EventPerformanceController controller =
        new EventPerformanceController(view, events, performances, new MockPaymentSystem());
    controller.setCurrentUser(new Student("student@ed.ac.uk", "secret", "XXXXRT", 123456789,
        new StudentPreferences(true, false, false, false, false)));

    controller.searchforPerformances();

    List<String> displayed = new ArrayList<>(view.getLastDisplayedPerformanceList());
    assertEquals(2, displayed.size());
    assertTrue(displayed.get(0).contains("Spring Concert"));
    assertTrue(displayed.get(1).contains("Varsity Match"));
    assertTrue(displayed.get(0).contains("Venue: McEwan Hall"));
    assertTrue(displayed.get(1).contains("Venue: Sports Hall"));
  }

  @Test
  void studentWithNoSelectedPreferencesKeepsInsertionOrder() {
    Event sportsEvent = createEventWithPerformance(1L, "Varsity Match", EventType.SPORTS,
        LocalDateTime.of(2026, 5, 10, 13, 0), LocalDateTime.of(2026, 5, 10, 15, 0), "Sports Hall");
    Event musicEvent = createEventWithPerformance(2L, "Spring Concert", EventType.MUSIC,
        LocalDateTime.of(2026, 5, 10, 19, 0), LocalDateTime.of(2026, 5, 10, 21, 0), "McEwan Hall");
    events.add(sportsEvent);
    events.add(musicEvent);

    ScriptedView view = new ScriptedView("2026-05-10");
    EventPerformanceController controller =
        new EventPerformanceController(view, events, performances, new MockPaymentSystem());
    controller.setCurrentUser(new Student("student@ed.ac.uk", "secret", "XXXXRT", 123456789,
        new StudentPreferences(false, false, false, false, false)));

    controller.searchforPerformances();

    List<String> displayed = new ArrayList<>(view.getLastDisplayedPerformanceList());
    assertEquals(2, displayed.size());
    assertTrue(displayed.get(0).contains("Varsity Match"));
    assertTrue(displayed.get(1).contains("Spring Concert"));
    assertTrue(displayed.get(0).contains("Venue: Sports Hall"));
    assertTrue(displayed.get(1).contains("Venue: McEwan Hall"));
  }

  @Test
  void invalidDateFormatShowsAnErrorMessageAndRequiresDateAgain() {
    events.add(createEventWithPerformance(1L, "Spring Concert", EventType.MUSIC,
        LocalDateTime.of(2026, 5, 10, 19, 0), LocalDateTime.of(2026, 5, 10, 21, 0), "McEwan Hall"));

    ScriptedView view = new ScriptedView("10-05-2026", "2026-05-10");
    EventPerformanceController controller =
        new EventPerformanceController(view, events, performances, new MockPaymentSystem());
    controller.setCurrentUser(
        new Student("student@ed.ac.uk", "secret", "XXXXRT", 123456789, new StudentPreferences()));

    controller.searchforPerformances();

    assertTrue(view.getErrorMessages().contains("ERROR: Date format is invalid. Use yyyy-MM-dd."));
    assertEquals(1, view.getLastDisplayedPerformanceList().size());
    assertTrue(view.getLastDisplayedPerformanceList().iterator().next().contains("Spring Concert"));
  }

  @Test
  void searchingADateWithoutPerformancesShowsAnErrorMessage() {
    events.add(createEventWithPerformance(1L, "Spring Concert", EventType.MUSIC,
        LocalDateTime.of(2026, 5, 10, 19, 0), LocalDateTime.of(2026, 5, 10, 21, 0), "McEwan Hall"));

    ScriptedView view = new ScriptedView("2026-05-11");
    EventPerformanceController controller =
        new EventPerformanceController(view, events, performances, new MockPaymentSystem());
    controller.setCurrentUser(
        new Student("student@ed.ac.uk", "secret", "XXXXRT", 123456789, new StudentPreferences()));

    controller.searchforPerformances();

    assertEquals("ERROR: There are no performances on that date.", view.getLastErrorMessage());
    assertTrue(view.getLastDisplayedPerformanceList().isEmpty());
  }

  @Test
  void sameEventCanReturnMultiplePerformancesOnTheSameDate() {
    Event event = new Event(1L, "Spring Concert", EventType.MUSIC, true, provider);
    event.createPerformance(1L, LocalDateTime.of(2026, 5, 10, 14, 0),
        LocalDateTime.of(2026, 5, 10, 16, 0), List.of("Performer"), "McEwan Hall", 300, false,
        false, 150, 12.50);
    event.createPerformance(2L, LocalDateTime.of(2026, 5, 10, 19, 0),
        LocalDateTime.of(2026, 5, 10, 21, 0), List.of("Performer"), "McEwan Hall", 300, false,
        false, 150, 12.50);
    events.add(event);

    ScriptedView view = new ScriptedView("2026-05-10");
    EventPerformanceController controller =
        new EventPerformanceController(view, events, performances, new MockPaymentSystem());
    controller.setCurrentUser(
        new Student("student@ed.ac.uk", "secret", "XXXXRT", 123456789, new StudentPreferences()));

    controller.searchforPerformances();

    List<String> displayed = new ArrayList<>(view.getLastDisplayedPerformanceList());
    assertEquals(2, displayed.size());
    assertTrue(displayed.get(0).contains("Performance ID: 1")
        || displayed.get(1).contains("Performance ID: 1"));
    assertTrue(displayed.get(0).contains("Performance ID: 2")
        || displayed.get(1).contains("Performance ID: 2"));
    assertTrue(displayed.get(0).contains("Event: Spring Concert"));
    assertTrue(displayed.get(1).contains("Event: Spring Concert"));
    assertTrue(displayed.get(0).contains("Venue: McEwan Hall"));
    assertTrue(displayed.get(1).contains("Venue: McEwan Hall"));
  }

  private Event createEventWithPerformance(long eventId, String title, EventType type,
      LocalDateTime startDateTime, LocalDateTime endDateTime, String venue) {
    Event event = new Event(eventId, title, type, true, provider);
    event.createPerformance(eventId, startDateTime, endDateTime, List.of("Performer"), venue, 300,
        false, false, 150, 12.50);
    return event;
  }
}
