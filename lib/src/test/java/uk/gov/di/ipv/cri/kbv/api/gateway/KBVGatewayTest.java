package uk.gov.di.ipv.cri.kbv.api.gateway;

import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebServiceSoap;
import com.experian.uk.schema.experian.identityiq.services.webservice.RTQRequest;
import com.experian.uk.schema.experian.identityiq.services.webservice.RTQResponse2;
import com.experian.uk.schema.experian.identityiq.services.webservice.SAARequest;
import com.experian.uk.schema.experian.identityiq.services.webservice.SAAResponse2;
import jakarta.xml.soap.Name;
import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPEnvelope;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPHeader;
import jakarta.xml.soap.SOAPHeaderElement;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.soap.SOAPPart;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvResult;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.exception.ExperianTimeoutException;
import uk.gov.di.ipv.cri.kbv.api.exception.HeaderHandlerException;
import uk.gov.di.ipv.cri.kbv.api.security.HeaderHandler;
import uk.gov.di.ipv.cri.kbv.api.security.SoapToken;
import uk.gov.di.ipv.cri.kbv.api.security.SoapTokenRetriever;
import uk.gov.di.ipv.cri.kbv.api.service.MetricsService;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static uk.gov.di.ipv.cri.kbv.api.util.SoapTokenUtilsTest.generateToken;

@ExtendWith(MockitoExtension.class)
class KBVGatewayTest {
    private static final String MOCK_CLIENT_ID = "mock-client-id";

    @Mock private QuestionRequest questionRequest;
    @Mock private QuestionAnswerRequest questionAnswerRequest;
    @Mock private QuestionsResponse mockQuestionsResponse;
    @Mock private StartAuthnAttemptRequestMapper mockSAARequestMapper;
    @Mock private ResponseToQuestionMapper mockResponseToQuestionMapper;
    @Mock private QuestionsResponseMapper mockQuestionsResponseMapper;
    @Mock private IdentityIQWebServiceSoap mockIdentityIQWebServiceSoap;
    @Mock private MetricsService mockMetricsService;
    @Mock private SAARequest mockSaaRequest;
    @Mock private RTQRequest mockRtqRequest;
    @Mock private RTQResponse2 mockRtqResponse;
    @Mock private SAAResponse2 mockSaaResponse;

    @Spy @InjectMocks private KBVGateway kbvGateway;

    @Test
    void shouldCallGetQuestionsSuccessfully() {
        KbvResult mockKbvResult = mock(KbvResult.class);

        when(mockQuestionsResponse.getResults()).thenReturn(mockKbvResult);
        when(mockSAARequestMapper.mapQuestionRequest(questionRequest)).thenReturn(mockSaaRequest);
        when(mockQuestionsResponseMapper.mapSAAResponse(mockSaaResponse))
                .thenReturn(mockQuestionsResponse);

        doReturn(mockSaaResponse)
                .when(kbvGateway)
                .getQuestionRequestResponse(mockIdentityIQWebServiceSoap, mockSaaRequest);

        QuestionsResponse result =
                kbvGateway.getQuestions(mockIdentityIQWebServiceSoap, questionRequest);

        assertNotNull(result);
        verify(mockSAARequestMapper).mapQuestionRequest(questionRequest);
        verify(mockQuestionsResponseMapper).mapSAAResponse(mockSaaResponse);
        verify(mockMetricsService).sendDurationMetric(eq("get_questions_duration"), anyLong());
        verify(mockMetricsService).sendResultMetric("initial_questions_response", mockKbvResult);
    }

