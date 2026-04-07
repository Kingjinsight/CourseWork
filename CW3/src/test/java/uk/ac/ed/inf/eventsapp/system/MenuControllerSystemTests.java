package uk.ac.ed.inf.eventsapp.system;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import external.MockPaymentSystem;
import uk.ac.ed.inf.eventsapp.controller.BookingController;
import uk.ac.ed.inf.eventsapp.controller.EventPerformanceController;
import uk.ac.ed.inf.eventsapp.controller.MenuController;
import uk.ac.ed.inf.eventsapp.controller.UserController;
import uk.ac.ed.inf.eventsapp.integration.MockVerificationSystem;
import uk.ac.ed.inf.eventsapp.model.Booking;
import uk.ac.ed.inf.eventsapp.model.BookingStatus;
import uk.ac.ed.inf.eventsapp.model.EntertainmentProvider;
import uk.ac.ed.inf.eventsapp.model.Event;
import uk.ac.ed.inf.eventsapp.model.EventType;
import uk.ac.ed.inf.eventsapp.model.Performance;
import uk.ac.ed.inf.eventsapp.model.PerformanceStatus;
import uk.ac.ed.inf.eventsapp.model.Student;
import uk.ac.ed.inf.eventsapp.model.StudentPreferences;
import uk.ac.ed.inf.eventsapp.model.User;
import uk.ac.ed.inf.eventsapp.view.View;

/**
 * System tests for the menu-driven system flow.
 */
public class MenuControllerSystemTests {
  private Collection<User> users;
  private Collection<Event> events;
  private Collection<Performance> performances;
  private Collection<Booking> bookings;
  private UserController userController;
  private EventPerformanceController eventPerformanceController;
  private BookingController bookingController;

  @BeforeEach
  @SuppressWarnings("unused")
  void setUp() {
    users = new ArrayList<>();
    events = new ArrayList<>();
    performances = new ArrayList<>();
    bookings = new ArrayList<>();
  }

  // Verifies that menu retry and login both work within the same guest-menu loop.
  @Test
  void guestMenuCanRetryMenuSelectionAndLogInStudent() {
    Student student =
        new Student("student@ed.ac.uk", "password", "Hagan", 123456789, new StudentPreferences());
    users.add(student);

    ExhaustingScriptedView view =
        new ExhaustingScriptedView("9", "1", "student@ed.ac.uk", "password");
    MenuController menuController = createMenuController(view);

    assertThrows(IllegalStateException.class, menuController::mainMenu,
        "The scripted menu run should stop once scripted inputs are exhausted.");

    assertTrue(view.getErrorMessages().contains("ERROR: Please select a valid menu option number."),
        "Invalid menu selections should show an error before retrying.");
    assertEquals(student, userController.getCurrentUser(),
        "Login from the guest menu should authenticate the student.");
    assertEquals(student, eventPerformanceController.getCurrentUser(),
        "Authenticated user should be synced to the event controller.");
    assertEquals(student, bookingController.getCurrentUser(),
        "Authenticated user should be synced to the booking controller.");
  }

  // Verifies that the guest menu can terminate the application loop without logging a user in.
  @Test
  void guestMenuCanExitCleanly() {
    ExhaustingScriptedView view = new ExhaustingScriptedView("0");
    MenuController menuController = createMenuController(view);

    menuController.mainMenu();

    assertNull(userController.getCurrentUser(),
        "Exiting from the guest menu should leave the user logged out.");
  }

  // Verifies that guest-menu registration updates shared state instead of only showing a success
  // message.
  @Test
  void guestMenuCanRegisterEntertainmentProvider() {
    ExhaustingScriptedView view = new ExhaustingScriptedView("2", "provider@example.com",
        "password", "Festival Org", "BN-1234567", "Provider Rep", "Runs live events");
    MenuController menuController = createMenuController(view);
    int initialUserCount = users.size();

    assertThrows(IllegalStateException.class, menuController::mainMenu,
        "The scripted menu run should stop once scripted inputs are exhausted.");

    assertEquals(initialUserCount + 1, users.size(),
        "Registering from the guest menu should add exactly one entertainment provider to the system.");
    assertTrue(
        users.stream()
            .anyMatch(user -> user instanceof EntertainmentProvider provider
                && "provider@example.com".equals(provider.getEmail())),
        "The registered entertainment provider should be present in the system.");
    assertEquals("SUCCESS: Registration successful.", view.getLastSuccessMessage(),
        "Successful registration should show a confirmation message.");
  }

  // Verifies that the student menu can route to the logout use case and clear the current user.
  @Test
  void studentMenuCanLogOut() {
    Student student =
        new Student("student@ed.ac.uk", "password", "Hagan", 123456789, new StudentPreferences());
    ExhaustingScriptedView view = new ExhaustingScriptedView("1");
    MenuController menuController = createMenuController(view);
    userController.setCurrentUser(student);

    assertThrows(IllegalStateException.class, menuController::mainMenu,
        "The scripted menu run should stop once scripted inputs are exhausted.");

    assertNull(userController.getCurrentUser(),
        "Selecting logout from the student menu should clear the logged-in user.");
    assertEquals("SUCCESS: Logout successful.", view.getLastSuccessMessage(),
        "Logout from the student menu should show a success message.");
  }

