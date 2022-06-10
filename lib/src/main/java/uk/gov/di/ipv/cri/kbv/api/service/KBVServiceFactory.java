package uk.gov.di.ipv.cri.kbv.api.service;

import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebService;
import com.experian.uk.wasp.TokenService;
import uk.gov.di.ipv.cri.kbv.api.gateway.KBVGateway;
import uk.gov.di.ipv.cri.kbv.api.gateway.ResponseToQuestionMapper;
import uk.gov.di.ipv.cri.kbv.api.gateway.StartAuthnAttemptRequestMapper;
import uk.gov.di.ipv.cri.kbv.api.security.Base64TokenCacheLoader;
import uk.gov.di.ipv.cri.kbv.api.security.HeaderHandler;
import uk.gov.di.ipv.cri.kbv.api.security.HeaderHandlerResolver;
import uk.gov.di.ipv.cri.kbv.api.security.KBVClientFactory;
import uk.gov.di.ipv.cri.kbv.api.security.SoapToken;

public class KBVServiceFactory {
    public KBVGateway create(AWSSecretsRetriever awsSecretsRetriever) {
        HeaderHandler headerHandler =
                new HeaderHandler(
                        new Base64TokenCacheLoader(
                                new SoapToken(
                                        "GDS DI", true, new TokenService(), awsSecretsRetriever)));

        return new KBVGateway(
                new StartAuthnAttemptRequestMapper(),
                new ResponseToQuestionMapper(),
                new KBVClientFactory(
                                new IdentityIQWebService(),
                                new HeaderHandlerResolver(headerHandler),
                                awsSecretsRetriever)
                        .createClient());
    }
}
