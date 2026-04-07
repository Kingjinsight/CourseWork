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
    // Builds one student and one provider so logout can be exercised across both supported roles.
    student =
        new Student("student@ed.ac.uk", "password", "Hagan", 1234567, new StudentPreferences());
    provider = new EntertainmentProvider("provider@gmail.com", "password", "Org", "1234567890",
        "Bob", "Desc");
  }



  // Verifies that a logged-in student can log out successfully.
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

  // Verifies that a logged-in entertainment provider can log out successfully.
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

  // Verifies that logout clears the controller's current user.
  @Test
  void currentUserIsNullAfterLogout() {
    ScriptedView view = new ScriptedView();
    UserController controller = new UserController(view, new MockVerificationSystem(),
        new ArrayList<>(), new ArrayList<>());
    controller.setCurrentUser(student);
    controller.logout();
    assertNull(controller.getCurrentUser(), "Current user should be null after logout.");
  }

  // Verifies that logout is rejected when no user is currently logged in.
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
