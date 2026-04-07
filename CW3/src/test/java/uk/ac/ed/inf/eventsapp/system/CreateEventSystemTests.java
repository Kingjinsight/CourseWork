package uk.ac.ed.inf.eventsapp.system;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import external.MockPaymentSystem;
import uk.ac.ed.inf.eventsapp.controller.EventPerformanceController;
import uk.ac.ed.inf.eventsapp.model.EntertainmentProvider;
import uk.ac.ed.inf.eventsapp.model.Event;
import uk.ac.ed.inf.eventsapp.model.EventType;
import uk.ac.ed.inf.eventsapp.model.Performance;

/**
 * System tests for the create-event use case.
 */
public class CreateEventSystemTests {
  private Collection<Event> events;
  private Collection<Performance> performances;
  private EntertainmentProvider provider;

  @BeforeEach
  @SuppressWarnings("unused")
  void setUp() {
    // Resets the shared event collections before each create-event scenario.
    events = new ArrayList<>();
    performances = new ArrayList<>();
    provider = new EntertainmentProvider("provider@example.com", "encrypted_password",
        "Festival Org", "BN-1234567", "Provider Rep", "Runs live events");
  }

  // Verifies that a logged-in provider can create a valid ticketed event.
  @Test
  void registeredProviderCanCreateATicketedEvent() {
    ScriptedView view = createView(ticketedInputs());
    EventPerformanceController controller = controllerForProvider(view);

    Event createdEvent = controller.createEvent();

    assertNotNull(createdEvent);
    assertEquals(1, events.size());
    assertEquals(1, provider.getEvents().size());
    assertEquals("SUCCESS: Event created successfully.", view.getLastSuccessMessage());
    assertTrue(view.getErrorMessages().isEmpty());
    assertNotNull(createdEvent.getPerformanceByID(1L));
  }

  // Verifies that a logged-in provider can create a valid non-ticketed event.
  @Test
  void registeredProviderCanCreateANonTicketedEvent() {
    ScriptedView view = createView(nonTicketedInputs());
    EventPerformanceController controller = controllerForProvider(view);

    Event createdEvent = controller.createEvent();

    assertNotNull(createdEvent);
    assertEquals(1, events.size());
    assertNotNull(createdEvent.getPerformanceByID(1L));
    assertEquals("SUCCESS: Event created successfully.", view.getLastSuccessMessage());
  }

  // Verifies that one event can be created with multiple performances in a single flow.
  @Test
  void registeredProviderCanCreateAnEventWithMultiplePerformances() {
    ScriptedView view =
        createView("Spring Concert", "music", "yes", "2", "2026-05-10 19:00", "2026-05-10 21:00",
            "Hagan, Bob", "McEwan Hall", "500", "no", "no", "250", "15.50", "2026-05-11 19:00",
            "2026-05-11 21:00", "Hagan, Bob", "McEwan Hall", "500", "no", "no", "250", "15.50");
    EventPerformanceController controller = controllerForProvider(view);

    Event createdEvent = controller.createEvent();

    assertNotNull(createdEvent);
    assertEquals(1, events.size());
    assertNotNull(createdEvent.getPerformanceByID(1L));
    assertNotNull(createdEvent.getPerformanceByID(2L));
    assertEquals("SUCCESS: Event created successfully.", view.getLastSuccessMessage());
  }

  // Verifies that unauthenticated users cannot create events.
  @Test
  void guestCannotCreateAnEvent() {
    ScriptedView view = new ScriptedView();
    EventPerformanceController controller =
        new EventPerformanceController(view, events, performances, new MockPaymentSystem());

    Event createdEvent = controller.createEvent();

    assertNull(createdEvent);
    assertTrue(events.isEmpty());
    assertEquals("ERROR: Only logged-in entertainment providers can create events.",
        view.getLastErrorMessage());
  }

  // Verifies that a missing title blocks event creation.
  @Test
  void emptyEventTitlePreventsEventCreation() {
    String[] inputs = ticketedInputs();
    inputs[0] = "   ";

    assertCreationFailsWith("ERROR: Event title is required.", inputs);
  }

  // Verifies that an unsupported event type blocks event creation.
  @Test
  void invalidEventTypePreventsEventCreation() {
    String[] inputs = ticketedInputs();
    inputs[1] = "opera";

    assertCreationFailsWith("ERROR: Invalid event type.", inputs);
  }

  // Verifies that an invalid ticketed flag blocks event creation.
  @Test
  void invalidTicketedFlagPreventsEventCreation() {
    String[] inputs = ticketedInputs();
    inputs[2] = "sometimes";

    assertCreationFailsWith("ERROR: Ticketed must be specified as yes or no.", inputs);
  }

  // Verifies that a non-positive performance count blocks event creation.
  @Test
  void invalidPerformanceCountPreventsEventCreation() {
    String[] inputs = ticketedInputs();
    inputs[3] = "0";

    assertCreationFailsWith("ERROR: Number of performances must be a positive integer.", inputs);
  }

  // Verifies that malformed performance times block event creation.
  @Test
  void invalidPerformanceDateTimePreventsEventCreation() {
    String[] inputs = ticketedInputs();
    inputs[4] = "2026/05/10 19:00";

    assertCreationFailsWith("ERROR: Performance dates/times are invalid.", inputs);
  }

