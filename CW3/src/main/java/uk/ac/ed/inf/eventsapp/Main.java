package uk.ac.ed.inf.eventsapp;

import java.util.ArrayList;
import java.util.Collection;

import external.MockPaymentSystem;
import external.PaymentSystem;
import uk.ac.ed.inf.eventsapp.controller.BookingController;
import uk.ac.ed.inf.eventsapp.controller.EventPerformanceController;
import uk.ac.ed.inf.eventsapp.controller.MenuController;
import uk.ac.ed.inf.eventsapp.controller.UserController;
import uk.ac.ed.inf.eventsapp.integration.MockVerificationSystem;
import uk.ac.ed.inf.eventsapp.integration.VerificationSystem;
import uk.ac.ed.inf.eventsapp.model.Booking;
import uk.ac.ed.inf.eventsapp.model.Event;
import uk.ac.ed.inf.eventsapp.model.Performance;
import uk.ac.ed.inf.eventsapp.model.User;
import uk.ac.ed.inf.eventsapp.view.ExitRequestedException;
import uk.ac.ed.inf.eventsapp.view.TextUserInterface;
import uk.ac.ed.inf.eventsapp.view.View;

/**
 * Application entry point for the text-based events system.
 */
public class Main {
  /**
   * Creates the shared controllers and launches the main menu loop.
   *
   * @param args command-line arguments, which are ignored by this application
   */
  public static void main(String[] args) {
    View view = new TextUserInterface();
    Collection<User> users = new ArrayList<>();
    Collection<Event> events = new ArrayList<>();
    Collection<Performance> performances = new ArrayList<>();
    Collection<Booking> bookings = new ArrayList<>();
    VerificationSystem verificationSystem = new MockVerificationSystem();
    PaymentSystem paymentSystem = new MockPaymentSystem();

    UserController userController = new UserController(view, verificationSystem, users, events);
    EventPerformanceController eventPerformanceController =
        new EventPerformanceController(view, events, performances, paymentSystem);
    BookingController bookingController =
        new BookingController(view, paymentSystem, events, performances, bookings);
    MenuController menuController =
        new MenuController(view, userController, eventPerformanceController, bookingController);

    try {
      menuController.mainMenu();
    } catch (ExitRequestedException exception) {
      System.out.println("Exiting application.");
    }
  }
}
