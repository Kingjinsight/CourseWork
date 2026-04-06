package uk.ac.ed.inf.eventsapp.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Performance entity
 */
public class Performance {
  private long performanceID;
  private LocalDateTime startDateTime;
  private LocalDateTime endDateTime;
  private final Collection<String> performerNames;
  private String venueAddress;
  private int venueCapacity;
  private boolean venueIsOutdoors;
  private boolean venueAllowsSmoking;
  private int numTicketsTotal;
  private int numTicketsSold;
  private double ticketPrice;

  @SuppressWarnings("unused")
  private final boolean isSponsered = false; // no need to implement
  @SuppressWarnings("unused")
  private final double sponsoredAmount = 0.0; // no need to implement
  @SuppressWarnings("unused")
  private final Collection<Integer> reviewRatings = new ArrayList<>(); // no need to implement
  @SuppressWarnings("unused")
  private final Collection<String> reviewComments = new ArrayList<>(); // no need to implement

  private PerformanceStatus status;

  private Event event;
  private final Collection<Booking> bookings;

  private static final DateTimeFormatter SEARCH_RESULT_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  public Performance() {
    this.performerNames = new ArrayList<>();
    this.bookings = new ArrayList<>();
  }

  /**
   * Creates a performance.
   *
   * @param performanceID the unique performance identifier
   * @param startDateTime the start date/time
   * @param endDateTime the end date/time
   * @param performerNames the performer names
   * @param venueAddress the venue address
   * @param venueCapacity the venue capacity
   * @param venueIsOutdoors whether the venue is outdoors
   * @param venueAllowsSmoking whether smoking is allowed at the venue
   * @param numTicketsTotal the total ticket count
   * @param numTicketsSold the number of tickets already sold
   * @param ticketPrice the ticket price
   * @param status the performance status
   * @param event the parent event
   */
  public Performance(long performanceID, LocalDateTime startDateTime, LocalDateTime endDateTime,
      Collection<String> performerNames, String venueAddress, int venueCapacity,
      boolean venueIsOutdoors, boolean venueAllowsSmoking, int numTicketsTotal, int numTicketsSold,
      double ticketPrice, PerformanceStatus status, Event event) {
    this.performanceID = performanceID;
    this.startDateTime = startDateTime;
    this.endDateTime = endDateTime;
    this.performerNames = new ArrayList<>(performerNames);
    this.venueAddress = venueAddress;
    this.venueCapacity = venueCapacity;
    this.venueIsOutdoors = venueIsOutdoors;
    this.venueAllowsSmoking = venueAllowsSmoking;
    this.numTicketsTotal = numTicketsTotal;
    this.numTicketsSold = numTicketsSold;
    this.ticketPrice = ticketPrice;
    this.status = status;
    this.event = event;
    this.bookings = new ArrayList<>();
  }

  /** Marks the performance as cancelled. */
  public void cancel() {
    status = PerformanceStatus.CANCELLED;
  }

  /**
   * Checks whether the parent event is ticketed.
   *
   * @return {@code true} if the parent event is ticketed, otherwise {@code false}
   */
  public boolean checkIfEventIsTicketed() {
    return event != null && event.isTicketed();
  }

  /**
   * Checks whether the remaining tickets can satisfy a requested quantity.
   *
   * @param numTicketsToBuy the requested ticket quantity
   * @return {@code true} if enough tickets remain, otherwise {@code false}
   */
  public boolean checkIfTicketsLeft(int numTicketsToBuy) {
    return numTicketsTotal - numTicketsSold >= numTicketsToBuy;
  }

  /** @return the effective ticket price for the performance */
  public double getFinalTicketPrice() {
    return ticketPrice;
  }

  /** @return the organiser email address of the parent event */
  public String getOrganiserEmail() {
    return event == null ? null : event.getOrganiserEmail();
  }

  /** @return the title of the parent event */
  public String getEventTitle() {
    return event == null ? null : event.titleValue();
  }

  /**
   * Checks whether this performance belongs to the supplied event.
   *
   * @param eventID the event identifier to compare against
   * @return {@code true} if the performance belongs to that event
   */
  public boolean belongsToEvent(long eventID) {
    return event != null && event.getEventID() == eventID;
  }

  /**
   * Checks whether the performance has not happened yet.
   *
   * @return {@code true} if the start time is still in the future
   */
  public boolean checkHasNotHappenedYet() {
    return startDateTime != null && startDateTime.isAfter(LocalDateTime.now());
  }

  /**
   * Checks whether this performance belongs to an event created by the supplied provider email.
   *
   * @param email the entertainment-provider email to compare against
   * @return {@code true} if the performance belongs to that provider
   */
  public boolean checkCreatedByEP(String email) {
    return event != null && email.equals(event.getOrganiserEmail());
  }

  /**
   * Checks whether the performance has any active bookings.
   *
   * @return {@code true} if at least one active booking exists
   */
  public boolean hasActiveBookings() {
    return !getActiveBookings().isEmpty();
  }

  /**
   * Serialises the active booking data required by the refund workflow.
   *
   * @return one line per active booking containing the refund details
   */
  public String getBookingDetailsForRefund() {
    Collection<String> activeBookingDetails = new ArrayList<>();
    for (Booking booking : getActiveBookings()) {
      activeBookingDetails.add(booking.toRefundDetailsString());
    }

    return String.join(System.lineSeparator(), activeBookingDetails);
  }

  /**
   * Placeholder for the 4-person-group sponsor-performance use case.
   *
   * @param amount the sponsorship amount
   * @throws UnsupportedOperationException because this use case is out of scope for 3-person groups
   */
  @SuppressWarnings("unused")
  public void sponsor(double amount) {
    // Sponsor performance is outside the 3-person-group scope, so this API remains a placeholder
    // to preserve the UML surface without implementing the 4-person-group use case.
    throw new UnsupportedOperationException("sponsor is not implemented yet.");
  }

