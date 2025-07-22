package uk.gov.di.ipv.cri.kbv.healthcheck.handler.config;

import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.util.ClientProviderFactory;

public class ExperianSecrets {
    private final ConfigurationService configurationService;

    public ExperianSecrets() {
        this(new ClientProviderFactory());
    }

    public ExperianSecrets(ClientProviderFactory clientProviderFactory) {
        this.configurationService =
                new ConfigurationService(
                        clientProviderFactory.getSSMProvider(),
                        clientProviderFactory.getSecretsProvider());
    }

    public String getWaspUrl() {
        return configurationService.getParameterValue(Configuration.WASP_URL_PARAMETER);
    }

    public String getKeystorePassword() {
        return configurationService.getSecretValue(Configuration.KEYSTORE_PASSWORD);
    }

    public String getKeystoreSecret() {
        return configurationService.getSecretValue(Configuration.KEYSTORE_SECRET);
    }
}
