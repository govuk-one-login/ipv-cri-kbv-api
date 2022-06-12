package uk.gov.di.ipv.cri.kbv.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.lambda.powertools.parameters.SecretsProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AWSSecretsRetrieverTest {
    private static final String STACK_NAME = "stack-name";
    @Mock private SecretsProvider mockSecretsProvider;
    private AWSSecretsRetriever awsSecretsRetriever;
    ArgumentCaptor<String> capturedSecretKey = ArgumentCaptor.forClass(String.class);

    @BeforeEach
    void setUp() {
        this.awsSecretsRetriever = new AWSSecretsRetriever(mockSecretsProvider, STACK_NAME);
    }

    @Test
    void shouldReturnSecretWhenGivenAKey() {
        when(mockSecretsProvider.get(capturedSecretKey.capture()))
                .thenReturn("some-expected-secret");

        String expectedSecret = awsSecretsRetriever.getValue("keySuffix");
        String capturedKeyValue = capturedSecretKey.getValue();

        assertEquals("/stack-name/keySuffix", capturedKeyValue);
        assertEquals("some-expected-secret", expectedSecret);
        verify(mockSecretsProvider).get(capturedKeyValue);
    }

    @Test
    void shouldThrowErrorWhenCalledWithASecretsProviderThatIsNull() {
        NullPointerException expectedException =
                assertThrows(
                        NullPointerException.class,
                        () -> new AWSSecretsRetriever(null, "keySuffix"));

        assertEquals("SecretsProvider must not be null", expectedException.getMessage());
    }

    @Test
    void shouldThrowErrorWhenCalledWithASecretsKeyPrefixThatIsNull() {
        NullPointerException expectedException =
                assertThrows(
                        NullPointerException.class,
                        () -> new AWSSecretsRetriever(mockSecretsProvider, null));

        assertEquals("key must not be null", expectedException.getMessage());
    }
}
