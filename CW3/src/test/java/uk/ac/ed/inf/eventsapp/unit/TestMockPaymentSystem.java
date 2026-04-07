package uk.ac.ed.inf.eventsapp.unit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import external.MockPaymentSystem;

/**
 * Unit tests for MockPaymentSystem, covering valid inputs, null inputs, and boundary conditions for
 * both processPayment and processRefund.
 */
public class TestMockPaymentSystem {
  private MockPaymentSystem paymentSystem;

  @BeforeEach
  @SuppressWarnings("unused")
  void setUp() {
    // Creates a fresh mock payment system for each payment and refund scenario.
    paymentSystem = new MockPaymentSystem();
  }

  // Verifies that valid payment input is accepted.
  @Test
  void processPaymentReturnsTrueForValidInputs() {
    assertTrue(paymentSystem.processPayment(2, "Live Music", "student@ed.ac.uk", 123456789,
        "provider@gmail.com", 30.0), "processPayment should return true for valid inputs.");
  }

  // Verifies that the minimum valid ticket count is accepted for payment.
  @Test
  void processPaymentReturnsTrueForMinimumValidTickets() {
    assertTrue(paymentSystem.processPayment(1, "Live Music", "student@ed.ac.uk", 123456789,
        "provider@gmail.com", 15.0), "processPayment should return true for exactly 1 ticket.");
  }

  // Verifies that the minimum positive transaction amount is accepted for payment.
  @Test
  void processPaymentReturnsTrueForMinimumValidAmount() {
    assertTrue(
        paymentSystem.processPayment(1, "Live Music", "student@ed.ac.uk", 123456789,
            "provider@gmail.com", 0.01),
        "processPayment should return true for the minimum positive transaction amount.");
  }

  // Verifies that phone-number value does not affect successful payment processing.
  @Test
  void processPaymentReturnsTrueForZeroPhoneNumber() {
    assertTrue(paymentSystem.processPayment(2, "Live Music", "student@ed.ac.uk", 0,
        "provider@gmail.com", 30.0),
        "processPayment should return true regardless of phone number value.");
  }

  // Verifies that negative phone numbers do not affect successful payment processing.
  @Test
  void processPaymentReturnsTrueForNegativePhoneNumber() {
    assertTrue(paymentSystem.processPayment(2, "Live Music", "student@ed.ac.uk", -1,
        "provider@gmail.com", 30.0),
        "processPayment should return true even with negative phone number.");
  }

  // Verifies that payment fails when the student email is missing.
  @Test
  void processPaymentReturnsFalseForNullStudentEmail() {
    assertFalse(
        paymentSystem.processPayment(2, "Live Music", null, 123456789, "provider@gmail.com", 30.0),
        "processPayment should return false when studentEmail is null.");
  }

  // Verifies that payment fails when the provider email is missing.
  @Test
  void processPaymentReturnsFalseForNullProviderEmail() {
    assertFalse(
        paymentSystem.processPayment(2, "Live Music", "student@ed.ac.uk", 123456789, null, 30.0),
        "processPayment should return false when epEmail is null.");
  }

  // Verifies that payment fails when the event title is missing.
  @Test
  void processPaymentReturnsFalseForNullEventTitle() {
    assertFalse(paymentSystem.processPayment(2, null, "student@ed.ac.uk", 123456789,
        "provider@gmail.com", 30.0), "processPayment should return false when eventTitle is null.");
  }

  // Verifies that payment fails when all required string inputs are missing.
  @Test
  void processPaymentReturnsFalseWhenAllStringsNull() {
    assertFalse(paymentSystem.processPayment(2, null, null, 123456789, null, 30.0),
        "processPayment should return false when all string parameters are null.");
  }

  // Verifies that very large ticket counts still pass the mock payment rules.
  @Test
  void processPaymentReturnsTrueForLargeTicketCount() {
    assertTrue(
        paymentSystem.processPayment(Integer.MAX_VALUE, "Live Music", "student@ed.ac.uk", 123456789,
            "provider@gmail.com", 30.0),
        "processPayment should return true for very large ticket count.");
  }

  // Verifies that extremely small positive transaction amounts still pass the mock payment rules.
  @Test
  void processPaymentReturnsTrueForVerySmallPositiveAmount() {
    assertTrue(
        paymentSystem.processPayment(1, "Live Music", "student@ed.ac.uk", 123456789,
            "provider@gmail.com", Double.MIN_VALUE),
        "processPayment should return true for the smallest positive double.");
  }

  // Verifies that zero tickets are rejected during payment.
  @Test
  void processPaymentReturnsFalseForZeroTickets() {
    assertFalse(
        paymentSystem.processPayment(0, "Live Music", "student@ed.ac.uk", 123456789,
            "provider@gmail.com", 30.0),
        "processPayment should return false when numTickets is zero.");
  }

  // Verifies that negative ticket counts are rejected during payment.
  @Test
  void processPaymentReturnsFalseForNegativeTickets() {
    assertFalse(
        paymentSystem.processPayment(-1, "Live Music", "student@ed.ac.uk", 123456789,
            "provider@gmail.com", 30.0),
        "processPayment should return false when numTickets is negative.");
  }

  // Verifies that zero transaction amounts are rejected during payment.
  @Test
  void processPaymentReturnsFalseForZeroAmount() {
    assertFalse(
        paymentSystem.processPayment(2, "Live Music", "student@ed.ac.uk", 123456789,
            "provider@gmail.com", 0.0),
        "processPayment should return false when transactionAmount is zero.");
  }

