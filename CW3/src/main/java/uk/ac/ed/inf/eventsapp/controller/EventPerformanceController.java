package uk.ac.ed.inf.eventsapp.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import external.PaymentSystem;
import uk.ac.ed.inf.eventsapp.model.Booking;
import uk.ac.ed.inf.eventsapp.model.EntertainmentProvider;
import uk.ac.ed.inf.eventsapp.model.Event;
import uk.ac.ed.inf.eventsapp.model.EventType;
import uk.ac.ed.inf.eventsapp.model.Performance;
import uk.ac.ed.inf.eventsapp.model.Student;
import uk.ac.ed.inf.eventsapp.model.StudentPreferences;
import uk.ac.ed.inf.eventsapp.util.InputParsers;
import uk.ac.ed.inf.eventsapp.view.View;

/**
 * Handles event creation, search, view, cancellation, and sponsorship.
 */
public class EventPerformanceController extends Controller {
  private long nextEventID;
  private long nextPerformanceID;
  private final Collection<Event> events;
  private final Collection<Performance> performances;
  private final PaymentSystem paymentSystem;

  /**
   * Creates an event/performance controller backed by the shared application collections.
   *
   * @param view the text view used for interaction
   * @param events the shared event collection
   * @param performances the shared performance collection
   * @param paymentSystem the external payment-system adapter
   */
  public EventPerformanceController(View view, Collection<Event> events,
      Collection<Performance> performances, PaymentSystem paymentSystem) {
    super(view);
    this.events = events;
    this.performances = performances;
    this.nextEventID = 1L;
    this.nextPerformanceID = 1L;
    this.paymentSystem = paymentSystem;
  }

  /**
   * Creates a new event and one or more performances owned by the current entertainment provider.
   *
   * @return the created event, or {@code null} if validation fails
   */
  public Event createEvent() {
    if (!checkCurrentUserIsEntertainmentProvider()) {
      view.displayError("Only logged-in entertainment providers can create events.");
      return null;
    }

    EntertainmentProvider organiser = (EntertainmentProvider) currentUser;

    String title = view.getInput("Enter event title").trim();
    if (title.isEmpty()) {
      view.displayError("Event title is required.");
      return null;
    }

    EventType type = InputParsers
        .parseEventType(view.getInput("Enter event type (music, theatre, dance, movie, sports)"));
    if (type == null) {
      view.displayError("Invalid event type.");
      return null;
    }

    Boolean isTicketed =
        InputParsers.parseBoolean(view.getInput("Is the event ticketed? (yes/no)"));
    if (isTicketed == null) {
      view.displayError("Ticketed must be specified as yes or no.");
      return null;
    }

    Integer performanceCount =
        InputParsers.parsePositiveInteger(view.getInput("How many performances?"));
    if (performanceCount == null) {
      view.displayError("Number of performances must be a positive integer.");
      return null;
    }

    Event event = new Event(getNextEventID(), title, type, isTicketed, organiser);
    long nextPerformanceId = getNextPerformanceID();

    for (int performanceIndex = 1; performanceIndex <= performanceCount; performanceIndex++) {
      LocalDateTime startDateTime = InputParsers.parseDateTime(
          view.getInput("Performance " + performanceIndex + " start (yyyy-MM-dd HH:mm)"));
      LocalDateTime endDateTime = InputParsers.parseDateTime(
          view.getInput("Performance " + performanceIndex + " end (yyyy-MM-dd HH:mm)"));
      if (startDateTime == null || endDateTime == null || !endDateTime.isAfter(startDateTime)) {
        view.displayError("Performance dates/times are invalid.");
        return null;
      }

      List<String> performerNames = InputParsers.parseCommaSeparatedValues(
          view.getInput("Performance " + performanceIndex + " performer names (comma-separated)"));
      if (performerNames.isEmpty()) {
        view.displayError("At least one performer name is required.");
        return null;
      }

      String venueAddress =
          view.getInput("Performance " + performanceIndex + " venue address").trim();
      if (venueAddress.isEmpty()) {
        view.displayError("Venue address is required.");
        return null;
      }

      Integer venueCapacity = InputParsers.parsePositiveInteger(
          view.getInput("Performance " + performanceIndex + " venue capacity"));
      if (venueCapacity == null) {
        view.displayError("Venue capacity must be a positive integer.");
        return null;
      }

      Boolean venueIsOutdoors = InputParsers
          .parseBoolean(view.getInput("Performance " + performanceIndex + " outdoors? (yes/no)"));
      Boolean venueAllowsSmoking = InputParsers.parseBoolean(
          view.getInput("Performance " + performanceIndex + " smoking allowed? (yes/no)"));
      if (venueIsOutdoors == null || venueAllowsSmoking == null) {
        view.displayError("Venue flags must be specified as yes or no.");
        return null;
      }

      int numTickets = 0;
      double ticketPrice = 0.0;
      if (isTicketed) {
        Integer parsedTickets = InputParsers.parseNonNegativeInteger(
            view.getInput("Performance " + performanceIndex + " total ticket count"));
        Double parsedPrice = InputParsers.parseNonNegativeDouble(
            view.getInput("Performance " + performanceIndex + " ticket price"));
        if (parsedTickets == null || parsedPrice == null) {
          view.displayError(
              "Ticket count must be a valid non-negative integer and ticket price must have at most two decimal places.");
          return null;
        }
        numTickets = parsedTickets;
        ticketPrice = parsedPrice;
      }

      if (eventWithSameTitleHasPerformanceAtSameTimes(title, startDateTime, endDateTime)) {
        view.displayError(
            "An event with the same title already exists for the same dates and times.");
        return null;
      }

      try {
        Performance performance = event.createPerformance(nextPerformanceId, startDateTime,
            endDateTime, performerNames, venueAddress, venueCapacity, venueIsOutdoors,
            venueAllowsSmoking, numTickets, ticketPrice);
        addPerformance(performance);
      } catch (IllegalArgumentException exception) {
        view.displayError(exception.getMessage());
        return null;
      }
      nextPerformanceId++;
    }

    addEvent(event);
    organiser.addEvent(event);
    nextEventID++;
    nextPerformanceID = nextPerformanceId;
    view.displaySuccess("Event created successfully.");
    return event;
  }

