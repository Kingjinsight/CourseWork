package uk.ac.ed.inf.eventsapp.view;

import java.util.Collection;

/**
 * User-facing {@code View} abstraction
 *
 * <p>
 * Controllers depend on this interface so that the command-line implementation can be replaced by
 * scripted test doubles during system testing.
 */
public interface View {
  /**
   * Prompts the user for a line of input using the default prompt suffix.
   *
   * @param inputPrompt the prompt text shown to the user
   * @return the entered line
   */
  String getInput(String inputPrompt);

  /**
   * Prompts the user for a line of input using a caller-specified prompt suffix.
   *
   * @param inputPrompt the main prompt text shown to the user
   * @param promptEnd the suffix appended after the prompt text
   * @return the entered line
   */
  String getInput(String inputPrompt, String promptEnd);

  /**
   * Displays a success message to the user.
   *
   * @param successMessage the message to display
   */
  void displaySuccess(String successMessage);

  /**
   * Displays an error message to the user.
   *
   * @param errorMessage the message to display
   */
  void displayError(String errorMessage);

  /**
   * Displays a list of performance summaries.
   *
   * @param listOfPerformanceInfo the summary strings to display
   */
  void displayListOfPerformances(Collection<String> listOfPerformanceInfo);

  /**
   * Displays the full details for a single performance.
   *
   * @param performanceInfo the performance details to display
   */
  void displaySpecificPerformance(String performanceInfo);

  /**
   * Displays a booking record to the user.
   *
   * @param bookingRecord the booking record text to display
   */
  void displayBookingRecord(String bookingRecord);
}
