package uk.gov.di.ipv.cri.kbv.api.gateway;

import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebServiceSoap;
import com.experian.uk.schema.experian.identityiq.services.webservice.RTQRequest;
import com.experian.uk.schema.experian.identityiq.services.webservice.RTQResponse2;
import com.experian.uk.schema.experian.identityiq.services.webservice.Results;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.util.TestDataCreator;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KBVGatewayTest {

    private class KbvGatewayContructorArgs {
        private final IdentityIQWebServiceSoap identityIQWebServiceSoap;
        private final ResponseToQuestionMapper responseToQuestionMapper;
        private final StartAuthnAttemptRequestMapper saaRequestMapper;

        public KbvGatewayContructorArgs(
                StartAuthnAttemptRequestMapper saaRequestMapper,
                ResponseToQuestionMapper responseToQuestionMapper,
                IdentityIQWebServiceSoap identityIQWebServiceSoap) {

            this.identityIQWebServiceSoap = identityIQWebServiceSoap;
            this.saaRequestMapper = saaRequestMapper;
            this.responseToQuestionMapper = responseToQuestionMapper;
        }
    }

    private static final String TEST_API_RESPONSE_BODY = "test-api-response-content";
    private KBVGateway kbvGateway;
    private StartAuthnAttemptRequestMapper mockSAARequestMapper =
            mock(StartAuthnAttemptRequestMapper.class);
    private ResponseToQuestionMapper mockResponseToQuestionMapper =
            mock(ResponseToQuestionMapper.class);
    private IdentityIQWebServiceSoap mockIdentityIQWebServiceSoap =
            mock(IdentityIQWebServiceSoap.class);

    @BeforeEach
    void setUp() {
        this.kbvGateway =
                new KBVGateway(
                        mockSAARequestMapper,
                        mockResponseToQuestionMapper,
                        mockIdentityIQWebServiceSoap);
    }

    @Test
    void shouldCallSubmitAnswersSuccessfully() throws InterruptedException {
        // final String testRequestBody = "serialisedKbvApiRequest";
        QuestionAnswerRequest questionAnswerRequest =
                TestDataCreator.createTestQuestionAnswerRequest();

        // when(this.mockObjectMapper.writeValueAsString(testApiRequest)).thenReturn(testRequestBody);
        // ArgumentCaptor<HttpRequest> httpRequestCaptor =
        // ArgumentCaptor.forClass(HttpRequest.class);
        com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebServiceSoap
                mockIdentityIQWebServiceSoap =
                        mock(
                                com.experian.uk.schema.experian.identityiq.services.webservice
                                        .IdentityIQWebServiceSoap.class);

        RTQResponse2 mockRtqResponse = mock(RTQResponse2.class);
        RTQRequest mockRtqRequest = mock(RTQRequest.class);
        Results mockResults = mock(Results.class);

        when(mockResponseToQuestionMapper.mapQuestionAnswersRtqRequest(questionAnswerRequest))
                .thenReturn(mockRtqRequest);

        when(mockIdentityIQWebServiceSoap.rtq(mockRtqRequest)).thenReturn(mockRtqResponse);
        when(mockRtqResponse.getResults()).thenReturn(mockResults);

        QuestionsResponse questionAnswerRequestResult =
                kbvGateway.submitAnswers(questionAnswerRequest);

        // assertEquals(TEST_API_RESPONSE_BODY, questionAnswerRequestResult);
        verify(mockResponseToQuestionMapper).mapQuestionAnswersRtqRequest(questionAnswerRequest);
        // verify(mockObjectMapper).writeValueAsString(mockRtqRequest);
        // verify(mockKbvApiConfig).getEndpointUri();
        // assertEquals(testEndpointUri, httpRequestCaptor.getValue().uri().toString());
        // assertEquals("POST", httpRequestCaptor.getValue().method());
        // HttpHeaders capturedHttpRequestHeaders = httpRequestCaptor.getValue().headers();
        // assertEquals("application/json", capturedHttpRequestHeaders.firstValue("Accept").get());
        // assertEquals(
        //         "application/json", capturedHttpRequestHeaders.firstValue("Content-Type").get());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenInvalidConstructorArgumentsAreProvided() {
        Map<String, KbvGatewayContructorArgs> testCases =
                Map.of(
                        "httpClient must not be null",
                        new KbvGatewayContructorArgs(null, null, null),
                        "rtqRequestMapper must not be null",
                        new KBVGatewayTest.KbvGatewayContructorArgs(
                                null, null, mock(IdentityIQWebServiceSoap.class)),
                        "objectMapper must not be null",
                        new KBVGatewayTest.KbvGatewayContructorArgs(
                                null,
                                mock(ResponseToQuestionMapper.class),
                                mock(IdentityIQWebServiceSoap.class)),
                        "kbvApiConfig must not be null",
                        new KBVGatewayTest.KbvGatewayContructorArgs(
                                null,
                                mock(ResponseToQuestionMapper.class),
                                mock(IdentityIQWebServiceSoap.class)));

        testCases.forEach(
                (errorMessage, constructorArgs) -> {
                    assertThrows(
                            NullPointerException.class,
                            () -> {
                                new KBVGateway(
                                        constructorArgs.saaRequestMapper,
                                        constructorArgs.responseToQuestionMapper,
                                        constructorArgs.identityIQWebServiceSoap);
                            },
                            errorMessage);
                });
    }
}
