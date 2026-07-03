public final class IdentityFormatValidator {
    private IdentityFormatValidator() {
    }

    public enum IdentityType {
        PAN,
        AADHAAR
    }

    public enum ReasonCode {
        VALID,
        EMPTY_INPUT,
        INVALID_LENGTH,
        INVALID_CHARACTER,
        INVALID_PAN_CATEGORY,
        INVALID_PAN_STRUCTURE,
        INVALID_AADHAAR_CHECK_DIGIT
    }

    public static final class ValidationResult {
        private final boolean valid;
        private final IdentityType type;
        private final String normalizedValue;
        private final ReasonCode reasonCode;
        private final String message;

        private ValidationResult(boolean valid, IdentityType type, String normalizedValue, ReasonCode reasonCode, String message) {
            this.valid = valid;
            this.type = type;
            this.normalizedValue = normalizedValue;
            this.reasonCode = reasonCode;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public IdentityType getType() {
            return type;
        }

        public String getNormalizedValue() {
            return normalizedValue;
        }

        public ReasonCode getReasonCode() {
            return reasonCode;
        }

        public String getMessage() {
            return message;
        }

        public static ValidationResult success(IdentityType type, String normalizedValue) {
            return new ValidationResult(true, type, normalizedValue, ReasonCode.VALID, "Accepted format");
        }

        public static ValidationResult failure(IdentityType type, String normalizedValue, ReasonCode reasonCode, String message) {
            return new ValidationResult(false, type, normalizedValue, reasonCode, message);
        }

        @Override
        public String toString() {
            return "ValidationResult{" +
                    "valid=" + valid +
                    ", type=" + type +
                    ", normalizedValue='" + normalizedValue + '\'' +
                    ", reasonCode=" + reasonCode +
                    ", message='" + message + '\'' +
                    '}';
        }
    }

    private static final String PAN_CATEGORY_CODES = "ABCFGHLJPT";

    private static final int[][] VERHOEFF_D = {
            {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
            {1, 2, 3, 4, 0, 6, 7, 8, 9, 5},
            {2, 3, 4, 0, 1, 7, 8, 9, 5, 6},
            {3, 4, 0, 1, 2, 8, 9, 5, 6, 7},
            {4, 0, 1, 2, 3, 9, 5, 6, 7, 8},
            {5, 9, 8, 7, 6, 0, 4, 3, 2, 1},
            {6, 5, 9, 8, 7, 1, 0, 4, 3, 2},
            {7, 6, 5, 9, 8, 2, 1, 0, 4, 3},
            {8, 7, 6, 5, 9, 3, 2, 1, 0, 4},
            {9, 8, 7, 6, 5, 4, 3, 2, 1, 0}
    };

    private static final int[][] VERHOEFF_P = {
            {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
            {1, 5, 7, 6, 2, 8, 3, 0, 9, 4},
            {5, 8, 0, 3, 7, 9, 6, 1, 4, 2},
            {8, 9, 1, 6, 0, 4, 3, 5, 2, 7},
            {9, 4, 5, 3, 1, 2, 6, 8, 7, 0},
            {4, 2, 8, 6, 5, 7, 3, 9, 0, 1},
            {2, 7, 9, 3, 8, 0, 6, 4, 1, 5},
            {7, 0, 4, 6, 9, 1, 3, 2, 5, 8}
    };

    public static ValidationResult validatePan(String input) {
        String normalized = normalize(input);
        if (normalized.isEmpty()) {
            return ValidationResult.failure(IdentityType.PAN, normalized, ReasonCode.EMPTY_INPUT, "PAN value is empty");
        }

        if (normalized.length() != 10) {
            return ValidationResult.failure(IdentityType.PAN, normalized, ReasonCode.INVALID_LENGTH, "PAN must be 10 characters long");
        }

        for (int index = 0; index < normalized.length(); index++) {
            char current = normalized.charAt(index);
            boolean shouldBeLetter = index < 5 || index == 9;
            if (shouldBeLetter) {
                if (!isAsciiUppercaseLetter(current)) {
                    return ValidationResult.failure(IdentityType.PAN, normalized, ReasonCode.INVALID_PAN_STRUCTURE, "PAN must contain only letters in positions 1-5 and 10");
                }
            } else {
                if (!isAsciiDigit(current)) {
                    return ValidationResult.failure(IdentityType.PAN, normalized, ReasonCode.INVALID_PAN_STRUCTURE, "PAN must contain only digits in positions 6-9");
                }
            }
        }

        char category = normalized.charAt(3);
        if (PAN_CATEGORY_CODES.indexOf(category) < 0) {
            return ValidationResult.failure(IdentityType.PAN, normalized, ReasonCode.INVALID_PAN_CATEGORY, "PAN category letter is not allowed");
        }

        return ValidationResult.success(IdentityType.PAN, normalized);
    }

    public static ValidationResult validateAadhaar(String input) {
        String normalized = normalize(input);
        if (normalized.isEmpty()) {
            return ValidationResult.failure(IdentityType.AADHAAR, normalized, ReasonCode.EMPTY_INPUT, "Aadhaar value is empty");
        }

        if (normalized.length() != 12) {
            return ValidationResult.failure(IdentityType.AADHAAR, normalized, ReasonCode.INVALID_LENGTH, "Aadhaar must be 12 digits long");
        }

        for (int index = 0; index < normalized.length(); index++) {
            if (!isAsciiDigit(normalized.charAt(index))) {
                return ValidationResult.failure(IdentityType.AADHAAR, normalized, ReasonCode.INVALID_CHARACTER, "Aadhaar must contain only ASCII digits");
            }
        }

        if (!passesVerhoeff(normalized)) {
            return ValidationResult.failure(IdentityType.AADHAAR, normalized, ReasonCode.INVALID_AADHAAR_CHECK_DIGIT, "Aadhaar check digit is invalid");
        }

        return ValidationResult.success(IdentityType.AADHAAR, normalized);
    }

    public static ValidationResult validate(String input) {
        String normalized = normalize(input);
        if (normalized.isEmpty()) {
            return ValidationResult.failure(null, normalized, ReasonCode.EMPTY_INPUT, "Identity value is empty");
        }

        if (normalized.length() == 10) {
            ValidationResult panResult = validatePan(normalized);
            if (panResult.isValid()) {
                return panResult;
            }
        }

        if (normalized.length() == 12) {
            ValidationResult aadhaarResult = validateAadhaar(normalized);
            if (aadhaarResult.isValid()) {
                return aadhaarResult;
            }
        }

        if (normalized.length() == 10) {
            return validatePan(normalized);
        }

        if (normalized.length() == 12) {
            return validateAadhaar(normalized);
        }

        return ValidationResult.failure(null, normalized, ReasonCode.INVALID_LENGTH, "Unsupported identity length; expected PAN or Aadhaar format");
    }

    private static String normalize(String input) {
        return input == null ? "" : input.trim().toUpperCase();
    }

    private static boolean isAsciiUppercaseLetter(char value) {
        return value >= 'A' && value <= 'Z';
    }

    private static boolean isAsciiDigit(char value) {
        return value >= '0' && value <= '9';
    }

    private static boolean passesVerhoeff(String value) {
        int checksum = 0;
        int position = 0;

        for (int index = value.length() - 1; index >= 0; index--) {
            int digit = value.charAt(index) - '0';
            checksum = VERHOEFF_D[checksum][VERHOEFF_P[position % 8][digit]];
            position++;
        }

        return checksum == 0;
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java IdentityFormatValidator <PAN|AADHAAR>");
            return;
        }

        ValidationResult result = validate(args[0]);
        System.out.println(result);
    }
}