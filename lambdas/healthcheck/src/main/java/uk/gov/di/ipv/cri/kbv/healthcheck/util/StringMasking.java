package uk.gov.di.ipv.cri.kbv.healthcheck.util;

public class StringMasking {

    private StringMasking() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    public static String maskString(String input, int keepLength) {
        if (keepLength >= input.length()) {
            return "*".repeat(input.length());
        }

        return input.substring(0, Math.max(0, keepLength))
                + "*".repeat(input.length() - keepLength);
    }
}
