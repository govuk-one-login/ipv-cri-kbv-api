package uk.gov.di.ipv.cri.kbv.api.service;

import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebServiceSoap;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.kbv.api.exception.ExperianTimeoutException;
import uk.gov.di.ipv.cri.kbv.api.security.KBVClientFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityIQWebServiceSoapCacheTest {
    @Mock private ServiceFactory mockServiceFactory;
    @Mock private KBVClientFactory mockKbvClientFactory;
    @Mock private IdentityIQWebServiceSoap mockIdentityIQWebServiceSoap;
    @Spy private IdentityIQWebServiceSoapCache identityIQWebServiceSoapCache;

    @BeforeEach
    void setUp() {
        when(mockServiceFactory.getKbvClientFactory(anyString()))
                .thenReturn(mockKbvClientFactory)
                .thenReturn(mock(KBVClientFactory.class));

        when(mockKbvClientFactory.createClient(anyString()))
                .thenReturn(mockIdentityIQWebServiceSoap);
    }

    @Test
    void shouldCache() {
        doNothing()
                .when(identityIQWebServiceSoapCache)
                .implementTimeout(mockIdentityIQWebServiceSoap);
        IdentityIQWebServiceSoap retrieved =
                identityIQWebServiceSoapCache.get("dummy", mockServiceFactory);

        assertNotNull(retrieved);
        assertEquals(mockIdentityIQWebServiceSoap, retrieved);
    }

    @Test
    void shouldUseExisting() {
        doNothing()
                .when(identityIQWebServiceSoapCache)
                .implementTimeout(mockIdentityIQWebServiceSoap);
        IdentityIQWebServiceSoap first =
                identityIQWebServiceSoapCache.get("dummy", mockServiceFactory);
        IdentityIQWebServiceSoap second =
                identityIQWebServiceSoapCache.get("dummy", mockServiceFactory);

        assertEquals(first, second);
    }

    @Test
    void shouldThrowExperianTimeoutException() {
        doThrow(new ExperianTimeoutException("timeout"))
                .when(identityIQWebServiceSoapCache)
                .implementTimeout(mockIdentityIQWebServiceSoap);

        ExperianTimeoutException exception =
                assertThrows(
                        ExperianTimeoutException.class,
                        () -> identityIQWebServiceSoapCache.get("dummy", mockServiceFactory));

        assertEquals("timeout", exception.getMessage());
    }

    @Test
    void shouldTestHttpConduitTimeoutMethod() throws ExperianTimeoutException {
        Client mockClient = mock(Client.class);
        HTTPConduit mockConduit = mock(HTTPConduit.class);

        try (MockedStatic<ClientProxy> clientProxyMock = mockStatic(ClientProxy.class)) {

            clientProxyMock
                    .when(() -> ClientProxy.getClient(mockIdentityIQWebServiceSoap))
                    .thenReturn(mockClient);

            when(mockClient.getConduit()).thenReturn(mockConduit);

            identityIQWebServiceSoapCache.get("dummy", mockServiceFactory);

            verify(mockConduit).setClient(any(HTTPClientPolicy.class));
        }
    }
}
