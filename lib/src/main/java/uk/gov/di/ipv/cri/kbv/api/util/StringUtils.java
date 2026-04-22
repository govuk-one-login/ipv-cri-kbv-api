package uk.gov.di.ipv.cri.kbv.api.util;

import java.util.Objects;

public class StringUtils {
    private StringUtils() {
        throw new AssertionError("Utility class");
    }

    public static boolean isNotBlank(String input) {
        return Objects.nonNull(input) && !input.isBlank();
    }
}
