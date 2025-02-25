package uk.gov.di.ipv.cri.kbv.api.security;

import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebService;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;

import java.util.function.Supplier;

public class KBVClientFactorySupplier {
    private final ConfigurationService configurationService;

    public KBVClientFactorySupplier(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public Supplier<KBVClientFactory> getKbvClientFactory(SoapTokenRetriever soapTokenRetriever) {
        return () ->
                new KBVClientFactory(
                        new IdentityIQWebService(),
                        new HeaderHandlerResolver(new HeaderHandler(soapTokenRetriever)),
                        this.configurationService);
    }
}