  // Verifies that exiting from the student menu leaves the authenticated user unchanged.
  @Test
  void studentMenuCanExitCleanly() {
    Student student =
        new Student("student@ed.ac.uk", "password", "Hagan", 123456789, new StudentPreferences());
    ExhaustingScriptedView view = new ExhaustingScriptedView("0");
    MenuController menuController = createMenuController(view);
    userController.setCurrentUser(student);

    menuController.mainMenu();

    assertEquals(student, userController.getCurrentUser(),
        "Exiting from the student menu should not change the current user.");
  }

  // Verifies that the EP menu delegates event creation to the same underlying controller flow.
  @Test
  void entertainmentProviderMenuCanCreateEvent() {
    EntertainmentProvider provider = new EntertainmentProvider("provider@example.com", "password",
        "Festival Org", "BN-1234567", "Provider Rep", "Runs live events");
    ExhaustingScriptedView view =
        new ExhaustingScriptedView("4", "Spring Concert", "music", "yes", "1", "2026-05-10 19:00",
            "2026-05-10 21:00", "Performer", "McEwan Hall", "300", "no", "no", "150", "12.50");
    MenuController menuController = createMenuController(view);
    userController.setCurrentUser(provider);

    assertThrows(IllegalStateException.class, menuController::mainMenu,
        "The scripted menu run should stop once scripted inputs are exhausted.");

    assertEquals(1, events.size(), "Creating an event from the EP menu should persist the event.");
    assertEquals(1, performances.size(),
        "Creating an event from the EP menu should add the created performance to the system.");
    assertEquals(1, provider.getEvents().size(),
        "Creating an event from the EP menu should add the event to the provider.");
    assertEquals("SUCCESS: Event created successfully.", view.getLastSuccessMessage(),
        "Successful event creation should show a confirmation message.");
  }

  // Verifies that booking through the menu still creates the booking and shows its record.
  @Test
  void studentMenuCanBookPerformance() {
    EntertainmentProvider provider = new EntertainmentProvider("provider@example.com", "password",
        "Festival Org", "BN-1234567", "Provider Rep", "Runs live events");
    Student student =
        new Student("student@ed.ac.uk", "password", "Hagan", 123456789, new StudentPreferences());
    Event event = new Event(1L, "Spring Concert", EventType.MUSIC, true, provider);
    Performance performance = new Performance(1L, LocalDateTime.now().plusDays(7),
        LocalDateTime.now().plusDays(7).plusHours(2), List.of("Performer"), "McEwan Hall", 300,
        false, false, 150, 0, 12.50, PerformanceStatus.ACTIVE, event);
    performances.add(performance);

    ExhaustingScriptedView view = new ExhaustingScriptedView("5", "1", "2");
    MenuController menuController = createMenuController(view);
    userController.setCurrentUser(student);

    assertThrows(IllegalStateException.class, menuController::mainMenu,
        "The scripted menu run should stop once scripted inputs are exhausted.");

    assertEquals(1, bookings.size(),
        "Booking from the student menu should add a booking to the system.");
    assertEquals("SUCCESS: Booking successful", view.getLastSuccessMessage(),
        "Successful booking through the student menu should show a success message.");
    assertNotNull(view.getLastDisplayedBookingRecord(),
        "Successful booking through the student menu should display a booking record.");
  }

  // Verifies that the student menu can route to the edit-preferences use case.
  @Test
  void studentMenuCanEditPreferences() {
    Student student =
        new Student("student@ed.ac.uk", "password", "Hagan", 123456789, new StudentPreferences());
    ExhaustingScriptedView view = new ExhaustingScriptedView("4", "music,dance");
    MenuController menuController = createMenuController(view);
    userController.setCurrentUser(student);

    assertThrows(IllegalStateException.class, menuController::mainMenu,
        "The scripted menu run should stop once scripted inputs are exhausted.");

    assertEquals("SUCCESS: Preferences updated successfully.", view.getLastSuccessMessage(),
        "Editing preferences through the student menu should succeed.");
  }

