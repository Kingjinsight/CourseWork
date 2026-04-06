package uk.ac.ed.inf.eventsapp.model;

import uk.ac.ed.inf.eventsapp.util.PasswordUtils;

/** Base type for application users. */
public abstract class User {
  private String email;
  private String password;

  /** Creates an empty user. */
  protected User() {}

  /**
   * Creates a user shell with the supplied email only.
   *
   * @param email user email address
   */
  protected User(String email) {
    this.email = email;
  }

  /**
   * Creates a user with the supplied credentials.
   *
   * @param email user email address
   * @param password user password used for authentication
   */
  protected User(String email, String password) {
    this(email);
    this.password = PasswordUtils.normalizePassword(password);
  }

  /** @return the user's email address */
  public String getEmail() {
    return email;
  }

  /**
   * Checks whether a candidate password matches the stored password.
   *
   * @param candidatePassword password provided for authentication
   * @return {@code true} if the password matches, otherwise {@code false}
   */
  public boolean passwordMatches(String candidatePassword) {
    return PasswordUtils.verifyPassword(candidatePassword, password);
  }
}
