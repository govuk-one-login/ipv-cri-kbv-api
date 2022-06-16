package uk.gov.di.ipv.cri.kbv.api.security;

import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebService;
import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebServiceSoap;

import javax.xml.ws.BindingProvider;

public class KBVClientFactory {
    private final HeaderHandlerResolver headerHandlerResolver;
    private final IdentityIQWebService identityIQWebService;
    private final String endpointUrl;

    public KBVClientFactory(
            IdentityIQWebService identityIQWebService,
            HeaderHandlerResolver headerHandlerResolver,
            String endpointUrl) {
        this.identityIQWebService = identityIQWebService;
        this.headerHandlerResolver = headerHandlerResolver;
        this.endpointUrl = endpointUrl;
    }

    public IdentityIQWebServiceSoap createClient() {
        identityIQWebService.setHandlerResolver(headerHandlerResolver);

        IdentityIQWebServiceSoap identityIQWebServiceSoap =
                identityIQWebService.getIdentityIQWebServiceSoap();

        ((BindingProvider) identityIQWebServiceSoap)
                .getRequestContext()
                .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);

        return identityIQWebServiceSoap;
    }
}