    @Test
    void shouldCallSubmitAnswersSuccessfully() {
        KbvResult mockKbvResult = mock(KbvResult.class);

        when(mockQuestionsResponse.getResults()).thenReturn(mockKbvResult);
        when(mockResponseToQuestionMapper.mapQuestionAnswersRtqRequest(questionAnswerRequest))
                .thenReturn(mockRtqRequest);
        when(mockQuestionsResponseMapper.mapRTQResponse(mockRtqResponse))
                .thenReturn(mockQuestionsResponse);

        doReturn(mockRtqResponse)
                .when(kbvGateway)
                .submitQuestionAnswerResponse(mockIdentityIQWebServiceSoap, mockRtqRequest);

        QuestionsResponse result =
                kbvGateway.submitAnswers(mockIdentityIQWebServiceSoap, questionAnswerRequest);

        assertNotNull(result);
        verify(mockResponseToQuestionMapper).mapQuestionAnswersRtqRequest(questionAnswerRequest);
        verify(mockQuestionsResponseMapper).mapRTQResponse(mockRtqResponse);
        verify(mockMetricsService).sendDurationMetric(eq("submit_answers_duration"), anyLong());
        verify(mockMetricsService).sendResultMetric("submit_questions_response", mockKbvResult);
    }

    @Test
    void shouldHandleGetQuestionsExperianTimeoutException() {
        when(mockSAARequestMapper.mapQuestionRequest(questionRequest)).thenReturn(mockSaaRequest);
        doThrow(new ExperianTimeoutException("TIMEOUT"))
                .when(kbvGateway)
                .getQuestionRequestResponse(mockIdentityIQWebServiceSoap, mockSaaRequest);

        QuestionsResponse result =
                kbvGateway.getQuestions(mockIdentityIQWebServiceSoap, questionRequest);

        assertNull(result);
        verify(mockMetricsService).sendErrorMetric("initial_questions_response_timeout", "TIMEOUT");
    }

    @Test
    void shouldThrowExperianTimeoutExceptionWhenHttpTimeoutOccursSubmitAnswerResponse()
            throws ExperianTimeoutException {
        WebServiceException wse =
                new WebServiceException("timeout", new HttpTimeoutException("test"));

        doThrow(wse).when(mockIdentityIQWebServiceSoap).rtq(mockRtqRequest);

        ExperianTimeoutException exception =
                assertThrows(
                        ExperianTimeoutException.class,
                        () ->
                                kbvGateway.submitQuestionAnswerResponse(
                                        mockIdentityIQWebServiceSoap, mockRtqRequest));

        assertTrue(exception.getMessage().contains("RTQ response timed out"));
    }

    @Test
    void shouldThrowExperianTimeoutExceptionWhenHttpTimeoutOccursGetQuestionsResponse()
            throws ExperianTimeoutException {
        WebServiceException wse =
                new WebServiceException("timeout", new HttpTimeoutException("test"));

        doThrow(wse).when(mockIdentityIQWebServiceSoap).saa(mockSaaRequest);

        ExperianTimeoutException exception =
                assertThrows(
                        ExperianTimeoutException.class,
                        () ->
                                kbvGateway.getQuestionRequestResponse(
                                        mockIdentityIQWebServiceSoap, mockSaaRequest));

        assertTrue(exception.getMessage().contains("SAA response timed out"));
    }

    @Test
    void shouldThrowExperianTimeoutExceptionWhenSocketTimeoutOccursSubmitAnswerResponse()
            throws ExperianTimeoutException {
        WebServiceException wse = new WebServiceException("timeout", new SocketTimeoutException());

        doThrow(wse).when(mockIdentityIQWebServiceSoap).rtq(mockRtqRequest);

        ExperianTimeoutException exception =
                assertThrows(
                        ExperianTimeoutException.class,
                        () ->
                                kbvGateway.submitQuestionAnswerResponse(
                                        mockIdentityIQWebServiceSoap, mockRtqRequest));

        assertTrue(exception.getMessage().contains("RTQ response timed out"));
    }

    @Test
    void shouldThrowExperianTimeoutExceptionWhenSocketTimeoutOccursGetQuestionsResponse()
            throws ExperianTimeoutException {
        WebServiceException wse = new WebServiceException("timeout", new SocketTimeoutException());

        doThrow(wse).when(mockIdentityIQWebServiceSoap).saa(mockSaaRequest);

        ExperianTimeoutException exception =
                assertThrows(
                        ExperianTimeoutException.class,
                        () ->
                                kbvGateway.getQuestionRequestResponse(
                                        mockIdentityIQWebServiceSoap, mockSaaRequest));

        assertTrue(exception.getMessage().contains("SAA response timed out"));
    }

