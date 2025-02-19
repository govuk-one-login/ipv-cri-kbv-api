package uk.gov.di.ipv.cri.kbv.api.gateway;

import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebServiceSoap;
import com.experian.uk.schema.experian.identityiq.services.webservice.RTQRequest;
import com.experian.uk.schema.experian.identityiq.services.webservice.RTQResponse2;
import com.experian.uk.schema.experian.identityiq.services.webservice.SAARequest;
import com.experian.uk.schema.experian.identityiq.services.webservice.SAAResponse2;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.gov.di.ipv.cri.kbv.api.exception.InvalidSoapTokenException;
import uk.gov.di.ipv.cri.kbv.api.security.KBVClientFactory;
import uk.gov.di.ipv.cri.kbv.api.security.SoapTokenRetriever;
import uk.gov.di.ipv.cri.kbv.api.util.SoapTokenUtils;

import java.util.function.Supplier;

public class IdentityIQWrapper {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Supplier<KBVClientFactory> kbvClientFactorySupplier;
    private final Supplier<SoapTokenRetriever> soapTokenRetrieverSupplier;

    private String soapToken;
    private IdentityIQWebServiceSoap identityIQWebServiceSoap;
    private SoapTokenRetriever soapTokenRetriever;

    public IdentityIQWrapper(
            Supplier<KBVClientFactory> kbvClientFactorySupplier,
            Supplier<SoapTokenRetriever> soapTokenRetriever) {

        this.kbvClientFactorySupplier = kbvClientFactorySupplier;
        this.soapTokenRetrieverSupplier = soapTokenRetriever;

        refreshClient();
    }

    public SAAResponse2 saa(SAARequest sAARequest) {
        refreshSoapToken();
        return identityIQWebServiceSoap.saa(sAARequest);
    }

    public RTQResponse2 rtq(RTQRequest rTQRequest) {
        refreshSoapToken();
        return identityIQWebServiceSoap.rtq(rTQRequest);
    }

    private void refreshSoapToken() {
        try {
            if (!SoapTokenUtils.isTokenValidWithinThreshold(soapToken)) {
                LOGGER.info("SOAP Token is valid - not expired.");
                return;
            }

            LOGGER.info("SOAP Token has expired: {}", SoapTokenUtils.getTokenExpiry(soapToken));

            refreshClient();

            LOGGER.info(
                    "New SOAP Token retrieved with expiry: {}",
                    SoapTokenUtils.getTokenExpiry(soapTokenRetriever.getSoapToken()));
        } catch (JsonProcessingException e) {
            throw new InvalidSoapTokenException(
                    "SOAP Token could not be refreshed: " + e.getMessage());
        }
    }

    private void refreshClient() {
        this.soapTokenRetriever = soapTokenRetrieverSupplier.get();
        this.soapToken = soapTokenRetriever.getSoapToken();
        this.identityIQWebServiceSoap = kbvClientFactorySupplier.get().createClient();
    }
}
