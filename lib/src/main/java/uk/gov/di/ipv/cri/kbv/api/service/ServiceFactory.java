package uk.gov.di.ipv.cri.kbv.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.lambda.powertools.parameters.SSMProvider;
import software.amazon.lambda.powertools.parameters.SecretsProvider;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.common.library.service.AuditEventFactory;
import uk.gov.di.ipv.cri.common.library.service.AuditService;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.service.SessionService;
import uk.gov.di.ipv.cri.common.library.util.ClientProviderFactory;

import java.time.Clock;

public class ServiceFactory {
    private final ClientProviderFactory clientProviderFactory;
    private ConfigurationService configurationService;

    @ExcludeFromGeneratedCoverageReport
    public ServiceFactory(ClientProviderFactory clientProviderFactory) {
        this.clientProviderFactory = clientProviderFactory;
    }

    @ExcludeFromGeneratedCoverageReport
    public ServiceFactory() {
        this(new ClientProviderFactory());
    }

    public SSMProvider getSsmProvider() {
        return clientProviderFactory.getSSMProvider();
    }

    public DynamoDbEnhancedClient getDynamoDbEnhancedClient() {
        return clientProviderFactory.getDynamoDbEnhancedClient();
    }

    public SecretsProvider getSecretsProvider() {
        return clientProviderFactory.getSecretsProvider();
    }

    public SqsClient getSqsClient() {
        return clientProviderFactory.getSqsClient();
    }

    public ConfigurationService getConfigurationService() {
        if (configurationService == null) {
            configurationService = new ConfigurationService(getSsmProvider(), getSecretsProvider());
        }
        return configurationService;
    }

    public SessionService getSessionService() {
        return new SessionService(getConfigurationService(), getDynamoDbEnhancedClient());
    }

    public AuditService getAuditService() {
        return new AuditService(
                getSqsClient(),
                getConfigurationService(),
                new ObjectMapper(),
                new AuditEventFactory(getConfigurationService(), Clock.systemDefaultZone()));
    }
}
