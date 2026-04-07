package uk.ac.ed.inf.eventsapp.system;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import external.MockPaymentSystem;
import uk.ac.ed.inf.eventsapp.controller.BookingController;
import uk.ac.ed.inf.eventsapp.controller.EventPerformanceController;
import uk.ac.ed.inf.eventsapp.controller.UserController;
import uk.ac.ed.inf.eventsapp.integration.MockVerificationSystem;
import uk.ac.ed.inf.eventsapp.model.Booking;
import uk.ac.ed.inf.eventsapp.model.EntertainmentProvider;
import uk.ac.ed.inf.eventsapp.model.Event;
import uk.ac.ed.inf.eventsapp.model.Performance;
import uk.ac.ed.inf.eventsapp.model.Student;
import uk.ac.ed.inf.eventsapp.model.StudentPreferences;
import uk.ac.ed.inf.eventsapp.model.User;

/**
 * Integrated system tests that chain multiple use cases to verify end-to-end application behaviour
 * across shared state.
 */
public class IntegratedSystemTests {
  private Collection<User> users;
  private Collection<Event> events;
  private Collection<Performance> performances;
  private Collection<Booking> bookings;

  private EntertainmentProvider provider;
  private Student student;

  private static final String[] CREATE_TICKETED_EVENT_INPUTS =
      {"Spring Concert", "music", "yes", "1", "2026-05-10 19:00", "2026-05-10 21:00", "Hagan, Bob",
          "McEwan Hall", "500", "no", "no", "100", "15.50"};
  private static final String[] CREATE_SPORTS_EVENT_INPUTS =
      {"Varsity Match", "sports", "yes", "1", "2026-05-10 13:00", "2026-05-10 15:00", "Team A",
          "Sports Hall", "300", "no", "no", "100", "10.00"};

  @BeforeEach
  @SuppressWarnings("unused")
  void setUp() {
    // Resets the shared collections and default users before each multi-use-case scenario.
    users = new ArrayList<>();
    events = new ArrayList<>();
    performances = new ArrayList<>();
    bookings = new ArrayList<>();

    provider = new EntertainmentProvider("provider@gmail.com", "password", "Festival Org",
        "1234567890", "Hagan", "Runs live events");
    student =
        new Student("student@ed.ac.uk", "password", "Hagan", 1234567, new StudentPreferences());
    users.add(provider);
    users.add(student);
  }

  private EventPerformanceController epController(ScriptedView view) {
    EventPerformanceController c =
        new EventPerformanceController(view, events, performances, new MockPaymentSystem());
    c.setCurrentUser(provider);
    return c;
  }

  private BookingController bookingController(ScriptedView view) {
    BookingController c =
        new BookingController(view, new MockPaymentSystem(), events, performances, bookings);
    c.setCurrentUser(student);
    return c;
  }

  private UserController userController(ScriptedView view) {
    return new UserController(view, new MockVerificationSystem(), users, events, false);
  }

  /** Creates a ticketed event with one performance (ID=1) and returns the controller. */
  private void createTicketedEvent() {
    epController(new ScriptedView(CREATE_TICKETED_EVENT_INPUTS)).createEvent();
  }

  private String[] concatenateInputs(String[]... blocks) {
    List<String> concatenated = new ArrayList<>();
    for (String[] block : blocks) {
      concatenated.addAll(List.of(block));
    }
    return concatenated.toArray(String[]::new);
  }

  // Verifies that a student can book a performance created earlier by a provider.
  @Test
  void studentCanBookPerformanceCreatedByProvider() {
    createTicketedEvent();

    ScriptedView view = new ScriptedView("1", "2");
    bookingController(view).bookPerformance();

    assertEquals("SUCCESS: Booking successful", view.getLastSuccessMessage(),
        "Student should succeed in booking after EP creates the event.");
    assertEquals(1, bookings.size(),
        "Exactly one booking should exist after one successful booking.");
  }

  // Verifies that booking every available ticket leaves no remaining capacity.
  @Test
  void bookingAllTicketsMeansNoneLeft() {
    createTicketedEvent();
    bookingController(new ScriptedView("1", "100")).bookPerformance();

    Performance perf = performances.iterator().next();
    assertFalse(perf.checkIfTicketsLeft(1),
        "After booking all 100 tickets, no tickets should remain.");
  }

