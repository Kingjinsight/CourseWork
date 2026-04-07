package uk.ac.ed.inf.eventsapp.system;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import uk.ac.ed.inf.eventsapp.facultypreregistration.FacultyMember;
import uk.ac.ed.inf.eventsapp.facultypreregistration.RegistrationUtility;

/**
 * System tests for the even-group lazy faculty registration feature.
 */
public class RegisterFacultyMemberSystemTests {
  @TempDir
  Path tempDir;

  // Verifies that the first successful faculty login lazily creates and returns the account.
  @Test
  void firstLoginAttemptCreatesFacultyAccountAndLogsTheFacultyMemberIn() throws IOException {
    Path facultyFile = createFacultyFile("xxxxrt@ed.ac.uk,encrypted-xxxxrt-password",
        "abcde@ed.ac.uk,encrypted-abcde-password");
    RegistrationUtility utility = new RegistrationUtility(facultyFile.toString());

    FacultyMember facultyMember =
        utility.loginFacultyMember("abcde@ed.ac.uk", "encrypted-abcde-password");

    assertNotNull(facultyMember,
        "The first listed faculty login attempt should create and return a faculty account.");
    assertEquals(1, utility.getRegisteredFacultyMembers().size(),
        "The first login attempt by a listed faculty member should create exactly one account.");
  }

  // Verifies that repeated faculty logins reuse the same account instead of creating duplicates.
  @Test
  void repeatedSuccessfulLoginAttemptsDoNotCreateDuplicateFacultyAccounts() throws IOException {
    Path facultyFile = createFacultyFile("xxxxrt@ed.ac.uk,encrypted-xxxxrt-password");
    RegistrationUtility utility = new RegistrationUtility(facultyFile.toString());

    utility.loginFacultyMember("xxxxrt@ed.ac.uk", "encrypted-xxxxrt-password");
    utility.loginFacultyMember("xxxxrt@ed.ac.uk", "encrypted-xxxxrt-password");

    assertEquals(1, utility.getRegisteredFacultyMembers().size(),
        "Repeated login attempts by the same faculty member should not create duplicates.");
  }

  // Verifies that emails missing from the configured faculty file do not produce accounts.
  @Test
  void unknownEmailDoesNotProduceAFacultyAccount() throws IOException {
    Path facultyFile = createFacultyFile("xxxxrt@ed.ac.uk,encrypted-xxxxrt-password");
    RegistrationUtility utility = new RegistrationUtility(facultyFile.toString());

    FacultyMember facultyMember = utility.loginFacultyMember("qwerty@ed.ac.uk", "some-password");

    assertNull(facultyMember,
        "Only email addresses present in the configured faculty file should be registered.");
  }

  // Verifies that malformed rows are ignored so valid faculty entries remain usable.
  @Test
  void invalidRowsDoNotBlockLoginForFacultyMembersOnValidRows() throws IOException {
    Path facultyFile = createFacultyFile("invalid-row-without-comma",
        "xxxxrt@ed.ac.uk,encrypted-xxxxrt-password", "abcde@ed.ac.uk, ");
    RegistrationUtility utility = new RegistrationUtility(facultyFile.toString());

    FacultyMember facultyMember =
        utility.loginFacultyMember("xxxxrt@ed.ac.uk", "encrypted-xxxxrt-password");

    assertNotNull(facultyMember,
        "Malformed rows should be ignored so valid faculty members can still log in.");
    assertEquals(1, utility.getRegisteredFacultyMembers().size(),
        "Only the valid matching row should result in a registered faculty account.");
  }

  private Path createFacultyFile(String... lines) throws IOException {
    Path facultyFile = tempDir.resolve("faculty.csv");
    Files.write(facultyFile, java.util.List.of(lines));
    return facultyFile;
  }
}
