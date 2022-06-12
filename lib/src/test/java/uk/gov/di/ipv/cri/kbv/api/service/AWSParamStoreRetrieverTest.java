package uk.gov.di.ipv.cri.kbv.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.lambda.powertools.parameters.SSMProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AWSParamStoreRetrieverTest {
    private static final String STACK_NAME = "stack-name";
    @Mock private SSMProvider mockSSMProvider;
    private AWSParamStoreRetriever awsParamStoreRetriever;
    ArgumentCaptor<String> capturedParamKey = ArgumentCaptor.forClass(String.class);

    @BeforeEach
    void setUp() {
        this.awsParamStoreRetriever = new AWSParamStoreRetriever(mockSSMProvider, STACK_NAME);
    }

    @Test
    void shouldReturnParamWhenGivenAKey() {
        when(mockSSMProvider.get(capturedParamKey.capture())).thenReturn("some-expected-param");

        String expectedParam = awsParamStoreRetriever.getValue("keySuffix");
        String capturedKeyValue = capturedParamKey.getValue();

        assertEquals("/stack-name/keySuffix", capturedKeyValue);
        assertEquals("some-expected-param", expectedParam);
        verify(mockSSMProvider).get(capturedKeyValue);
    }

    @Test
    void shouldThrowErrorWhenCalledWithASSMProviderThatIsNull() {
        NullPointerException expectedException =
                assertThrows(
                        NullPointerException.class,
                        () -> new AWSParamStoreRetriever(null, "keySuffix"));

        assertEquals("ssmProvider must not be null", expectedException.getMessage());
    }

    @Test
    void shouldThrowErrorWhenCalledWithASecretsKeyPrefixThatIsNull() {
        NullPointerException expectedException =
                assertThrows(
                        NullPointerException.class,
                        () -> new AWSParamStoreRetriever(mockSSMProvider, null));

        assertEquals("key must not be null", expectedException.getMessage());
    }
}