    @Test
    void shouldHandleSubmitAnswersExperianTimeoutException() {
        when(mockResponseToQuestionMapper.mapQuestionAnswersRtqRequest(questionAnswerRequest))
                .thenReturn(mockRtqRequest);
        doThrow(new ExperianTimeoutException("TIMEOUT"))
                .when(kbvGateway)
                .submitQuestionAnswerResponse(mockIdentityIQWebServiceSoap, mockRtqRequest);

        QuestionsResponse result =
                kbvGateway.submitAnswers(mockIdentityIQWebServiceSoap, questionAnswerRequest);

        assertNull(result);
        verify(mockMetricsService).sendErrorMetric("submit_questions_response_timeout", "TIMEOUT");
    }

    @Test
    void shouldSendErrorMetricForSubmitAnswerResponsesError() {
        when(mockQuestionsResponse.hasError()).thenReturn(true);
        when(mockQuestionsResponse.getErrorCode()).thenReturn("test");
        doReturn(mockRtqResponse).when(kbvGateway).submitQuestionAnswerResponse(any(), any());
        when(mockQuestionsResponseMapper.mapRTQResponse(mockRtqResponse))
                .thenReturn(mockQuestionsResponse);

        kbvGateway.submitAnswers(mockIdentityIQWebServiceSoap, questionAnswerRequest);

        verify(mockMetricsService).sendErrorMetric("submit_questions_response_error", "test");
    }

    @Test
    void shouldThrowNullPointerExceptionWhenSaaRequestMapperIsNull() {
        assertThrows(
                NullPointerException.class,
                () ->
                        new KBVGateway(
                                null,
                                mockResponseToQuestionMapper,
                                mockQuestionsResponseMapper,
                                mockMetricsService),
                "saaRequestMapper must not be null");
    }

    @Test
    void shouldThrowNullPointerExceptionWhenQuestionsResponseMapperIsNull() {
        assertThrows(
                NullPointerException.class,
                () ->
                        new KBVGateway(
                                mockSAARequestMapper,
                                mockResponseToQuestionMapper,
                                null,
                                mockMetricsService),
                "questionsResponseMapper must not be null");
    }

    @Test
    void shouldThrowNullPointerExceptionWhenMetricsServiceIsNull() {
        assertThrows(
                NullPointerException.class,
                () ->
                        new KBVGateway(
                                mockSAARequestMapper,
                                mockResponseToQuestionMapper,
                                mockQuestionsResponseMapper,
                                null),
                "metricsService must not be null");
    }

    @Test
    void shouldThrowNullPointerExceptionWhenRtqRequestMapperIsNull() {
        assertThrows(
                NullPointerException.class,
                () ->
                        new KBVGateway(
                                mockSAARequestMapper,
                                null,
                                mockQuestionsResponseMapper,
                                mockMetricsService),
                "rtqRequestMapper must not be null");
    }

    @Nested
    class SoapTokenRetryTestOnExperianEndPoints {
        @Mock private SOAPMessageContext soapMessageContextMock;
        @Mock private SOAPMessage soapMessageMock;
        @Mock private SOAPPart soapPartMock;
        @Mock private SOAPEnvelope soapEnvelopeMock;
        @Mock private SOAPHeader newSoapHeader;
        @Mock private SoapToken soapTokenMock;

        private SoapTokenRetriever soapTokenRetriever;

        private final String soapToken =
                generateToken(
                        String.format(
                                "{\"exp\": %d}",
                                Instant.now().getEpochSecond() + TimeUnit.DAYS.toSeconds(2)));

        @BeforeEach
        void setUp() throws SOAPException {
            mockSoapMessageHandling();
            mockTokenRetriever();
            initializeWrapper();
        }

        private void mockSoapMessageHandling() throws SOAPException {
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

        private void mockTokenRetriever() {
            soapTokenRetriever = new SoapTokenRetriever(soapTokenMock);
            when(soapTokenMock.getToken(MOCK_CLIENT_ID)).thenReturn(soapToken);
            when(soapMessageContextMock.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY))
                    .thenReturn(true);
        }

