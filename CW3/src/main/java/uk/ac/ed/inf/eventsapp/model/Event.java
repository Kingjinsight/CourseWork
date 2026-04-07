package uk.ac.ed.inf.eventsapp.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Event aggregate
 *
 * <p>
 * An event owns one or more {@link Performance} instances and stores shared event-level data such
 * as title, type, ticketing mode, and organiser.
 */
public class Event {
  private long eventID;
  private String title;
  private EventType type;
  private boolean isTicketed;
  private EntertainmentProvider organiser;
  private final Collection<Performance> performances;

  /** Creates an empty event placeholder instance. */
  public Event() {
    this.performances = new ArrayList<>();
  }

  /**
   * Creates an event.
   *
   * @param eventID the unique event identifier
   * @param title the event title
   * @param type the event type
   * @param isTicketed whether the event requires tickets
   * @param organiser the entertainment provider that owns the event
   */
  public Event(long eventID, String title, EventType type, boolean isTicketed,
      EntertainmentProvider organiser) {
    this.eventID = eventID;
    this.title = title;
    this.type = type;
    this.isTicketed = isTicketed;
    this.organiser = organiser;
    this.performances = new ArrayList<>();
  }

  /**
   * Creates and adds a new performance for this event.
   *
   * @param performanceID the performance identifier
   * @param startDateTime the performance start date/time
   * @param endDateTime the performance end date/time
   * @param performerNames the performer names
   * @param venueAddress the venue address
   * @param venueCapacity the venue capacity
   * @param venueIsOutdoors whether the venue is outdoors
   * @param venueAllowsSmoking whether smoking is allowed at the venue
   * @param numTickets the total ticket count for ticketed events
   * @param ticketPrice the ticket price for ticketed events
   * @return the created performance
   * @throws IllegalArgumentException if the supplied data is invalid
   */
  public Performance createPerformance(long performanceID, LocalDateTime startDateTime,
      LocalDateTime endDateTime, Collection<String> performerNames, String venueAddress,
      int venueCapacity, boolean venueIsOutdoors, boolean venueAllowsSmoking, int numTickets,
      double ticketPrice) {
    if (startDateTime == null || endDateTime == null || !endDateTime.isAfter(startDateTime)) {
      throw new IllegalArgumentException("Performance dates/times are invalid.");
    }

    if (performerNames == null || performerNames.isEmpty()) {
      throw new IllegalArgumentException("At least one performer name is required.");
    }

    if (venueAddress == null || venueAddress.isBlank()) {
      throw new IllegalArgumentException("Venue address is required.");
    }

    if (venueCapacity <= 0) {
      throw new IllegalArgumentException("Venue capacity must be a positive integer.");
    }

    if (hasPerformanceAtSameTimes(startDateTime, endDateTime)) {
      throw new IllegalArgumentException(
          "A performance already exists for the same dates and times.");
    }

    int totalTickets = isTicketed ? numTickets : 0;
    double finalTicketPrice = isTicketed ? ticketPrice : 0.0;
    if (isTicketed && (numTickets < 0 || ticketPrice < 0.0)) {
      throw new IllegalArgumentException(
          "Ticket count and ticket price must be valid non-negative numbers.");
    }

    Performance performance = new Performance(performanceID, startDateTime, endDateTime,
        performerNames, venueAddress, venueCapacity, venueIsOutdoors, venueAllowsSmoking,
        totalTickets, 0, finalTicketPrice, PerformanceStatus.ACTIVE, this);
    addPerformance(performance);
    return performance;
  }

  /**
   * Finds a performance belonging to this event by ID.
   *
   * @param performanceID the performance identifier to look up
   * @return the matching performance, or {@code null} if none exists
   */
  public Performance getPerformanceByID(long performanceID) {
    for (Performance performance : performances) {
      if (performance.hasID(performanceID)) {
        return performance;
      }
    }
    return null;
  }

  /**
   * Returns summary strings for active performances scheduled on a given date.
   *
   * @param searchDateTime any date/time on the date being searched
   * @return the matching performance summary strings
   */
  public Collection<String> getInfoOfPerformancesOnDate(LocalDateTime searchDateTime) {
    ArrayList<String> performanceInfoList = new ArrayList<>();
    for (Performance performance : getPerformancesByDate(searchDateTime)) {
      performanceInfoList.add(performance.toString());
    }
    return performanceInfoList;
  }

