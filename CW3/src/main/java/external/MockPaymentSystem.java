package external;

/**
 * A mock implementation of {@link PaymentSystem} for testing purposes. In a real life application,
 * this implementation would be making network requests to a payment service API. However,
 * networking is the topic of another course (if this sounds interesting, you may want to take COMN
 * - Computer Communications and Networks in year 3 or 4).
 */
public class MockPaymentSystem implements PaymentSystem {
  private static final String ANSI_CYAN = "\u001B[36m";
  private static final String ANSI_RESET = "\u001B[0m";

  @Override
  public boolean processPayment(int numTickets, String eventTitle, String studentEmail,
      int studentPhone, String epEmail, double transactionAmount) {
    if (studentEmail == null || epEmail == null || eventTitle == null) {
      return false;
    }
    if (numTickets <= 0 || transactionAmount <= 0) {
      return false;
    }
    System.out.print(ANSI_CYAN);
    System.out.println("Student with email " + studentEmail + " and phone number " + studentPhone
        + " has purchased " + numTickets + " tickets for the event " + eventTitle
        + " ran by the provider with email " + epEmail + " totalling £" + transactionAmount);
    System.out.print(ANSI_RESET);
    return true;
  }

  @Override
  public boolean processRefund(int numTickets, String eventTitle, String studentEmail,
      int studentPhone, String epEmail, double transactionAmount, String organiserMsg) {
    if (studentEmail == null || epEmail == null || eventTitle == null) {
      return false;
    }
    if (numTickets <= 0 || transactionAmount <= 0) {
      return false;
    }
    System.out.print(ANSI_CYAN);
    System.out.println("Student with email " + studentEmail + " and phone number " + studentPhone
        + " who had purchased " + numTickets + " tickets for the event " + eventTitle
        + " ran by the provider with email " + epEmail + " totalling £" + transactionAmount
        + " has been refunded.");
    if (organiserMsg != null) {
      System.out.println("Message from the provider who cancelled the event: " + organiserMsg);
    }
    System.out.print(ANSI_RESET);
    return true;
  }
}
