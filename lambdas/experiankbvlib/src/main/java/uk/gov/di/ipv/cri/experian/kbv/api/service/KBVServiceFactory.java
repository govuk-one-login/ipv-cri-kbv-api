package uk.gov.di.ipv.cri.experian.kbv.api.service;

import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebService;
import uk.gov.di.ipv.cri.experian.kbv.api.gateway.KBVGateway;
import uk.gov.di.ipv.cri.experian.kbv.api.gateway.ResponseToQuestionMapper;
import uk.gov.di.ipv.cri.experian.kbv.api.gateway.StartAuthnAttemptRequestMapper;
import uk.gov.di.ipv.cri.experian.kbv.api.security.Base64TokenCacheLoader;
import uk.gov.di.ipv.cri.experian.kbv.api.security.HeaderHandler;
import uk.gov.di.ipv.cri.experian.kbv.api.security.HeaderHandlerResolver;
import uk.gov.di.ipv.cri.experian.kbv.api.security.KBVClientFactory;

public class KBVServiceFactory {
    public KBVService create() {
        HeaderHandler headerHandler = new HeaderHandler(new Base64TokenCacheLoader());
        return new KBVService(
                new KBVGateway(
                        new StartAuthnAttemptRequestMapper(),
                        new ResponseToQuestionMapper(),
                        new KBVClientFactory(
                                        new IdentityIQWebService(),
                                        new HeaderHandlerResolver(headerHandler))
                                .createClient()));
    }
}
