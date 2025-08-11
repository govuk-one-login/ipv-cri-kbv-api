package uk.gov.di.ipv.cri.kbv.api.security;

import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebService;
import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebServiceSoap;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.soap.SOAPFaultException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.kbv.api.exception.InvalidSoapTokenException;

public class KBVClientFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(KBVClientFactory.class);
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

    public IdentityIQWebServiceSoap createClient(String clientId) {
        try {
            identityIQWebService.setHandlerResolver(headerHandlerResolver);

            IdentityIQWebServiceSoap identityIQWebServiceSoap =
                    identityIQWebService.getIdentityIQWebServiceSoap();

            LOGGER.info("Fetching SSM parameter experian/iiq-webservice/{}", clientId);

            String value =
                    configurationService.getParameterValue(
                            "experian/iiq-webservice/%s".formatted(clientId));

            LOGGER.info("Fetched value from SSM: {}", value);

            ((BindingProvider) identityIQWebServiceSoap)
                    .getRequestContext()
                    .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, value);

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
