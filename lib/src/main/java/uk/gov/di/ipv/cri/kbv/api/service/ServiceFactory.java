package uk.gov.di.ipv.cri.kbv.api.service;

import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebService;
import com.experian.uk.wasp.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.lambda.powertools.parameters.secrets.SecretsProvider;
import software.amazon.lambda.powertools.parameters.ssm.SSMProvider;
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

import java.time.Clock;

public class ServiceFactory {
    private static final String APPLICATION = "GDS DI";
    private final ClientProviderFactory clientProviderFactory;
    private final ConfigurationService configurationService;
    private final SoapTokenRetriever soapTokenRetriever;

    private KBVGateway kbvGateway;

    private SoapToken soapToken;
    private TokenService tokenService;

    @ExcludeFromGeneratedCoverageReport
    public ServiceFactory(ClientProviderFactory clientProviderFactory) {
        this.clientProviderFactory = clientProviderFactory;
        this.configurationService =
                new ConfigurationService(getSsmProvider(), getSecretsProvider());
        this.soapTokenRetriever = new SoapTokenRetriever(getSoapToken());
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

    public KBVGateway getKbvGateway() {
        if (this.kbvGateway == null) {
            this.kbvGateway = getKbvGateway(new KeyStoreLoader(getConfigurationService()));
        }
        return this.kbvGateway;
    }

    KBVGateway getKbvGateway(KeyStoreLoader keyStoreLoader) {
        return new KBVGatewayFactory(keyStoreLoader, getConfigurationService()).create();
    }

    public KBVClientFactory getKbvClientFactory(String clientId) {
        HeaderHandler headerHandler = new HeaderHandler(soapTokenRetriever, clientId);
        HeaderHandlerResolver headerResolver = new HeaderHandlerResolver(headerHandler);

        return new KBVClientFactory(
                new IdentityIQWebService(), headerResolver, getConfigurationService());
    }

    private SoapToken getSoapToken() {
        if (tokenService == null) {
            tokenService = new TokenService();
        }
        if (soapToken == null) {
            soapToken =
                    new SoapToken(
                            APPLICATION,
                            true,
                            tokenService,
                            new ConfigurationService(getSsmProvider(), getSecretsProvider()),
                            new MetricsService(new EventProbe()));
        }
        return soapToken;
    }
}
