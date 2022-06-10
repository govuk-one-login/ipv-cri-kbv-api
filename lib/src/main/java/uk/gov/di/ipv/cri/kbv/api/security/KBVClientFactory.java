package uk.gov.di.ipv.cri.kbv.api.security;

import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebService;
import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebServiceSoap;
import uk.gov.di.ipv.cri.kbv.api.service.AWSSecretsRetriever;

import javax.xml.ws.BindingProvider;

import static uk.gov.di.ipv.cri.kbv.api.config.ConfigurationConstants.EXPERIAN_IIQ_WEBSERVICE;

public class KBVClientFactory {
    private final HeaderHandlerResolver headerHandlerResolver;
    private final IdentityIQWebService identityIQWebService;
    private final AWSSecretsRetriever awsSecretsRetriever;

    public KBVClientFactory(
            IdentityIQWebService identityIQWebService,
            HeaderHandlerResolver headerHandlerResolver,
            AWSSecretsRetriever awsSecretsRetriever) {
        this.identityIQWebService = identityIQWebService;
        this.headerHandlerResolver = headerHandlerResolver;
        this.awsSecretsRetriever = awsSecretsRetriever;
    }

    public IdentityIQWebServiceSoap createClient() {
        identityIQWebService.setHandlerResolver(headerHandlerResolver);

        IdentityIQWebServiceSoap identityIQWebServiceSoap =
                identityIQWebService.getIdentityIQWebServiceSoap();

        var bindingProvider = (BindingProvider) identityIQWebServiceSoap;
        bindingProvider
                .getRequestContext()
                .put(
                        BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                        awsSecretsRetriever.getValue(EXPERIAN_IIQ_WEBSERVICE));

        return identityIQWebServiceSoap;
    }
}
