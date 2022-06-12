package uk.gov.di.ipv.cri.kbv.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.dynamodb.model.InternalServerErrorException;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventTypes;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentity;
import uk.gov.di.ipv.cri.common.library.exception.SqsException;
import uk.gov.di.ipv.cri.common.library.service.AuditService;
import uk.gov.di.ipv.cri.common.library.service.PersonIdentityService;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionRequest;
import uk.gov.di.ipv.cri.kbv.api.gateway.KBVGateway;
import uk.gov.di.ipv.cri.kbv.api.gateway.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.service.ClockService;
import uk.gov.di.ipv.cri.kbv.api.service.KBVService;
import uk.gov.di.ipv.cri.kbv.api.service.KBVStorageService;
import uk.gov.di.ipv.cri.kbv.api.service.KBVSystemProperty;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.kbv.api.handler.QuestionHandler.GET_QUESTION;
import static uk.gov.di.ipv.cri.kbv.api.handler.QuestionHandler.HEADER_SESSION_ID;

@ExtendWith(MockitoExtension.class)
class QuestionHandlerTest {
    private QuestionHandler questionHandler;
    // private final Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    @Mock private KBVStorageService mockKBVStorageService;
    @Mock private PersonIdentityService mockPersonIdentityService;
    @Mock private EventProbe mockEventProbe;
    @Mock private KBVGateway mockKBVGateway;
    @Mock private KBVSystemProperty mockSystemProperty;
    @Mock private AuditService mockAuditService;
    @Mock private ClockService mockClockService;
    private KBVService spyKBVService;

    @BeforeEach
    void setUp() {
        doNothing().when(mockSystemProperty).save();
        spyKBVService = Mockito.spy(new KBVService(mockKBVGateway));
        questionHandler =
                new QuestionHandler(
                        mockKBVStorageService,
                        mockPersonIdentityService,
                        mockSystemProperty,
                        spyKBVService,
                        mockEventProbe,
                        mockClockService,
                        mockAuditService);
    }

    @Test
    void shouldReturn200OkWhen1stCalledAndReturn1stUnAnsweredQuestionFromExperianEndpoint()
            throws IOException, SqsException {
        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, UUID.randomUUID().toString());

