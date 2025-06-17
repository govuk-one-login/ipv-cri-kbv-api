package uk.gov.di.ipv.cri.kbv.healthcheck.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

class StringMaskingTest {

    @ParameterizedTest(name = "maskString(\"{0}\", {1}) should return \"{2}\"")
    @CsvSource({
        "SensitiveData, 4, Sens*********",
        "Secret, 0, ******",
        "dummy, 5, *****",
        "Short, 10, *****",
        "'', 3, ''"
    })
    void shouldMaskStringCorrectly(String input, int keepLength, String expected) {
        String result = StringMasking.maskString(input, keepLength);
        assertEquals(expected, result);
    }

    @Test
    void testPrivateConstructor() throws Exception {
        Constructor<StringMasking> constructor = StringMasking.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        InvocationTargetException exception =
                assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertTrue(exception.getCause() instanceof AssertionError);
        assertEquals("Utility class cannot be instantiated", exception.getCause().getMessage());
    }
}
