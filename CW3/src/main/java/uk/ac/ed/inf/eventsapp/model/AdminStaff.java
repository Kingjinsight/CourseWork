package uk.ac.ed.inf.eventsapp.model;

/**
 * Admin-staff user from the UML diagram.
 */
public class AdminStaff extends User {
  private String name;

  /** Creates an empty admin-staff placeholder instance. */
  public AdminStaff() {}

  /**
   * Creates an admin-staff user.
   *
   * @param email the admin's email address
   * @param password the admin's password or stored password hash
   * @param name the admin's display name
   */
  public AdminStaff(String email, String password, String name) {
    super(email, password);
    this.name = name;
  }

  /**
   * Returns the admin's name.
   *
   * @return the admin-staff member's display name
   */
  public String getName() {
    return name;
  }
}
