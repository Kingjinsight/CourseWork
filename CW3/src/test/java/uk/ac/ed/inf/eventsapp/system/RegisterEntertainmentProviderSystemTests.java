package uk.ac.ed.inf.eventsapp.system;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uk.ac.ed.inf.eventsapp.controller.UserController;
import uk.ac.ed.inf.eventsapp.integration.MockVerificationSystem;
import uk.ac.ed.inf.eventsapp.model.EntertainmentProvider;
import uk.ac.ed.inf.eventsapp.model.Student;
import uk.ac.ed.inf.eventsapp.model.StudentPreferences;
import uk.ac.ed.inf.eventsapp.model.User;

/**
 * System tests for the register entertainment provider use case.
 */
public class RegisterEntertainmentProviderSystemTests {
  private static final String VALID_EMAIL = "provider@gmail.com";
  private static final String VALID_PASSWORD = "password";
  private static final String VALID_ORG = "New Events Ltd";
  private static final String VALID_BIZ_NUM = "1234567890"; // exactly 10 chars
  private static final String VALID_NAME = "Hagan";
  private static final String VALID_DESC = "Live events company";

  private Collection<User> users;

  @BeforeEach
  @SuppressWarnings("unused")
  void setUp() {
    users = new ArrayList<>();
  }


  @Test
  void providerCanBeRegisteredWithValidBusinessNumber() {
    ScriptedView view = new ScriptedView(VALID_EMAIL, VALID_PASSWORD, VALID_ORG, VALID_BIZ_NUM,
        VALID_NAME, VALID_DESC);
    UserController controller =
        new UserController(view, new MockVerificationSystem(), users, new ArrayList<>());
    controller.registerEntertainmentProvider();
    assertEquals("SUCCESS: Registration successful.", view.getLastSuccessMessage(),
        "Provider should receive success message after valid registration.");
  }

  @Test
  void registeredProviderIsAddedToUsers() {
    ScriptedView view = new ScriptedView(VALID_EMAIL, VALID_PASSWORD, VALID_ORG, VALID_BIZ_NUM,
        VALID_NAME, VALID_DESC);
    UserController controller =
        new UserController(view, new MockVerificationSystem(), users, new ArrayList<>());
    controller.registerEntertainmentProvider();
    assertTrue(users.stream().anyMatch(u -> u.getEmail().equals(VALID_EMAIL)),
        "Newly registered provider should be in the users collection.");
  }

  @Test
  void registeredProviderCanLogInAfterRegistration() {
    ScriptedView regView = new ScriptedView(VALID_EMAIL, VALID_PASSWORD, VALID_ORG, VALID_BIZ_NUM,
        VALID_NAME, VALID_DESC);
    UserController controller =
        new UserController(regView, new MockVerificationSystem(), users, new ArrayList<>());
    controller.registerEntertainmentProvider();

    ScriptedView loginView = new ScriptedView(VALID_EMAIL, VALID_PASSWORD);
    UserController loginController =
        new UserController(loginView, new MockVerificationSystem(), users, new ArrayList<>());
    loginController.login();
    assertEquals("SUCCESS: Login successful.", loginView.getLastSuccessMessage(),
        "Newly registered provider should be able to log in.");
  }

  // --- Access control ---

  @Test
  void loggedInUserCannotRegisterProvider() {
    Student student =
        new Student("student@ed.ac.uk", "pass", "Bob", 1234567, new StudentPreferences());
    ScriptedView view = new ScriptedView();
    UserController controller =
        new UserController(view, new MockVerificationSystem(), users, new ArrayList<>());
    controller.setCurrentUser(student);
    controller.registerEntertainmentProvider();
    assertEquals("ERROR: You must be logged out to register.", view.getLastErrorMessage(),
        "A logged-in user should not be able to register.");
  }

  // --- Verification failure ---

