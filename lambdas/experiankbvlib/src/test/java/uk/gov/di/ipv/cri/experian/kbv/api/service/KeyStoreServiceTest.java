package uk.gov.di.ipv.cri.experian.kbv.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.lambda.powertools.parameters.SecretsProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.experian.kbv.api.service.KeyStoreService.KBV_API_KEYSTORE;
import static uk.gov.di.ipv.cri.experian.kbv.api.service.KeyStoreService.KBV_API_KEYSTORE_PASSWORD;

@ExtendWith(MockitoExtension.class)
class KeyStoreServiceTest {
    public static final String BASE64_KEYSTORE_VALUE = "a2V5c3RvcmUtdmFsdWU=";
    public static final String BASE64_KEYSTORE_PASSWORD = "keystore-password";
    private KeyStoreService keyStoreService;
    @Mock SecretsProvider secretsProvider;

    @BeforeEach
    void setUp() {
        this.keyStoreService = new KeyStoreService(secretsProvider);
    }

    @Test
    void shouldReturnKeyStoreValueWhenSecretIsRetrieved() {
        when(secretsProvider.get(KBV_API_KEYSTORE)).thenReturn(BASE64_KEYSTORE_VALUE);

        assertNotNull(keyStoreService.getKeyStorePath());
    }

    @Test
    void shouldReturnNullWhenKeyStoreValueIsNotSupplied() {
        keyStoreService.getKeyStorePath();

        assertNull(keyStoreService.getKeyStorePath());
    }

    @Test
    void shouldReturnKeyStorePasswordWhenSecretIsRetrieved() {
        when(secretsProvider.get(KBV_API_KEYSTORE_PASSWORD)).thenReturn(BASE64_KEYSTORE_PASSWORD);

        assertEquals("keystore-password", keyStoreService.getPassword());
    }
}