  // Verifies that a student can cancel a booking that was created earlier in the same flow.
  @Test
  void studentCanCancelBookingAfterBooking() {
    createTicketedEvent();
    bookingController(new ScriptedView("1", "2")).bookPerformance();

    ScriptedView cancelView = new ScriptedView("1");
    bookingController(cancelView).cancelBooking();

    assertEquals("SUCCESS: Booking cancelled successfully.", cancelView.getLastSuccessMessage(),
        "Student should be able to cancel their booking.");
  }

  // Verifies that a student-cancelled booking is no longer active after the full booking flow.
  @Test
  void cancelledBookingHasCancelledByStudentStatus() {
    createTicketedEvent();
    bookingController(new ScriptedView("1", "1")).bookPerformance();

    bookingController(new ScriptedView("1")).cancelBooking();

    Booking booking = bookings.iterator().next();
    assertFalse(booking.isActive(), "Booking should no longer be active after student cancels.");
  }

  // Verifies that provider cancellation deactivates bookings created earlier on the same
  // performance.
  @Test
  void providerCancelsPerformanceAndBookingBecomesProviderCancelled() {
    createTicketedEvent();
    bookingController(new ScriptedView("1", "2")).bookPerformance();

    ScriptedView cancelView =
        new ScriptedView("1", "Event cancelled due to unforeseen circumstances.");
    epController(cancelView).cancelPerformance();

    assertEquals("SUCCESS: Cancellation Successful!", cancelView.getLastSuccessMessage(),
        "EP should successfully cancel the performance.");
    Booking booking = bookings.iterator().next();
    assertFalse(booking.isActive(),
        "Booking should no longer be active after EP cancels the performance.");
  }

  // Verifies that a performance becomes inactive after a provider cancels it.
  @Test
  void cancelledPerformanceIsNoLongerActive() {
    createTicketedEvent();
    bookingController(new ScriptedView("1", "1")).bookPerformance();

    epController(new ScriptedView("1", "Sorry")).cancelPerformance();

    Performance perf = performances.iterator().next();
    assertFalse(perf.isActive(), "Performance should no longer be active after cancellation.");
  }

  // Verifies that shared controller state survives a role change across login, creation, and
  // booking.
  @Test
  void loginCreateEventLogoutLoginAsStudentBook() {
    ScriptedView loginView = new ScriptedView("provider@gmail.com", "password");
    UserController uc = userController(loginView);
    uc.login();
    assertEquals("SUCCESS: Login successful.", loginView.getLastSuccessMessage(),
        "EP should log in successfully.");

    EventPerformanceController epc =
        new EventPerformanceController(new ScriptedView(CREATE_TICKETED_EVENT_INPUTS), events,
            performances, new MockPaymentSystem());
    epc.setCurrentUser(uc.getCurrentUser());
    epc.createEvent();
    assertEquals(1, performances.size(), "One performance should exist after event creation.");

    ScriptedView logoutView = new ScriptedView();
    UserController uc2 = userController(logoutView);
    uc2.setCurrentUser(uc.getCurrentUser());
    uc2.logout();
    assertEquals("SUCCESS: Logout successful.", logoutView.getLastSuccessMessage(),
        "EP should log out successfully.");

    ScriptedView studentLoginView = new ScriptedView("student@ed.ac.uk", "password");
    UserController uc3 = userController(studentLoginView);
    uc3.login();

    ScriptedView bookView = new ScriptedView("1", "1");
    BookingController bc =
        new BookingController(bookView, new MockPaymentSystem(), events, performances, bookings);
    bc.setCurrentUser(uc3.getCurrentUser());
    bc.bookPerformance();

    assertEquals("SUCCESS: Booking successful", bookView.getLastSuccessMessage(),
        "Student should book the performance after the full login/create/logout/login flow.");
  }

