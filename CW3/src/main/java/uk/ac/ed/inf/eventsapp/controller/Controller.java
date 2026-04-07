package uk.ac.ed.inf.eventsapp.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import uk.ac.ed.inf.eventsapp.model.AdminStaff;
import uk.ac.ed.inf.eventsapp.model.EntertainmentProvider;
import uk.ac.ed.inf.eventsapp.model.Student;
import uk.ac.ed.inf.eventsapp.model.User;
import uk.ac.ed.inf.eventsapp.util.InputParsers;
import uk.ac.ed.inf.eventsapp.view.View;

/**
 * Shared base for the concrete controllers
 */
public abstract class Controller {
  protected final View view;
  protected User currentUser;

  /**
   * Creates a controller that interacts through the supplied view.
   *
   * @param view view used to read input and display output
   */
  protected Controller(View view) {
    this.view = view;
  }

  /**
   * Returns the user currently interacting with the system.
   *
   * @return the current user, or {@code null} when the session is a guest session
   */
  public User getCurrentUser() {
    return currentUser;
  }

  /**
   * Updates the user associated with the current controller session.
   *
   * @param currentUser the user to associate with this controller, or {@code null} for guest mode
   */
  public void setCurrentUser(User currentUser) {
    this.currentUser = currentUser;
  }

  /**
   * Checks whether the current session is unauthenticated.
   *
   * @return {@code true} when no user is logged in, otherwise {@code false}
   */
  protected boolean checkCurrentUserIsGuest() {
    return currentUser == null;
  }

  /**
   * Checks whether the current user is an admin-staff member.
   *
   * @return {@code true} when the current user is admin staff, otherwise {@code false}
   */
  protected boolean checkCurrentUserIsAdmin() {
    return currentUser instanceof AdminStaff;
  }

  /**
   * Checks whether the current user is a student.
   *
   * @return {@code true} when the current user is a student, otherwise {@code false}
   */
  protected boolean checkCurrentUserIsStudent() {
    return currentUser instanceof Student;
  }

  /**
   * Checks whether the current user is an entertainment provider.
   *
   * @return {@code true} when the current user is an entertainment provider, otherwise
   *         {@code false}
   */
  protected boolean checkCurrentUserIsEntertainmentProvider() {
    return currentUser instanceof EntertainmentProvider;
  }

  /**
   * Displays a numbered menu, validates the chosen option, and returns the selected index.
   *
   * @param options the menu options to present
   * @param prompt the heading shown above the menu entries
   * @param <T> the option type
   * @return the zero-based index of the chosen option, or {@code -1} when the user selects exit
   */
  public <T> int selectFromMenu(Collection<T> options, String prompt) {
    List<T> optionList = new ArrayList<>(options);
    while (true) {
      StringBuilder menuPrompt =
          new StringBuilder(prompt).append(System.lineSeparator()).append("0. Exit");
      for (int index = 0; index < optionList.size(); index++) {
        menuPrompt.append(System.lineSeparator()).append(index + 1).append(". ")
            .append(formatMenuOption(optionList.get(index)));
      }

      Integer selectedOption = InputParsers.parseNonNegativeInteger(
          view.getInput(menuPrompt.toString() + System.lineSeparator(), ">>> "));
      if (selectedOption == null || selectedOption > optionList.size()) {
        view.displayError("Please select a valid menu option number.");
        continue;
      }

      if (selectedOption == 0) {
        return -1;
      }

      return selectedOption - 1;
    }
  }

  private String formatMenuOption(Object option) {
    String rawLabel = option.toString().toLowerCase().replace('_', ' ').replace("Ep", "EP");
    String[] words = rawLabel.split(" ");
    StringBuilder formattedLabel = new StringBuilder();
    for (String word : words) {
      if (word.isEmpty()) {
        continue;
      }

      if (!formattedLabel.isEmpty()) {
        formattedLabel.append(' ');
      }
      formattedLabel.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
    }
    return formattedLabel.toString();
  }
}
