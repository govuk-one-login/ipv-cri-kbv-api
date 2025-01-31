package uk.gov.di.ipv.cri.kbv.api.security;

import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebService;
import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebServiceSoap;
import io.opentelemetry.api.trace.Span;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.kbv.api.exception.InvalidSoapTokenException;
import uk.gov.di.ipv.cri.kbv.api.util.OpenTelemetryUtil;

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
        Span span =
                OpenTelemetryUtil.createSpan(
                        this.getClass(),
                        "createClient",
                        "IdentityIQWebServiceSoap createClient",
                        "_UNKNOWN",
                        "http://schema.uk.experian.com/Experian/IdentityIQ/Services/WebService");

        try {
            identityIQWebService.setHandlerResolver(headerHandlerResolver);

            IdentityIQWebServiceSoap identityIQWebServiceSoap =
                    identityIQWebService.getIdentityIQWebServiceSoap();

            ((BindingProvider) identityIQWebServiceSoap)
                    .getRequestContext()
                    .put(
                            BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                            configurationService.getSecretValue("experian/iiq-webservice"));

            OpenTelemetryUtil.endSpan(span);

            return identityIQWebServiceSoap;

        } catch (SOAPFaultException e) {
            OpenTelemetryUtil.endSpanWithError(span);
            throw new InvalidSoapTokenException("SOAP Fault occurred: " + e.getMessage());
        } catch (WebServiceException e) {
            OpenTelemetryUtil.endSpanWithError(span);
            throw new InvalidSoapTokenException("Web Service error occurred: " + e.getMessage());
        } catch (Exception e) {
            OpenTelemetryUtil.endSpanWithError(span);
            throw new InvalidSoapTokenException("Unexpected error occurred: " + e.getMessage());
        }
    }
}
