package uk.gov.di.ipv.cri.kbv.api.security;

import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebService;
import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebServiceSoap;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;

import javax.xml.ws.BindingProvider;

public class KBVClientFactory {
    private final HeaderHandlerResolver headerHandlerResolver;
    private final IdentityIQWebService identityIQWebService;
    private final ConfigurationService configurationService;

    public KBVClientFactory(
            IdentityIQWebService identityIQWebService,
            HeaderHandlerResolver headerHandlerResolver,
            ConfigurationService configurationService) {
        this.identityIQWebService = identityIQWebService;
        this.headerHandlerResolver = headerHandlerResolver;
        this.configurationService = configurationService;
    }

    public IdentityIQWebServiceSoap createClient() {
        identityIQWebService.setHandlerResolver(headerHandlerResolver);

        IdentityIQWebServiceSoap identityIQWebServiceSoap =
                identityIQWebService.getIdentityIQWebServiceSoap();

        ((BindingProvider) identityIQWebServiceSoap)
                .getRequestContext()
                .put(
                        BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                        configurationService.getSecretValue("experian/iiq-webservice"));

        return identityIQWebServiceSoap;
    }
}