        var contextMock = mock(Context.class);
        var personIdentityMock = mock(PersonIdentity.class);
        var kbvItem = new KBVItem();
        kbvItem.setSessionId(UUID.fromString(sessionHeader.get(HEADER_SESSION_ID)));
        var questionsResponseString =
                "{\"control\":{\"urn\":\"f0746bbd-ed6f-44c7-bc3b-82c336c10815\",\"authRefNo\":\"7DCTWBC7LR\",\"dateTime\":null,\"testDatabase\":\"A\",\"clientAccountNo\":\"J8193\",\"clientBranchNo\":null,\"operatorID\":\"GDSCABINETUIIQ01U\",\"parameters\":{\"oneShotAuthentication\":\"N\",\"storeCaseData\":\"P\"}},\"questions\":{\"question\":[{\"questionID\":\"Q00015\",\"text\":\"What is the outstanding balance of your current mortgage?\",\"tooltip\":\"The approximate amount in £s, including interest. A loan to buy property (or land) where the loan is secured by a charge on that property.\",\"answerHeldFlag\":null,\"answerFormat\":{\"identifier\":\"A00004\",\"fieldType\":\"G \",\"answerList\":[\"UP TO £10,000\",\"OVER £10,000 UP TO £35,000\",\"OVER £35,000 UP TO £60,000\",\"OVER £60,000 UP TO £85,000\",\"NONE OF THE ABOVE / DOES NOT APPLY\"]}},{\"questionID\":\"Q00040\",\"text\":\"How much was your recent loan for?\",\"tooltip\":\"The approximate starting balance, in £s, on an active personal loan. Does not include HP Loans, 2nd Mortgages or Home Credit.\",\"answerFormat\":{\"identifier\":\"A00004\",\"fieldType\":\"G \",\"answerList\":[\"UP TO £8,500\",\"OVER £8,500 UP TO £9,000\",\"OVER £9,000 UP TO £9,500\",\"OVER £9,500 UP TO £10,000\",\"NONE OF THE ABOVE / DOES NOT APPLY\"]},\"answerHeldFlag\":null}],\"skipsRemaining\":null,\"skipWarning\":null},\"results\":{\"outcome\":\"Authentication Questions returned\",\"authenticationResult\":null,\"questions\":null,\"alerts\":null,\"nextTransId\":{\"string\":[\"RTQ\"]},\"caseFoundFlag\":null,\"confirmationCode\":null},\"error\":null}";
        QuestionsResponse questionResponse =
                objectMapper.readValue(questionsResponseString, QuestionsResponse.class);

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockPersonIdentityService.getPersonIdentity(kbvItem.getSessionId()))
                .thenReturn(personIdentityMock);
        doReturn(questionResponse).when(spyKBVService).getQuestions(any());

        APIGatewayProxyResponseEvent response = questionHandler.handleRequest(input, contextMock);

        assertEquals(HttpStatusCode.OK, response.getStatusCode());
        assertEquals(TestData.EXPECTED_QUESTION, response.getBody());

        verify(input, times(2)).getHeaders();
        verify(mockPersonIdentityService).getPersonIdentity(kbvItem.getSessionId());
        verify(mockAuditService).sendAuditEvent(AuditEventTypes.IPV_KBV_CRI_REQUEST_SENT);
        verify(mockEventProbe).counterMetric(GET_QUESTION);
    }

    @Test
    void shouldReturn200OkWhenCalledAgainAndReturnNextUnAnsweredQuestionFromStorage() {
        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, UUID.randomUUID().toString());

        Context contextMock = mock(Context.class);
        KBVItem kbvItem = new KBVItem();
        kbvItem.setSessionId(UUID.fromString(sessionHeader.get(HEADER_SESSION_ID)));
        String questionStateString =
                "{\"qaPairs\":[{\"question\":{\"questionID\":\"Q00015\",\"text\":\"What is the outstanding balance of your current mortgage?\",\"tooltip\":\"The approximate amount in £s, including interest. A loan to buy property (or land) where the loan is secured by a charge on that property.\",\"answerHeldFlag\":null,\"answerFormat\":{\"identifier\":\"A00004\",\"fieldType\":\"G \",\"answerList\":[\"UP TO £10,000\",\"OVER £10,000 UP TO £35,000\",\"OVER £35,000 UP TO £60,000\",\"OVER £60,000 UP TO £85,000\",\"NONE OF THE ABOVE / DOES NOT APPLY\"]}},\"answer\":null},{\"question\":{\"questionID\":\"Q00040\",\"text\":\"How much was your recent loan for?\",\"tooltip\":\"The approximate starting balance, in £s, on an active personal loan. Does not include HP Loans, 2nd Mortgages or Home Credit.\",\"answerHeldFlag\":null,\"answerFormat\":{\"identifier\":\"A00004\",\"fieldType\":\"G \",\"answerList\":[\"UP TO £8,500\",\"OVER £8,500 UP TO £9,000\",\"OVER £9,000 UP TO £9,500\",\"OVER £9,500 UP TO £10,000\",\"NONE OF THE ABOVE / DOES NOT APPLY\"]}},\"answer\":\"Answer given here\"}],\"nextQuestion\":{\"empty\":false,\"present\":true},\"answers\":[{\"questionId\":\"Q00040\",\"answer\":null},{\"questionId\":\"Q00015\",\"answer\":null}]}";
        kbvItem.setQuestionState(questionStateString);

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockKBVStorageService.getKBVItem(
                        UUID.fromString(sessionHeader.get(HEADER_SESSION_ID))))
                .thenReturn(kbvItem);

        APIGatewayProxyResponseEvent response = questionHandler.handleRequest(input, contextMock);

        assertEquals(HttpStatusCode.OK, response.getStatusCode());
        assertEquals(TestData.EXPECTED_QUESTION, response.getBody());

        verify(input, times(2)).getHeaders();
        verify(mockKBVStorageService)
                .getKBVItem(UUID.fromString(sessionHeader.get(HEADER_SESSION_ID)));
        verify(mockEventProbe).counterMetric(GET_QUESTION);
    }

    @Test // this is a case where all the questions submitted have been answered previously. and
    // then resubmitted
    void shouldReturn400ErrorWhenNoFurtherQuestions() throws IOException {
        Context contextMock = mock(Context.class);
        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, UUID.randomUUID().toString());

        KBVItem kbvItem = new KBVItem();
        kbvItem.setSessionId(UUID.fromString(sessionHeader.get(HEADER_SESSION_ID)));
        kbvItem.setExpiryDate(97989);
        String questionState =
                "{\"qaPairs\":[{\"question\":{\"questionID\":\"Q00015\",\"text\":\"What is the outstanding balance of your current mortgage?\",\"tooltip\":\"The approximate amount in £s, including interest. A loan to buy property (or land) where the loan is secured by a charge on that property.\",\"answerHeldFlag\":null,\"answerFormat\":{\"identifier\":\"A00004\",\"fieldType\":\"G \",\"answerList\":[\"UP TO £10,000\",\"OVER £10,000 UP TO £35,000\",\"OVER £35,000 UP TO £60,000\",\"OVER £60,000 UP TO £85,000\",\"NONE OF THE ABOVE / DOES NOT APPLY\"]}},\"answer\":\"Answer given here\"},{\"question\":{\"questionID\":\"Q00040\",\"text\":\"How much was your recent loan for?\",\"tooltip\":\"The approximate starting balance, in £s, on an active personal loan. Does not include HP Loans, 2nd Mortgages or Home Credit.\",\"answerHeldFlag\":null,\"answerFormat\":{\"identifier\":\"A00004\",\"fieldType\":\"G \",\"answerList\":[\"UP TO £8,500\",\"OVER £8,500 UP TO £9,000\",\"OVER £9,000 UP TO £9,500\",\"OVER £9,500 UP TO £10,000\",\"NONE OF THE ABOVE / DOES NOT APPLY\"]}},\"answer\":\"another answer given\"}],\"nextQuestion\":{\"empty\":false,\"present\":true},\"answers\":[{\"questionId\":\"Q00040\",\"answer\":null},{\"questionId\":\"Q00015\",\"answer\":null}]}";
        kbvItem.setQuestionState(questionState);

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockKBVStorageService.getKBVItem(
                        UUID.fromString(sessionHeader.get(HEADER_SESSION_ID))))
                .thenReturn(kbvItem);

        APIGatewayProxyResponseEvent response = questionHandler.handleRequest(input, contextMock);

        assertEquals(HttpStatusCode.BAD_REQUEST, response.getStatusCode());

        verify(input, times(2)).getHeaders();
        verify(mockKBVStorageService)
                .getKBVItem(UUID.fromString(sessionHeader.get(HEADER_SESSION_ID)));
        verify(spyKBVService).submitAnswers(any());
        verify(mockEventProbe).counterMetric(GET_QUESTION);
    }

    @Test
    void shouldReturn400ErrorWhenNoSessionIdProvided() {
        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        setupEventProbeErrorBehaviour();

        APIGatewayProxyResponseEvent response =
                questionHandler.handleRequest(input, mock(Context.class));

        String expectedMessage = "java.lang.NullPointerException";

        assertTrue(response.getBody().contains(expectedMessage));

        assertEquals(HttpStatusCode.BAD_REQUEST, response.getStatusCode());
        verify(mockEventProbe).counterMetric(GET_QUESTION, 0d);
    }

    @Test
    void shouldReturn500ErrorWhenAWSDynamoDBServiceDown() {
        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, UUID.randomUUID().toString());

        when(input.getHeaders()).thenReturn(sessionHeader);
        doThrow(InternalServerErrorException.class)
                .when(mockKBVStorageService)
                .getKBVItem(UUID.fromString(sessionHeader.get(HEADER_SESSION_ID)));

        setupEventProbeErrorBehaviour();
        APIGatewayProxyResponseEvent response =
                questionHandler.handleRequest(input, mock(Context.class));

        assertEquals("{ \"error\":\"AWS Server error occurred.\" }", response.getBody());
        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR, response.getStatusCode());

        verify(input, times(2)).getHeaders();
        verify(mockEventProbe).counterMetric(GET_QUESTION, 0d);
    }

    @Test
    void shouldReturn500ErrorWhenPersonIdentityCannotBeRetrievedDueToAnAwsError() {
        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, UUID.randomUUID().toString());

        when(input.getHeaders()).thenReturn(sessionHeader);

        AwsErrorDetails awsErrorDetails =
                AwsErrorDetails.builder()
                        .errorCode("")
                        .sdkHttpResponse(
                                SdkHttpResponse.builder()
                                        .statusCode(HttpStatusCode.INTERNAL_SERVER_ERROR)
                                        .build())
                        .errorMessage("AWS DynamoDbException Occurred")
                        .build();
        when(mockPersonIdentityService.getPersonIdentity(
                        UUID.fromString(sessionHeader.get(HEADER_SESSION_ID))))
                .thenThrow(
                        AwsServiceException.builder()
                                .statusCode(500)
                                .awsErrorDetails(awsErrorDetails)
                                .build());

        setupEventProbeErrorBehaviour();
        APIGatewayProxyResponseEvent response =
                questionHandler.handleRequest(input, mock(Context.class));

        assertEquals("{ \"error\":\"AWS Server error occurred.\" }", response.getBody());
        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR, response.getStatusCode());

        verify(mockPersonIdentityService)
                .getPersonIdentity(UUID.fromString(sessionHeader.get(HEADER_SESSION_ID)));
        verify(mockEventProbe).counterMetric(GET_QUESTION, 0d);
    }

    @Test
    void shouldReturn500ErrorWhenExperianServiceIsDown() throws IOException {

        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, UUID.randomUUID().toString());

        var personIdentityMock = mock(PersonIdentity.class);
        var kbvItem = new KBVItem();
        kbvItem.setSessionId(UUID.fromString(sessionHeader.get(HEADER_SESSION_ID)));

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockPersonIdentityService.getPersonIdentity(kbvItem.getSessionId()))
                .thenReturn(personIdentityMock);

        doThrow(RuntimeException.class)
                .when(spyKBVService)
                .getQuestions(any(QuestionRequest.class));

        setupEventProbeErrorBehaviour();
        APIGatewayProxyResponseEvent response =
                questionHandler.handleRequest(input, mock(Context.class));

        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(mockEventProbe).counterMetric(GET_QUESTION, 0d);
    }

    @Test
    void shouldReturn204WhenAGivenSessionHasReceivedFinalResponseFromExperian() throws IOException {

        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, UUID.randomUUID().toString());

        KBVItem kbvItem = new KBVItem();
        kbvItem.setSessionId(UUID.fromString(sessionHeader.get(HEADER_SESSION_ID)));
        kbvItem.setStatus("status-code");
        String questionState =
                "{\"qaPairs\":[{\"question\":{\"questionID\":\"Q00015\",\"text\":\"What is the outstanding balance of your current mortgage?\",\"tooltip\":\"The approximate amount in £s, including interest. A loan to buy property (or land) where the loan is secured by a charge on that property.\",\"answerHeldFlag\":null,\"answerFormat\":{\"identifier\":\"A00004\",\"fieldType\":\"G \",\"answerList\":[\"UP TO £10,000\",\"OVER £10,000 UP TO £35,000\",\"OVER £35,000 UP TO £60,000\",\"OVER £60,000 UP TO £85,000\",\"NONE OF THE ABOVE / DOES NOT APPLY\"]}},\"answer\":\"Answer given here\"},{\"question\":{\"questionID\":\"Q00040\",\"text\":\"How much was your recent loan for?\",\"tooltip\":\"The approximate starting balance, in £s, on an active personal loan. Does not include HP Loans, 2nd Mortgages or Home Credit.\",\"answerHeldFlag\":null,\"answerFormat\":{\"identifier\":\"A00004\",\"fieldType\":\"G \",\"answerList\":[\"UP TO £8,500\",\"OVER £8,500 UP TO £9,000\",\"OVER £9,000 UP TO £9,500\",\"OVER £9,500 UP TO £10,000\",\"NONE OF THE ABOVE / DOES NOT APPLY\"]}},\"answer\":\"another answer given\"}],\"nextQuestion\":{\"empty\":false,\"present\":true},\"answers\":[{\"questionId\":\"Q00040\",\"answer\":null},{\"questionId\":\"Q00015\",\"answer\":null}]}";
        kbvItem.setQuestionState(questionState);

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockKBVStorageService.getKBVItem(
                        UUID.fromString(sessionHeader.get(HEADER_SESSION_ID))))
                .thenReturn(kbvItem);

        APIGatewayProxyResponseEvent response =
                questionHandler.handleRequest(input, mock(Context.class));

        assertEquals(HttpStatusCode.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(mockEventProbe).counterMetric(GET_QUESTION);
    }

    private void setupEventProbeErrorBehaviour() {
        when(mockEventProbe.counterMetric(anyString(), anyDouble())).thenReturn(mockEventProbe);
        when(mockEventProbe.log(any(Level.class), any(Exception.class))).thenReturn(mockEventProbe);
    }
}
