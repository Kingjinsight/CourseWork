import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Interface for verifying whether an entertainment provider has a valid government business
 * registration number.
 */
public interface BusinessVerifier {
    boolean verifyBusiness(String businessNumber);
}


/**
 * Implementation that verifies business numbers using a government registry. The registry list is
 * converted to a HashSet for constant-time lookup.
 */
class GovernmentBusinessVerifier implements BusinessVerifier {

    private final String location;
    private final Set<String> validNumbers;

    /**
     * Constructor. Converts the primitive government list into a HashSet.
     */
    public GovernmentBusinessVerifier(String location, List<String> governmentList) {
        if (location == null || location.isEmpty()) {
            throw new IllegalArgumentException("Location must not be empty");
        }

        if (governmentList == null) {
            throw new IllegalArgumentException("Government list must not be null");
        }

        this.location = location;
        this.validNumbers = new HashSet<>();

        for (String number : governmentList) {
            if (number != null && !number.isEmpty()) {
                validNumbers.add(normalize(number));
            }
        }
    }

    /**
     * Returns the location (country/region) this verifier is responsible for.
     */
    public String getLocation() {
        return location;
    }

    /**
     * Verifies whether a business number exists in the government registry.
     */
    @Override
    public boolean verifyBusiness(String businessNumber) {
        if (businessNumber == null || businessNumber.isEmpty()) {
            return false;
        }

        // Lookup complexity: O(1)
        return validNumbers.contains(normalize(businessNumber));
    }

    /**
     * Normalizes a registration number to avoid format mismatches.
     */
    private String normalize(String number) {
        return number.trim().toUpperCase();
    }
}