  /**
   * Searches for performances on a given date and displays matching summaries.
   *
   * Student searches prioritise performances whose events match the student's saved preferences.
   */
  public void searchforPerformances() {
    if (checkCurrentUserIsGuest()) {
      view.displayError("Only logged-in users can search for performances.");
      return;
    }

    LocalDate performanceDate = null;
    while (performanceDate == null) {
      performanceDate = InputParsers.parseDate(view.getInput("Enter search date (yyyy-MM-dd)"));
      if (performanceDate == null) {
        view.displayError("Date format is invalid. Use yyyy-MM-dd.");
      }
    }

    Collection<String> prioritisedPerformanceInfo = new ArrayList<>();
    Collection<String> otherPerformanceInfo = new ArrayList<>();
    StudentPreferences preferences = getStudentPreferences();
    boolean shouldPrioritisePreferences = hasSpecifiedPreferences(preferences);

    for (Event event : getEvents()) {
      Collection<String> performanceInfo =
          event.getInfoOfPerformancesOnDate(performanceDate.atStartOfDay());
      if (performanceInfo.isEmpty()) {
        continue;
      }

      if (shouldPrioritisePreferences && event.matchesPreferences(preferences)) {
        prioritisedPerformanceInfo.addAll(performanceInfo);
      } else {
        otherPerformanceInfo.addAll(performanceInfo);
      }
    }

    if (prioritisedPerformanceInfo.isEmpty() && otherPerformanceInfo.isEmpty()) {
      view.displayError("There are no performances on that date.");
      return;
    }

    Collection<String> orderedPerformanceInfo = new ArrayList<>(prioritisedPerformanceInfo);
    orderedPerformanceInfo.addAll(otherPerformanceInfo);
    view.displayListOfPerformances(orderedPerformanceInfo);
  }

  /**
   * Displays the detailed information for a single performance selected by ID.
   */
  public void viewPerformance() {
    if (checkCurrentUserIsGuest()) {
      view.displayError("Only logged-in users can view performances.");
      return;
    }

    Performance performance = null;
    while (performance == null) {
      Long performanceID = InputParsers.parsePositiveLong(view.getInput("Performance ID"));
      if (performanceID == null) {
        view.displayError("Performance ID must be a valid positive whole number.");
        continue;
      }

      performance = getPerformanceByID(performanceID);
      if (performance == null) {
        view.displayError("Performance not found.");
      }
    }

    view.displaySpecificPerformance(performance.toString(true));
  }

