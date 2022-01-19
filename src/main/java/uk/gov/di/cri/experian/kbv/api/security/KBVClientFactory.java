package uk.gov.di.cri.experian.kbv.api.security;

import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebService;
import com.experian.uk.wasp.TokenService;
import com.experian.uk.wasp.TokenServiceSoap;

public class KBVClientFactory {

    public static KbvSoapWebServiceClient createClient(String application, boolean checkIp) {
        TokenService tokenService = new TokenService();
        TokenServiceSoap tokenServiceSoap = tokenService.getTokenServiceSoap();
        SoapTokenGenerator soapTokenGenerator =
                new SoapTokenGenerator(tokenServiceSoap.loginWithCertificate(application, checkIp));
        IdentityIQWebService identityIQWebService = new IdentityIQWebService();
        HeaderHandler headerHandler = new HeaderHandler(soapTokenGenerator.getToken());
        HeaderHandlerResolver headerHandlerResolver = new HeaderHandlerResolver(headerHandler);
        identityIQWebService.setHandlerResolver(headerHandlerResolver);
        return new KbvSoapWebServiceClient(identityIQWebService);
    }
}
