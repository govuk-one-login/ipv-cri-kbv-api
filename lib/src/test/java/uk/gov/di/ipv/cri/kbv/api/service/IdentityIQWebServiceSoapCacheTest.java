package uk.gov.di.ipv.cri.kbv.api.service;

import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebServiceSoap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.kbv.api.security.KBVClientFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityIQWebServiceSoapCacheTest {
    @Mock private ServiceFactory mockServiceFactory;
    @Mock private KBVClientFactory mockKbvClientFactory;
    @Mock private IdentityIQWebServiceSoap mockIdentityIQWebServiceSoap;

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
        IdentityIQWebServiceSoapCache cache = new IdentityIQWebServiceSoapCache();
        IdentityIQWebServiceSoap retrieved = cache.get("dummy", mockServiceFactory);

        assertEquals(mockIdentityIQWebServiceSoap, retrieved);
    }

    @Test
    void shouldUseExisting() {
        IdentityIQWebServiceSoapCache cache = new IdentityIQWebServiceSoapCache();
        IdentityIQWebServiceSoap first = cache.get("dummy", mockServiceFactory);
        IdentityIQWebServiceSoap second = cache.get("dummy", mockServiceFactory);

        assertEquals(first, second);
    }
}
