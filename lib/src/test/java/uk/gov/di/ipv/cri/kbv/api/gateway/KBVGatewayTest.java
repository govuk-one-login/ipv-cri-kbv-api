package uk.gov.di.ipv.cri.kbv.api.gateway;

import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebServiceSoap;
import com.experian.uk.schema.experian.identityiq.services.webservice.RTQRequest;
import com.experian.uk.schema.experian.identityiq.services.webservice.RTQResponse2;
import com.experian.uk.schema.experian.identityiq.services.webservice.SAARequest;
import com.experian.uk.schema.experian.identityiq.services.webservice.SAAResponse2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvResult;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.exception.HeaderHandlerException;
import uk.gov.di.ipv.cri.kbv.api.security.HeaderHandler;
import uk.gov.di.ipv.cri.kbv.api.security.SoapToken;
import uk.gov.di.ipv.cri.kbv.api.security.SoapTokenRetriever;
import uk.gov.di.ipv.cri.kbv.api.service.MetricsService;

import javax.xml.soap.Name;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static uk.gov.di.ipv.cri.kbv.api.util.SoapTokenUtilsTest.generateToken;

@ExtendWith(MockitoExtension.class)
class KBVGatewayTest {
    @Mock private QuestionRequest questionRequest;
    @Mock private QuestionAnswerRequest questionAnswerRequest;
    @Mock private StartAuthnAttemptRequestMapper mockSAARequestMapper;
    @Mock private ResponseToQuestionMapper mockResponseToQuestionMapper;
    @Mock private QuestionsResponseMapper mockQuestionsResponseMapper;
    @Mock private IdentityIQWebServiceSoap mockIdentityIQWebServiceSoap;
    @Mock private MetricsService mockMetricsService;

    @InjectMocks private KBVGateway kbvGateway;

    @Test
    void shouldCallGetQuestionsSuccessfully() {
        SAARequest mockSaaRequest = mock(SAARequest.class);
        SAAResponse2 mockSaaResponse = mock(SAAResponse2.class);
        QuestionsResponse mockQuestionsResponse = mock(QuestionsResponse.class);
        KbvResult mockKbvResult = mock(KbvResult.class);
        when(mockQuestionsResponse.getResults()).thenReturn(mockKbvResult);
        when(mockSAARequestMapper.mapQuestionRequest(questionRequest)).thenReturn(mockSaaRequest);
        when(mockIdentityIQWebServiceSoap.saa(mockSaaRequest)).thenReturn(mockSaaResponse);
        when(mockQuestionsResponseMapper.mapSAAResponse(mockSaaResponse))
                .thenReturn(mockQuestionsResponse);

        kbvGateway.getQuestions(questionRequest);

        verify(mockSAARequestMapper).mapQuestionRequest(questionRequest);
        verify(mockIdentityIQWebServiceSoap).saa(mockSaaRequest);
        verify(mockQuestionsResponseMapper).mapSAAResponse(mockSaaResponse);
        verify(mockMetricsService).sendDurationMetric(eq("get_questions_duration"), anyLong());
        verify(mockMetricsService).sendResultMetric("initial_questions_response", mockKbvResult);
    }

    @Test
    void shouldCallSubmitAnswersSuccessfully() {
        RTQRequest mockRtqRequest = mock(RTQRequest.class);
        RTQResponse2 mockRtqResponse = mock(RTQResponse2.class);
        QuestionsResponse mockQuestionsResponse = mock(QuestionsResponse.class);
        KbvResult mockKbvResult = mock(KbvResult.class);
        when(mockQuestionsResponse.getResults()).thenReturn(mockKbvResult);
        when(mockResponseToQuestionMapper.mapQuestionAnswersRtqRequest(questionAnswerRequest))
                .thenReturn(mockRtqRequest);
        when(mockIdentityIQWebServiceSoap.rtq(mockRtqRequest)).thenReturn(mockRtqResponse);
        when(mockQuestionsResponseMapper.mapRTQResponse(mockRtqResponse))
                .thenReturn(mockQuestionsResponse);
        kbvGateway.submitAnswers(questionAnswerRequest);

        verify(mockResponseToQuestionMapper).mapQuestionAnswersRtqRequest(questionAnswerRequest);
        verify(mockIdentityIQWebServiceSoap).rtq(mockRtqRequest);
        verify(mockQuestionsResponseMapper).mapRTQResponse(mockRtqResponse);
        verify(mockMetricsService).sendDurationMetric(eq("submit_answers_duration"), anyLong());
        verify(mockMetricsService).sendResultMetric("submit_questions_response", mockKbvResult);
    }

