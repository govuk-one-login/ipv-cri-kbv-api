package uk.gov.di.ipv.cri.kbv.healthcheck.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EpochConverterTest {

    @Test
    void shouldFormatStringEpoch() {
        long firstJan2023At1am = 1672531199000L;

        String result = EpochConverter.convertEpochMillisToDate(firstJan2023At1am);

        SimpleDateFormat expectedFormat = new SimpleDateFormat("MMMM dd, yyyy HH:mm:ss z");

        String expected = expectedFormat.format(new Date(firstJan2023At1am));

        assertEquals(expected, result);
    }

    @Test
    void shouldFormatDateObject() {
        SimpleDateFormat expectedFormat = new SimpleDateFormat("MMMM dd, yyyy HH:mm:ss z");

        Date date = new Date(1609459200000L); // Jan 1, 2021 00:00:00 UTC

        String result = EpochConverter.convertEpochMillisToDate(date);

        assertEquals(expectedFormat.format(date), result);
    }

    @Test
    void testPrivateConstructor() throws Exception {
        Constructor<EpochConverter> constructor = EpochConverter.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        InvocationTargetException exception =
                assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertTrue(exception.getCause() instanceof AssertionError);
        assertEquals("Utility class cannot be instantiated", exception.getCause().getMessage());
    }
}
