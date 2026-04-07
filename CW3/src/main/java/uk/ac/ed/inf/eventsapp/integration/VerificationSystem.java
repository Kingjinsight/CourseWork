package uk.ac.ed.inf.eventsapp.integration;

/**
 * {@code VerificationSystem} abstraction named to match the UML diagram.
 *
 * <p>
 * The user-registration flow depends on this interface rather than the provided external
 * implementation directly, allowing the system to remain aligned with the coursework diagrams.
 */
public interface VerificationSystem {
  /**
   * Verifies that an entertainment provider is legitimate.
   *
   * @param businessRegistrationNumber provider business-registration number
   * @return {@code true} if the provider can be verified, otherwise {@code false}
   */
  boolean verifyEntertainmentProvider(String businessRegistrationNumber);
}
