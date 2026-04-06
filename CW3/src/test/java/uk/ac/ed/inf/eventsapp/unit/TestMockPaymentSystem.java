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
    paymentSystem = new MockPaymentSystem();
  }

  // --- processPayment ---

  @Test
  void processPaymentReturnsTrueForValidInputs() {
    assertTrue(paymentSystem.processPayment(2, "Live Music", "student@ed.ac.uk", 123456789,
        "provider@gmail.com", 30.0), "processPayment should return true for valid inputs.");
  }

  @Test
  void processPaymentReturnsTrueForMinimumValidTickets() {
    assertTrue(paymentSystem.processPayment(1, "Live Music", "student@ed.ac.uk", 123456789,
        "provider@gmail.com", 15.0), "processPayment should return true for exactly 1 ticket.");
  }

  @Test
  void processPaymentReturnsTrueForMinimumValidAmount() {
    assertTrue(
        paymentSystem.processPayment(1, "Live Music", "student@ed.ac.uk", 123456789,
            "provider@gmail.com", 0.01),
        "processPayment should return true for the minimum positive transaction amount.");
  }

  // --- processPayment: phone number is not validated ---

  @Test
  void processPaymentReturnsTrueForZeroPhoneNumber() {
    assertTrue(paymentSystem.processPayment(2, "Live Music", "student@ed.ac.uk", 0,
        "provider@gmail.com", 30.0),
        "processPayment should return true regardless of phone number value.");
  }

  @Test
  void processPaymentReturnsTrueForNegativePhoneNumber() {
    assertTrue(paymentSystem.processPayment(2, "Live Music", "student@ed.ac.uk", -1,
        "provider@gmail.com", 30.0),
        "processPayment should return true even with negative phone number.");
  }

  // --- processPayment: null inputs ---

  @Test
  void processPaymentReturnsFalseForNullStudentEmail() {
    assertFalse(
        paymentSystem.processPayment(2, "Live Music", null, 123456789, "provider@gmail.com", 30.0),
        "processPayment should return false when studentEmail is null.");
  }

  @Test
  void processPaymentReturnsFalseForNullProviderEmail() {
    assertFalse(
        paymentSystem.processPayment(2, "Live Music", "student@ed.ac.uk", 123456789, null, 30.0),
        "processPayment should return false when epEmail is null.");
  }

  @Test
  void processPaymentReturnsFalseForNullEventTitle() {
    assertFalse(paymentSystem.processPayment(2, null, "student@ed.ac.uk", 123456789,
        "provider@gmail.com", 30.0), "processPayment should return false when eventTitle is null.");
  }

  // --- processPayment: multiple nulls ---

  @Test
  void processPaymentReturnsFalseWhenAllStringsNull() {
    assertFalse(paymentSystem.processPayment(2, null, null, 123456789, null, 30.0),
        "processPayment should return false when all string parameters are null.");
  }

  // --- processPayment: large values ---

  @Test
  void processPaymentReturnsTrueForLargeTicketCount() {
    assertTrue(
        paymentSystem.processPayment(Integer.MAX_VALUE, "Live Music", "student@ed.ac.uk", 123456789,
            "provider@gmail.com", 30.0),
        "processPayment should return true for very large ticket count.");
  }

  @Test
  void processPaymentReturnsTrueForVerySmallPositiveAmount() {
    assertTrue(
        paymentSystem.processPayment(1, "Live Music", "student@ed.ac.uk", 123456789,
            "provider@gmail.com", Double.MIN_VALUE),
        "processPayment should return true for the smallest positive double.");
  }

  // --- processPayment: invalid numeric inputs ---

  @Test
  void processPaymentReturnsFalseForZeroTickets() {
    assertFalse(
        paymentSystem.processPayment(0, "Live Music", "student@ed.ac.uk", 123456789,
            "provider@gmail.com", 30.0),
        "processPayment should return false when numTickets is zero.");
  }

  @Test
  void processPaymentReturnsFalseForNegativeTickets() {
    assertFalse(
        paymentSystem.processPayment(-1, "Live Music", "student@ed.ac.uk", 123456789,
            "provider@gmail.com", 30.0),
        "processPayment should return false when numTickets is negative.");
  }

  @Test
  void processPaymentReturnsFalseForZeroAmount() {
    assertFalse(
        paymentSystem.processPayment(2, "Live Music", "student@ed.ac.uk", 123456789,
            "provider@gmail.com", 0.0),
        "processPayment should return false when transactionAmount is zero.");
  }

  @Test
  void processPaymentReturnsFalseForNegativeAmount() {
    assertFalse(
        paymentSystem.processPayment(2, "Live Music", "student@ed.ac.uk", 123456789,
            "provider@gmail.com", -10.0),
        "processPayment should return false when transactionAmount is negative.");
  }

  // --- processRefund ---

  @Test
  void processRefundReturnsTrueForValidInputsWithMessage() {
    assertTrue(
        paymentSystem.processRefund(2, "Live Music", "student@ed.ac.uk", 123456789,
            "provider@gmail.com", 30.0, "Event cancelled due to weather."),
        "processRefund should return true for valid inputs with an organiser message.");
  }

  @Test
  void processRefundReturnsTrueForNullOrganiserMessage() {
    assertTrue(
        paymentSystem.processRefund(2, "Live Music", "student@ed.ac.uk", 123456789,
            "provider@gmail.com", 30.0, null),
        "processRefund should return true even when organiserMsg is null.");
  }

  @Test
  void processRefundReturnsTrueForMinimumValidTickets() {
    assertTrue(
        paymentSystem.processRefund(1, "Live Music", "student@ed.ac.uk", 123456789,
            "provider@gmail.com", 15.0, "Sorry"),
        "processRefund should return true for exactly 1 ticket.");
  }

  @Test
  void processRefundReturnsTrueForMinimumValidAmount() {
    assertTrue(
        paymentSystem.processRefund(1, "Live Music", "student@ed.ac.uk", 123456789,
            "provider@gmail.com", 0.01, "Sorry"),
        "processRefund should return true for the minimum positive transaction amount.");
  }

  // --- processRefund: large values ---

  @Test
  void processRefundReturnsTrueForLargeTicketCount() {
    assertTrue(
        paymentSystem.processRefund(10000, "Live Music", "student@ed.ac.uk", 123456789,
            "provider@gmail.com", 30.0, "Sorry"),
        "processRefund should return true for very large ticket count.");
  }

  // --- processRefund: null inputs ---

  @Test
  void processRefundReturnsFalseForNullStudentEmail() {
    assertFalse(paymentSystem.processRefund(2, "Live Music", null, 123456789, "provider@gmail.com",
        30.0, "Cancelled"), "processRefund should return false when studentEmail is null.");
  }

  @Test
  void processRefundReturnsFalseForNullProviderEmail() {
    assertFalse(paymentSystem.processRefund(2, "Live Music", "student@ed.ac.uk", 123456789, null,
        30.0, "Cancelled"), "processRefund should return false when epEmail is null.");
  }

  @Test
  void processRefundReturnsFalseForNullEventTitle() {
    assertFalse(paymentSystem.processRefund(2, null, "student@ed.ac.uk", 123456789,
        "provider@gmail.com", 30.0, "Cancelled"),
        "processRefund should return false when eventTitle is null.");
  }

  // --- processRefund: empty string inputs ---

  @Test
  void processRefundReturnsTrueForEmptyOrganiserMessage() {
    assertTrue(
        paymentSystem.processRefund(2, "Live Music", "student@ed.ac.uk", 123456789,
            "provider@gmail.com", 30.0, ""),
        "processRefund should return true when organiserMsg is an empty string.");
  }

  // --- processRefund: invalid numeric inputs ---

  @Test
  void processRefundReturnsFalseForZeroTickets() {
    assertFalse(
        paymentSystem.processRefund(0, "Live Music", "student@ed.ac.uk", 123456789,
            "provider@gmail.com", 30.0, "Cancelled"),
        "processRefund should return false when numTickets is zero.");
  }

  @Test
  void processRefundReturnsFalseForNegativeTickets() {
    assertFalse(
        paymentSystem.processRefund(-1, "Live Music", "student@ed.ac.uk", 123456789,
            "provider@gmail.com", 30.0, "Cancelled"),
        "processRefund should return false when numTickets is negative.");
  }

  @Test
  void processRefundReturnsFalseForZeroAmount() {
    assertFalse(
        paymentSystem.processRefund(2, "Live Music", "student@ed.ac.uk", 123456789,
            "provider@gmail.com", 0.0, "Cancelled"),
        "processRefund should return false when transactionAmount is zero.");
  }

  @Test
  void processRefundReturnsFalseForNegativeAmount() {
    assertFalse(
        paymentSystem.processRefund(2, "Live Music", "student@ed.ac.uk", 123456789,
            "provider@gmail.com", -10.0, "Cancelled"),
        "processRefund should return false when transactionAmount is negative.");
  }
}
