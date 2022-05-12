package uk.gov.di.ipv.cri.kbv.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.kbv.api.annotations.SystemProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.kbv.api.service.KBVSystemProperty.JAVAX_NET_SSL_KEYSTORE;
import static uk.gov.di.ipv.cri.kbv.api.service.KBVSystemProperty.JAVAX_NET_SSL_KEYSTORE_PASSWORD;
import static uk.gov.di.ipv.cri.kbv.api.service.KBVSystemProperty.JAVAX_NET_SSL_KEYSTORE_TYPE;

@ExtendWith(MockitoExtension.class)
class KBVSystemPropertyTest {
    private KBVSystemProperty systemProperty;
    @Mock KeyStoreService keyStoreServiceMock;

    @BeforeEach
    void setUp() {
        when(keyStoreServiceMock.getKeyStorePath()).thenReturn("key-store-value");
        when(keyStoreServiceMock.getPassword()).thenReturn("key-store-password");
        this.systemProperty = new KBVSystemProperty(keyStoreServiceMock);
    }

    @Test
    @SystemProperties(key = JAVAX_NET_SSL_KEYSTORE, value = "key-store-value")
    void shouldSaveKeyStoreValueToSystemProperties() {
        this.systemProperty.save();

        assertEquals("key-store-value", System.getProperty(JAVAX_NET_SSL_KEYSTORE));
    }

    @Test
    @SystemProperties(key = JAVAX_NET_SSL_KEYSTORE_PASSWORD, value = "key-store-password")
    void shouldSaveKeyStorePasswordToSystemProperties() {
        this.systemProperty.save();

        assertEquals("key-store-password", System.getProperty(JAVAX_NET_SSL_KEYSTORE_PASSWORD));
    }

    @Test
    @SystemProperties(key = JAVAX_NET_SSL_KEYSTORE_TYPE, value = "pkcs12")
    void shouldSaveKeyStoreTypeToSystemProperties() {
        assertEquals("pkcs12", System.getProperty(JAVAX_NET_SSL_KEYSTORE_TYPE));
    }
}
