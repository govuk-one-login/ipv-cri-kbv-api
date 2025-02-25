package uk.gov.di.ipv.cri.kbv.api.gateway;

import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebServiceSoap;
import com.experian.uk.schema.experian.identityiq.services.webservice.RTQRequest;
import com.experian.uk.schema.experian.identityiq.services.webservice.RTQResponse2;
import com.experian.uk.schema.experian.identityiq.services.webservice.SAARequest;
import com.experian.uk.schema.experian.identityiq.services.webservice.SAAResponse2;
import uk.gov.di.ipv.cri.kbv.api.exception.InvalidSoapTokenException;
import uk.gov.di.ipv.cri.kbv.api.security.KBVClientFactory;
import uk.gov.di.ipv.cri.kbv.api.security.SoapTokenRetriever;

public class IdentityIQWrapper {
    private final KBVClientFactory kbvClientFactory;

    private String currentToken;
    private IdentityIQWebServiceSoap identityIQWebServiceSoap;
    private SoapTokenRetriever soapTokenRetriever;

    public IdentityIQWrapper(
            KBVClientFactory kbvClientFactory, SoapTokenRetriever soapTokenRetriever) {

        this.kbvClientFactory = kbvClientFactory;
        this.soapTokenRetriever = soapTokenRetriever;

        this.refreshSoapToken();
    }

    public SAAResponse2 saa(SAARequest sAARequest) {
        this.refreshSoapToken();

        return identityIQWebServiceSoap.saa(sAARequest);
    }

    public RTQResponse2 rtq(RTQRequest rTQRequest) {
        refreshSoapToken();

        return identityIQWebServiceSoap.rtq(rTQRequest);
    }

    private void refreshSoapToken() {
        try {
            String newSoapToken = soapTokenRetriever.getSoapToken();
            if (this.currentToken == null || !this.currentToken.equalsIgnoreCase(newSoapToken)) {
                this.currentToken = newSoapToken;
                this.identityIQWebServiceSoap = kbvClientFactory.createClient();
            }
        } catch (Exception e) {
            throw new InvalidSoapTokenException(
                    "SOAP Token could not be refreshed: " + e.getMessage());
        }
    }
}
