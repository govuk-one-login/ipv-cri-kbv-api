package uk.gov.di.ipv.cri.kbv.api.gateway;

import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.kbv.api.exception.KBVGatewayCreationException;
import uk.gov.di.ipv.cri.kbv.api.service.MetricsService;

public class KBVGatewayFactory {
    private final KeyStoreLoader keyStoreLoader;
    private final ConfigurationService configurationService;

    public KBVGatewayFactory(
            KeyStoreLoader keyStoreLoader, ConfigurationService configurationService) {
        this.keyStoreLoader = keyStoreLoader;
        this.configurationService = configurationService;
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
                new MetricsService(new EventProbe()));
    }
}
