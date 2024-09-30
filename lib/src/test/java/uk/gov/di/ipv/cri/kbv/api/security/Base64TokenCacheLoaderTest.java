package uk.gov.di.ipv.cri.kbv.api.security;

import com.experian.uk.wasp.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.kbv.api.exception.InvalidSoapTokenException;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPFault;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPFaultException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Base64TokenCacheLoaderTest {
    private String key = "valid-key";
    private final String application = "testApplication";
    private Boolean checkIp = true;
    private SoapToken soapTokenSpy;
    @Mock private SoapToken soapTokenMock;
    @Mock private TokenService tokenServiceMock;
    @Mock private ConfigurationService configurationServiceMock;
    @InjectMocks Base64TokenCacheLoader base64TokenCacheLoader;

    @BeforeEach
    void setUp() {
        soapTokenSpy =
                spy(
                        new SoapToken(
                                application, checkIp, tokenServiceMock, configurationServiceMock));
    }

    @Test
    void shouldLoadValidSoapToken() throws InvalidSoapTokenException {
        String key = "valid-key";
        when(soapTokenMock.getToken()).thenReturn("token");

        Base64TokenEncoder result = base64TokenCacheLoader.load(key);

        verify(soapTokenMock).getToken();
        assertNotNull(result);
    }

    @Test
    void shouldThrowInValidSoapTokenExceptionWhenSOAPFaultException() throws SOAPException {
        SOAPFault soapFault =
                SOAPFactory.newInstance()
                        .createFault(
                                "Invalid namespace for SOAP Envelope",
                                new QName("http://schemas.xmlsoap.org/soap/envelope/", "Client"));
        SOAPFaultException soapFaultException = new SOAPFaultException(soapFault);

        var invalidSoapTokenException =
                new InvalidSoapTokenException(
                        "SOAP Fault occurred: " + soapFaultException.getMessage());

        doThrow(invalidSoapTokenException).when(soapTokenSpy).getToken();

        base64TokenCacheLoader = new Base64TokenCacheLoader(soapTokenSpy);

        InvalidSoapTokenException expectedException =
                assertThrows(
                        InvalidSoapTokenException.class, () -> base64TokenCacheLoader.load(key));
        assertEquals(
                "SOAP Fault occurred: Invalid namespace for SOAP Envelope",
                expectedException.getMessage());
    }

    @Test
    void shouldThrowInValidSoapTokenExceptionWhenWebServiceException() {
        WebServiceException webServiceException =
                new WebServiceException("Failed to connect to the service endpoint");

        var invalidSoapTokenException =
                new InvalidSoapTokenException(
                        "Web Service error occurred: " + webServiceException.getMessage());

        doThrow(invalidSoapTokenException).when(soapTokenSpy).getToken();

        base64TokenCacheLoader = new Base64TokenCacheLoader(soapTokenSpy);

        InvalidSoapTokenException expectedException =
                assertThrows(
                        InvalidSoapTokenException.class, () -> base64TokenCacheLoader.load(key));
        assertEquals(
                "Web Service error occurred: Failed to connect to the service endpoint",
                expectedException.getMessage());
    }

    @Test
    void shouldThrowInValidSoapTokenExceptionWhenAnUnExpectedExceptionOccurs() {
        Exception unknownException = new Exception("Unknown exception");

        var invalidSoapTokenException =
                new InvalidSoapTokenException(
                        "Unexpected error occurred: " + unknownException.getMessage());

        doThrow(invalidSoapTokenException).when(soapTokenSpy).getToken();

        base64TokenCacheLoader = new Base64TokenCacheLoader(soapTokenSpy);

        InvalidSoapTokenException expectedException =
                assertThrows(
                        InvalidSoapTokenException.class, () -> base64TokenCacheLoader.load(key));
        assertEquals(
                "Unexpected error occurred: Unknown exception", expectedException.getMessage());
    }
}
