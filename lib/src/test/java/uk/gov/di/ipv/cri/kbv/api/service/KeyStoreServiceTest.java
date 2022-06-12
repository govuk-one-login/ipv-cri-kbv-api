package uk.gov.di.ipv.cri.kbv.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.lambda.powertools.parameters.SecretsProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeyStoreServiceTest {
    ArgumentCaptor<String> capturedSecretKey = ArgumentCaptor.forClass(String.class);
    private static final String BASE64_KEYSTORE_VALUE = "a2V5c3RvcmUtdmFsdWU=";
    private static final String BASE64_KEYSTORE_PASSWORD = "keystore-password";
    private static final String TEST_STACK_NAME = "stack-name";
    @Mock private SecretsProvider secretsProvider;
    private KeyStoreService keyStoreService;

    @BeforeEach
    void setUp() {
        this.keyStoreService =
                new KeyStoreService(new AWSSecretsRetriever(secretsProvider, TEST_STACK_NAME));
    }

    @Test
    void shouldReturnKeyStoreValueWhenSecretIsRetrieved() {
        when(secretsProvider.get(capturedSecretKey.capture())).thenReturn(BASE64_KEYSTORE_VALUE);

        assertNotNull(keyStoreService.getKeyStorePath());
    }

    @Test
    void shouldReturnNullWhenKeyStoreValueIsNotSupplied() {
        assertNull(keyStoreService.getKeyStorePath());
    }

    @Test
    void shouldReturnKeyStorePasswordWhenSecretIsRetrieved() {
        when(secretsProvider.get(capturedSecretKey.capture())).thenReturn(BASE64_KEYSTORE_PASSWORD);

        assertEquals("keystore-password", keyStoreService.getPassword());
    }
}
