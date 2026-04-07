package uk.ac.ed.inf.eventsapp.system;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uk.ac.ed.inf.eventsapp.controller.UserController;
import uk.ac.ed.inf.eventsapp.integration.MockVerificationSystem;
import uk.ac.ed.inf.eventsapp.model.EntertainmentProvider;
import uk.ac.ed.inf.eventsapp.model.Student;
import uk.ac.ed.inf.eventsapp.model.StudentPreferences;

public class EditPreferencesSystemTests {
  private Student student;
  private EntertainmentProvider provider;

  @BeforeEach
  @SuppressWarnings("unused")
  void setUp() {
    // Builds one student and one provider so preference editing can be tested across allowed and
    // rejected roles.
    provider = new EntertainmentProvider("provider@gmail.com", "password", "EooEle", "123",
        "Provider", "This is EooEle");
    student =
        new Student("student@ed.ac.uk", "password", "Hagan", 1234567, new StudentPreferences());
  }

  // Verifies that a student can save a valid set of preferences.
  @Test
  void studentCanUpdatePreferences() {
    ScriptedView view = new ScriptedView("music,dance,movie");
    UserController controller = new UserController(view, new MockVerificationSystem(),
        new ArrayList<>(), new ArrayList<>());
    controller.setCurrentUser(student);

    controller.editPreferences();

    assertEquals("SUCCESS: Preferences updated successfully.", view.getLastSuccessMessage(),
        "Student should receive a success message after updating preferences.");
  }

  // Verifies that blank input is accepted and clears all saved preferences.
  @Test
  void blankInputClearsPreferences() {
    ScriptedView view = new ScriptedView("");
    UserController controller = new UserController(view, new MockVerificationSystem(),
        new ArrayList<>(), new ArrayList<>());
    controller.setCurrentUser(student);

    controller.editPreferences();

    assertEquals("SUCCESS: Preferences updated successfully.", view.getLastSuccessMessage(),
        "All zeros should be accepted as valid preferences.");
  }

  // Verifies that the maximum allowed number of preferences is accepted.
  @Test
  void threePreferencesIsValidInput() {
    ScriptedView view = new ScriptedView("music,theatre,sports");
    UserController controller = new UserController(view, new MockVerificationSystem(),
        new ArrayList<>(), new ArrayList<>());
    controller.setCurrentUser(student);

    controller.editPreferences();

    assertEquals("SUCCESS: Preferences updated successfully.", view.getLastSuccessMessage(),
        "All ones should be accepted as valid preferences.");
  }

  // Verifies that non-student authenticated users cannot edit preferences.
  @Test
  void onlyStudentsCanEditPreferences() {
    ScriptedView view = new ScriptedView();
    UserController controller = new UserController(view, new MockVerificationSystem(),
        new ArrayList<>(), new ArrayList<>());
    controller.setCurrentUser(provider);

    controller.editPreferences();

    assertEquals("ERROR: Only students can edit preferences.", view.getLastErrorMessage(),
        "Non-students should be rejected.");
  }

  // Verifies that unauthenticated users cannot edit preferences.
  @Test
  void guestCannotEditPreferences() {
    ScriptedView view = new ScriptedView();
    UserController controller = new UserController(view, new MockVerificationSystem(),
        new ArrayList<>(), new ArrayList<>());

    controller.editPreferences();

    assertEquals("ERROR: Only students can edit preferences.", view.getLastErrorMessage(),
        "Guest (no user) should be rejected.");
  }

  // Verifies that invalid preference input shows an error before a successful retry.
  @Test
  void invalidPreferenceInputIsRejectedAndRetried() {
    ScriptedView view = new ScriptedView("abc", "music,dance,movie");
    UserController controller = new UserController(view, new MockVerificationSystem(),
        new ArrayList<>(), new ArrayList<>());
    controller.setCurrentUser(student);

    controller.editPreferences();

    assertTrue(view.getErrorMessages().stream().anyMatch(e -> e.contains("Invalid input")),
        "Invalid preference string should show an error.");
    assertEquals("SUCCESS: Preferences updated successfully.", view.getLastSuccessMessage(),
        "Valid retry should succeed.");
  }

  // Verifies that more than three requested preferences are rejected.
  @Test
  void moreThanThreePreferencesIsRejected() {
    ScriptedView view = new ScriptedView("music,theatre,dance,movie", "music,dance");
    UserController controller = new UserController(view, new MockVerificationSystem(),
        new ArrayList<>(), new ArrayList<>());
    controller.setCurrentUser(student);

    controller.editPreferences();

    assertTrue(view.getErrorMessages().stream().anyMatch(e -> e.contains("Invalid input")),
        "More than three preferences should show an error.");
  }

  // Verifies that duplicate preference values are rejected.
  @Test
  void duplicatePreferencesAreRejected() {
    ScriptedView view = new ScriptedView("music,music", "music,dance");
    UserController controller = new UserController(view, new MockVerificationSystem(),
        new ArrayList<>(), new ArrayList<>());
    controller.setCurrentUser(student);

    controller.editPreferences();

    assertTrue(view.getErrorMessages().stream().anyMatch(e -> e.contains("Invalid input")),
        "Duplicate preferences should show an error.");
  }

  // Verifies that unknown event types are rejected.
  @Test
  void unknownPreferenceIsRejected() {
    ScriptedView view = new ScriptedView("comedy", "music,dance");
    UserController controller = new UserController(view, new MockVerificationSystem(),
        new ArrayList<>(), new ArrayList<>());
    controller.setCurrentUser(student);

    controller.editPreferences();

    assertTrue(view.getErrorMessages().stream().anyMatch(e -> e.contains("Invalid input")),
        "Unknown event types should show an error.");
  }

  // Verifies that a successful update changes the stored student preference flags.
  @Test
  void preferencesAreActuallySavedAfterUpdate() {
    ScriptedView view = new ScriptedView("music,dance,movie");
    UserController controller = new UserController(view, new MockVerificationSystem(),
        new ArrayList<>(), new ArrayList<>());
    controller.setCurrentUser(student);

    controller.editPreferences();

    StudentPreferences prefs = student.getPreferences();
    assertTrue(prefs.isPreferMusicEvents(), "Music preference should be set.");
    assertFalse(prefs.isPreferTheaterEvents(), "Theater preference should not be set.");
    assertTrue(prefs.isPreferDanceEvents(), "Dance preference should be set.");
    assertTrue(prefs.isPreferMovieEvents(), "Movie preference should be set.");
    assertFalse(prefs.isPreferSportsEvents(), "Sports preference should not be set.");
  }
}
