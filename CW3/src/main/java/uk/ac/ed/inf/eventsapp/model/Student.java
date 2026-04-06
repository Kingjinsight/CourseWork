package uk.ac.ed.inf.eventsapp.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Student user from the UML diagram.
 */
public class Student extends User {
  private String name;
  private int phoneNumber;
  private final Collection<Booking> bookings;
  private final StudentPreferences preferences;

  public Student() {
    this.preferences = new StudentPreferences();
    this.bookings = new ArrayList<>();
  }

  /**
   * Creates a student user.
   *
   * @param email the student's email address
   * @param password the student's password or stored password hash
   * @param name the student's name
   * @param phoneNumber the student's phone number
   * @param preferences the student's saved preferences
   */
  public Student(String email, String password, String name, int phoneNumber,
      StudentPreferences preferences) {
    super(email, password);
    this.name = name;
    this.phoneNumber = phoneNumber;
    this.preferences = preferences == null ? new StudentPreferences() : preferences;
    this.bookings = new ArrayList<>();
  }

  /**
   * Adds a booking to the student's booking history.
   *
   * @param booking the booking to record
   */
  public void addBooking(Booking booking) {
    bookings.add(booking);
  }

  /** @return the student's name */
  public String getName() {
    return name;
  }

  /** @return the student's phone number */
  public int getPhoneNumber() {
    return phoneNumber;
  }

  /**
   * Returns the student's recorded bookings.
   *
   * @return an unmodifiable view of the student's bookings
   */
  public Collection<Booking> getBookings() {
    return Collections.unmodifiableCollection(bookings);
  }

  /** @return the student's current event preferences */
  public StudentPreferences getPreferences() {
    return preferences;
  }
}