  // Verifies that a newly registered provider can create a performance that a student can later
  // find and inspect.
  @Test
  void registerLoginCreateEventSearchAndViewPerformance() {
    users.clear();
    ScriptedView registrationView = new ScriptedView("newprovider@example.com", "password",
        "Fresh Org", "1234567890", "Fresh Rep", "Runs live events");
    UserController registrationController = userController(registrationView);
    registrationController.registerEntertainmentProvider();

    ScriptedView providerLoginView = new ScriptedView("newprovider@example.com", "password");
    UserController providerController = userController(providerLoginView);
    providerController.login();

    EventPerformanceController epc =
        new EventPerformanceController(new ScriptedView(CREATE_TICKETED_EVENT_INPUTS), events,
            performances, new MockPaymentSystem());
    epc.setCurrentUser(providerController.getCurrentUser());
    epc.createEvent();

    users.add(student);
    ScriptedView studentLoginView = new ScriptedView("student@ed.ac.uk", "password");
    UserController studentController = userController(studentLoginView);
    studentController.login();

    ScriptedView searchView = new ScriptedView("2026-05-10");
    EventPerformanceController studentEventController =
        new EventPerformanceController(searchView, events, performances, new MockPaymentSystem());
    studentEventController.setCurrentUser(studentController.getCurrentUser());
    studentEventController.searchforPerformances();

    ScriptedView viewPerformanceView = new ScriptedView("1");
    EventPerformanceController viewController = new EventPerformanceController(viewPerformanceView,
        events, performances, new MockPaymentSystem());
    viewController.setCurrentUser(studentController.getCurrentUser());
    viewController.viewPerformance();

    assertEquals("SUCCESS: Registration successful.", registrationView.getLastSuccessMessage(),
        "A newly registered entertainment provider should be created successfully.");
    assertEquals("SUCCESS: Login successful.", providerLoginView.getLastSuccessMessage(),
        "The newly registered entertainment provider should be able to log in.");
    assertEquals("SUCCESS: Login successful.", studentLoginView.getLastSuccessMessage(),
        "The student should be able to log in before searching and viewing performances.");
    assertEquals(1, performances.size(),
        "One performance should exist after the newly registered provider creates an event.");
    assertTrue(
        searchView.getLastDisplayedPerformanceList().stream()
            .anyMatch(info -> info.contains("Spring Concert")),
        "The created performance should appear in search results after the chained registration, login, and creation flow.");
    assertNotNull(viewPerformanceView.getLastDisplayedPerformance(),
        "Viewing a searched performance should display its details.");
    assertTrue(viewPerformanceView.getLastDisplayedPerformance().contains("Performance ID: 1"),
        "The detailed view should include the searched performance ID.");
    assertTrue(viewPerformanceView.getLastDisplayedPerformance().contains("Event: Spring Concert"),
        "The detailed view should include the event title.");
  }

  // Verifies the main happy path across registration, authentication, discovery, booking, and
  // cancellation.
  @Test
  void registerLoginCreateEventLogoutLoginSearchViewBookAndCancelBooking() {
    users.clear();
    ScriptedView registrationView = new ScriptedView("newprovider@example.com", "password",
        "Fresh Org", "1234567890", "Fresh Rep", "Runs live events");
    UserController registrationController = userController(registrationView);
    registrationController.registerEntertainmentProvider();

    ScriptedView providerLoginView = new ScriptedView("newprovider@example.com", "password");
    UserController providerController = userController(providerLoginView);
    providerController.login();

    EventPerformanceController providerEventController =
        new EventPerformanceController(new ScriptedView(CREATE_TICKETED_EVENT_INPUTS), events,
            performances, new MockPaymentSystem());
    providerEventController.setCurrentUser(providerController.getCurrentUser());
    providerEventController.createEvent();

    ScriptedView providerLogoutView = new ScriptedView();
    UserController providerLogoutController = userController(providerLogoutView);
    providerLogoutController.setCurrentUser(providerController.getCurrentUser());
    providerLogoutController.logout();

    users.add(student);
    ScriptedView studentLoginView = new ScriptedView("student@ed.ac.uk", "password");
    UserController studentController = userController(studentLoginView);
    studentController.login();

    ScriptedView searchView = new ScriptedView("2026-05-10");
    EventPerformanceController searchController =
        new EventPerformanceController(searchView, events, performances, new MockPaymentSystem());
    searchController.setCurrentUser(studentController.getCurrentUser());
    searchController.searchforPerformances();

    ScriptedView viewPerformanceView = new ScriptedView("1");
    EventPerformanceController viewController = new EventPerformanceController(viewPerformanceView,
        events, performances, new MockPaymentSystem());
    viewController.setCurrentUser(studentController.getCurrentUser());
    viewController.viewPerformance();

    ScriptedView bookView = new ScriptedView("1", "2");
    BookingController bookingController =
        new BookingController(bookView, new MockPaymentSystem(), events, performances, bookings);
    bookingController.setCurrentUser(studentController.getCurrentUser());
    bookingController.bookPerformance();

    ScriptedView cancelView = new ScriptedView("1");
    BookingController cancelBookingController =
        new BookingController(cancelView, new MockPaymentSystem(), events, performances, bookings);
    cancelBookingController.setCurrentUser(studentController.getCurrentUser());
    cancelBookingController.cancelBooking();

    assertEquals("SUCCESS: Registration successful.", registrationView.getLastSuccessMessage(),
        "The entertainment provider should register successfully at the start of the long flow.");
    assertEquals("SUCCESS: Login successful.", providerLoginView.getLastSuccessMessage(),
        "The entertainment provider should log in before creating an event.");
    assertEquals("SUCCESS: Logout successful.", providerLogoutView.getLastSuccessMessage(),
        "The entertainment provider should be able to log out before the student logs in.");
    assertEquals("SUCCESS: Login successful.", studentLoginView.getLastSuccessMessage(),
        "The student should log in successfully before searching and booking.");
    assertTrue(
        searchView.getLastDisplayedPerformanceList().stream()
            .anyMatch(info -> info.contains("Spring Concert")),
        "The student should be able to find the created performance in search results.");
    assertNotNull(viewPerformanceView.getLastDisplayedPerformance(),
        "The student should be able to view the created performance in detail.");
    assertEquals("SUCCESS: Booking successful", bookView.getLastSuccessMessage(),
        "The student should be able to book the searched performance.");
    assertEquals("SUCCESS: Booking cancelled successfully.", cancelView.getLastSuccessMessage(),
        "The student should be able to cancel the booking at the end of the long flow.");
    assertEquals(1, bookings.size(),
        "Exactly one booking should have been created during the long end-to-end flow.");
    assertFalse(bookings.iterator().next().isActive(),
        "The booking created in the long end-to-end flow should be inactive after cancellation.");
  }

