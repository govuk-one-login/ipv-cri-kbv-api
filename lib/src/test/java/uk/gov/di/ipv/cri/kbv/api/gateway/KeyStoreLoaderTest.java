package uk.gov.di.ipv.cri.kbv.api.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeyStoreLoaderTest {
    @Mock private ConfigurationService mockConfigurationService;
    private KeyStoreLoader keyStoreLoader;

    @BeforeEach
    void setUp() {
        this.keyStoreLoader = new KeyStoreLoader(mockConfigurationService);
    }

    @Test
    void loadShouldSetupSystemProperties() {
        final String base64KeyStorePassword = "keystore-password";
        final String keystoreValue = "a2V5c3RvcmUtdmFsdWU=";
        when(mockConfigurationService.getSecretValue("experian/keystore"))
                .thenReturn(keystoreValue);
        when(mockConfigurationService.getSecretValue("experian/keystore-password"))
                .thenReturn(base64KeyStorePassword);
        keyStoreLoader.load();
        verify(mockConfigurationService).getSecretValue("experian/keystore");
        verify(mockConfigurationService).getSecretValue("experian/keystore-password");

        assertEquals("pkcs12", System.getProperty("javax.net.ssl.keyStoreType"));
        String keyStoreSysProperty = System.getProperty("javax.net.ssl.keyStore");
        assertTrue(keyStoreSysProperty.endsWith(".tmp"));
        assertEquals(base64KeyStorePassword, System.getProperty("javax.net.ssl.keyStorePassword"));
    }
}
