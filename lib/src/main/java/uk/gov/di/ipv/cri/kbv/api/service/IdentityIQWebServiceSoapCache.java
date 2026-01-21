package uk.gov.di.ipv.cri.kbv.api.service;

import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebServiceSoap;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class IdentityIQWebServiceSoapCache {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(IdentityIQWebServiceSoapCache.class);
    private final Map<String, IdentityIQWebServiceSoap> cache;
    private static final int RESPONSE_TIMEOUT = 5000;

    public IdentityIQWebServiceSoapCache() {
        this.cache = new HashMap<>();
    }

    public IdentityIQWebServiceSoap get(String clientId, ServiceFactory serviceFactory) {
        IdentityIQWebServiceSoap identityIQWebServiceSoap;

        if (!cache.containsKey(clientId)) {
            identityIQWebServiceSoap =
                    serviceFactory.getKbvClientFactory(clientId).createClient(clientId);

            implementTimeout(identityIQWebServiceSoap);

            cache.put(clientId, identityIQWebServiceSoap);
            LOGGER.info("Cached IdentityIQWebServiceSoap for client {}", clientId);
        } else {
            identityIQWebServiceSoap = cache.get(clientId);
            LOGGER.info("Using cached IdentityIQWebServiceSoap for client {}", clientId);
        }

        return identityIQWebServiceSoap;
    }

    protected void implementTimeout(IdentityIQWebServiceSoap identityIQWebServiceSoap) {
        Client client = ClientProxy.getClient(identityIQWebServiceSoap);
        HTTPConduit httpConduit = (HTTPConduit) client.getConduit();
        HTTPClientPolicy clientPolicy = new HTTPClientPolicy();
        clientPolicy.setConnectionTimeout(RESPONSE_TIMEOUT);
        clientPolicy.setReceiveTimeout(RESPONSE_TIMEOUT);
        httpConduit.setClient(clientPolicy);
    }
}
