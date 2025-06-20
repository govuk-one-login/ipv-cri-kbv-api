package uk.gov.di.ipv.cri.kbv.healthcheck.util.keytool;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import uk.gov.di.ipv.cri.kbv.healthcheck.util.bash.Bash;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mockStatic;

class KeytoolTest {

    @Test
    void shouldReturnImportCertOutput() {
        String outputPfx = "output.pfx";
        String srcKeystore = "source.jks";
        String password = "password123";
        String expectedOutput = "dummy output";

        try (MockedStatic<Bash> mockedBash = mockStatic(Bash.class)) {
            mockedBash.when(() -> Bash.execute(anyString())).thenReturn(expectedOutput);
            String result = Keytool.importCertificate(outputPfx, srcKeystore, password);
            assertEquals(expectedOutput, result);
        }
    }

    @Test
    void showThrowWhenKeyToolFailsToRun() {
        String outputPfx = "output.pfx";
        String srcKeystore = "source.jks";
        String password = "password123";

        try (MockedStatic<Bash> mockedBash = mockStatic(Bash.class)) {
            mockedBash
                    .when(() -> Bash.execute(anyString()))
                    .thenThrow(new RuntimeException("Bash failed"));
            SecurityException exception =
                    assertThrows(
                            SecurityException.class,
                            () -> Keytool.importCertificate(outputPfx, srcKeystore, password));
            assertTrue(exception.getMessage().contains("Failed to import certificate"));
        }
    }

    @Test
    void shouldReturnKeyStoreContent() {
        String keystore = "keystore.jks";
        String password = "secret";
        String expectedOutput = "dummy output";

        try (MockedStatic<Bash> mockedBash = mockStatic(Bash.class)) {
            mockedBash.when(() -> Bash.execute(anyString())).thenReturn(expectedOutput);
            String result = Keytool.getKeyStoreContents(keystore, password);
            assertEquals(expectedOutput, result);
        }
    }

    @Test
    void shouldThrowWhenKeyStoreFailsToRun() {
        String keystore = "keystore.jks";
        String password = "secret";

        try (MockedStatic<Bash> mockedBash = mockStatic(Bash.class)) {
            mockedBash
                    .when(() -> Bash.execute(anyString()))
                    .thenThrow(new RuntimeException("Failure"));
            SecurityException exception =
                    assertThrows(
                            SecurityException.class,
                            () -> Keytool.getKeyStoreContents(keystore, password));
            assertTrue(exception.getMessage().contains("Failed to list keystore contents"));
        }
    }

    @Test
    void testPrivateConstructor() throws Exception {
        Constructor<Keytool> constructor = Keytool.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        InvocationTargetException exception =
                assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertTrue(exception.getCause() instanceof AssertionError);
        assertEquals("Utility class cannot be instantiated", exception.getCause().getMessage());
    }
}
