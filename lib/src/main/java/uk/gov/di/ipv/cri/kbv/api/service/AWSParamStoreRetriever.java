package uk.gov.di.ipv.cri.kbv.api.service;

import software.amazon.lambda.powertools.parameters.ParamManager;
import software.amazon.lambda.powertools.parameters.SSMProvider;
import uk.gov.di.ipv.cri.kbv.api.config.ConfigurationConstants;

import java.util.Objects;

public class AWSParamStoreRetriever {
    private final SSMProvider ssmProvider;
    private final String parameterPrefix;

    public AWSParamStoreRetriever() {
        this(ParamManager.getSsmProvider(), System.getenv(ConfigurationConstants.AWS_STACK_NAME));
    }

    public AWSParamStoreRetriever(SSMProvider ssmProvider, String parameterPrefix) {
        this.ssmProvider = Objects.requireNonNull(ssmProvider, "ssmProvider must not be null");
        this.parameterPrefix = Objects.requireNonNull(parameterPrefix, "key must not be null");
    }

    public String getValue(String keySuffix) {
        return ssmProvider.get(String.format("/%s/%s", parameterPrefix, keySuffix));
    }
}
