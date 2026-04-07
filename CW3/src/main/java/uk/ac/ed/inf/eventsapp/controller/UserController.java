package uk.ac.ed.inf.eventsapp.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import uk.ac.ed.inf.eventsapp.integration.VerificationSystem;
import uk.ac.ed.inf.eventsapp.model.AdminStaff;
import uk.ac.ed.inf.eventsapp.model.EntertainmentProvider;
import uk.ac.ed.inf.eventsapp.model.Event;
import uk.ac.ed.inf.eventsapp.model.Student;
import uk.ac.ed.inf.eventsapp.model.StudentPreferences;
import uk.ac.ed.inf.eventsapp.model.User;
import uk.ac.ed.inf.eventsapp.util.InputParsers;
import uk.ac.ed.inf.eventsapp.util.PasswordUtils;
import uk.ac.ed.inf.eventsapp.view.View;

/**
 * Handles login, logout, and entertainment-provider registration.
 */
public class UserController extends Controller {
  public static final String PREREGISTERED_USERS_FILE_PATH =
      "src/main/resources/preregistered-users.csv";
  public static final String PREREGISTERED_ADMIN_FILE_PATH =
      "src/main/resources/preregistered-admin.csv";

  private final VerificationSystem verificationSystem;
  private final Collection<User> users;
  private final Collection<Event> events;

  /**
   * Creates a user controller and loads preregistered student/admin accounts from the configured
   * CSV files.
   *
   * @param view the text view used for interaction
   * @param verificationSystem the external verification-system adapter
   * @param users the shared user collection
   * @param events the shared event collection
   */
  public UserController(View view, VerificationSystem verificationSystem, Collection<User> users,
      Collection<Event> events) {
    this(view, verificationSystem, users, events, true);
  }

  /**
   * Creates a user controller.
   *
   * @param view the text view used for interaction
   * @param verificationSystem the external verification-system adapter
   * @param users the shared user collection
   * @param events the shared event collection
   * @param loadPreregisteredUsersFromFiles whether preregistered student/admin CSV files should be
   *        loaded during construction
   */
  public UserController(View view, VerificationSystem verificationSystem, Collection<User> users,
      Collection<Event> events, boolean loadPreregisteredUsersFromFiles) {
    super(view);
    this.verificationSystem = verificationSystem;
    this.users = users;
    this.events = events;
    if (loadPreregisteredUsersFromFiles) {
      addPreregisteredUsers();
    }
  }

  /**
   * Logs in a student, admin staff member, or entertainment provider.
   *
   * Re-prompts for credentials until a valid login is provided, unless the session is already
   * authenticated.
   */
  public void login() {
    if (!checkCurrentUserIsGuest()) {
      view.displayError("You are already logged in.");
      return;
    }

    boolean loggedIn = false;
    while (!loggedIn) {
      String email = view.getInput("Enter email").trim();
      if (!InputParsers.isValidEmail(email)) {
        view.displayError("Invalid email or password.");
        continue;
      }

      String password = view.getInput("Enter password").trim();
      if (password.isEmpty()) {
        view.displayError("Invalid email or password.");
        continue;
      }

      User user = findUserByCredentials(email, password);
      if (user == null) {
        view.displayError("Invalid email or password.");
        continue;
      }

      setCurrentUser(user);
      view.displaySuccess("Login successful.");
      loggedIn = true;
    }
  }

  /**
   * Logs out the current user.
   */
  public void logout() {
    if (checkCurrentUserIsGuest()) {
      view.displayError("You are not logged in.");
      return;
    }

    setCurrentUser(null);
    view.displaySuccess("Logout successful.");
  }

