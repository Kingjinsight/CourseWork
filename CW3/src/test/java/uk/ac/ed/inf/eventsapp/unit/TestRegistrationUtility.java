package uk.ac.ed.inf.eventsapp.unit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uk.ac.ed.inf.eventsapp.facultypreregistration.FacultyMember;
import uk.ac.ed.inf.eventsapp.facultypreregistration.RegistrationUtility;

/**
 * Unit tests for the even-group faculty registration utility.
 */
public class TestRegistrationUtility {
  @TempDir
  Path tempDir;

  // Verifies that a matching faculty email is lazily registered from the configured file.
  @Test
  void registerFacultyMemberReturnsMatchingFacultyFromConfiguredFile() throws IOException {
    Path facultyFile = createFacultyFile("xxxxrt@ed.ac.uk,encrypted-xxxxrt-password",
        "abcde@ed.ac.uk,encrypted-abcde-password");
    RegistrationUtility utility = new RegistrationUtility(facultyFile.toString());

    FacultyMember facultyMember = utility.registerFacultyMember("xxxxrt@ed.ac.uk");

    assertEquals("xxxxrt@ed.ac.uk", facultyMember.getEmail(),
        "A matching faculty email should be lazily registered from the configured file.");
  }

  // Verifies that unknown faculty emails do not create accounts.
  @Test
  void registerFacultyMemberReturnsNullWhenEmailIsMissingFromConfiguredFile() throws IOException {
    Path facultyFile = createFacultyFile("xxxxrt@ed.ac.uk,encrypted-xxxxrt-password");
    RegistrationUtility utility = new RegistrationUtility(facultyFile.toString());

    FacultyMember facultyMember = utility.registerFacultyMember("qwerty@ed.ac.uk");

    assertNull(facultyMember,
        "A login attempt for an email outside the faculty file should not create an account.");
  }

  // Verifies that malformed rows are skipped while valid rows remain usable.
  @Test
  void registerFacultyMemberSkipsInvalidRowsAndStillFindsValidFaculty() throws IOException {
    Path facultyFile = createFacultyFile("invalid-row-without-comma", " ,missing-email", "abc,def",
        "xxxxrt@ed.ac.uk,encrypted-xxxxrt-password", "abcde@ed.ac.uk, ");
    RegistrationUtility utility = new RegistrationUtility(facultyFile.toString());

    FacultyMember facultyMember = utility.registerFacultyMember("xxxxrt@ed.ac.uk");

    assertNotNull(facultyMember,
        "Invalid rows should be skipped so valid faculty entries remain usable.");
  }

  // Verifies that repeated registration attempts reuse the same faculty instance.
  @Test
  void repeatedRegistrationReturnsSameFacultyInstance() throws IOException {
    Path facultyFile = createFacultyFile("xxxxrt@ed.ac.uk,encrypted-xxxxrt-password");
    RegistrationUtility utility = new RegistrationUtility(facultyFile.toString());

    FacultyMember firstRegistration = utility.registerFacultyMember("xxxxrt@ed.ac.uk");
    FacultyMember secondRegistration = utility.registerFacultyMember("xxxxrt@ed.ac.uk");

    assertSame(firstRegistration, secondRegistration,
        "Repeated login attempts should reuse the previously created faculty account.");
  }

  // Verifies that listed faculty members can log in using the configured password.
  @Test
  void loginFacultyMemberReturnsFacultyMemberWhenCredentialsMatch() throws IOException {
    Path facultyFile = createFacultyFile("xxxxrt@ed.ac.uk,encrypted-xxxxrt-password");
    RegistrationUtility utility = new RegistrationUtility(facultyFile.toString());

    FacultyMember facultyMember =
        utility.loginFacultyMember("xxxxrt@ed.ac.uk", "encrypted-xxxxrt-password");

    assertNotNull(facultyMember,
        "A listed faculty member should be able to log in using the configured password.");
  }