  // Verifies that menu-driven cancellation runs the full cancellation workflow and updates state.
  @Test
  void studentMenuCanCancelBooking() {
    EntertainmentProvider provider = new EntertainmentProvider("provider@example.com", "password",
        "Festival Org", "BN-1234567", "Provider Rep", "Runs live events");
    Student student =
        new Student("student@ed.ac.uk", "password", "Hagan", 123456789, new StudentPreferences());
    Event event = new Event(1L, "Spring Concert", EventType.MUSIC, true, provider);
    Performance performance = new Performance(1L, LocalDateTime.now().plusDays(7),
        LocalDateTime.now().plusDays(7).plusHours(2), List.of("Performer"), "McEwan Hall", 300,
        false, false, 150, 2, 12.50, PerformanceStatus.ACTIVE, event);
    Booking booking =
        new Booking(1L, 2, 25.00, LocalDateTime.now(), BookingStatus.ACTIVE, student, performance);
    performance.addBooking(booking);
    events.add(event);
    performances.add(performance);
    bookings.add(booking);

    ExhaustingScriptedView view = new ExhaustingScriptedView("6", "1");
    MenuController menuController = createMenuController(view);
    userController.setCurrentUser(student);

    assertThrows(IllegalStateException.class, menuController::mainMenu,
        "The scripted menu run should stop once scripted inputs are exhausted.");

    assertFalse(booking.isActive(),
        "Cancelling from the student menu should update the booking status.");
    assertEquals("SUCCESS: Booking cancelled successfully.", view.getLastSuccessMessage(),
        "Cancelling a booking through the student menu should show a success message.");
  }

  // Verifies that the EP menu routes to cancel-performance and updates the performance status.
  @Test
  void entertainmentProviderMenuCanCancelPerformance() {
    EntertainmentProvider provider = new EntertainmentProvider("provider@example.com", "password",
        "Festival Org", "BN-1234567", "Provider Rep", "Runs live events");
    Event event = new Event(1L, "Spring Concert", EventType.MUSIC, true, provider);
    Performance performance = new Performance(1L, LocalDateTime.now().plusDays(7),
        LocalDateTime.now().plusDays(7).plusHours(2), List.of("Performer"), "McEwan Hall", 300,
        false, false, 150, 0, 12.50, PerformanceStatus.ACTIVE, event);
    events.add(event);
    performances.add(performance);

    ExhaustingScriptedView view = new ExhaustingScriptedView("5", "1");
    MenuController menuController = createMenuController(view);
    userController.setCurrentUser(provider);

    assertThrows(IllegalStateException.class, menuController::mainMenu,
        "The scripted menu run should stop once scripted inputs are exhausted.");

    assertFalse(performance.isActive(),
        "Cancelling through the entertainment provider menu should cancel the performance.");
    assertEquals("SUCCESS: Cancellation Successful!", view.getLastSuccessMessage(),
        "Cancelling a performance through the entertainment provider menu should show a success message.");
  }

  private MenuController createMenuController(View view) {
    userController = new UserController(view, new MockVerificationSystem(), users, events);
    eventPerformanceController =
        new EventPerformanceController(view, events, performances, new MockPaymentSystem());
    bookingController =
        new BookingController(view, new MockPaymentSystem(), events, performances, bookings);
    return new MenuController(view, userController, eventPerformanceController, bookingController);
  }

  private static final class ExhaustingScriptedView implements View {
    private final Deque<String> scriptedInputs;
    private final List<String> successMessages;
    private final List<String> errorMessages;
    private final List<Collection<String>> displayedPerformanceLists;
    private final List<String> displayedPerformances;
    private final List<String> displayedBookingRecords;

    private ExhaustingScriptedView(String... scriptedInputs) {
      this.scriptedInputs = new ArrayDeque<>(List.of(scriptedInputs));
      this.successMessages = new ArrayList<>();
      this.errorMessages = new ArrayList<>();
      this.displayedPerformanceLists = new ArrayList<>();
      this.displayedPerformances = new ArrayList<>();
      this.displayedBookingRecords = new ArrayList<>();
    }

    @Override
    public String getInput(String inputPrompt) {
      if (scriptedInputs.isEmpty()) {
        throw new IllegalStateException("No scripted input remaining for prompt: " + inputPrompt);
      }
      return scriptedInputs.removeFirst();
    }

    @Override
    public String getInput(String inputPrompt, String promptEnd) {
      return getInput(inputPrompt);
    }

    @Override
    public void displaySuccess(String successMessage) {
      successMessages.add("SUCCESS: " + successMessage);
    }

    @Override
    public void displayError(String errorMessage) {
      errorMessages.add("ERROR: " + errorMessage);
    }

    @Override
    public void displayListOfPerformances(Collection<String> listOfPerformanceInfo) {
      displayedPerformanceLists.add(new ArrayList<>(listOfPerformanceInfo));
    }

    @Override
    public void displaySpecificPerformance(String performanceInfo) {
      displayedPerformances.add(performanceInfo);
    }

    @Override
    public void displayBookingRecord(String bookingRecord) {
      displayedBookingRecords.add(bookingRecord);
    }

    private String getLastSuccessMessage() {
      return successMessages.isEmpty() ? null : successMessages.get(successMessages.size() - 1);
    }

    private List<String> getErrorMessages() {
      return errorMessages;
    }

    private String getLastDisplayedBookingRecord() {
      return displayedBookingRecords.isEmpty() ? null
          : displayedBookingRecords.get(displayedBookingRecords.size() - 1);
    }
  }
}
