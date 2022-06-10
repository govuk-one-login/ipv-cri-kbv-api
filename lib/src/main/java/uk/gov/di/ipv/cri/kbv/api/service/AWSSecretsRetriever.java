package uk.gov.di.ipv.cri.kbv.api.service;

import software.amazon.lambda.powertools.parameters.ParamManager;
import software.amazon.lambda.powertools.parameters.SecretsProvider;
import uk.gov.di.ipv.cri.kbv.api.config.ConfigurationConstants;

import java.util.Objects;

public class AWSSecretsRetriever {
    private final SecretsProvider secretsProvider;
    private final String secretKeyPrefix;

    public AWSSecretsRetriever() {
        this(
                ParamManager.getSecretsProvider(),
                System.getenv(ConfigurationConstants.AWS_STACK_NAME));
    }

    public AWSSecretsRetriever(SecretsProvider secretsProvider, String secretKeyPrefix) {
        this.secretsProvider =
                Objects.requireNonNull(secretsProvider, "secretsProvider must not be null");
        this.secretKeyPrefix = Objects.requireNonNull(secretKeyPrefix, "key must not be null");
    }

    public String getValue(String keySuffix) {
        return secretsProvider.get(String.format("/%s/%s", secretKeyPrefix, keySuffix));
    }
}