  /**
   * Cancels a future performance owned by the current entertainment provider.
   *
   * If active bookings exist, the controller first gathers a non-empty cancellation message and
   * then refunds every affected booking before cancelling the performance locally.
   */
  public void cancelPerformance() {
    if (!checkCurrentUserIsEntertainmentProvider()) {
      view.displayError("Only entertainment providers can cancel performance.");
      return;
    }
    EntertainmentProvider ep = (EntertainmentProvider) currentUser;

    Performance performance = null;
    while (performance == null) {
      Long performanceID =
          InputParsers.parsePositiveLong(view.getInput("Enter performance ID to cancel"));
      if (performanceID == null) {
        view.displayError("Performance ID must be a valid positive number.");
        continue;
      }

      Performance candidatePerformance = getPerformanceByID(performanceID);
      if (candidatePerformance == null) {
        view.displayError("Performance with given ID does not exist.");
        continue;
      }

      if (!candidatePerformance.checkCreatedByEP(ep.getEmail())) {
        view.displayError("You can only cancel your own performance.");
        continue;
      }

      if (!candidatePerformance.checkHasNotHappenedYet()) {
        view.displayError("Performance has already happened");
        continue;
      }

      performance = candidatePerformance;
    }

    if (performance.hasActiveBookings()) {
      Collection<Booking> activeBookings = performance.getActiveBookings();
      String bookingDetailsForRefund = performance.getBookingDetailsForRefund();
      // we need to fit the UML model, though it may not be efficient to use string to pass booking
      // details and parse details from string for refund instead of using getter directly
      String cancellationMessage = "";
      while (cancellationMessage.isBlank()) {
        cancellationMessage =
            view.getInput("Provide a cancellation message for affected students").trim();
        if (cancellationMessage.isBlank()) {
          view.displayError("Cancellation message is required.");
        }
      }

      if (bookingDetailsForRefund.isBlank()) {
        view.displayError("There are no active booking details available for refund.");
        return;
      }

      String[] refundDetailLines = bookingDetailsForRefund.split("\\R");
      if (refundDetailLines.length != activeBookings.size()) {
        view.displayError("Active booking refund details are inconsistent.");
        return;
      }

      Collection<RefundDetails> refundDetailsCollection = new ArrayList<>();
      for (String refundDetailLine : refundDetailLines) {
        RefundDetails refundDetails = parseRefundDetails(refundDetailLine);
        if (refundDetails == null) {
          view.displayError("Active booking refund details are inconsistent.");
          return;
        }

        refundDetailsCollection.add(refundDetails);
      }

      for (RefundDetails refundDetails : refundDetailsCollection) {
        boolean refundSuccessful = paymentSystem.processRefund(refundDetails.numTickets(),
            performance.getEventTitle(), refundDetails.studentEmail(), refundDetails.studentPhone(),
            performance.getOrganiserEmail(), refundDetails.amountPaid(), cancellationMessage);
        if (!refundSuccessful) {
          view.displayError(
              "There was an issue with a refund. The performance cannot be cancelled.");
          return;
        }
      }

      for (Booking booking : activeBookings) {
        booking.cancelByProvider();
      }
    }

    performance.cancel();
    view.displaySuccess("Cancellation Successful!");
  }

  /**
   * Parses one line of serialised refund data produced by
   * {@link Performance#getBookingDetailsForRefund()}.
   *
   * @param refundDetailLine the serialised refund-data line
   * @return the parsed refund details, or {@code null} if the line is invalid
   */
  private RefundDetails parseRefundDetails(String refundDetailLine) {
    if (refundDetailLine == null || refundDetailLine.isBlank()) {
      return null;
    }

    String[] parts = refundDetailLine.split(";", 3);
    if (parts.length != 3) {
      return null;
    }

    Integer numTickets = InputParsers.parsePositiveInteger(parts[0]);
    Double amountPaid = InputParsers.parseNonNegativeDouble(parts[1]);
    String[] studentDetails = parts[2].split("\\|", 3);
    if (numTickets == null || amountPaid == null || studentDetails.length != 3) {
      return null;
    }

    Integer studentPhone = InputParsers.parsePhoneNumber(studentDetails[2]);
    if (studentDetails[1].isBlank() || studentPhone == null) {
      return null;
    }

    return new RefundDetails(numTickets, amountPaid, studentDetails[1], studentPhone);
  }

  /**
   * Value object used while converting booking refund data into arguments for
   * {@link PaymentSystem#processRefund(int, String, String, int, String, double, String)}.
   *
   * @param numTickets the number of tickets to refund
   * @param amountPaid the refund amount
   * @param studentEmail the refunded student's email address
   * @param studentPhone the refunded student's phone number
   */
  private record RefundDetails(int numTickets, double amountPaid, String studentEmail,
      int studentPhone) {}

