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
    provider = new EntertainmentProvider("provider@gmail.com", "password", "EooEle", "123",
        "Provider", "This is EooEle");
    student =
        new Student("student@ed.ac.uk", "password", "Hagan", 1234567, new StudentPreferences());
  }

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

  // --- Access control ---

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

  @Test
  void guestCannotEditPreferences() {
    ScriptedView view = new ScriptedView();
    UserController controller = new UserController(view, new MockVerificationSystem(),
        new ArrayList<>(), new ArrayList<>());

    controller.editPreferences();

    assertEquals("ERROR: Only students can edit preferences.", view.getLastErrorMessage(),
        "Guest (no user) should be rejected.");
  }

  // --- Input validation ---

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

  // --- State verification ---

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
