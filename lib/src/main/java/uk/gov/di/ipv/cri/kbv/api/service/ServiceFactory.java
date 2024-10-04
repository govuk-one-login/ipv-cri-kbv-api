package uk.gov.di.ipv.cri.kbv.api.service;

import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebService;
import com.experian.uk.wasp.TokenService;
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
import uk.gov.di.ipv.cri.kbv.api.gateway.KBVGateway;
import uk.gov.di.ipv.cri.kbv.api.gateway.KBVGatewayFactory;
import uk.gov.di.ipv.cri.kbv.api.gateway.KeyStoreLoader;
import uk.gov.di.ipv.cri.kbv.api.security.HeaderHandler;
import uk.gov.di.ipv.cri.kbv.api.security.HeaderHandlerResolver;
import uk.gov.di.ipv.cri.kbv.api.security.KBVClientFactory;
import uk.gov.di.ipv.cri.kbv.api.security.SoapToken;

import java.time.Clock;

public class ServiceFactory {
    private static final String APPLICATION = "GDS DI";
    private final ClientProviderFactory clientProviderFactory;
    private final KeyStoreLoader keyStoreLoader;
    private KBVClientFactory kbvClientFactory;
    private ConfigurationService configurationService;
    private KBVGateway kbvGateway;

    @ExcludeFromGeneratedCoverageReport
    public ServiceFactory(
            ConfigurationService configurationService,
            ClientProviderFactory clientProviderFactory,
            KBVClientFactory kbvClientFactory,
            KeyStoreLoader keyStoreLoader) {
        this.configurationService = configurationService;
        this.clientProviderFactory = clientProviderFactory;
        this.kbvClientFactory = kbvClientFactory;
        this.keyStoreLoader = keyStoreLoader;
    }

    @ExcludeFromGeneratedCoverageReport
    public ServiceFactory() {
        TokenService tokenService = new TokenService();
        this.clientProviderFactory = new ClientProviderFactory();
        this.configurationService =
                new ConfigurationService(getSsmProvider(), getSecretsProvider());

        this.keyStoreLoader = new KeyStoreLoader(getConfigurationService());

        SoapToken soapToken =
                new SoapToken(APPLICATION, true, tokenService, getConfigurationService());
        HeaderHandler headerHandler = new HeaderHandler(soapToken);

        setKBVClientFactory(new IdentityIQWebService(), new HeaderHandlerResolver(headerHandler));
        setKbvGateway(keyStoreLoader);
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
        return this.kbvGateway;
    }

    void setKbvGateway(KeyStoreLoader keyStoreLoader) {
        this.kbvGateway =
                new KBVGatewayFactory(
                                keyStoreLoader, getKBVClientFactory(), getConfigurationService())
                        .create();
    }

    void setKBVClientFactory(
            IdentityIQWebService identityIQWebService,
            HeaderHandlerResolver headerHandlerResolver) {
        kbvClientFactory =
                new KBVClientFactory(
                        identityIQWebService, headerHandlerResolver, getConfigurationService());
    }

    private KBVClientFactory getKBVClientFactory() {
        return this.kbvClientFactory;
    }
}