  /**
   * Placeholder for the 4-person-group sponsorship validation helper shown in the UML diagram.
   *
   * @param performance the performance that would be sponsored
   * @param amount the proposed sponsorship amount
   * @return never returns normally
   * @throws UnsupportedOperationException because sponsor performance is out of scope for
   *         three-person-group submissions
   */
  @SuppressWarnings("unused")
  private Boolean checkIfSponsorshipPossible(Performance performance, int amount) {
    // Sponsor performance is a 4-person-group-only use case and is intentionally left
    // unimplemented in the current 3-person-group submission.
    throw new UnsupportedOperationException("checkIfSponsorshipPossible is not implemented yet.");
  }

  /**
   * Placeholder for the 4-person-group {@code Sponsor performance} use case shown in the UML
   * diagram.
   *
   * @throws UnsupportedOperationException because this use case is out of scope for
   *         three-person-group submissions
   */
  @SuppressWarnings("unused")
  public void sponsorPerformance() {
    // Sponsor performance is a 4-person-group-only use case and is intentionally left
    // unimplemented in the current 3-person-group submission.
    throw new UnsupportedOperationException("sponsorPerformance is not implemented yet.");
  }

  /**
   * Adds an event to the shared application event collection.
   *
   * @param event the event to store
   */
  private void addEvent(Event event) {
    events.add(event);
  }

  /**
   * Adds a performance to the shared application performance collection.
   *
   * @param performance the performance to store
   */
  private void addPerformance(Performance performance) {
    performances.add(performance);
  }

  /**
   * Finds an event by its identifier.
   *
   * @param eventID the event identifier to look up
   * @return the matching event, or {@code null} if no event matches
   */
  @SuppressWarnings("unused")
  private Event getEventByID(long eventID) {
    for (Event event : getEvents()) {
      if (event.getEventID() == eventID) {
        return event;
      }
    }
    return null;
  }

  /**
   * Finds an event by title.
   *
   * @param title the event title to look up
   * @return the matching event, or {@code null} if no event matches
   */
  @SuppressWarnings("unused")
  private Event getEventByTitle(String title) {
    if (title == null || title.isBlank()) {
      return null;
    }

    for (Event event : getEvents()) {
      if (event.hasTitle(title)) {
        return event;
      }
    }
    return null;
  }

  /**
   * Finds a performance by its identifier.
   *
   * @param performanceID the performance identifier to look up
   * @return the matching performance, or {@code null} if no performance matches
   */
  private Performance getPerformanceByID(long performanceID) {
    for (Performance performance : performances) {
      if (performance.hasID(performanceID)) {
        return performance;
      }
    }
    return null;
  }

  /**
   * Returns the next event identifier that will be assigned.
   *
   * @return the next event identifier
   */
  private long getNextEventID() {
    return nextEventID;
  }

  /**
   * Returns the next performance identifier that will be assigned.
   *
   * @return the next performance identifier
   */
  private long getNextPerformanceID() {
    return nextPerformanceID;
  }

  /**
   * Returns the shared event collection managed by this controller.
   *
   * @return the shared event collection
   */
  private Collection<Event> getEvents() {
    return events;
  }

  /**
   * Checks whether another event already uses the same title and contains a performance scheduled
   * at the supplied times.
   *
   * @param title the candidate event title
   * @param startDateTime the candidate performance start date/time
   * @param endDateTime the candidate performance end date/time
   * @return {@code true} if a conflicting event/performance combination exists
   */
  private boolean eventWithSameTitleHasPerformanceAtSameTimes(String title,
      LocalDateTime startDateTime, LocalDateTime endDateTime) {
    for (Event event : events) {
      if (event.hasTitle(title) && event.hasPerformanceAtTimes(startDateTime, endDateTime)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the current student's saved preferences when the logged-in user is a student.
   *
   * @return the current student's preferences, or {@code null} for non-students
   */
  private StudentPreferences getStudentPreferences() {
    if (!checkCurrentUserIsStudent()) {
      return null;
    }

    return ((Student) currentUser).getPreferences();
  }

  /**
   * Checks whether a preference set contains at least one preferred event type.
   *
   * @param preferences the preference set to inspect
   * @return {@code true} if at least one preference is enabled
   */
  private boolean hasSpecifiedPreferences(StudentPreferences preferences) {
    return preferences != null && (preferences.isPreferMusicEvents()
        || preferences.isPreferTheaterEvents() || preferences.isPreferDanceEvents()
        || preferences.isPreferMovieEvents() || preferences.isPreferSportsEvents());
  }
}
