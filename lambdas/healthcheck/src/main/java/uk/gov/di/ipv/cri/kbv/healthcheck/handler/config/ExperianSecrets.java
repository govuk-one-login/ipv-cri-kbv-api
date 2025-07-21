package uk.gov.di.ipv.cri.kbv.healthcheck.handler.config;

import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.util.ClientProviderFactory;

import java.util.Optional;

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
        return Optional.ofNullable(System.getenv("WaspURL"))
                .orElse(
                        "https://identityiq.xml.uk.experian.com/IdentityIQWebService/IdentityIQWebService.asmx");
    }

    public String getKeystorePassword() {
        return configurationService.getSecretValue(Configuration.KEYSTORE_PASSWORD);
    }

    public String getKeystoreSecret() {
        return configurationService.getSecretValue(Configuration.KEYSTORE_SECRET);
    }
}
