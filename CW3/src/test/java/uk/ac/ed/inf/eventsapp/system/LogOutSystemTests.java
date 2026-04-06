package uk.ac.ed.inf.eventsapp.system;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uk.ac.ed.inf.eventsapp.controller.UserController;
import uk.ac.ed.inf.eventsapp.integration.MockVerificationSystem;
import uk.ac.ed.inf.eventsapp.model.EntertainmentProvider;
import uk.ac.ed.inf.eventsapp.model.Student;
import uk.ac.ed.inf.eventsapp.model.StudentPreferences;

/**
 * System tests for the log-out use case.
 */
public class LogOutSystemTests {
  private Student student;
  private EntertainmentProvider provider;

  @BeforeEach
  @SuppressWarnings("unused")
  void setUp() {
    student =
        new Student("student@ed.ac.uk", "password", "Hagan", 1234567, new StudentPreferences());
    provider = new EntertainmentProvider("provider@gmail.com", "password", "Org", "1234567890",
        "Bob", "Desc");
  }



  @Test
  void loggedInStudentCanLogOut() {
    ScriptedView view = new ScriptedView();
    UserController controller = new UserController(view, new MockVerificationSystem(),
        new ArrayList<>(), new ArrayList<>());
    controller.setCurrentUser(student);
    controller.logout();
    assertEquals("SUCCESS: Logout successful.", view.getLastSuccessMessage(),
        "Logged-in student should receive success message after logout.");
  }

  @Test
  void loggedInProviderCanLogOut() {
    ScriptedView view = new ScriptedView();
    UserController controller = new UserController(view, new MockVerificationSystem(),
        new ArrayList<>(), new ArrayList<>());
    controller.setCurrentUser(provider);
    controller.logout();
    assertEquals("SUCCESS: Logout successful.", view.getLastSuccessMessage(),
        "Logged-in provider should receive success message after logout.");
  }

  @Test
  void currentUserIsNullAfterLogout() {
    ScriptedView view = new ScriptedView();
    UserController controller = new UserController(view, new MockVerificationSystem(),
        new ArrayList<>(), new ArrayList<>());
    controller.setCurrentUser(student);
    controller.logout();
    assertNull(controller.getCurrentUser(), "Current user should be null after logout.");
  }

  // --- Access control ---

  @Test
  void guestCannotLogOut() {
    ScriptedView view = new ScriptedView();
    UserController controller = new UserController(view, new MockVerificationSystem(),
        new ArrayList<>(), new ArrayList<>());
    controller.logout();
    assertEquals("ERROR: You are not logged in.", view.getLastErrorMessage(),
        "Guest user should see an error when attempting to log out.");
  }
}