  // Verifies that negative transaction amounts are rejected during payment.
  @Test
  void processPaymentReturnsFalseForNegativeAmount() {
    assertFalse(
        paymentSystem.processPayment(2, "Live Music", "student@ed.ac.uk", 123456789,
            "provider@gmail.com", -10.0),
        "processPayment should return false when transactionAmount is negative.");
  }

  // Verifies that valid refund input with an organiser message is accepted.
  @Test
  void processRefundReturnsTrueForValidInputsWithMessage() {
    assertTrue(
        paymentSystem.processRefund(2, "Live Music", "student@ed.ac.uk", 123456789,
            "provider@gmail.com", 30.0, "Event cancelled due to weather."),
        "processRefund should return true for valid inputs with an organiser message.");
  }

  // Verifies that a null organiser message is allowed during refund processing.
  @Test
  void processRefundReturnsTrueForNullOrganiserMessage() {
    assertTrue(
        paymentSystem.processRefund(2, "Live Music", "student@ed.ac.uk", 123456789,
            "provider@gmail.com", 30.0, null),
        "processRefund should return true even when organiserMsg is null.");
  }

  // Verifies that the minimum valid ticket count is accepted for refunds.
  @Test
  void processRefundReturnsTrueForMinimumValidTickets() {
    assertTrue(
        paymentSystem.processRefund(1, "Live Music", "student@ed.ac.uk", 123456789,
            "provider@gmail.com", 15.0, "Sorry"),
        "processRefund should return true for exactly 1 ticket.");
  }

  // Verifies that the minimum positive transaction amount is accepted for refunds.
  @Test
  void processRefundReturnsTrueForMinimumValidAmount() {
    assertTrue(
        paymentSystem.processRefund(1, "Live Music", "student@ed.ac.uk", 123456789,
            "provider@gmail.com", 0.01, "Sorry"),
        "processRefund should return true for the minimum positive transaction amount.");
  }

  // Verifies that very large ticket counts still pass the mock refund rules.
  @Test
  void processRefundReturnsTrueForLargeTicketCount() {
    assertTrue(
        paymentSystem.processRefund(10000, "Live Music", "student@ed.ac.uk", 123456789,
            "provider@gmail.com", 30.0, "Sorry"),
        "processRefund should return true for very large ticket count.");
  }

  // Verifies that refunds fail when the student email is missing.
  @Test
  void processRefundReturnsFalseForNullStudentEmail() {
    assertFalse(paymentSystem.processRefund(2, "Live Music", null, 123456789, "provider@gmail.com",
        30.0, "Cancelled"), "processRefund should return false when studentEmail is null.");
  }

  // Verifies that refunds fail when the provider email is missing.
  @Test
  void processRefundReturnsFalseForNullProviderEmail() {
    assertFalse(paymentSystem.processRefund(2, "Live Music", "student@ed.ac.uk", 123456789, null,
        30.0, "Cancelled"), "processRefund should return false when epEmail is null.");
  }

  // Verifies that refunds fail when the event title is missing.
  @Test
  void processRefundReturnsFalseForNullEventTitle() {
    assertFalse(paymentSystem.processRefund(2, null, "student@ed.ac.uk", 123456789,
        "provider@gmail.com", 30.0, "Cancelled"),
        "processRefund should return false when eventTitle is null.");
  }

  // Verifies that an empty organiser message is still accepted during refunds.
  @Test
  void processRefundReturnsTrueForEmptyOrganiserMessage() {
    assertTrue(
        paymentSystem.processRefund(2, "Live Music", "student@ed.ac.uk", 123456789,
            "provider@gmail.com", 30.0, ""),
        "processRefund should return true when organiserMsg is an empty string.");
  }

  // Verifies that zero tickets are rejected during refund processing.
  @Test
  void processRefundReturnsFalseForZeroTickets() {
    assertFalse(
        paymentSystem.processRefund(0, "Live Music", "student@ed.ac.uk", 123456789,
            "provider@gmail.com", 30.0, "Cancelled"),
        "processRefund should return false when numTickets is zero.");
  }

  // Verifies that negative ticket counts are rejected during refund processing.
  @Test
  void processRefundReturnsFalseForNegativeTickets() {
    assertFalse(
        paymentSystem.processRefund(-1, "Live Music", "student@ed.ac.uk", 123456789,
            "provider@gmail.com", 30.0, "Cancelled"),
        "processRefund should return false when numTickets is negative.");
  }

  // Verifies that zero transaction amounts are rejected during refund processing.
  @Test
  void processRefundReturnsFalseForZeroAmount() {
    assertFalse(
        paymentSystem.processRefund(2, "Live Music", "student@ed.ac.uk", 123456789,
            "provider@gmail.com", 0.0, "Cancelled"),
        "processRefund should return false when transactionAmount is zero.");
  }

  // Verifies that negative transaction amounts are rejected during refund processing.
  @Test
  void processRefundReturnsFalseForNegativeAmount() {
    assertFalse(
        paymentSystem.processRefund(2, "Live Music", "student@ed.ac.uk", 123456789,
            "provider@gmail.com", -10.0, "Cancelled"),
        "processRefund should return false when transactionAmount is negative.");
  }
}