  @Test
  void shortBusinessNumberFailsVerification() {
    ScriptedView view =
        new ScriptedView(VALID_EMAIL, VALID_PASSWORD, VALID_ORG, "123", VALID_NAME, VALID_DESC);
    UserController controller =
        new UserController(view, new MockVerificationSystem(), users, new ArrayList<>());
    controller.registerEntertainmentProvider();
    assertEquals("ERROR: Business registration number could not be verified.",
        view.getLastErrorMessage(),
        "Business number shorter than 10 characters should fail verification.");
  }

  @Test
  void longBusinessNumberFailsVerification() {
    ScriptedView view = new ScriptedView(VALID_EMAIL, VALID_PASSWORD, VALID_ORG, "12345678901",
        VALID_NAME, VALID_DESC);
    UserController controller =
        new UserController(view, new MockVerificationSystem(), users, new ArrayList<>());
    controller.registerEntertainmentProvider();
    assertEquals("ERROR: Business registration number could not be verified.",
        view.getLastErrorMessage(),
        "Business number longer than 10 characters should fail verification.");
  }

  // --- Duplicate account detection ---

  @Test
  void duplicateEmailShowsError() {
    users.add(new EntertainmentProvider(VALID_EMAIL, "oldpass", "Old Org", "0987654321", "Old Name",
        "Old Desc"));
    ScriptedView view = new ScriptedView(VALID_EMAIL, VALID_PASSWORD, VALID_ORG, VALID_BIZ_NUM,
        VALID_NAME, VALID_DESC);
    UserController controller =
        new UserController(view, new MockVerificationSystem(), users, new ArrayList<>());
    controller.registerEntertainmentProvider();
    assertEquals("ERROR: An account already exists for that email address.",
        view.getLastErrorMessage(), "Duplicate email should be rejected.");
  }

  @Test
  void duplicateOrgNameShowsError() {
    users.add(new EntertainmentProvider("other@example.com", "oldpass", VALID_ORG, VALID_BIZ_NUM,
        "Old Name", "Old Desc"));
    ScriptedView view = new ScriptedView(VALID_EMAIL, VALID_PASSWORD, VALID_ORG, VALID_BIZ_NUM,
        VALID_NAME, VALID_DESC);
    UserController controller =
        new UserController(view, new MockVerificationSystem(), users, new ArrayList<>());
    controller.registerEntertainmentProvider();
    assertEquals("ERROR: An account already exists for that entertainment provider.",
        view.getLastErrorMessage(), "Duplicate organisation name should be rejected.");
  }

  @Test
  void duplicateBusinessNumberShowsError() {
    users.add(new EntertainmentProvider("other@example.com", "oldpass", "Other Org", VALID_BIZ_NUM,
        "Old Name", "Old Desc"));
    ScriptedView view = new ScriptedView(VALID_EMAIL, VALID_PASSWORD, VALID_ORG, VALID_BIZ_NUM,
        VALID_NAME, VALID_DESC);
    UserController controller =
        new UserController(view, new MockVerificationSystem(), users, new ArrayList<>());
    controller.registerEntertainmentProvider();
    assertEquals("ERROR: An account already exists for that entertainment provider.",
        view.getLastErrorMessage(), "Duplicate business registration number should be rejected.");
  }

  @Test
  void invalidEmailShowsErrorAndRequiresEmailAgain() {
    ScriptedView view = new ScriptedView("not-an-email", VALID_EMAIL, VALID_PASSWORD, VALID_ORG,
        VALID_BIZ_NUM, VALID_NAME, VALID_DESC);
    UserController controller =
        new UserController(view, new MockVerificationSystem(), users, new ArrayList<>());

    controller.registerEntertainmentProvider();

    assertEquals("ERROR: A valid email address is required.", view.getLastErrorMessage(),
        "Invalid email should show the corresponding validation error.");
    assertEquals("SUCCESS: Registration successful.", view.getLastSuccessMessage(),
        "Registration should continue after re-entering a valid email.");
  }

