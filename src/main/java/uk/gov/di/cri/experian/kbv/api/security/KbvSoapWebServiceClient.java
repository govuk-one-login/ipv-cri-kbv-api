package uk.gov.di.cri.experian.kbv.api.security;

import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebService;
import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebServiceSoap;

public class KbvSoapWebServiceClient {
    private final IdentityIQWebService identityIQWebService;

    public KbvSoapWebServiceClient(IdentityIQWebService identityIQWebService) {
        this.identityIQWebService = identityIQWebService;
    }

    public IdentityIQWebServiceSoap getIdentityIQWebServiceSoapEndpoint() {
        return identityIQWebService.getIdentityIQWebServiceSoap();
    }
}