    @Test
    void shouldThrowNullPointerExceptionWhenIdentityIQWebServiceIsNull() {
        assertThrows(
                NullPointerException.class,
                () ->
                        new KBVGateway(
                                mockSAARequestMapper,
                                mockResponseToQuestionMapper,
                                mockQuestionsResponseMapper,
                                null,
                                mockMetricsService),
                "identityIQWebServiceSoap must not be null");
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
                                mockIdentityIQWebServiceSoap,
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
                                mockIdentityIQWebServiceSoap,
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
                                mockIdentityIQWebServiceSoap,
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
                                mockIdentityIQWebServiceSoap,
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

        private final String SoapToken =
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
            when(soapTokenMock.getToken()).thenReturn(SoapToken);
            when(soapMessageContextMock.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY))
                    .thenReturn(true);
        }

        private void initializeWrapper() {
            HeaderHandler headerHandler = new HeaderHandler(soapTokenRetriever);
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
            verify(soapTokenMock, times(1)).getToken();
        }

        @Test
        void callsRTQEndPointWithAValidToken() {
            RTQRequest rTQRequest = new RTQRequest();
            RTQResponse2 response = new RTQResponse2();

            when(mockIdentityIQWebServiceSoap.rtq(rTQRequest)).thenReturn(response);

            RTQResponse2 result = mockIdentityIQWebServiceSoap.rtq(rTQRequest);

            assertEquals(response, result);
            verify(mockIdentityIQWebServiceSoap).rtq(rTQRequest);
            verify(soapTokenMock, times(1)).getToken();
        }

        @Test
        void callsSAARefreshSoapTokenWhenTokenIsOutsideOfExpiryThreshold() {
            SAARequest sAARequest = new SAARequest();
            SAAResponse2 response = new SAAResponse2();

            when(mockIdentityIQWebServiceSoap.saa(sAARequest)).thenReturn(response);
            mockIdentityIQWebServiceSoap.saa(sAARequest);

            verify(soapTokenMock, times(1)).getToken();
        }

        @Test
        void callsRTQRefreshSoapTokenWhenTokenIsOutsideOfExpiryThreshold() {
            RTQRequest rTQRequest = new RTQRequest();
            RTQResponse2 response = new RTQResponse2();

            when(mockIdentityIQWebServiceSoap.rtq(rTQRequest)).thenReturn(response);
            mockIdentityIQWebServiceSoap.rtq(rTQRequest);

            verify(soapTokenMock, times(1)).getToken();
        }

        @Test
        void refreshSoapTokenFailedWhenThereIsAnErrorOccurredDuringASAARequest() {
            SAARequest sAARequest = new SAARequest();
            soapTokenMock = mock(SoapToken.class);
            soapTokenRetriever = new SoapTokenRetriever(soapTokenMock);
            HeaderHandler headerHandler = new HeaderHandler(soapTokenRetriever);

            HeaderHandlerException exception =
                    assertThrows(
                            HeaderHandlerException.class,
                            () -> headerHandler.handleMessage(soapMessageContextMock));

            mockIdentityIQWebServiceSoap.saa(sAARequest);

            assertEquals(
                    "Error in SOAP HeaderHandler: The token must not be null",
                    exception.getMessage());

            verify(soapTokenMock, times(3)).getToken();
        }

        @Test
        void refreshSoapTokenFailedWhenThereIsAnErrorOccurredDuringARTQRequest()
                throws HeaderHandlerException {
            RTQRequest rTQRequest = new RTQRequest();
            soapTokenMock = mock(SoapToken.class);
            soapTokenRetriever = new SoapTokenRetriever(soapTokenMock);
            HeaderHandler headerHandler = new HeaderHandler(soapTokenRetriever);

            HeaderHandlerException exception =
                    assertThrows(
                            HeaderHandlerException.class,
                            () -> headerHandler.handleMessage(soapMessageContextMock));
            mockIdentityIQWebServiceSoap.rtq(rTQRequest);

            assertEquals(
                    "Error in SOAP HeaderHandler: The token must not be null",
                    exception.getMessage());

            verify(soapTokenMock, times(3)).getToken();
        }
    }
}
