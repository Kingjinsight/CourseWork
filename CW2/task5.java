import java.util.HashSet;
import java.util.List;

public interface VerificationSystem {
    boolean verifyBusiness(String businessNumber);
}

class BusinessVerifier implements VerificationSystem {

    private HashSet<String> validNumbers;

    public BusinessVerifier(List<String> governmentList) {
        validNumbers = new HashSet<>();
        for (String number : governmentList) {
            validNumbers.add(number);
        }
    }

    @Override
    public boolean verifyBusiness(String businessNumber) {
        if (businessNumber == null || businessNumber.isEmpty()) {
            return false;
        }
        return validNumbers.contains(businessNumber);
    }
}
