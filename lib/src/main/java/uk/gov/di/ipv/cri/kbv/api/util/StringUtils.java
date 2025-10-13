package uk.gov.di.ipv.cri.kbv.api.util;

import java.util.Objects;

public class StringUtils {
    public static boolean isNotBlank(String input) {
        return Objects.nonNull(input) && !input.isEmpty() && !input.isBlank();
    }

    public static String whitespaceToUnderscore(String value) {
        return value.replaceAll("\\s+", "_");
    }
}
