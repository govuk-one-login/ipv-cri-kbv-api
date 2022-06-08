package uk.gov.di.ipv.cri.kbv.api.service;

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

@ExtendWith(MockitoExtension.class)
class KeyStoreServiceTest {
    private static final String BASE64_KEYSTORE_VALUE = "a2V5c3RvcmUtdmFsdWU=";
    private static final String BASE64_KEYSTORE_PASSWORD = "keystore-password";
    private static final String TEST_STACK_NAME = "stack-name";
    private static final String SECRET_KEY_FORMAT = "/%s/%s";

    @Mock private SecretsProvider secretsProvider;
    private KeyStoreService keyStoreService;

    @BeforeEach
    void setUp() {
        this.keyStoreService = new KeyStoreService(secretsProvider, TEST_STACK_NAME);
    }

    @Test
    void shouldReturnKeyStoreValueWhenSecretIsRetrieved() {
        when(secretsProvider.get(
                        String.format(SECRET_KEY_FORMAT, TEST_STACK_NAME, "experian/keystore")))
                .thenReturn(BASE64_KEYSTORE_VALUE);

        assertNotNull(keyStoreService.getKeyStorePath());
    }

    @Test
    void shouldReturnNullWhenKeyStoreValueIsNotSupplied() {
        assertNull(keyStoreService.getKeyStorePath());
    }

    @Test
    void shouldReturnKeyStorePasswordWhenSecretIsRetrieved() {
        when(secretsProvider.get(
                        String.format(
                                SECRET_KEY_FORMAT, TEST_STACK_NAME, "experian/keystore-password")))
                .thenReturn(BASE64_KEYSTORE_PASSWORD);

        assertEquals("keystore-password", keyStoreService.getPassword());
    }
}
