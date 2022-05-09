package uk.gov.di.ipv.cri.experian.kbv.api.util;

import java.util.Objects;

public class StringUtils {
    public static boolean isNotBlank(String input) {
        return Objects.nonNull(input) && !input.isEmpty() && !input.isBlank();
    }
}
