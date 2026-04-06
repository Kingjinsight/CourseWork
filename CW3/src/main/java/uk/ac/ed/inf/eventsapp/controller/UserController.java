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

  public UserController(View view, VerificationSystem verificationSystem, Collection<User> users,
      Collection<Event> events) {
    this(view, verificationSystem, users, events, true);
  }

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

      if (EPAccountAlreadyExists(orgName, businessNumber)) {
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

  private boolean EPAccountAlreadyExists(String orgName, String businessNumber) {
    for (User user : users) {
      if (!(user instanceof EntertainmentProvider provider)) {
        continue;
      }

      if (providerRepresentsSameOrganisation(provider, orgName, businessNumber)) {
        return true;
      }
    }

    return false;
  }

  private boolean emailAlreadyExists(String email) {
    for (User user : users) {
      if (user.getEmail().equals(email)) {
        return true;
      }
    }
    return false;
  }

  private User findUserByCredentials(String email, String password) {
    for (User user : users) {
      if (user.getEmail().equals(email) && user.passwordMatches(password)) {
        return user;
      }
    }
    return null;
  }

  private void addUser(User user) {
    if (!emailAlreadyExists(user.getEmail())) {
      users.add(user);
    }
  }

  private void addPreregisteredUsers() {
    loadStudentsFromFile();
    loadAdminsFromFile();
  }

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

  private Path firstExistingPath(String... candidates) {
    for (String candidate : candidates) {
      Path path = Path.of(candidate);
      if (Files.exists(path)) {
        return path;
      }
    }
    return null;
  }

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

  private AdminStaff createAdmin(String[] fields) {
    if (fields.length < 3) {
      return null;
    }

    if (!PasswordUtils.isStoredPasswordHash(fields[1].trim())) {
      return null;
    }

    return new AdminStaff(fields[0].trim(), fields[1].trim(), fields[2].trim());
  }

  private boolean providerRepresentsSameOrganisation(EntertainmentProvider provider, String orgName,
      String businessNumber) {
    return provider.getBusinessNumber().equals(businessNumber)
        || (provider.getOrgName().equals(orgName)
            && provider.getBusinessNumber().equals(businessNumber));
  }

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
