package uk.gov.di.ipv.cri.experian.kbv.api.security;

import com.experian.uk.wasp.TokenService;
import com.experian.uk.wasp.TokenServiceSoap;

public class SoapToken {
    private final TokenService tokenService;
    private final String application;
    private final boolean checkIp;

    public SoapToken(String application, boolean checkIp, TokenService tokenService) {
        this.application = application;
        this.checkIp = checkIp;
        this.tokenService = tokenService;
    }

    public String getToken() {
        TokenServiceSoap tokenServiceSoap = tokenService.getTokenServiceSoap();
        return tokenServiceSoap.loginWithCertificate(application, checkIp);
    }
}