  // Verifies that faculty login fails when the supplied password does not match the stored one.
  @Test
  void loginFacultyMemberReturnsNullWhenPasswordDoesNotMatch() throws IOException {
    Path facultyFile = createFacultyFile("xxxxrt@ed.ac.uk,encrypted-xxxxrt-password");
    RegistrationUtility utility = new RegistrationUtility(facultyFile.toString());

    FacultyMember facultyMember = utility.loginFacultyMember("xxxxrt@ed.ac.uk", "wrong-password");

    assertNull(facultyMember,
        "Faculty login should fail when the supplied password does not match.");
  }

  // Verifies that file changes are picked up so later valid faculty rows can be registered.
  @Test
  void registerFacultyMemberReloadsFacultyFileAfterItsContentsChange() throws IOException {
    Path facultyFile = createFacultyFile("xxxxrt@ed.ac.uk,encrypted-xxxxrt-password");
    RegistrationUtility utility = new RegistrationUtility(facultyFile.toString());

    utility.registerFacultyMember("xxxxrt@ed.ac.uk");
    Files.write(facultyFile, java.util.List.of("xxxxrt@ed.ac.uk,encrypted-xxxxrt-password",
        "abcde@ed.ac.uk,encrypted-abcde-password"));

    FacultyMember facultyMember = utility.registerFacultyMember("abcde@ed.ac.uk");

    assertEquals("abcde@ed.ac.uk", facultyMember.getEmail(),
        "The faculty cache should refresh when the configured file contents change.");
  }

  // Verifies that no faculty account is created when the configured file contains only invalid
  // rows.
  @Test
  void registerFacultyMemberReturnsNullWhenEveryRowIsInvalid() throws IOException {
    Path facultyFile = createFacultyFile("invalid-row-without-comma", "abc,def",
        "xxxxrt@ed.ac.uk, ", ",missing-password");
    RegistrationUtility utility = new RegistrationUtility(facultyFile.toString());

    FacultyMember facultyMember = utility.registerFacultyMember("xxxxrt@ed.ac.uk");

    assertNull(facultyMember, "When every row is invalid, no faculty account should be created.");
  }

  // Verifies that concurrent registration attempts still create only one shared faculty account.
  @Test
  void concurrentRegistrationForSameEmailCreatesOnlyOneAccount() throws Exception {
    Path facultyFile = createFacultyFile("xxxxrt@ed.ac.uk,encrypted-xxxxrt-password");
    RegistrationUtility utility = new RegistrationUtility(facultyFile.toString());
    int concurrentAttempts = 8;
    ExecutorService executorService = Executors.newFixedThreadPool(concurrentAttempts);
    CountDownLatch ready = new CountDownLatch(concurrentAttempts);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<FacultyMember>> futures = new ArrayList<>();
    FacultyMember firstFacultyMember;

    try {
      for (int i = 0; i < concurrentAttempts; i++) {
        futures.add(executorService.submit(() -> {
          ready.countDown();
          start.await();
          return utility.loginFacultyMember("xxxxrt@ed.ac.uk", "encrypted-xxxxrt-password");
        }));
      }

      ready.await();
      start.countDown();

      firstFacultyMember = futures.get(0).get();
      for (Future<FacultyMember> future : futures) {
        assertSame(firstFacultyMember, future.get(),
            "Concurrent registration attempts should all reuse the same faculty account.");
      }
    } finally {
      executorService.shutdownNow();
    }

    assertEquals(1, utility.getRegisteredFacultyMembers().size(),
        "Concurrent login attempts should not create duplicate faculty accounts.");
    assertEquals(concurrentAttempts, firstFacultyMember.getLoginAttempts(),
        "Every concurrent login attempt should be reflected in the faculty account.");
  }

  private Path createFacultyFile(String... lines) throws IOException {
    Path facultyFile = tempDir.resolve("faculty.csv");
    Files.write(facultyFile, java.util.List.of(lines));
    return facultyFile;
  }
}
