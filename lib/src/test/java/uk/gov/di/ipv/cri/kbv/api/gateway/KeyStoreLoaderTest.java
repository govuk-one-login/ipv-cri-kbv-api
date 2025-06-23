package uk.gov.di.ipv.cri.kbv.api.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeyStoreLoaderTest {
    final String base64KeyStorePassword = "keystore-password";
    final String keystoreValue = "a2V5c3RvcmUtdmFsdWU=";
    @Mock private ConfigurationService mockConfigurationService;
    private KeyStoreLoader keyStoreLoader;

    @BeforeEach
    void setUp() {
        this.keyStoreLoader = new KeyStoreLoader(mockConfigurationService);
        when(mockConfigurationService.getSecretValue("experian/keystore"))
                .thenReturn(keystoreValue);
    }

    @Test
    void loadShouldSetupSystemProperties() throws IOException {
        when(mockConfigurationService.getSecretValue("experian/keystore-password"))
                .thenReturn(base64KeyStorePassword);

        verify(mockConfigurationService).getSecretValue("experian/keystore");
        verify(mockConfigurationService).getSecretValue("experian/keystore-password");

        assertEquals("pkcs12", System.getProperty("javax.net.ssl.keyStoreType"));
        String keyStoreSysProperty = System.getProperty("javax.net.ssl.keyStore");
        assertTrue(keyStoreSysProperty.startsWith(System.getProperty("java.io.tmpdir")));
        assertTrue(keyStoreSysProperty.endsWith(".tmp"));
        assertEquals(base64KeyStorePassword, System.getProperty("javax.net.ssl.keyStorePassword"));
    }

    @Test
    void shouldHandleNullKeystoreSecretValueGracefully() {
        when(mockConfigurationService.getSecretValue("experian/keystore")).thenReturn(null);

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> {
                            keyStoreLoader.load();
                        });

        assertEquals(
                "Persist keystore to file failed: Cannot invoke \"String.getBytes(java.nio.charset.Charset)\" because \"src\" is null",
                exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenKeystorePathFails() {
        when(mockConfigurationService.getSecretValue("experian/keystore"))
                .thenReturn("wrong keystore");

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> {
                            keyStoreLoader.load();
                        });

        assertEquals(
                "Persist keystore to file failed: Illegal base64 character 20",
                exception.getMessage());
    }
}
