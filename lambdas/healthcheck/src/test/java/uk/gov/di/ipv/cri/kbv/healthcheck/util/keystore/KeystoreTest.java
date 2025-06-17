package uk.gov.di.ipv.cri.kbv.healthcheck.util.keystore;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class KeystoreTest {

    @Test
    void shouldCreateKeyStoreFile() throws IOException {
        byte[] keystoreContent = "dummy keystore content".getBytes();

        String base64Keystore = Base64.getEncoder().encodeToString(keystoreContent);

        String result = Keystore.createKeyStoreFile(base64Keystore);

        assertTrue(result.endsWith(".jks"));
    }

    @Test
    void shouldThrowWhenInvalidBase64Keystore() {
        String invalidBase64 = "invalid@@@";

        IOException ex =
                assertThrows(IOException.class, () -> Keystore.createKeyStoreFile(invalidBase64));

        assertTrue(ex.getMessage().contains("Failed to decode Base64 keystore content"));
    }

    @Test
    void testPrivateConstructor() throws Exception {
        Constructor<Keystore> constructor = Keystore.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        InvocationTargetException exception =
                assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertTrue(exception.getCause() instanceof AssertionError);
        assertEquals("Utility class cannot be instantiated", exception.getCause().getMessage());
    }
}
