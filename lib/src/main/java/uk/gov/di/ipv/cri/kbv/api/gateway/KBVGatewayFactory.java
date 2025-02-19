package uk.gov.di.ipv.cri.kbv.api.gateway;

import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.kbv.api.exception.KBVGatewayCreationException;
import uk.gov.di.ipv.cri.kbv.api.security.KBVClientFactory;
import uk.gov.di.ipv.cri.kbv.api.security.SoapTokenRetriever;
import uk.gov.di.ipv.cri.kbv.api.service.MetricsService;

import java.util.function.Supplier;

public class KBVGatewayFactory {
    private final KeyStoreLoader keyStoreLoader;
    private final ConfigurationService configurationService;
    private final Supplier<KBVClientFactory> kbvClientFactorySupplier;
    private final Supplier<SoapTokenRetriever> soapTokenRetrieverSupplier;

    public KBVGatewayFactory(
            KeyStoreLoader keyStoreLoader,
            Supplier<KBVClientFactory> kbvClientFactorySupplier,
            ConfigurationService configurationService,
            Supplier<SoapTokenRetriever> soapTokenRetrieverSupplier) {
        this.keyStoreLoader = keyStoreLoader;
        this.kbvClientFactorySupplier = kbvClientFactorySupplier;
        this.configurationService = configurationService;
        this.soapTokenRetrieverSupplier = soapTokenRetrieverSupplier;
    }

    public KBVGateway create() {
        try {
            keyStoreLoader.load();
            return getKbvGateway(configurationService);
        } catch (Exception e) {
            throw new KBVGatewayCreationException("Failed to create KBVGateway: " + e.getMessage());
        }
    }

    private KBVGateway getKbvGateway(ConfigurationService configurationService) {
        return new KBVGateway(
                new StartAuthnAttemptRequestMapper(configurationService),
                new ResponseToQuestionMapper(),
                new QuestionsResponseMapper(),
                new IdentityIQWrapper(kbvClientFactorySupplier, soapTokenRetrieverSupplier),
                new MetricsService(new EventProbe()));
    }
}
