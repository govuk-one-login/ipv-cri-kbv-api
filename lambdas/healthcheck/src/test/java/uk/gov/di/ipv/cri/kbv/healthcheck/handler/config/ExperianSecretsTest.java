package uk.gov.di.ipv.cri.kbv.healthcheck.handler.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExperianSecretsTest {
    @Mock private ConfigurationService configurationService;
    @InjectMocks private ExperianSecrets experianSecrets;

    @Test
    void shouldReturnWaspUrl() {
        when(configurationService.getParameterValue(any())).thenReturn("dummy");

        assertEquals("dummy", experianSecrets.getWaspUrl());
    }

    @Test
    void shouldReturnKeyStoreSecret() {
        when(configurationService.getSecretValue(any())).thenReturn("dummy");

        assertEquals("dummy", experianSecrets.getKeystoreSecret());
    }

    @Test
    void shouldReturnKeyStorePassword() {
        when(configurationService.getSecretValue(any())).thenReturn("dummy");

        assertEquals("dummy", experianSecrets.getKeystorePassword());
    }
}
