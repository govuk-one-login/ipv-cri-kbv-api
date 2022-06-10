package uk.gov.di.ipv.cri.kbv.api.security;

import com.experian.uk.wasp.TokenService;
import com.experian.uk.wasp.TokenServiceSoap;
import uk.gov.di.ipv.cri.kbv.api.service.AWSSecretsRetriever;

import javax.xml.ws.BindingProvider;

import static uk.gov.di.ipv.cri.kbv.api.config.ConfigurationConstants.EXPERIAN_IIQ_WASP_TOKEN_SERVICE;

public class SoapToken {
    private final TokenService tokenService;
    private final String application;
    private final boolean checkIp;
    private final AWSSecretsRetriever awsSecretsRetriever;

    public SoapToken(
            String application,
            boolean checkIp,
            TokenService tokenService,
            AWSSecretsRetriever awsSecretsRetriever) {
        this.application = application;
        this.checkIp = checkIp;
        this.tokenService = tokenService;
        this.awsSecretsRetriever = awsSecretsRetriever;
    }

    public String getToken() {
        TokenServiceSoap tokenServiceSoap = tokenService.getTokenServiceSoap();

        BindingProvider bindingProvider = (BindingProvider) tokenServiceSoap;
        bindingProvider
                .getRequestContext()
                .put(
                        BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                        awsSecretsRetriever.getValue(EXPERIAN_IIQ_WASP_TOKEN_SERVICE));

        return tokenServiceSoap.loginWithCertificate(application, checkIp);
    }
}