  private String getOrganiserName() {
    if (organiser == null) {
      return null;
    }

    if (organiser.getOrgName() != null && !organiser.getOrgName().isBlank()) {
      return organiser.getOrgName();
    }

    if (organiser.getName() != null && !organiser.getName().isBlank()) {
      return organiser.getName();
    }

    return organiser.getEmail();
  }

  /** @return the organiser email address */
  public String getOrganiserEmail() {
    return organiser == null ? null : organiser.getEmail();
  }

  /**
   * Returns the average review rating for the event.
   *
   * @return the average review rating, currently {@code 0.0} in the 3-person-group solution
   */
  public double getAverageReviewRating() {
    return 0.0;
  }

  /**
   * Returns all review strings for the event.
   *
   * @return all review strings for the event
   * @throws UnsupportedOperationException because review handling is out of scope for 3-person
   *         groups
   */
  public Collection<String> getAllPerformanceReivews() {
    // Review performance is a 4-person-group-only use case, so the 3-person-group submission
    // intentionally keeps the review aggregation API as a documented placeholder.
    throw new UnsupportedOperationException("getAllPerformanceReivews is not implemented yet.");
  }

  private boolean hasPerformanceAtSameTimes(LocalDateTime startDateTime,
      LocalDateTime endDateTime) {
    for (Performance performance : performances) {
      if (performance.hasSameSchedule(startDateTime, endDateTime)) {
        return true;
      }
    }
    return false;
  }

  private void addPerformance(Performance performance) {
    performances.add(performance);
  }

  /**
   * Returns the event title for concise user-facing display.
   *
   * @return the event title
   */
  @Override
  public String toString() {
    return title;
  }

  /**
   * Returns the raw title value used internally by related model objects.
   *
   * @return the event title
   */
  String titleValue() {
    return title;
  }

  /**
   * Checks whether this event has the supplied title.
   *
   * @param candidateTitle the title to compare against
   * @return {@code true} if the titles match ignoring case, otherwise {@code false}
   */
  public boolean hasTitle(String candidateTitle) {
    return title != null && title.equalsIgnoreCase(candidateTitle);
  }

  /**
   * Checks whether any performance for this event is scheduled at the supplied times.
   *
   * @param startDateTime the candidate start date/time
   * @param endDateTime the candidate end date/time
   * @return {@code true} if a matching performance exists, otherwise {@code false}
   */
  public boolean hasPerformanceAtTimes(LocalDateTime startDateTime, LocalDateTime endDateTime) {
    return hasPerformanceAtSameTimes(startDateTime, endDateTime);
  }

  /**
   * Checks whether this event matches the student's saved preferences.
   *
   * @param preferences the student's preferences
   * @return {@code true} if the event type is one of the student's preferred types
   */
  public boolean matchesPreferences(StudentPreferences preferences) {
    if (preferences == null || type == null) {
      return false;
    }

    return switch (type) {
      case MUSIC -> preferences.isPreferMusicEvents();
      case THEATRE -> preferences.isPreferTheaterEvents();
      case DANCE -> preferences.isPreferDanceEvents();
      case MOVIE -> preferences.isPreferMovieEvents();
      case SPORTS -> preferences.isPreferSportsEvents();
    };
  }

  /**
   * Indicates whether this event requires tickets.
   *
   * @return {@code true} if the event is ticketed, otherwise {@code false}
   */
  public boolean isTicketed() {
    return isTicketed;
  }

  /**
   * Returns the unique event identifier.
   *
   * @return the event identifier
   */
  public long getEventID() {
    return eventID;
  }

  /**
   * Returns the active performances for this event occurring on the supplied date.
   *
   * @param searchDateTime any date/time on the date being searched
   * @return the matching active performances
   */
  public Collection<Performance> getPerformancesByDate(LocalDateTime searchDateTime) {
    ArrayList<Performance> eligiblePerformances = new ArrayList<>();
    for (Performance performance : performances) {
      if (performance.isActive()
          && searchDateTime.toLocalDate().equals(performance.getStartDateTime().toLocalDate())) {
        eligiblePerformances.add(performance);
      }
    }
    return eligiblePerformances;
  }

  /**
   * Returns the organiser display name chosen for UI output.
   *
   * @return the organiser display name, or {@code null} if no organiser is attached
   */
  String organiserDisplayName() {
    return getOrganiserName();
  }
}
