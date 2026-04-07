package uk.ac.ed.inf.eventsapp.integration;

/**
 * Adapter around the provided external mock verification service.
 */
public class MockVerificationSystem implements VerificationSystem {
  private final external.VerificationService delegate;

  /** Creates an adapter backed by the provided external mock verification service. */
  public MockVerificationSystem() {
    this(new external.MockVerificationService());
  }

  /**
   * Creates an adapter backed by the supplied verification-service implementation.
   *
   * @param delegate verification-service implementation used to perform checks
   */
  public MockVerificationSystem(external.VerificationService delegate) {
    this.delegate = delegate;
  }

  /**
   * Delegates verification to the wrapped external service.
   *
   * @param businessRegistrationNumber provider business-registration number
   * @return {@code true} if the provider can be verified, otherwise {@code false}
   */
  @Override
  public boolean verifyEntertainmentProvider(String businessRegistrationNumber) {
    return delegate.verifyEntertainmentProvider(businessRegistrationNumber);
  }
}
