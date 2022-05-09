package uk.gov.di.ipv.cri.experian.kbv.api.security;

import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebService;
import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebServiceSoap;

public class KBVClientFactory {
    private final HeaderHandlerResolver headerHandlerResolver;
    private final IdentityIQWebService identityIQWebService;

    public KBVClientFactory(
            IdentityIQWebService identityIQWebService,
            HeaderHandlerResolver headerHandlerResolver) {
        this.identityIQWebService = identityIQWebService;
        this.headerHandlerResolver = headerHandlerResolver;
    }

    public IdentityIQWebServiceSoap createClient() {
        identityIQWebService.setHandlerResolver(headerHandlerResolver);
        return identityIQWebService.getIdentityIQWebServiceSoap();
    }
}
