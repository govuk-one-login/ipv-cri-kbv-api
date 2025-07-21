package uk.gov.di.ipv.cri.kbv.healthcheck.handler.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.lambda.powertools.parameters.SSMProvider;
import software.amazon.lambda.powertools.parameters.SecretsProvider;
import uk.gov.di.ipv.cri.common.library.util.ClientProviderFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExperianSecretsTest {

    private ExperianSecrets experianSecrets;

    @BeforeEach
    void setUp() {
        ClientProviderFactory clientProviderFactory = mock(ClientProviderFactory.class);
        SecretsProvider mockSecretsProvider = mock(SecretsProvider.class);
        SSMProvider mockSsmProvider = mock(SSMProvider.class);

        when(clientProviderFactory.getSecretsProvider()).thenReturn(mockSecretsProvider);
        when(clientProviderFactory.getSSMProvider()).thenReturn(mockSsmProvider);
        lenient().when(mockSecretsProvider.get(any())).thenReturn("dummy");

        experianSecrets = new ExperianSecrets(clientProviderFactory);
    }

    @Test
    void shouldReturnWaspUrl() {
        assertEquals(
                "https://identityiq.xml.uk.experian.com/IdentityIQWebService/IdentityIQWebService.asmx",
                experianSecrets.getWaspUrl());
    }

    @Test
    void shouldReturnKeyStoreSecret() {
        assertEquals("dummy", experianSecrets.getKeystoreSecret());
    }

    @Test
    void shouldReturnKeyStorePassword() {
        assertEquals("dummy", experianSecrets.getKeystorePassword());
    }
}
