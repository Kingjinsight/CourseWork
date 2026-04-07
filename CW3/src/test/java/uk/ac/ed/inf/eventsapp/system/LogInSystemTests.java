package uk.ac.ed.inf.eventsapp.system;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uk.ac.ed.inf.eventsapp.controller.UserController;
import uk.ac.ed.inf.eventsapp.integration.MockVerificationSystem;
import uk.ac.ed.inf.eventsapp.model.EntertainmentProvider;
import uk.ac.ed.inf.eventsapp.model.Student;
import uk.ac.ed.inf.eventsapp.model.StudentPreferences;
import uk.ac.ed.inf.eventsapp.model.User;

/**
 * System tests for the log-in use case.
 */
public class LogInSystemTests {
  private Collection<User> users;
  private Student student;
  private EntertainmentProvider provider;

  @BeforeEach
  @SuppressWarnings("unused")
  void setUp() {
    // Builds a shared user collection containing one student and one provider for each login
    // scenario.
    users = new ArrayList<>();
    student =
        new Student("student@ed.ac.uk", "password", "Hagan", 1234567, new StudentPreferences());
    provider = new EntertainmentProvider("provider@gmail.com", "password", "Org", "1234567890",
        "Bob", "Desc");
    users.add(student);
    users.add(provider);
  }


  // Verifies that a registered student can log in successfully.
  @Test
  void registeredStudentCanLogIn() {
    ScriptedView view = new ScriptedView("student@ed.ac.uk", "password");
    UserController controller =
        new UserController(view, new MockVerificationSystem(), users, new ArrayList<>());
    controller.login();
    assertEquals("SUCCESS: Login successful.", view.getLastSuccessMessage(),
        "Student should receive success message after login.");
  }

  // Verifies that a registered entertainment provider can log in successfully.
  @Test
  void registeredProviderCanLogIn() {
    ScriptedView view = new ScriptedView("provider@gmail.com", "password");
    UserController controller =
        new UserController(view, new MockVerificationSystem(), users, new ArrayList<>());
    controller.login();
    assertEquals("SUCCESS: Login successful.", view.getLastSuccessMessage(),
        "Provider should receive success message after login.");
  }

  // Verifies that a successful login updates the controller's current user.
  @Test
  void currentUserIsSetAfterLogin() {
    ScriptedView view = new ScriptedView("student@ed.ac.uk", "password");
    UserController controller =
        new UserController(view, new MockVerificationSystem(), users, new ArrayList<>());
    controller.login();
    assertEquals(student, controller.getCurrentUser(),
        "Current user should be the logged-in student.");
  }

  // Verifies that login is rejected when another user is already authenticated.
  @Test
  void alreadyLoggedInUserCannotLoginAgain() {
    ScriptedView view = new ScriptedView();
    UserController controller =
        new UserController(view, new MockVerificationSystem(), users, new ArrayList<>());
    controller.setCurrentUser(student);
    controller.login();
    assertEquals("ERROR: You are already logged in.", view.getLastErrorMessage(),
        "Already logged-in user should see an error.");
  }

  // Verifies that an incorrect password triggers the generic login error before a successful retry.
  @Test
  void wrongPasswordShowsError() {
    ScriptedView view =
        new ScriptedView("student@ed.ac.uk", "wrongpass", "student@ed.ac.uk", "password");
    UserController controller =
        new UserController(view, new MockVerificationSystem(), users, new ArrayList<>());
    controller.login();
    assertEquals("ERROR: Invalid email or password.", view.getLastErrorMessage(),
        "Wrong password should show an error.");
  }

  // Verifies that an unknown email address triggers the generic login error before a successful
  // retry.
  @Test
  void nonExistentEmailShowsError() {
    ScriptedView view =
        new ScriptedView("unknown@ed.ac.uk", "password", "student@ed.ac.uk", "password");
    UserController controller =
        new UserController(view, new MockVerificationSystem(), users, new ArrayList<>());
    controller.login();
    assertEquals("ERROR: Invalid email or password.", view.getLastErrorMessage(),
        "Non-existent email should show an error.");
  }

  // Verifies that email matching remains case-sensitive.
  @Test
  void loginIsCaseSensitiveForEmail() {
    ScriptedView view =
        new ScriptedView("STUDENT@ED.AC.UK", "password", "student@ed.ac.uk", "password");
    UserController controller =
        new UserController(view, new MockVerificationSystem(), users, new ArrayList<>());
    controller.login();
    assertEquals("ERROR: Invalid email or password.", view.getLastErrorMessage(),
        "Email matching should be case-sensitive.");
  }

  // Verifies that password matching remains case-sensitive.
  @Test
  void loginIsCaseSensitiveForPassword() {
    ScriptedView view =
        new ScriptedView("student@ed.ac.uk", "PASSWORD", "student@ed.ac.uk", "password");
    UserController controller =
        new UserController(view, new MockVerificationSystem(), users, new ArrayList<>());
    controller.login();
    assertEquals("ERROR: Invalid email or password.", view.getLastErrorMessage(),
        "Password matching should be case-sensitive.");
  }

  // Verifies that malformed email input still produces the generic login failure message.
  @Test
  void invalidEmailFormatShowsError() {
    ScriptedView view = new ScriptedView("not-an-email", "student@ed.ac.uk", "password");
    UserController controller =
        new UserController(view, new MockVerificationSystem(), users, new ArrayList<>());
    controller.login();
    assertEquals("ERROR: Invalid email or password.", view.getLastErrorMessage(),
        "Invalid email format should show the generic login error.");
  }

  // Verifies that an empty password produces the generic login failure message before a successful
  // retry.
  @Test
  void emptyPasswordShowsError() {
    ScriptedView view = new ScriptedView("student@ed.ac.uk", "", "student@ed.ac.uk", "password");
    UserController controller =
        new UserController(view, new MockVerificationSystem(), users, new ArrayList<>());
    controller.login();
    assertEquals("ERROR: Invalid email or password.", view.getLastErrorMessage(),
        "Empty password should show the generic login error.");
  }
}
