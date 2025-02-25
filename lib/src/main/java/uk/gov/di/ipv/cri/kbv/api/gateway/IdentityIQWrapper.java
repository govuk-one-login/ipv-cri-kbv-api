package uk.gov.di.ipv.cri.kbv.api.gateway;

import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebServiceSoap;
import com.experian.uk.schema.experian.identityiq.services.webservice.RTQRequest;
import com.experian.uk.schema.experian.identityiq.services.webservice.RTQResponse2;
import com.experian.uk.schema.experian.identityiq.services.webservice.SAARequest;
import com.experian.uk.schema.experian.identityiq.services.webservice.SAAResponse2;
import uk.gov.di.ipv.cri.kbv.api.security.KBVClientFactory;

public class IdentityIQWrapper {
    private final IdentityIQWebServiceSoap identityIQWebServiceSoap;

    public IdentityIQWrapper(KBVClientFactory kbvClientFactory) {

        this.identityIQWebServiceSoap = kbvClientFactory.createClient();
    }

    public SAAResponse2 saa(SAARequest sAARequest) {
        return identityIQWebServiceSoap.saa(sAARequest);
    }

    public RTQResponse2 rtq(RTQRequest rTQRequest) {
        return identityIQWebServiceSoap.rtq(rTQRequest);
    }
}
