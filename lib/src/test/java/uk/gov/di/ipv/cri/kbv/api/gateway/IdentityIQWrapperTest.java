package uk.gov.di.ipv.cri.kbv.api.gateway;

import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebServiceSoap;
import com.experian.uk.schema.experian.identityiq.services.webservice.RTQRequest;
import com.experian.uk.schema.experian.identityiq.services.webservice.RTQResponse2;
import com.experian.uk.schema.experian.identityiq.services.webservice.SAARequest;
import com.experian.uk.schema.experian.identityiq.services.webservice.SAAResponse2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.kbv.api.security.KBVClientFactory;
import uk.gov.di.ipv.cri.kbv.api.security.SoapTokenRetriever;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.kbv.api.util.SoapTokenUtilsTest.generateToken;

@ExtendWith(MockitoExtension.class)
class IdentityIQWrapperTest {
    public static final String GENERATE_TOKEN =
            generateToken(
                    String.format(
                            "{\"exp\": %d}",
                            Instant.now().getEpochSecond() + TimeUnit.DAYS.toSeconds(2)));

    @Mock private Supplier<KBVClientFactory> kbvClientFactorySupplier;
    @Mock private Supplier<SoapTokenRetriever> soapTokenRetrieverSupplier;
    @Mock private KBVClientFactory kbvClientFactory;
    @Mock private SoapTokenRetriever mockTokenRetriever;
    @Mock private IdentityIQWebServiceSoap identityIQWebServiceSoap;

    private IdentityIQWrapper identityIQWrapper;

    @BeforeEach
    void setUp() {
        when(kbvClientFactorySupplier.get()).thenReturn(kbvClientFactory);
        when(soapTokenRetrieverSupplier.get()).thenReturn(mockTokenRetriever);
        when(kbvClientFactory.createClient()).thenReturn(identityIQWebServiceSoap);
        when(mockTokenRetriever.getSoapToken()).thenReturn(GENERATE_TOKEN);

        identityIQWrapper =
                new IdentityIQWrapper(kbvClientFactorySupplier, soapTokenRetrieverSupplier);
    }

    @Test
    void callsSAAEndPointWithAValidToken() {
        SAARequest request = new SAARequest();
        SAAResponse2 response = new SAAResponse2();

        when(identityIQWebServiceSoap.saa(request)).thenReturn(response);

        SAAResponse2 result = identityIQWrapper.saa(request);

        assertEquals(response, result);
        verify(identityIQWebServiceSoap).saa(request);
    }

    @Test
    void callsRTQEndPointWithAValidToken() {
        RTQRequest rTQRequest = new RTQRequest();
        RTQResponse2 response = new RTQResponse2();

        when(identityIQWebServiceSoap.rtq(rTQRequest)).thenReturn(response);

        RTQResponse2 result = identityIQWrapper.rtq(rTQRequest);

        assertEquals(response, result);
        verify(identityIQWebServiceSoap).rtq(rTQRequest);
    }

    @Test
    void callsSAARefreshSoapTokenWhenTokenIsExpired() {
        SAARequest sAARequest = new SAARequest();
        SAAResponse2 response = new SAAResponse2();

        KBVClientFactory mockNewKbvClientFactory = mock(KBVClientFactory.class);
        IdentityIQWebServiceSoap mockNewIdentityIQWebServiceSoap =
                mock(IdentityIQWebServiceSoap.class);

        when(kbvClientFactorySupplier.get()).thenReturn(mockNewKbvClientFactory);
        when(mockNewIdentityIQWebServiceSoap.saa(sAARequest)).thenReturn(response);
        when(kbvClientFactorySupplier.get()).thenReturn(mockNewKbvClientFactory);
        when(mockNewKbvClientFactory.createClient()).thenReturn(mockNewIdentityIQWebServiceSoap);

        identityIQWrapper.saa(sAARequest);

        verify(kbvClientFactorySupplier, times(2)).get();
        verify(mockNewKbvClientFactory).createClient();
    }

    @Test
    void callsRTQRefreshSoapTokenWhenTokenIsExpired() {
        RTQRequest rTQRequest = new RTQRequest();
        RTQResponse2 response = new RTQResponse2();

        KBVClientFactory mockNewKbvClientFactory = mock(KBVClientFactory.class);
        IdentityIQWebServiceSoap mockNewIdentityIQWebServiceSoap =
                mock(IdentityIQWebServiceSoap.class);

        when(kbvClientFactorySupplier.get()).thenReturn(mockNewKbvClientFactory);
        when(mockNewIdentityIQWebServiceSoap.rtq(rTQRequest)).thenReturn(response);
        when(kbvClientFactorySupplier.get()).thenReturn(mockNewKbvClientFactory);
        when(mockNewKbvClientFactory.createClient()).thenReturn(mockNewIdentityIQWebServiceSoap);

        identityIQWrapper.rtq(rTQRequest);

        verify(kbvClientFactorySupplier, times(2)).get();
        verify(mockNewKbvClientFactory).createClient();
    }
}