  // Verifies that a performance ending before it starts blocks event creation.
  @Test
  void endTimeBeforeStartTimePreventsEventCreation() {
    String[] inputs = ticketedInputs();
    inputs[5] = "2026-05-10 18:00";

    assertCreationFailsWith("ERROR: Performance dates/times are invalid.", inputs);
  }

  // Verifies that missing performer names block event creation.
  @Test
  void emptyPerformerNamesPreventEventCreation() {
    String[] inputs = ticketedInputs();
    inputs[6] = " , ";

    assertCreationFailsWith("ERROR: At least one performer name is required.", inputs);
  }

  // Verifies that a missing venue address blocks event creation.
  @Test
  void emptyVenueAddressPreventsEventCreation() {
    String[] inputs = ticketedInputs();
    inputs[7] = "   ";

    assertCreationFailsWith("ERROR: Venue address is required.", inputs);
  }

  // Verifies that an invalid venue capacity blocks event creation.
  @Test
  void invalidVenueCapacityPreventsEventCreation() {
    String[] inputs = ticketedInputs();
    inputs[8] = "-5";

    assertCreationFailsWith("ERROR: Venue capacity must be a positive integer.", inputs);
  }

  // Verifies that invalid venue flags block event creation.
  @Test
  void invalidVenueFlagsPreventEventCreation() {
    String[] inputs = ticketedInputs();
    inputs[9] = "maybe";

    assertCreationFailsWith("ERROR: Venue flags must be specified as yes or no.", inputs);
  }

  // Verifies that an invalid ticket count blocks creation of a ticketed event.
  @Test
  void invalidTicketCountPreventsEventCreation() {
    String[] inputs = ticketedInputs();
    inputs[11] = "-1";

    assertCreationFailsWith(
        "ERROR: Ticket count must be a valid non-negative integer and ticket price must have at most two decimal places.",
        inputs);
  }

  // Verifies that an invalid ticket price format blocks creation of a ticketed event.
  @Test
  void invalidTicketPriceFormatPreventsEventCreation() {
    String[] inputs = ticketedInputs();
    inputs[12] = "15.999";

    assertCreationFailsWith(
        "ERROR: Ticket count must be a valid non-negative integer and ticket price must have at most two decimal places.",
        inputs);
  }

  // Verifies that an event cannot be created when another event already uses the same title and
  // schedule.
  @Test
  void duplicateEventTitleAndPerformanceTimeIsRejected() {
    Event existingEvent = new Event(99L, "Spring Concert", EventType.MUSIC, true, provider);
    existingEvent.createPerformance(55L, LocalDateTime.of(2026, 5, 10, 19, 0),
        LocalDateTime.of(2026, 5, 10, 21, 0), List.of("Existing Artist"), "Old College", 400, false,
        false, 200, 10.0);
    events.add(existingEvent);

    ScriptedView view = createView(ticketedInputs());
    EventPerformanceController controller = controllerForProvider(view);

    Event createdEvent = controller.createEvent();

    assertNull(createdEvent);
    assertEquals(1, events.size());
    assertEquals("ERROR: An event with the same title already exists for the same dates and times.",
        view.getLastErrorMessage());
  }

  // Verifies that one event cannot contain two performances with the same schedule.
  @Test
  void duplicatePerformanceTimesWithinTheSameEventAreRejected() {
    ScriptedView view =
        createView("Spring Concert", "music", "yes", "2", "2026-05-10 19:00", "2026-05-10 21:00",
            "Hagan, Bob", "McEwan Hall", "500", "no", "no", "250", "15.50", "2026-05-10 19:00",
            "2026-05-10 21:00", "Hagan, Bob", "McEwan Hall", "500", "no", "no", "250", "15.50");
    EventPerformanceController controller = controllerForProvider(view);

    Event createdEvent = controller.createEvent();

    assertNull(createdEvent);
    assertTrue(events.isEmpty());
    assertTrue(provider.getEvents().isEmpty());
    assertEquals("ERROR: A performance already exists for the same dates and times.",
        view.getLastErrorMessage());
  }

  private EventPerformanceController controllerForProvider(ScriptedView view) {
    EventPerformanceController controller =
        new EventPerformanceController(view, events, performances, new MockPaymentSystem());
    controller.setCurrentUser(provider);
    return controller;
  }

  private ScriptedView createView(String... inputs) {
    return new ScriptedView(inputs);
  }

  private void assertCreationFailsWith(String expectedErrorMessage, String... inputs) {
    // Verifies that invalid create-event input produces the expected error and no persisted state.
    ScriptedView view = createView(inputs);
    EventPerformanceController controller = controllerForProvider(view);

    Event createdEvent = controller.createEvent();

    assertNull(createdEvent);
    assertTrue(events.isEmpty());
    assertTrue(provider.getEvents().isEmpty());
    assertEquals(expectedErrorMessage, view.getLastErrorMessage());
  }

  private String[] ticketedInputs() {
    return new String[] {"Spring Concert", "music", "yes", "1", "2026-05-10 19:00",
        "2026-05-10 21:00", "Hagan, Bob", "McEwan Hall", "500", "no", "no", "250", "15.50"};
  }

  private String[] nonTicketedInputs() {
    return new String[] {"Campus Drama", "theatre", "no", "1", "2026-05-10 19:00",
        "2026-05-10 21:00", "Hagan, Bob", "Bedlam Theatre", "120", "no", "no"};
  }
}
