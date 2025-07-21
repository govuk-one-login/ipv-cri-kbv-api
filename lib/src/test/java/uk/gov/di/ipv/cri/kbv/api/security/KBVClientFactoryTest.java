package uk.gov.di.ipv.cri.kbv.api.security;

import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebService;
import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebServiceSoap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.kbv.api.exception.InvalidSoapTokenException;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPFaultException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
class KBVClientFactoryTest {
    private static final String MOCK_CLIENT_ID = "mock_client_id";

    @Mock private IdentityIQWebService identityIQWebService;
    @Mock private HeaderHandlerResolver headerHandlerResolver;
    @Mock private ConfigurationService configurationService;
    @Mock private IdentityIQWebServiceSoap identityIQWebServiceSoap;

    @InjectMocks private KBVClientFactory kbvClientFactory;

    @Test
    void shouldCreateClientSuccessfully() {
        identityIQWebServiceSoap =
                mock(
                        IdentityIQWebServiceSoap.class,
                        withSettings().extraInterfaces(BindingProvider.class));

        when(identityIQWebService.getIdentityIQWebServiceSoap())
                .thenReturn(identityIQWebServiceSoap);
        when(configurationService.getParameterValue("experian/iiq-webservice/" + MOCK_CLIENT_ID))
                .thenReturn("http://test-endpoint");

        IdentityIQWebServiceSoap result = kbvClientFactory.createClient(MOCK_CLIENT_ID);

        verify(identityIQWebService).setHandlerResolver(headerHandlerResolver);
        verify(identityIQWebService).getIdentityIQWebServiceSoap();
        verify(configurationService).getParameterValue("experian/iiq-webservice/" + MOCK_CLIENT_ID);
        assertEquals(identityIQWebServiceSoap, result);
    }

    @Test
    void shouldThrowInvalidSoapTokenExceptionWhenSoapFaultOccurs() {
        SOAPFaultException soapFaultException = mock(SOAPFaultException.class);
        when(identityIQWebService.getIdentityIQWebServiceSoap()).thenThrow(soapFaultException);

        InvalidSoapTokenException exception =
                assertThrows(
                        InvalidSoapTokenException.class,
                        () -> kbvClientFactory.createClient(MOCK_CLIENT_ID));

        assertEquals("SOAP Fault occurred: null", exception.getMessage());
        verify(identityIQWebService).setHandlerResolver(headerHandlerResolver);
        verify(identityIQWebService).getIdentityIQWebServiceSoap();
    }

    @Test
    void shouldThrowInvalidSoapTokenExceptionWhenWebServiceErrorOccurs() {
        WebServiceException webServiceException = mock(WebServiceException.class);
        when(identityIQWebService.getIdentityIQWebServiceSoap()).thenThrow(webServiceException);

        InvalidSoapTokenException exception =
                assertThrows(
                        InvalidSoapTokenException.class,
                        () -> kbvClientFactory.createClient(MOCK_CLIENT_ID));

        assertEquals("Web Service error occurred: null", exception.getMessage());
        verify(identityIQWebService).setHandlerResolver(headerHandlerResolver);
        verify(identityIQWebService).getIdentityIQWebServiceSoap();
    }

    @Test
    void shouldThrowInvalidSoapTokenExceptionWhenUnexpectedErrorOccurs() {
        RuntimeException unexpectedException = mock(RuntimeException.class);
        when(identityIQWebService.getIdentityIQWebServiceSoap()).thenThrow(unexpectedException);

        InvalidSoapTokenException exception =
                assertThrows(
                        InvalidSoapTokenException.class,
                        () -> kbvClientFactory.createClient(MOCK_CLIENT_ID));

        assertEquals("Unexpected error occurred: null", exception.getMessage());
        verify(identityIQWebService).setHandlerResolver(headerHandlerResolver);
        verify(identityIQWebService).getIdentityIQWebServiceSoap();
    }
}
