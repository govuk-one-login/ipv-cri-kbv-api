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
    public KBVGateway create(ConfigurationService configurationService) {
        new KeyStoreLoader(configurationService).load();
        return getKbvGateway(configurationService);
    }

    private KBVGateway getKbvGateway(ConfigurationService configurationService) {
        var metricsService = new MetricsService(new EventProbe());
        return new KBVGateway(
                new StartAuthnAttemptRequestMapper(
                        configurationService, metricsService, new EnvironmentVariablesService()),
                new ResponseToQuestionMapper(metricsService),
                new KBVClientFactory(
                                new IdentityIQWebService(),
                                new HeaderHandlerResolver(getHeaderHandler(configurationService)),
                                configurationService)
                        .createClient());
    }

    private HeaderHandler getHeaderHandler(ConfigurationService configurationService) {
        return new HeaderHandler(
                new Base64TokenCacheLoader(
                        new SoapToken("GDS DI", true, new TokenService(), configurationService)));
    }
}
