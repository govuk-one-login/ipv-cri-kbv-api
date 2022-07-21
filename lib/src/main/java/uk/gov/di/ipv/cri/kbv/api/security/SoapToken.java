package uk.gov.di.ipv.cri.kbv.api.security;

import com.experian.uk.wasp.TokenService;
import com.experian.uk.wasp.TokenServiceSoap;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;

import javax.xml.ws.BindingProvider;

public class SoapToken {
    private final TokenService tokenService;
    private final String application;
    private final boolean checkIp;
    private final ConfigurationService configurationService;

    public SoapToken(
            String application,
            boolean checkIp,
            TokenService tokenService,
            ConfigurationService configurationService) {
        this.application = application;
        this.checkIp = checkIp;
        this.tokenService = tokenService;
        this.configurationService = configurationService;
    }

    public String getToken() {
        TokenServiceSoap tokenServiceSoap = tokenService.getTokenServiceSoap();

        BindingProvider bindingProvider = (BindingProvider) tokenServiceSoap;
        bindingProvider
                .getRequestContext()
                .put(
                        BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                        configurationService.getSecretValue("experian/iiq-wasp-service"));

        return tokenServiceSoap.loginWithCertificate(application, checkIp);
    }
}
