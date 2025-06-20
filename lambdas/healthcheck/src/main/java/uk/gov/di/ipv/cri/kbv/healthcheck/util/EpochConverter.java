package uk.gov.di.ipv.cri.kbv.healthcheck.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class EpochConverter {

    private EpochConverter() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    public static String convertEpochMillisToDate(long epochMillis) {
        return convertEpochMillisToDate(new Date(epochMillis));
    }

    public static String convertEpochMillisToDate(Date date) {
        SimpleDateFormat df = new SimpleDateFormat("MMMM dd, yyyy HH:mm:ss z");
        return df.format(date);
    }
}
