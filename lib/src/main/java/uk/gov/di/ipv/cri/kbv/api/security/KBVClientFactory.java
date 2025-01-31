package uk.gov.di.ipv.cri.kbv.api.security;

import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebService;
import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebServiceSoap;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.kbv.api.exception.InvalidSoapTokenException;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPFaultException;

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
        try {
            identityIQWebService.setHandlerResolver(headerHandlerResolver);

            IdentityIQWebServiceSoap identityIQWebServiceSoap =
                    identityIQWebService.getIdentityIQWebServiceSoap();

            ((BindingProvider) identityIQWebServiceSoap)
                    .getRequestContext()
                    .put(
                            BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                            configurationService.getSecretValue("experian/iiq-webservice"));

            return identityIQWebServiceSoap;

        } catch (SOAPFaultException e) {
            throw new InvalidSoapTokenException("SOAP Fault occurred: " + e.getMessage());
        } catch (WebServiceException e) {
            throw new InvalidSoapTokenException("Web Service error occurred: " + e.getMessage());
        } catch (Exception e) {
            throw new InvalidSoapTokenException("Unexpected error occurred: " + e.getMessage());
        }
    }
}
