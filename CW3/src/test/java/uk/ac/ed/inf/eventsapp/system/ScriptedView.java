package uk.ac.ed.inf.eventsapp.system;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import uk.ac.ed.inf.eventsapp.view.View;

/**
 * Test double for scripted input and captured view output.
 */
final class ScriptedView implements View {
  private final Deque<String> scriptedInputs;
  private final List<String> successMessages;
  private final List<String> errorMessages;
  private final List<Collection<String>> displayedPerformanceLists;
  private final List<String> displayedPerformances;
  private final List<String> displayedBookingRecords;

  ScriptedView(String... scriptedInputs) {
    this.scriptedInputs = new ArrayDeque<>(List.of(scriptedInputs));
    this.successMessages = new ArrayList<>();
    this.errorMessages = new ArrayList<>();
    this.displayedPerformanceLists = new ArrayList<>();
    this.displayedPerformances = new ArrayList<>();
    this.displayedBookingRecords = new ArrayList<>();
  }

  @Override
  public String getInput(String inputPrompt) {
    return scriptedInputs.isEmpty() ? "" : scriptedInputs.removeFirst();
  }

  @Override
  public String getInput(String inputPrompt, String promptEnd) {
    return getInput(inputPrompt);
  }

  @Override
  public void displaySuccess(String successMessage) {
    successMessages.add("SUCCESS: " + successMessage);
  }

  @Override
  public void displayError(String errorMessage) {
    errorMessages.add("ERROR: " + errorMessage);
  }

  @Override
  public void displayListOfPerformances(Collection<String> listOfPerformanceInfo) {
    displayedPerformanceLists.add(new ArrayList<>(listOfPerformanceInfo));
  }

  @Override
  public void displaySpecificPerformance(String performanceInfo) {
    displayedPerformances.add(performanceInfo);
  }

  @Override
  public void displayBookingRecord(String bookingRecord) {
    displayedBookingRecords.add(bookingRecord);
  }

  String getLastSuccessMessage() {
    return successMessages.isEmpty() ? null : successMessages.get(successMessages.size() - 1);
  }

  String getLastErrorMessage() {
    return errorMessages.isEmpty() ? null : errorMessages.get(errorMessages.size() - 1);
  }

  List<String> getErrorMessages() {
    return errorMessages;
  }

  Collection<String> getLastDisplayedPerformanceList() {
    return displayedPerformanceLists.isEmpty() ? List.of()
        : displayedPerformanceLists.get(displayedPerformanceLists.size() - 1);
  }

  String getLastDisplayedPerformance() {
    return displayedPerformances.isEmpty() ? null
        : displayedPerformances.get(displayedPerformances.size() - 1);
  }

  String getLastDisplayedBookingRecord() {
    return displayedBookingRecords.isEmpty() ? null
        : displayedBookingRecords.get(displayedBookingRecords.size() - 1);
  }
}
