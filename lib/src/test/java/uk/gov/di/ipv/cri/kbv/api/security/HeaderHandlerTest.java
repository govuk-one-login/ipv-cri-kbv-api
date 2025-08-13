package uk.gov.di.ipv.cri.kbv.api.security;

import jakarta.xml.soap.Name;
import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPEnvelope;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPHeader;
import jakarta.xml.soap.SOAPHeaderElement;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.soap.SOAPPart;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.kbv.api.exception.HeaderHandlerException;
import uk.gov.di.ipv.cri.kbv.api.exception.InvalidSoapTokenException;

import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.kbv.api.security.SoapTokenRetrieverTest.encodeBase64Url;
import static uk.gov.di.ipv.cri.kbv.api.security.SoapTokenRetrieverTest.generateValidToken;

@ExtendWith(MockitoExtension.class)
class HeaderHandlerTest {
    private static final String MOCKED_TOKEN_VALUE = generateValidToken();
    @Mock private SoapTokenRetriever soapTokenRetrieverMock;
    @Mock private SOAPMessageContext soapMessageContextMock;
    @Mock private SOAPMessage soapMessageMock;
    @Mock private SOAPPart soapPartMock;
    @Mock private SOAPEnvelope soapEnvelopeMock;
    @Mock private SOAPHeader soapHeader;
    @Mock private SOAPHeader newSoapHeader;
    @InjectMocks private HeaderHandler headerHandler;

    @Test
    void shouldHandleMessageNotOutbound() {
        when(soapMessageContextMock.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY))
                .thenReturn(false);

        boolean result = headerHandler.handleMessage(soapMessageContextMock);

        verify(soapMessageMock, never()).getSOAPPart();
        assertTrue(result);
    }

    @Test
    void shouldImplementInterfaceMethods() {
        assertNotNull(headerHandler.getHeaders());
        assertEquals(Collections.emptySet(), headerHandler.getHeaders());
        assertTrue(headerHandler.handleFault(soapMessageContextMock));
        assertDoesNotThrow(() -> headerHandler.close(soapMessageContextMock));
    }

    @Nested
    class HandlesSoapMessage {
        @BeforeEach
        void setUp() throws SOAPException {
            SOAPHeaderElement soapHeaderElementMock = mock(SOAPHeaderElement.class);
            SOAPElement soapElementMock = mock(SOAPElement.class);
            Name securityNameMock = mock(Name.class);

            when(soapMessageContextMock.getMessage()).thenReturn(soapMessageMock);
            when(soapMessageMock.getSOAPPart()).thenReturn(soapPartMock);
            when(soapPartMock.getEnvelope()).thenReturn(soapEnvelopeMock);
            when(soapEnvelopeMock.addHeader()).thenReturn(newSoapHeader);
            when(soapEnvelopeMock.createName(
                            "Security",
                            "wsse",
                            "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"))
                    .thenReturn(securityNameMock);
            when(newSoapHeader.addHeaderElement(securityNameMock))
                    .thenReturn(soapHeaderElementMock);
            when(soapHeaderElementMock.addChildElement("BinarySecurityToken", "wsse"))
                    .thenReturn(soapElementMock);
        }

        @Test
        void shouldHandleMessageOutbound() throws SOAPException {
            when(soapTokenRetrieverMock.getSoapToken(any())).thenReturn(MOCKED_TOKEN_VALUE);

            when(soapMessageContextMock.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY))
                    .thenReturn(true);
            when(soapEnvelopeMock.getHeader()).thenReturn(soapHeader);

            boolean result = headerHandler.handleMessage(soapMessageContextMock);

            verify(soapEnvelopeMock).addHeader();
            verify(soapMessageMock).saveChanges();
            assertTrue(result);
        }

        @Test
        void shouldDetachSoapCurrentHeaderIfNotNull() throws SOAPException {
            when(soapTokenRetrieverMock.getSoapToken(any())).thenReturn(MOCKED_TOKEN_VALUE);
            when(soapMessageContextMock.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY))
                    .thenReturn(true);
            when(soapEnvelopeMock.getHeader()).thenReturn(soapHeader);

            boolean result = headerHandler.handleMessage(soapMessageContextMock);

            verify(soapHeader).detachNode();
            verify(soapEnvelopeMock).addHeader();
            verify(soapMessageMock).saveChanges();
            assertTrue(result);
        }

        @Test
        void shouldThrowHeaderHandlerExceptionWhenTokenIsNull() {
            when(soapTokenRetrieverMock.getSoapToken(any())).thenReturn(null);
            when(soapMessageContextMock.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY))
                    .thenReturn(true);
            Exception exception =
                    assertThrows(
                            HeaderHandlerException.class,
                            () -> headerHandler.handleMessage(soapMessageContextMock),
                            "Error in SOAP HeaderHandler");

            assertEquals(
                    "Error in SOAP HeaderHandler: The token must not be null",
                    exception.getMessage());
        }

        @Test
        void shouldThrowWhenTokenHasExpired() {
            String token = "{}." + encodeBase64Url(String.format("{\"exp\": \"%d\"}", 0)) + ".{}";

            when(soapTokenRetrieverMock.getSoapToken(any())).thenReturn(token);
            when(soapMessageContextMock.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY))
                    .thenReturn(true);
            Exception exception =
                    assertThrows(
                            HeaderHandlerException.class,
                            () -> headerHandler.handleMessage(soapMessageContextMock),
                            "Error in SOAP HeaderHandler");

            assertEquals(
                    "Error in SOAP HeaderHandler: The SOAP token is expired",
                    exception.getMessage());
        }

        @Test
        void shouldNotThrowWhenTokenIsNotExpired() {
            String token =
                    "{}."
                            + encodeBase64Url(
                                    String.format(
                                            "{\"exp\": \"%d\"}",
                                            Instant.now().getEpochSecond()
                                                    + TimeUnit.HOURS.toSeconds(3)))
                            + ".{}";

            when(soapTokenRetrieverMock.getSoapToken(any())).thenReturn(token);
            when(soapMessageContextMock.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY))
                    .thenReturn(true);

            assertTrue(headerHandler.handleMessage(soapMessageContextMock));
        }

        @Test
        void shouldThrowHeaderHandlerExceptionWhenSoapTokenHasAnError() {
            when(soapTokenRetrieverMock.getSoapToken(any())).thenReturn("Error");
            when(soapMessageContextMock.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY))
                    .thenReturn(true);
            Exception exception =
                    assertThrows(
                            HeaderHandlerException.class,
                            () -> headerHandler.handleMessage(soapMessageContextMock),
                            "Error in SOAP HeaderHandler");

            assertEquals(
                    "Error in SOAP HeaderHandler: The SOAP token contains an error: Error",
                    exception.getMessage());
        }

        @Test
        void shouldThrowHeaderHandlerExceptionWhenThereIsASoapFault() {
            when(soapTokenRetrieverMock.getSoapToken(any()))
                    .thenThrow(new InvalidSoapTokenException("SOAP Fault occurred"));
            when(soapMessageContextMock.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY))
                    .thenReturn(true);
            Exception exception =
                    assertThrows(
                            HeaderHandlerException.class,
                            () -> headerHandler.handleMessage(soapMessageContextMock),
                            "Error in SOAP HeaderHandler");

            assertEquals(
                    "Error in SOAP HeaderHandler: SOAP Fault occurred", exception.getMessage());
        }
    }
}
