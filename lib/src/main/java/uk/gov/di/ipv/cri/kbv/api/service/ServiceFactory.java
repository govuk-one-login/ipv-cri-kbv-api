package uk.gov.di.ipv.cri.kbv.api.service;

import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebService;
import com.experian.uk.wasp.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.lambda.powertools.parameters.SSMProvider;
import software.amazon.lambda.powertools.parameters.SecretsProvider;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.common.library.service.AuditEventFactory;
import uk.gov.di.ipv.cri.common.library.service.AuditService;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.service.SessionService;
import uk.gov.di.ipv.cri.common.library.util.ClientProviderFactory;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.kbv.api.gateway.KBVGateway;
import uk.gov.di.ipv.cri.kbv.api.gateway.KBVGatewayFactory;
import uk.gov.di.ipv.cri.kbv.api.gateway.KeyStoreLoader;
import uk.gov.di.ipv.cri.kbv.api.security.HeaderHandler;
import uk.gov.di.ipv.cri.kbv.api.security.HeaderHandlerResolver;
import uk.gov.di.ipv.cri.kbv.api.security.KBVClientFactory;
import uk.gov.di.ipv.cri.kbv.api.security.SoapToken;
import uk.gov.di.ipv.cri.kbv.api.security.SoapTokenRetriever;

public class ServiceFactory {
    private static final String APPLICATION = "GDS DI";
    private final ClientProviderFactory clientProviderFactory;
    private ConfigurationService configurationService;
    private KBVGateway kbvGateway;

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

    public KmsClient getKMSClient() {
        return clientProviderFactory.getKMSClient();
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
                new AuditEventFactory(getConfigurationService()));
    }

    public KBVGateway getKbvGateway() {
        if (this.kbvGateway == null) {
            this.kbvGateway =
                    getKbvGateway(
                            new KeyStoreLoader(getConfigurationService()), getKbvClientFactory());
        }
        return this.kbvGateway;
    }

    KBVGateway getKbvGateway(KeyStoreLoader keyStoreLoader, KBVClientFactory kbvClientFactory) {
        return new KBVGatewayFactory(keyStoreLoader, kbvClientFactory, getConfigurationService())
                .create();
    }

    private KBVClientFactory getKbvClientFactory() {
        TokenService tokenService = new TokenService();
        SoapToken soapToken =
                new SoapToken(
                        APPLICATION,
                        true,
                        tokenService,
                        configurationService,
                        new MetricsService(new EventProbe()));
        HeaderHandler headerHandler = new HeaderHandler(new SoapTokenRetriever(soapToken));
        HeaderHandlerResolver headerResolver = new HeaderHandlerResolver(headerHandler);

        return new KBVClientFactory(
                new IdentityIQWebService(), headerResolver, getConfigurationService());
    }
}