  /**
   * Registers a new entertainment provider after validating their details and business number.
   *
   * Missing or malformed fields restart the registration flow, while duplicate-account and failed
   * verification cases terminate the use case after displaying an error.
   */
  public void registerEntertainmentProvider() {
    if (!checkCurrentUserIsGuest()) {
      view.displayError("You must be logged out to register.");
      return;
    }

    while (true) {
      String email = view.getInput("Enter email").trim();
      if (!InputParsers.isValidEmail(email)) {
        view.displayError("A valid email address is required.");
        continue;
      }

      if (emailAlreadyExists(email)) {
        view.displayError("An account already exists for that email address.");
        return;
      }

      String password = view.getInput("Enter password").trim();
      if (password.isEmpty()) {
        view.displayError("Password is required.");
        continue;
      }

      String orgName = view.getInput("Enter your organisation's name").trim();
      if (orgName.isEmpty()) {
        view.displayError("Organisation name is required.");
        continue;
      }

      String businessNumber = view.getInput("Enter your business registration number").trim();
      if (businessNumber.isEmpty()) {
        view.displayError("Business registration number is required.");
        continue;
      }

      if (EPAccountAlreadyExists(email, orgName, businessNumber)) {
        view.displayError("An account already exists for that entertainment provider.");
        return; // exit
      }

      String contactName = view.getInput("Enter your name").trim();
      if (contactName.isEmpty()) {
        view.displayError("Main contact name is required.");
        continue;
      }

      String description = view.getInput("Enter description").trim();
      if (description.isEmpty()) {
        view.displayError("Description is required.");
        continue;
      }

      if (!verificationSystem.verifyEntertainmentProvider(businessNumber)) {
        view.displayError("Business registration number could not be verified.");
        return; // exit
      }

      EntertainmentProvider newProvider = new EntertainmentProvider(email, password, orgName,
          businessNumber, contactName, description);
      addUser(newProvider);
      view.displaySuccess("Registration successful.");
      return;
    }
  }

  /**
   * Updates the current student's preferences.
   *
   * The method accepts up to three comma-separated event types and keeps prompting until valid
   * input is provided.
   */
  public void editPreferences() {
    if (!checkCurrentUserIsStudent()) {
      view.displayError("Only students can edit preferences.");
      return;
    }

    Student student = (Student) getCurrentUser();
    boolean updated = false;
    while (!updated) {
      String input = view.getInput(
          "Enter up to 3 preferred event types separated by commas (music, theatre, dance, movie, sports). Leave blank for none");
      updated = student.getPreferences().updatePreferences(input);
      if (!updated) {
        view.displayError("Invalid input. Enter up to 3 unique event types separated by commas.");
      }
    }

    view.displaySuccess("Preferences updated successfully.");
  }

