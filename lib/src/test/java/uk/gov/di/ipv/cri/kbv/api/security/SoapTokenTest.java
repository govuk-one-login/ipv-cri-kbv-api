package uk.gov.di.ipv.cri.kbv.api.security;

import com.experian.uk.wasp.TokenService;
import com.experian.uk.wasp.TokenServiceSoap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.kbv.api.exception.InvalidSoapTokenException;
import uk.gov.di.ipv.cri.kbv.api.service.MetricsService;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPFaultException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
class SoapTokenTest {
    @Mock private TokenService tokenServiceMock;
    @Mock private ConfigurationService configurationServiceMock;
    @Mock private TokenServiceSoap tokenServiceSoapMock;
    @Mock private MetricsService mockMetricsService;

    private SoapToken soapToken;
    private final String application = "testApplication";
    private Boolean checkIp = true;
    private final String endpointUrl = "https://example.com/soap-service";
    private final String token = "mockedToken";

    @BeforeEach
    void setUp() {
        configurationServiceMock = mock(ConfigurationService.class);
        tokenServiceSoapMock =
                mock(TokenServiceSoap.class, withSettings().extraInterfaces(BindingProvider.class));

        soapToken =
                new SoapToken(
                        application,
                        checkIp,
                        tokenServiceMock,
                        configurationServiceMock,
                        mockMetricsService);

        when(configurationServiceMock.getSecretValue("experian/iiq-wasp-service"))
                .thenReturn(endpointUrl);
        when(tokenServiceMock.getTokenServiceSoap()).thenReturn(tokenServiceSoapMock);
    }

    @Test
    void shouldReturnAValidSoapToken() {
        when(tokenServiceSoapMock.loginWithCertificate(application, checkIp)).thenReturn(token);

        String result = soapToken.getToken();

        verify(tokenServiceMock).getTokenServiceSoap();
        verify(configurationServiceMock).getSecretValue("experian/iiq-wasp-service");
        verify(mockMetricsService).sendDurationMetric(eq("get_soap_token_duration"), anyLong());
        assertEquals(token, result);
    }

    @Test
    void shouldHandleSOAPFaultException() {
        SOAPFaultException soapFaultException = mock(SOAPFaultException.class);
        when(tokenServiceSoapMock.loginWithCertificate(application, checkIp))
                .thenThrow(soapFaultException);
        when(soapFaultException.getMessage()).thenReturn("SOAP Fault");

        InvalidSoapTokenException exception =
                assertThrows(InvalidSoapTokenException.class, soapToken::getToken);

        verify(tokenServiceMock).getTokenServiceSoap();
        verify(configurationServiceMock).getSecretValue("experian/iiq-wasp-service");
        assertEquals("SOAP Fault occurred: SOAP Fault", exception.getMessage());
    }

    @Test
    void shouldHandleWebServiceException() {
        when(tokenServiceSoapMock.loginWithCertificate(application, checkIp))
                .thenThrow(new WebServiceException("Web Service error"));

        InvalidSoapTokenException exception =
                assertThrows(InvalidSoapTokenException.class, soapToken::getToken);

        verify(tokenServiceMock).getTokenServiceSoap();
        verify(configurationServiceMock).getSecretValue("experian/iiq-wasp-service");
        assertTrue(exception.getMessage().contains("Web Service error occurred"));
    }

    @Test
    void shouldHandleAnUnexpectedException() {
        when(tokenServiceSoapMock.loginWithCertificate(application, checkIp))
                .thenThrow(new RuntimeException("Unexpected error"));

        InvalidSoapTokenException exception =
                assertThrows(InvalidSoapTokenException.class, soapToken::getToken);

        verify(tokenServiceMock).getTokenServiceSoap();
        verify(configurationServiceMock).getSecretValue("experian/iiq-wasp-service");

        assertTrue(exception.getMessage().contains("Unexpected error occurred"));
    }
}