  // Verifies that a performance booked to capacity cannot accept additional bookings.
  @Test
  void soldOutPerformancePreventsBooking() {
    String[] smallEvent = {"Small Gig", "music", "yes", "1", "2026-06-10 19:00", "2026-06-10 21:00",
        "Hagan", "Small Hall", "10", "no", "no", "5", "10.00"};
    epController(new ScriptedView(smallEvent)).createEvent();

    bookingController(new ScriptedView("1", "5")).bookPerformance();

    Performance perf = performances.iterator().next();
    assertFalse(perf.checkIfTicketsLeft(1),
        "After booking all 5 tickets, no more tickets should be available.");
  }

  // Verifies that provider cancellation deactivates every active booking on the cancelled
  // performance.
  @Test
  void providerCancelsPerformanceAllBookingsBecomeInactive() {
    createTicketedEvent();
    Student student2 =
        new Student("student2@ed.ac.uk", "password", "Bob", 7654321, new StudentPreferences());
    users.add(student2);

    bookingController(new ScriptedView("1", "2")).bookPerformance();

    ScriptedView bookView2 = new ScriptedView("1", "3");
    BookingController bc2 =
        new BookingController(bookView2, new MockPaymentSystem(), events, performances, bookings);
    bc2.setCurrentUser(student2);
    bc2.bookPerformance();

    assertEquals(2, bookings.size(), "Two bookings should exist.");

    epController(new ScriptedView("1", "Event cancelled")).cancelPerformance();

    for (Booking b : bookings) {
      assertFalse(b.isActive(),
          "All bookings should be inactive after EP cancels the performance.");
    }
  }

  // Verifies that a cancelled performance disappears from later search results.
  @Test
  void providerCancelsPerformanceThenStudentCanNoLongerFindItInSearch() {
    createTicketedEvent();
    bookingController(new ScriptedView("1", "1")).bookPerformance();

    ScriptedView cancelView = new ScriptedView("1", "Cancelled by organiser");
    epController(cancelView).cancelPerformance();

    ScriptedView searchView = new ScriptedView("2026-05-10");
    EventPerformanceController searchController =
        new EventPerformanceController(searchView, events, performances, new MockPaymentSystem());
    searchController.setCurrentUser(student);
    searchController.searchforPerformances();

    assertEquals("SUCCESS: Cancellation Successful!", cancelView.getLastSuccessMessage(),
        "The provider should be able to cancel the booked performance.");
    assertEquals("ERROR: There are no performances on that date.", searchView.getLastErrorMessage(),
        "A cancelled performance should no longer appear in search results.");
  }