        private void initializeWrapper() {
            HeaderHandler headerHandler = new HeaderHandler(soapTokenRetriever, MOCK_CLIENT_ID);
            headerHandler.handleMessage(soapMessageContextMock);

            mockIdentityIQWebServiceSoap =
                    mock(
                            IdentityIQWebServiceSoap.class,
                            withSettings().extraInterfaces(BindingProvider.class));
        }

        @Test
        void callsSAAEndPointWithAValidToken() {
            SAARequest request = new SAARequest();
            SAAResponse2 response = new SAAResponse2();

            when(mockIdentityIQWebServiceSoap.saa(request)).thenReturn(response);

            SAAResponse2 result = mockIdentityIQWebServiceSoap.saa(request);

            assertEquals(response, result);
            verify(mockIdentityIQWebServiceSoap).saa(request);
            verify(soapTokenMock, times(1)).getToken(MOCK_CLIENT_ID);
        }

        @Test
        void callsRTQEndPointWithAValidToken() {
            RTQRequest rTQRequest = new RTQRequest();
            RTQResponse2 response = new RTQResponse2();

            when(mockIdentityIQWebServiceSoap.rtq(rTQRequest)).thenReturn(response);

            RTQResponse2 result = mockIdentityIQWebServiceSoap.rtq(rTQRequest);

            assertEquals(response, result);
            verify(mockIdentityIQWebServiceSoap).rtq(rTQRequest);
            verify(soapTokenMock, times(1)).getToken(MOCK_CLIENT_ID);
        }

        @Test
        void callsSAARefreshSoapTokenWhenTokenIsOutsideOfExpiryThreshold() {
            SAARequest sAARequest = new SAARequest();
            SAAResponse2 response = new SAAResponse2();

            when(mockIdentityIQWebServiceSoap.saa(sAARequest)).thenReturn(response);
            mockIdentityIQWebServiceSoap.saa(sAARequest);

            verify(soapTokenMock, times(1)).getToken(MOCK_CLIENT_ID);
        }

        @Test
        void callsRTQRefreshSoapTokenWhenTokenIsOutsideOfExpiryThreshold() {
            RTQRequest rTQRequest = new RTQRequest();
            RTQResponse2 response = new RTQResponse2();

            when(mockIdentityIQWebServiceSoap.rtq(rTQRequest)).thenReturn(response);
            mockIdentityIQWebServiceSoap.rtq(rTQRequest);

            verify(soapTokenMock, times(1)).getToken(MOCK_CLIENT_ID);
        }

        @Test
        void refreshSoapTokenFailedWhenThereIsAnErrorOccurredDuringASAARequest() {
            SAARequest sAARequest = new SAARequest();
            soapTokenMock = mock(SoapToken.class);
            soapTokenRetriever = new SoapTokenRetriever(soapTokenMock);
            HeaderHandler headerHandler = new HeaderHandler(soapTokenRetriever, MOCK_CLIENT_ID);

            HeaderHandlerException exception =
                    assertThrows(
                            HeaderHandlerException.class,
                            () -> headerHandler.handleMessage(soapMessageContextMock));

            mockIdentityIQWebServiceSoap.saa(sAARequest);

            assertEquals(
                    "Error in SOAP HeaderHandler: The token must not be null",
                    exception.getMessage());

            verify(soapTokenMock, times(3)).getToken(MOCK_CLIENT_ID);
        }

        @Test
        void refreshSoapTokenFailedWhenThereIsAnErrorOccurredDuringARTQRequest()
                throws HeaderHandlerException {
            RTQRequest rTQRequest = new RTQRequest();
            soapTokenMock = mock(SoapToken.class);
            soapTokenRetriever = new SoapTokenRetriever(soapTokenMock);
            HeaderHandler headerHandler = new HeaderHandler(soapTokenRetriever, MOCK_CLIENT_ID);

            HeaderHandlerException exception =
                    assertThrows(
                            HeaderHandlerException.class,
                            () -> headerHandler.handleMessage(soapMessageContextMock));
            mockIdentityIQWebServiceSoap.rtq(rTQRequest);

            assertEquals(
                    "Error in SOAP HeaderHandler: The token must not be null",
                    exception.getMessage());

            verify(soapTokenMock, times(3)).getToken(MOCK_CLIENT_ID);
        }
    }
}
