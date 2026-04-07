package uk.ac.ed.inf.eventsapp.facultypreregistration;

import uk.ac.ed.inf.eventsapp.model.User;

/** Faculty member created through preregistration. */
public class FacultyMember extends User {
  private int loginAttempts;

  /** Creates an empty faculty member. */
  public FacultyMember() {
    super();
  }

  /**
   * Creates a faculty member with the supplied state.
   *
   * @param email faculty email address
   * @param password faculty password or stored password hash
   * @param loginAttempts recorded login attempts
   */
  public FacultyMember(String email, String password, int loginAttempts) {
    super(email, password);
    this.loginAttempts = loginAttempts;
  }

  /**
   * Returns the number of recorded login attempts for this faculty member.
   *
   * @return the number of recorded login attempts
   */
  public synchronized int getLoginAttempts() {
    return loginAttempts;
  }

  /** Records one additional login attempt for this faculty member. */
  public synchronized void recordLoginAttempt() {
    loginAttempts++;
  }
}