  @Test
  void emptyPasswordShowsErrorAndRequiresRegistrationDetailsAgain() {
    ScriptedView view = new ScriptedView(VALID_EMAIL, "", VALID_EMAIL, VALID_PASSWORD, VALID_ORG,
        VALID_BIZ_NUM, VALID_NAME, VALID_DESC);
    UserController controller =
        new UserController(view, new MockVerificationSystem(), users, new ArrayList<>());

    controller.registerEntertainmentProvider();

    assertEquals("ERROR: Password is required.", view.getLastErrorMessage(),
        "Empty password should show the corresponding validation error.");
    assertEquals("SUCCESS: Registration successful.", view.getLastSuccessMessage(),
        "Registration should continue after re-entering all details.");
  }

  @Test
  void emptyOrganisationNameShowsErrorAndRequiresRegistrationDetailsAgain() {
    ScriptedView view = new ScriptedView(VALID_EMAIL, VALID_PASSWORD, "", VALID_EMAIL,
        VALID_PASSWORD, VALID_ORG, VALID_BIZ_NUM, VALID_NAME, VALID_DESC);
    UserController controller =
        new UserController(view, new MockVerificationSystem(), users, new ArrayList<>());

    controller.registerEntertainmentProvider();

    assertEquals("ERROR: Organisation name is required.", view.getLastErrorMessage(),
        "Empty organisation name should show the corresponding validation error.");
    assertEquals("SUCCESS: Registration successful.", view.getLastSuccessMessage(),
        "Registration should continue after re-entering all details.");
  }

  @Test
  void emptyBusinessRegistrationNumberShowsErrorAndRequiresRegistrationDetailsAgain() {
    ScriptedView view = new ScriptedView(VALID_EMAIL, VALID_PASSWORD, VALID_ORG, "", VALID_EMAIL,
        VALID_PASSWORD, VALID_ORG, VALID_BIZ_NUM, VALID_NAME, VALID_DESC);
    UserController controller =
        new UserController(view, new MockVerificationSystem(), users, new ArrayList<>());

    controller.registerEntertainmentProvider();

    assertEquals("ERROR: Business registration number is required.", view.getLastErrorMessage(),
        "Empty business number should show the corresponding validation error.");
    assertEquals("SUCCESS: Registration successful.", view.getLastSuccessMessage(),
        "Registration should continue after re-entering all details.");
  }

  @Test
  void emptyContactNameShowsErrorAndRequiresRegistrationDetailsAgain() {
    ScriptedView view = new ScriptedView(VALID_EMAIL, VALID_PASSWORD, VALID_ORG, VALID_BIZ_NUM, "",
        VALID_EMAIL, VALID_PASSWORD, VALID_ORG, VALID_BIZ_NUM, VALID_NAME, VALID_DESC);
    UserController controller =
        new UserController(view, new MockVerificationSystem(), users, new ArrayList<>());

    controller.registerEntertainmentProvider();

    assertEquals("ERROR: Main contact name is required.", view.getLastErrorMessage(),
        "Empty contact name should show the corresponding validation error.");
    assertEquals("SUCCESS: Registration successful.", view.getLastSuccessMessage(),
        "Registration should continue after re-entering all details.");
  }

  @Test
  void emptyDescriptionShowsErrorAndRequiresRegistrationDetailsAgain() {
    ScriptedView view =
        new ScriptedView(VALID_EMAIL, VALID_PASSWORD, VALID_ORG, VALID_BIZ_NUM, VALID_NAME, "",
            VALID_EMAIL, VALID_PASSWORD, VALID_ORG, VALID_BIZ_NUM, VALID_NAME, VALID_DESC);
    UserController controller =
        new UserController(view, new MockVerificationSystem(), users, new ArrayList<>());

    controller.registerEntertainmentProvider();

    assertEquals("ERROR: Description is required.", view.getLastErrorMessage(),
        "Empty description should show the corresponding validation error.");
    assertEquals("SUCCESS: Registration successful.", view.getLastSuccessMessage(),
        "Registration should continue after re-entering all details.");
  }
}
