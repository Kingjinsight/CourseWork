package uk.ac.ed.inf.eventsapp.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import uk.ac.ed.inf.eventsapp.facultypreregistration.FacultyMember;

/**
 * Unit tests for faculty-member accounts.
 */
public class TestFacultyMember {
  // Verifies that a newly created faculty account stores the login-attempt count that created it.
  @Test
  void facultyMemberTracksInitialLoginAttempts() {
    FacultyMember facultyMember =
        new FacultyMember("xxxxrt@ed.ac.uk", "encrypted-xxxxrt-password", 1);

    assertEquals(1, facultyMember.getLoginAttempts(),
        "A lazily created faculty account should record the triggering login attempt.");
  }

  // Verifies that recording another login attempt increments the stored count.
  @Test
  void recordLoginAttemptIncrementsTheAttemptCount() {
    FacultyMember facultyMember =
        new FacultyMember("xxxxrt@ed.ac.uk", "encrypted-xxxxrt-password", 1);

    facultyMember.recordLoginAttempt();

    assertEquals(2, facultyMember.getLoginAttempts(),
        "Each additional login attempt should increment the faculty member's counter.");
  }

  // Verifies that password matching succeeds when the supplied password matches the stored
  // password.
  @Test
  void passwordMatchesReturnsTrueWhenTheProvidedPasswordMatches() {
    FacultyMember facultyMember =
        new FacultyMember("xxxxrt@ed.ac.uk", "encrypted-xxxxrt-password", 0);

    assertTrue(facultyMember.passwordMatches("encrypted-xxxxrt-password"),
        "Password matching should succeed when the supplied password matches the stored one.");
  }

  // Verifies that password matching fails when the supplied password differs from the stored
  // password.
  @Test
  void passwordMatchesReturnsFalseWhenTheProvidedPasswordDoesNotMatch() {
    FacultyMember facultyMember =
        new FacultyMember("xxxxrt@ed.ac.uk", "encrypted-xxxxrt-password", 0);

    assertFalse(facultyMember.passwordMatches("wrong-password"),
        "Password matching should fail when the supplied password does not match.");
  }
}
