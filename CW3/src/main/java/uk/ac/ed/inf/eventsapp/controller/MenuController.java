package uk.ac.ed.inf.eventsapp.controller;

import java.util.List;
import uk.ac.ed.inf.eventsapp.view.View;

/**
 * Top-level main menu dispatcher.
 */
public class MenuController extends Controller {
  private final UserController userController;
  private final EventPerformanceController eventPerformanceController;
  private final BookingController bookingController;

  public MenuController(View view, UserController userController,
      EventPerformanceController eventPerformanceController, BookingController bookingController) {
    super(view);
    this.userController = userController;
    this.eventPerformanceController = eventPerformanceController;
    this.bookingController = bookingController;
  }

  /**
   * Runs the top-level menu loop until the user chooses to exit the application.
   */
  public void mainMenu() {
    while (true) {
      syncCurrentUserAcrossControllers();

      if (checkCurrentUserIsGuest()) {
        if (!handleGuestMainMenu()) {
          return;
        }
      } else if (checkCurrentUserIsStudent()) {
        if (!handleStudentMainMenu()) {
          return;
        }
      } else if (checkCurrentUserIsEntertainmentProvider()) {
        if (!handleEntertainmentProviderMainMenu()) {
          return;
        }
      } else if (checkCurrentUserIsAdmin()) {
        if (!handleAdminStaffMainMenu()) {
          return;
        }
      } else {
        view.displayError("Current user role is not supported.");
      }
    }
  }

  private boolean handleGuestMainMenu() {
    int selectedIndex = selectFromMenu(List.of(GuestMenuOptions.values()), "Guest Menu");
    if (selectedIndex < 0) {
      return false;
    }

    GuestMenuOptions selectedOption = List.of(GuestMenuOptions.values()).get(selectedIndex);

    switch (selectedOption) {
      case LOGIN -> getUserController().login();
      case REGISTER_EP -> getUserController().registerEntertainmentProvider();
    }

    return true;
  }

  private boolean handleStudentMainMenu() {
    int selectedIndex = selectFromMenu(List.of(StudentMenuOptions.values()), "Student Menu");
    if (selectedIndex < 0) {
      return false;
    }

    StudentMenuOptions selectedOption = List.of(StudentMenuOptions.values()).get(selectedIndex);

    switch (selectedOption) {
      case LOGOUT -> getUserController().logout();
      case SEARCH_FOR_PERFORMANCES -> getEventPerformanceController().searchforPerformances();
      case VIEW_PERFORMANCE -> getEventPerformanceController().viewPerformance();
      case EDIT_PREFERENCES -> getUserController().editPreferences();
      case BOOK_EVENT -> getBookingController().bookPerformance();
      case CANCEL_BOOKING -> getBookingController().cancelBooking();
    }

    return true;
  }

  private boolean handleEntertainmentProviderMainMenu() {
    int selectedIndex =
        selectFromMenu(List.of(EPMenuOptions.values()), "Entertainment Provider Menu");
    if (selectedIndex < 0) {
      return false;
    }

    EPMenuOptions selectedOption = List.of(EPMenuOptions.values()).get(selectedIndex);

    switch (selectedOption) {
      case LOGOUT -> getUserController().logout();
      case SEARCH_FOR_PERFORMANCES -> getEventPerformanceController().searchforPerformances();
      case VIEW_PERFORMANCE -> getEventPerformanceController().viewPerformance();
      case CREATE_EVENT -> getEventPerformanceController().createEvent();
      case CANCEL_PERFORMANCE -> getEventPerformanceController().cancelPerformance();
    }

    return true;
  }

  private boolean handleAdminStaffMainMenu() {
    int selectedIndex = selectFromMenu(List.of(AdminMenuOptions.values()), "Admin Menu");
    if (selectedIndex < 0) {
      return false;
    }

    AdminMenuOptions selectedOption = List.of(AdminMenuOptions.values()).get(selectedIndex);

    switch (selectedOption) {
      case LOGOUT -> getUserController().logout();
      case SEARCH_FOR_PERFORMANCES -> getEventPerformanceController().searchforPerformances();
      case VIEW_PERFORMANCE -> getEventPerformanceController().viewPerformance();
    }

    return true;
  }

  private UserController getUserController() {
    return userController;
  }

  private EventPerformanceController getEventPerformanceController() {
    return eventPerformanceController;
  }

  private BookingController getBookingController() {
    return bookingController;
  }

  private void syncCurrentUserAcrossControllers() {
    UserController uc = getUserController();
    setCurrentUser(uc.getCurrentUser());
    getEventPerformanceController().setCurrentUser(uc.getCurrentUser());
    getBookingController().setCurrentUser(uc.getCurrentUser());
  }
}
