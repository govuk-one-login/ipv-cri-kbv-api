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
import uk.gov.di.ipv.cri.kbv.api.exception.InvalidSoapTokenException;
import uk.gov.di.ipv.cri.kbv.api.security.KBVClientFactory;
import uk.gov.di.ipv.cri.kbv.api.security.SoapTokenRetriever;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.kbv.api.util.SoapTokenUtilsTest.generateToken;

@ExtendWith(MockitoExtension.class)
class IdentityIQWrapperTest {
    public static final String GENERATE_TOKEN =
            generateToken(
                    String.format(
                            "{\"exp\": %d}",
                            Instant.now().getEpochSecond() + TimeUnit.DAYS.toSeconds(2)));

    @Mock private KBVClientFactory kbvClientFactory;
    @Mock private SoapTokenRetriever mockTokenRetriever;
    @Mock private IdentityIQWebServiceSoap identityIQWebServiceSoap;

    private IdentityIQWrapper identityIQWrapper;

    @BeforeEach
    void setUp() {
        when(kbvClientFactory.createClient()).thenReturn(identityIQWebServiceSoap);
        when(mockTokenRetriever.getSoapToken()).thenReturn(GENERATE_TOKEN);

        identityIQWrapper = new IdentityIQWrapper(kbvClientFactory, mockTokenRetriever);
    }

    @Test
    void callsSAAEndPointWithAValidToken() {
        SAARequest request = new SAARequest();
        SAAResponse2 response = new SAAResponse2();

        when(identityIQWebServiceSoap.saa(request)).thenReturn(response);

        SAAResponse2 result = identityIQWrapper.saa(request);

        assertEquals(response, result);
        verify(identityIQWebServiceSoap).saa(request);
        verifyNoMoreInteractions(kbvClientFactory.createClient());
    }

    @Test
    void callsRTQEndPointWithAValidToken() {
        RTQRequest rTQRequest = new RTQRequest();
        RTQResponse2 response = new RTQResponse2();

        when(identityIQWebServiceSoap.rtq(rTQRequest)).thenReturn(response);

        RTQResponse2 result = identityIQWrapper.rtq(rTQRequest);

        assertEquals(response, result);
        verify(identityIQWebServiceSoap).rtq(rTQRequest);
        verifyNoMoreInteractions(kbvClientFactory.createClient());
    }

    @Test
    void callsSAARefreshSoapTokenWhenTokenIsOutsideOfExpiryThreshold() {
        SAARequest sAARequest = new SAARequest();
        SAAResponse2 response = new SAAResponse2();

        when(identityIQWebServiceSoap.saa(sAARequest)).thenReturn(response);

        identityIQWrapper.saa(sAARequest);

        verify(mockTokenRetriever, times(2)).getSoapToken();
        verify(kbvClientFactory, times(1)).createClient();
    }

    @Test
    void callsRTQRefreshSoapTokenWhenTokenIsOutsideOfExpiryThreshold() {
        RTQRequest rTQRequest = new RTQRequest();
        RTQResponse2 response = new RTQResponse2();

        when(identityIQWebServiceSoap.rtq(rTQRequest)).thenReturn(response);

        identityIQWrapper.rtq(rTQRequest);

        verify(mockTokenRetriever, times(2)).getSoapToken();
        verify(kbvClientFactory, times(1)).createClient();
    }

    @Test
    void doesNotRefreshSoapTokenWhenThereIsAnErrorOccurredDuringASAARequest() {
        SAARequest sAARequest = new SAARequest();
        InvalidSoapTokenException exception =
                new InvalidSoapTokenException("Some Unknown exception occurred");

        when(mockTokenRetriever.getSoapToken()).thenThrow(exception);

        Exception errorOccurred =
                assertThrows(
                        InvalidSoapTokenException.class, () -> identityIQWrapper.saa(sAARequest));

        assertEquals(
                "SOAP Token could not be refreshed: Some Unknown exception occurred",
                errorOccurred.getMessage());
    }

    @Test
    void doesNotRefreshSoapTokenWhenThereIsAnErrorOccurredDuringARTQRequest() {
        RTQRequest rTQRequest = new RTQRequest();
        RuntimeException exception = new RuntimeException("Some Unknown exception occurred");

        when(mockTokenRetriever.getSoapToken()).thenThrow(exception);

        Exception errorOccurred =
                assertThrows(RuntimeException.class, () -> identityIQWrapper.rtq(rTQRequest));
        assertEquals(
                "SOAP Token could not be refreshed: Some Unknown exception occurred",
                errorOccurred.getMessage());
    }
}
