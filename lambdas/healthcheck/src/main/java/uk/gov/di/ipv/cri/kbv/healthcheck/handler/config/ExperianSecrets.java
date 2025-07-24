package uk.gov.di.ipv.cri.kbv.healthcheck.handler.config;

import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.util.ClientProviderFactory;

public class ExperianSecrets {
    private final ConfigurationService configurationService;

    public ExperianSecrets() {
        ClientProviderFactory clientProviderFactory = new ClientProviderFactory();

        this.configurationService =
                new ConfigurationService(
                        clientProviderFactory.getSSMProvider(),
                        clientProviderFactory.getSecretsProvider());
    }

    public ExperianSecrets(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public String getWaspUrl() {
        return configurationService.getParameterValue(
                "experian/iiq-wasp-service/%s".formatted(Configuration.CLIENT_ID));
    }

    public String getKeystorePassword() {
        return configurationService.getSecretValue(Configuration.KEYSTORE_PASSWORD);
    }

    public String getKeystoreSecret() {
        return configurationService.getSecretValue(Configuration.KEYSTORE_SECRET);
    }
}
