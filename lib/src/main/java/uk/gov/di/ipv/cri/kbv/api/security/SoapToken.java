package uk.gov.di.ipv.cri.kbv.api.security;

import com.experian.uk.wasp.TokenService;
import com.experian.uk.wasp.TokenServiceSoap;

import javax.xml.ws.BindingProvider;

public class SoapToken {
    private final TokenService tokenService;
    private final String application;
    private final boolean checkIp;
    private final String endpointUrl;

    public SoapToken(
            String application, boolean checkIp, TokenService tokenService, String endpointUrl) {
        this.application = application;
        this.checkIp = checkIp;
        this.tokenService = tokenService;
        this.endpointUrl = endpointUrl;
    }

    public String getToken() {
        TokenServiceSoap tokenServiceSoap = tokenService.getTokenServiceSoap();

        BindingProvider bindingProvider = (BindingProvider) tokenServiceSoap;
        bindingProvider
                .getRequestContext()
                .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);

        return tokenServiceSoap.loginWithCertificate(application, checkIp);
    }
}
