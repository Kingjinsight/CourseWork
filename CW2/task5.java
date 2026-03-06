import java.util.HashSet;
import java.util.List;


public interface BusinessVerifier {
    boolean verifyBusiness(String businessNumber);
}

// Converts the government's primitive list into a HashSet for O(1) lookup.
class GovernmentBusinessVerifier implements BusinessVerifier {
    private final String location;
    private final HashSet<String> validNumbers;

    public GovernmentBusinessVerifier(String location, List<String> governmentList) {
        this.location = location;
        this.validNumbers = new HashSet<>();
        for (String number : governmentList) {
            validNumbers.add(number);
        }
    }

    public String getLocation() {
        return location;
    }

    @Override
    public boolean verifyBusiness(String businessNumber) {
        if (businessNumber == null || businessNumber.isEmpty()) {
            return false;
        }
        return validNumbers.contains(businessNumber);
    }
}