  // Verifies that editing preferences does not prevent a later booking flow from succeeding.
  @Test
  void studentEditsPreferencesThenBooks() {
    createTicketedEvent();

    ScriptedView prefView = new ScriptedView("music,dance,movie");
    UserController uc = userController(prefView);
    uc.setCurrentUser(student);
    uc.editPreferences();
    assertEquals("SUCCESS: Preferences updated successfully.", prefView.getLastSuccessMessage(),
        "Preferences update should succeed.");

    ScriptedView bookView = new ScriptedView("1", "1");
    bookingController(bookView).bookPerformance();
    assertEquals("SUCCESS: Booking successful", bookView.getLastSuccessMessage(),
        "Student should still be able to book after editing preferences.");
  }

  // Verifies that cancelled tickets return to the available pool for later bookings.
  @Test
  void studentCancelsBookingThenReturnedTicketsCanBeBookedAgain() {
    createTicketedEvent();

    ScriptedView firstBookingView = new ScriptedView("1", "100");
    bookingController(firstBookingView).bookPerformance();

    ScriptedView cancelView = new ScriptedView("1");
    bookingController(cancelView).cancelBooking();

    ScriptedView secondBookingView = new ScriptedView("1", "100");
    bookingController(secondBookingView).bookPerformance();

    assertEquals("SUCCESS: Booking successful", firstBookingView.getLastSuccessMessage(),
        "The initial booking should succeed and consume all available tickets.");
    assertEquals("SUCCESS: Booking cancelled successfully.", cancelView.getLastSuccessMessage(),
        "Cancelling the booking should succeed and return the tickets.");
    assertEquals("SUCCESS: Booking successful", secondBookingView.getLastSuccessMessage(),
        "Returned tickets should be available for booking again.");
  }

  // Verifies that updated preferences affect the ordering of later search results.
  @Test
  void studentEditsPreferencesThenSearchesPerformances() {
    ScriptedView createView = new ScriptedView(
        concatenateInputs(CREATE_SPORTS_EVENT_INPUTS, CREATE_TICKETED_EVENT_INPUTS));
    EventPerformanceController createController = epController(createView);
    createController.createEvent();
    createController.createEvent();

    ScriptedView prefView = new ScriptedView("music");
    UserController uc = userController(prefView);
    uc.setCurrentUser(student);
    uc.editPreferences();

    ScriptedView searchView = new ScriptedView("2026-05-10");
    EventPerformanceController searchController =
        new EventPerformanceController(searchView, events, performances, new MockPaymentSystem());
    searchController.setCurrentUser(student);
    searchController.searchforPerformances();

    assertEquals("SUCCESS: Preferences updated successfully.", prefView.getLastSuccessMessage(),
        "Preferences update should succeed before searching.");
    assertEquals(2, performances.size(),
        "Two performances should exist after creating two events through the create-event use case.");
    assertTrue(
        new ArrayList<>(searchView.getLastDisplayedPerformanceList()).get(0)
            .contains("Spring Concert"),
        "After editing preferences, matching performances should be prioritised in search results.");
  }


  // Verifies that students cannot access the create-event use case.
  @Test
  void studentCannotCreateEvent() {
    ScriptedView view = new ScriptedView();
    EventPerformanceController epc =
        new EventPerformanceController(view, events, performances, new MockPaymentSystem());
    epc.setCurrentUser(student);
    epc.createEvent();
    assertEquals("ERROR: Only logged-in entertainment providers can create events.",
        view.getLastErrorMessage(), "Student should be blocked from creating events.");
  }

  // Verifies that providers cannot access the book-performance use case.
  @Test
  void providerCannotBookPerformance() {
    createTicketedEvent();
    ScriptedView view = new ScriptedView();
    BookingController bc =
        new BookingController(view, new MockPaymentSystem(), events, performances, bookings);
    bc.setCurrentUser(provider);
    bc.bookPerformance();
    assertEquals("ERROR: Only students can book performances.", view.getLastErrorMessage(),
        "Provider should be blocked from booking performances.");
  }

  // Verifies that students cannot access the cancel-performance use case.
  @Test
  void studentCannotCancelPerformance() {
    createTicketedEvent();
    ScriptedView view = new ScriptedView();
    EventPerformanceController epc =
        new EventPerformanceController(view, events, performances, new MockPaymentSystem());
    epc.setCurrentUser(student);
    epc.cancelPerformance();
    assertTrue(view.getLastErrorMessage().contains("entertainment provider"),
        "Student should be blocked from cancelling performances.");
  }
}
