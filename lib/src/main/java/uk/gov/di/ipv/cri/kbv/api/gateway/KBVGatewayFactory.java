package uk.gov.di.ipv.cri.kbv.api.gateway;

import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebService;
import com.experian.uk.wasp.TokenService;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.kbv.api.security.Base64TokenCacheLoader;
import uk.gov.di.ipv.cri.kbv.api.security.HeaderHandler;
import uk.gov.di.ipv.cri.kbv.api.security.HeaderHandlerResolver;
import uk.gov.di.ipv.cri.kbv.api.security.KBVClientFactory;
import uk.gov.di.ipv.cri.kbv.api.security.SoapToken;
import uk.gov.di.ipv.cri.kbv.api.service.EnvironmentVariablesService;
import uk.gov.di.ipv.cri.kbv.api.service.MetricsService;

public class KBVGatewayFactory {
    private final KBVGateway kbvGateway;

    public KBVGatewayFactory(ConfigurationService configurationService) {
        HeaderHandler headerHandler =
                new HeaderHandler(
                        new Base64TokenCacheLoader(
                                new SoapToken(
                                        "GDS DI", true, new TokenService(), configurationService)));

        new KeyStoreLoader(configurationService).load();

        MetricsService metricsService = new MetricsService(new EventProbe());
        this.kbvGateway =
                new KBVGateway(
                        new StartAuthnAttemptRequestMapper(
                                configurationService,
                                metricsService,
                                new EnvironmentVariablesService()),
                        new ResponseToQuestionMapper(metricsService),
                        new KBVClientFactory(
                                        new IdentityIQWebService(),
                                        new HeaderHandlerResolver(headerHandler),
                                        configurationService)
                                .createClient());
    }

    public KBVGateway getKbvGateway() {
        return this.kbvGateway;
    }
}