  /**
   * Placeholder for the 4-person-group review-performance use case.
   *
   * @param rating the review rating
   * @param comment the review comment
   * @throws UnsupportedOperationException because this use case is out of scope for 3-person groups
   */
  @SuppressWarnings("unused")
  public void review(int rating, String comment) {
    // Review performance is outside the 3-person-group scope, so this API remains a placeholder
    // to preserve the UML surface without implementing the 4-person-group use case.
    throw new UnsupportedOperationException("review is not implemented yet.");
  }

  /**
   * Associates a booking with the performance.
   *
   * @param booking the booking to add
   */
  public void addBooking(Booking booking) {
    bookings.add(booking);
  }

  @Override
  public String toString() {
    return toString(false);
  }

  /**
   * Returns either the summary or detailed representation of the performance.
   *
   * @param detailed whether to return the detailed representation
   * @return the summary or detailed string form
   */
  public String toString(boolean detailed) {
    return detailed ? toDetailedString() : toSummaryString();
  }

  /** @return the performance start date/time */
  public LocalDateTime getStartDateTime() {
    return startDateTime;
  }

  /**
   * Returns the currently active bookings for this performance.
   *
   * @return the active bookings
   */
  public Collection<Booking> getActiveBookings() {
    Collection<Booking> activeBookings = new ArrayList<>();
    for (Booking booking : bookings) {
      if (booking.isActive()) {
        activeBookings.add(booking);
      }
    }
    return activeBookings;
  }

  /**
   * Increases the count of sold tickets.
   *
   * @param numTickets the number of newly sold tickets
   */
  public void addNumTicketsSold(int numTickets) {
    numTicketsSold += numTickets;
  }

  /**
   * Decreases the count of sold tickets without allowing it to go below zero.
   *
   * @param numTickets the number of tickets to remove from the sold total
   */
  public void removeNumTicketsSold(int numTickets) {
    numTicketsSold = Math.max(0, numTicketsSold - numTickets);
  }

  /** @return whether the performance is active */
  public boolean isActive() {
    return status == PerformanceStatus.ACTIVE;
  }

  /**
   * Checks whether the performance has the supplied identifier.
   *
   * @param candidatePerformanceID the performance identifier to compare against
   * @return {@code true} if the identifiers match
   */
  public boolean hasID(long candidatePerformanceID) {
    return performanceID == candidatePerformanceID;
  }

  boolean hasSameSchedule(LocalDateTime candidateStartDateTime,
      LocalDateTime candidateEndDateTime) {
    return startDateTime != null && endDateTime != null
        && startDateTime.equals(candidateStartDateTime) && endDateTime.equals(candidateEndDateTime);
  }

  private String toSummaryString() {
    String eventTitle = getEventTitle() == null ? "Unknown event" : getEventTitle();
    String startTime =
        startDateTime == null ? "Unknown time" : startDateTime.format(SEARCH_RESULT_TIME_FORMATTER);
    String endTime =
        endDateTime == null ? "Unknown time" : endDateTime.format(SEARCH_RESULT_TIME_FORMATTER);
    String venue = venueAddress == null || venueAddress.isBlank() ? "Unknown venue" : venueAddress;
    String organiser = event == null || event.organiserDisplayName() == null ? "Unknown provider"
        : event.organiserDisplayName();
    double eventReviewAverage = event == null ? 0.0 : event.getAverageReviewRating();

    return String.format(
        "Performance ID: %d | Event: %s | Time: %s to %s | Venue: %s | Provider: %s | Event review average: %.1f",
        performanceID, eventTitle, startTime, endTime, venue, organiser, eventReviewAverage);
  }

  private String toDetailedString() {
    String eventTitle = getEventTitle() == null ? "Unknown event" : getEventTitle();
    String startTime =
        startDateTime == null ? "Unknown time" : startDateTime.format(SEARCH_RESULT_TIME_FORMATTER);
    String endTime =
        endDateTime == null ? "Unknown time" : endDateTime.format(SEARCH_RESULT_TIME_FORMATTER);
    String venue = venueAddress == null || venueAddress.isBlank() ? "Unknown venue" : venueAddress;
    String organiser = event == null || event.organiserDisplayName() == null ? "Unknown provider"
        : event.organiserDisplayName();
    String performerList = performerNames == null || performerNames.isEmpty() ? "Unknown performers"
        : String.join(", ", performerNames);
    String venueEnvironment = venueIsOutdoors ? "Outdoors" : "Indoors";
    String smokingPolicy = venueAllowsSmoking ? "Smoking allowed" : "Non-smoking";
    String ticketing = numTicketsTotal > 0 || ticketPrice > 0.0 ? "Ticketed" : "Non-ticketed";
    int ticketsRemaining = Math.max(numTicketsTotal - numTicketsSold, 0);
    String ticketInfo = "Ticketed".equals(ticketing)
        ? String.format("Price: %.2f | Tickets remaining: %d", ticketPrice, ticketsRemaining)
        : "No tickets required";
    String statusLabel = status == null ? "Unknown" : status.name();
    // Review-related details are omitted because groups of 3 do not implement Review performance.

    return String.format(
        "Performance ID: %d%nEvent: %s%nTime: %s to %s%nVenue: %s%nVenue details: capacity %d, %s, %s%nPerformers: %s%nProvider: %s%nTicketing: %s%nTicket details: %s%nStatus: %s",
        performanceID, eventTitle, startTime, endTime, venue, venueCapacity, venueEnvironment,
        smokingPolicy, performerList, organiser, ticketing, ticketInfo, statusLabel);
  }
}
