package uk.ac.ed.inf.eventsapp.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * {@code EntertainmentProvider} user
 *
 * <p>
 * This subtype stores organisation information and the collection of events owned by the provider.
 */
public class EntertainmentProvider extends User {
  private String orgName;
  private String businessNumber;
  private String name;
  private String description;
  private final Collection<Event> events;

  /** Creates an empty entertainment-provider placeholder instance. */
  public EntertainmentProvider() {
    this.events = new ArrayList<>();
  }

  /**
   * Creates an entertainment provider.
   *
   * @param email the provider's email address
   * @param password the provider's password or stored password hash
   * @param orgName the organisation name
   * @param businessNumber the business registration number
   * @param name the main contact name
   * @param description the provider description
   */
  public EntertainmentProvider(String email, String password, String orgName, String businessNumber,
      String name, String description) {
    super(email, password);
    this.orgName = orgName;
    this.businessNumber = businessNumber;
    this.name = name;
    this.description = description;
    this.events = new ArrayList<>();
  }

  /**
   * Associates an event with this provider.
   *
   * @param event the created event
   */
  public void addEvent(Event event) {
    events.add(event);
  }

  /** @return the provider organisation name */
  public String getOrgName() {
    return orgName;
  }

  /** @return the provider business registration number */
  public String getBusinessNumber() {
    return businessNumber;
  }

  /** @return the provider's main contact name */
  public String getName() {
    return name;
  }

  /** @return the provider description */
  public String getDescription() {
    return description;
  }

  /**
   * Returns the provider's events.
   *
   * @return an unmodifiable view of the provider's events
   */
  public Collection<Event> getEvents() {
    return Collections.unmodifiableCollection(events);
  }
}