  /**
   * Checks whether an entertainment-provider account already exists for the supplied organisation
   * details.
   *
   * @param email the email address to compare against
   * @param orgName the organisation name to compare against
   * @param businessNumber the business-registration number to compare against
   * @return {@code true} if a matching provider account already exists
   */
  private boolean EPAccountAlreadyExists(String email, String orgName, String businessNumber) {
    for (User user : users) {
      if (!(user instanceof EntertainmentProvider provider)) {
        continue;
      }

      if (provider.getEmail().equals(email)) {
        return true;
      } // Actually we don't need to check this since it has already been checked in
        // emailAlreadyExists()

      if (providerRepresentsSameOrganisation(provider, orgName, businessNumber)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Checks whether an email address is already used by an existing user.
   *
   * @param email the email address to check
   * @return {@code true} if an existing user already has that email address
   */
  private boolean emailAlreadyExists(String email) {
    for (User user : users) {
      if (user.getEmail().equals(email)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Finds a user whose credentials match the supplied login attempt.
   *
   * @param email the candidate email address
   * @param password the candidate password
   * @return the matching user, or {@code null} if the credentials do not match
   */
  private User findUserByCredentials(String email, String password) {
    for (User user : users) {
      if (user.getEmail().equals(email) && user.passwordMatches(password)) {
        return user;
      }
    }
    return null;
  }

  /**
   * Adds a user to the shared application user collection if their email address is not already
   * present.
   *
   * @param user the user to add
   */
  private void addUser(User user) {
    if (!emailAlreadyExists(user.getEmail())) {
      users.add(user);
    }
  }

  /**
   * Loads preregistered students and admin-staff accounts from the configured CSV files.
   */
  private void addPreregisteredUsers() {
    loadStudentsFromFile();
    loadAdminsFromFile();
  }

  /**
   * Loads preregistered students from the configured student CSV file.
   */
  private void loadStudentsFromFile() {
    Path studentsPath =
        firstExistingPath(PREREGISTERED_USERS_FILE_PATH, "CW3/" + PREREGISTERED_USERS_FILE_PATH);
    if (studentsPath == null) {
      return;
    }

    for (String[] fields : readDelimitedRecords(studentsPath)) {
      Student student = createStudent(fields);
      if (student != null) {
        addUser(student);
      }
    }
  }

  /**
   * Loads preregistered admin-staff members from the configured admin CSV file.
   */
  private void loadAdminsFromFile() {
    Path adminPath =
        firstExistingPath(PREREGISTERED_ADMIN_FILE_PATH, "CW3/" + PREREGISTERED_ADMIN_FILE_PATH);
    if (adminPath == null) {
      return;
    }

    for (String[] fields : readDelimitedRecords(adminPath)) {
      AdminStaff admin = createAdmin(fields);
      if (admin != null) {
        addUser(admin);
      }
    }
  }

  /**
   * Reads comma-delimited records from a preregistration file.
   *
   * @param path the file to read
   * @return the parsed CSV rows
   */
  private List<String[]> readDelimitedRecords(Path path) {
    try {
      return Files.readAllLines(path, StandardCharsets.UTF_8).stream().map(String::trim)
          .filter(line -> !line.isEmpty() && !line.startsWith("#")).map(line -> line.split(","))
          .filter(Objects::nonNull).toList();
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to read preregistration data from " + path + ".",
          exception);
    }
  }

  /**
   * Returns the first candidate path that exists on disk.
   *
   * @param candidates ordered candidate file paths
   * @return the first existing path, or {@code null} if none exist
   */
  private Path firstExistingPath(String... candidates) {
    for (String candidate : candidates) {
      Path path = Path.of(candidate);
      if (Files.exists(path)) {
        return path;
      }
    }
    return null;
  }

  /**
   * Creates a {@link Student} from one row of preregistration data.
   *
   * @param fields the parsed CSV fields
   * @return the created student, or {@code null} if the row is invalid
   */
  private Student createStudent(String[] fields) {
    if (fields.length < 4) {
      return null;
    }

    if (!PasswordUtils.isStoredPasswordHash(fields[1].trim())) {
      return null;
    }

    Integer phoneNumber = InputParsers.parsePhoneNumber(fields[3]);
    if (phoneNumber == null) {
      return null;
    }

    StudentPreferences preferences = new StudentPreferences();
    if (fields.length > 4) {
      preferences.updatePreferences(fields[4].trim());
    }

    return new Student(fields[0].trim(), fields[1].trim(), fields[2].trim(), phoneNumber,
        preferences);
  }

  /**
   * Creates an {@link AdminStaff} user from one row of preregistration data.
   *
   * @param fields the parsed CSV fields
   * @return the created admin user, or {@code null} if the row is invalid
   */
  private AdminStaff createAdmin(String[] fields) {
    if (fields.length < 3) {
      return null;
    }

    if (!PasswordUtils.isStoredPasswordHash(fields[1].trim())) {
      return null;
    }

    return new AdminStaff(fields[0].trim(), fields[1].trim(), fields[2].trim());
  }

  /**
   * Checks whether a stored entertainment provider represents the same organisation details as a
   * registration attempt.
   *
   * @param provider the existing provider account
   * @param orgName the candidate organisation name
   * @param businessNumber the candidate business-registration number
   * @return {@code true} if the organisation details should be treated as the same provider
   */
  private boolean providerRepresentsSameOrganisation(EntertainmentProvider provider, String orgName,
      String businessNumber) {
    return provider.getBusinessNumber().equals(businessNumber)
        || (provider.getOrgName().equals(orgName)
            && provider.getBusinessNumber().equals(businessNumber));
  }

  /**
   * Finds the entertainment provider that owns a given event.
   *
   * <p>
   * This helper is retained to match the UML API surface even though the current three-person-group
   * flows do not call it directly.
   *
   * @param eventNumber the event identifier to resolve
   * @return the owning entertainment provider, or {@code null} if none can be found
   */
  @SuppressWarnings("unused")
  private EntertainmentProvider getEntertainmentProviderOwningEvent(long eventNumber) {
    for (Event event : events) {
      if (event.getEventID() != eventNumber) {
        continue;
      }

      String organiserEmail = event.getOrganiserEmail();
      if (organiserEmail == null) {
        return null;
      }

      for (User user : users) {
        if (user instanceof EntertainmentProvider provider
            && organiserEmail.equals(provider.getEmail())) {
          return provider;
        }
      }

      return null;
    }

    return null;
  }
}
