package uk.gov.di.ipv.cri.kbv.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.services.dynamodb.model.InternalServerErrorException;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventContext;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventType;
import uk.gov.di.ipv.cri.common.library.exception.SqsException;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;
import uk.gov.di.ipv.cri.common.library.service.AuditService;
import uk.gov.di.ipv.cri.common.library.service.SessionService;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestion;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestionAnswerSummary;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestionOptions;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvResult;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswer;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.gateway.KBVGateway;
import uk.gov.di.ipv.cri.kbv.api.service.KBVService;
import uk.gov.di.ipv.cri.kbv.api.service.KBVStorageService;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionAnswerHandlerTest {

    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final String SESSION_ID_AS_STRING = String.valueOf(SESSION_ID);
    private static final String HEADER_SESSION_ID = "session-id";
    public static final String REQUEST_PAYLOAD =
            "\"questionID\":\" Q0008 \",\"answer\":\" some-answer \"";
    private QuestionAnswerHandler questionAnswerHandler;
    @Mock private ObjectMapper mockObjectMapper;
    @Mock private KBVStorageService mockKBVStorageService;
    @Mock private APIGatewayProxyRequestEvent input;
    @Mock private Context contextMock;
    @Mock private EventProbe mockEventProbe;
    @Mock private SessionService mockSessionService;
    @Mock private AuditService mockAuditService;
    @Mock private KBVGateway mockKBVGateway;
    @Captor private ArgumentCaptor<Map<String, Object>> auditEventExtensionsArgCaptor;

    @BeforeEach
    void setUp() {
        questionAnswerHandler =
                new QuestionAnswerHandler(
                        mockObjectMapper,
                        mockKBVStorageService,
                        Mockito.spy(new KBVService(mockKBVGateway)),
                        mockEventProbe,
                        mockSessionService,
                        mockAuditService);
    }

    @Test
    void shouldReturn200WithWhen1stAnswerIsSubmitted() throws JsonProcessingException {
        KBVItem kbvItemMock = mock(KBVItem.class);
        SessionItem sessionItem = mock(SessionItem.class);
        QuestionState questionStateMock = mock(QuestionState.class);

        when(input.getHeaders()).thenReturn(createRequestHeaders());
        when(input.getBody()).thenReturn(REQUEST_PAYLOAD);
        when(sessionItem.getSessionId()).thenReturn(SESSION_ID);
        when(mockSessionService.validateSessionId(SESSION_ID_AS_STRING)).thenReturn(sessionItem);
        when(mockKBVStorageService.getKBVItem(SESSION_ID)).thenReturn(kbvItemMock);
        when(mockObjectMapper.readValue(kbvItemMock.getQuestionState(), QuestionState.class))
                .thenReturn(questionStateMock);
        when(mockObjectMapper.writeValueAsString(questionStateMock)).thenReturn("question-state");
        doNothing().when(mockKBVStorageService).update(kbvItemMock);
        when(questionStateMock.hasAtLeastOneUnAnswered()).thenReturn(true);

        APIGatewayProxyResponseEvent result =
                questionAnswerHandler.handleRequest(input, contextMock);

        verify(questionStateMock).setAnswer(any());
        verify(mockSessionService).validateSessionId(SESSION_ID_AS_STRING);
        assertEquals(HttpStatusCode.OK, result.getStatusCode());
        assertNull(result.getBody());
    }

    @Test
    void shouldReturn200WithFinalResponseFromExperianAPI() throws IOException, SqsException {
        ArgumentCaptor<AuditEventContext> auditEventContextArgCaptor =
                ArgumentCaptor.forClass(AuditEventContext.class);
        KBVItem kbvItemMock = mock(KBVItem.class);
        SessionItem mockSessionItem = mock(SessionItem.class);
        QuestionState questionStateMock = mock(QuestionState.class);
        QuestionAnswer questionAnswerMock = mock(QuestionAnswer.class);
        QuestionsResponse questionsResponseMock = mock(QuestionsResponse.class);
        KbvResult resultsMock = mock(KbvResult.class);
        KbvQuestionAnswerSummary mockAnswerSummary = mock(KbvQuestionAnswerSummary.class);

        String responseStatus = "AUTHORISED";
        int totalQuestionsAsked = 4;
        int totalCorrectAnswers = 3;
        int totalIncorrectAnswers = 1;

        when(input.getHeaders()).thenReturn(createRequestHeaders());
        when(input.getBody()).thenReturn(REQUEST_PAYLOAD);
        when(mockSessionItem.getSessionId()).thenReturn(SESSION_ID);
        when(mockSessionService.validateSessionId(SESSION_ID_AS_STRING))
                .thenReturn(mockSessionItem);
        when(mockKBVStorageService.getKBVItem(SESSION_ID)).thenReturn(kbvItemMock);
        when(mockObjectMapper.readValue(kbvItemMock.getQuestionState(), QuestionState.class))
                .thenReturn(questionStateMock);
        when(mockObjectMapper.readValue(REQUEST_PAYLOAD, QuestionAnswer.class))
                .thenReturn(questionAnswerMock);
        when(questionStateMock.hasAtLeastOneUnAnswered()).thenReturn(false);
        when(resultsMock.getAnswerSummary()).thenReturn(mockAnswerSummary);
        when(questionsResponseMock.getResults()).thenReturn(resultsMock);
        when(mockAnswerSummary.getQuestionsAsked()).thenReturn(totalQuestionsAsked);
        when(mockAnswerSummary.getAnsweredCorrect()).thenReturn(totalCorrectAnswers);
        when(mockAnswerSummary.getAnsweredIncorrect()).thenReturn(totalIncorrectAnswers);
        when(mockKBVGateway.submitAnswers(any())).thenReturn(questionsResponseMock);
        when(mockObjectMapper.writeValueAsString(any())).thenReturn("question-response");
        when(questionsResponseMock.hasQuestions()).thenReturn(false);
        when(questionsResponseMock.hasQuestionRequestEnded()).thenReturn(true);
        when(questionsResponseMock.getStatus()).thenReturn(responseStatus);

        APIGatewayProxyResponseEvent result =
                questionAnswerHandler.handleRequest(input, contextMock);

        verify(mockKBVStorageService, times(2)).update(kbvItemMock);
        verify(mockSessionService).validateSessionId(SESSION_ID_AS_STRING);
        verify(mockSessionService).createAuthorizationCode(mockSessionItem);
        verify(mockAuditService)
                .sendAuditEvent(
                        eq(AuditEventType.THIRD_PARTY_REQUEST_ENDED),
                        auditEventContextArgCaptor.capture(),
                        auditEventExtensionsArgCaptor.capture());
        assertEquals(mockSessionItem, auditEventContextArgCaptor.getValue().getSessionItem());
        assertEquals(
                createRequestHeaders(), auditEventContextArgCaptor.getValue().getRequestHeaders());
        Map<String, Object> auditEventExtensionEntries =
                (Map<String, Object>)
                        auditEventExtensionsArgCaptor.getValue().get("experianIiqResponse");
        assertNotNull(auditEventExtensionEntries);
        assertEquals(responseStatus, auditEventExtensionEntries.get("outcome"));
        assertEquals(totalQuestionsAsked, auditEventExtensionEntries.get("totalQuestionsAsked"));
        assertEquals(
                totalCorrectAnswers,
                auditEventExtensionEntries.get("totalQuestionsAnsweredCorrect"));
        assertEquals(
                totalIncorrectAnswers,
                auditEventExtensionEntries.get("totalQuestionsAnsweredIncorrect"));
        assertEquals(HttpStatusCode.OK, result.getStatusCode());
        assertNull(result.getBody());
    }

    @Test
    void shouldReturn200WhenNextSetOfQuestionsAreReceivedFromExperian() throws IOException {
        KBVItem kbvItemMock = mock(KBVItem.class);
        SessionItem mockSessionItem = mock(SessionItem.class);
        QuestionState questionStateMock = mock(QuestionState.class);
        QuestionAnswer questionAnswerMock = mock(QuestionAnswer.class);
        QuestionsResponse questionsResponseMock = mock(QuestionsResponse.class);

        when(input.getHeaders()).thenReturn(createRequestHeaders());
        when(input.getBody()).thenReturn(REQUEST_PAYLOAD);
        when(mockSessionItem.getSessionId()).thenReturn(SESSION_ID);
        when(mockSessionService.validateSessionId(SESSION_ID_AS_STRING))
                .thenReturn(mockSessionItem);
        when(mockKBVStorageService.getKBVItem(SESSION_ID)).thenReturn(kbvItemMock);
        when(mockObjectMapper.readValue(kbvItemMock.getQuestionState(), QuestionState.class))
                .thenReturn(questionStateMock);
        when(mockObjectMapper.readValue(REQUEST_PAYLOAD, QuestionAnswer.class))
                .thenReturn(questionAnswerMock);
        when(mockObjectMapper.writeValueAsString(questionStateMock)).thenReturn("question-state");
        when(questionStateMock.hasAtLeastOneUnAnswered()).thenReturn(false);
        when(mockKBVGateway.submitAnswers(any())).thenReturn(questionsResponseMock);
        when(questionsResponseMock.hasQuestions()).thenReturn(true);
        doNothing().when(questionStateMock).setQAPairs(any());
        when(mockObjectMapper.writeValueAsString(questionStateMock)).thenReturn("question-state");

        APIGatewayProxyResponseEvent result =
                questionAnswerHandler.handleRequest(input, contextMock);

        verify(mockKBVStorageService, times(2)).update(kbvItemMock);
        assertEquals(HttpStatusCode.OK, result.getStatusCode());
        assertNull(result.getBody());
    }

    @Test
    void shouldReturn500ErrorWhenAWSDynamoDBServiceDown() {
        SessionItem mockSessionItem = mock(SessionItem.class);
        when(input.getHeaders()).thenReturn(createRequestHeaders());
        when(mockSessionItem.getSessionId()).thenReturn(SESSION_ID);
        when(mockSessionService.validateSessionId(SESSION_ID_AS_STRING))
                .thenReturn(mockSessionItem);
        doThrow(InternalServerErrorException.class)
                .when(mockKBVStorageService)
                .getKBVItem(SESSION_ID);
        setupMockEventProbe();

        APIGatewayProxyResponseEvent response =
                questionAnswerHandler.handleRequest(input, contextMock);

        assertEquals("{\"error\":\"AWS Server error occurred.\"}", response.getBody());
        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(mockEventProbe).counterMetric("post_answer", 0d);
    }

    @Test
    void shouldReturn400ErrorWhenNoSessionIdProvided() {
        setupMockEventProbe();
        APIGatewayProxyResponseEvent response =
                questionAnswerHandler.handleRequest(input, contextMock);

        assertEquals("{\"error\":\"Error finding the requested resource.\"}", response.getBody());
        assertEquals(HttpStatusCode.BAD_REQUEST, response.getStatusCode());
        verify(mockEventProbe).counterMetric("post_answer", 0d);
    }

    @Test
    void shouldReturn500ErrorWhenQuestionStateCannotBeParsedToJSON() throws IOException {
        KBVItem kbvItemMock = mock(KBVItem.class);
        SessionItem mockSessionItem = mock(SessionItem.class);

        when(input.getHeaders()).thenReturn(createRequestHeaders());
        when(mockSessionItem.getSessionId()).thenReturn(SESSION_ID);
        when(mockSessionService.validateSessionId(SESSION_ID_AS_STRING))
                .thenReturn(mockSessionItem);
        when(mockKBVStorageService.getKBVItem(SESSION_ID)).thenReturn(kbvItemMock);
        when(mockObjectMapper.readValue(kbvItemMock.getQuestionState(), QuestionState.class))
                .thenThrow(JsonProcessingException.class);
        setupMockEventProbe();

        APIGatewayProxyResponseEvent response =
                questionAnswerHandler.handleRequest(input, contextMock);

        assertEquals(
                "{\"error\":\"Failed to parse object using ObjectMapper.\"}", response.getBody());
        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(mockEventProbe).counterMetric("post_answer", 0d);
    }

    @Test
    void shouldReturn500ErrorWhenExperianAPIIsDown() throws IOException {
        KBVItem kbvItemMock = mock(KBVItem.class);
        SessionItem mockSessionItem = mock(SessionItem.class);
        QuestionState questionStateMock = mock(QuestionState.class);
        QuestionAnswer questionAnswerMock = mock(QuestionAnswer.class);

        when(input.getHeaders()).thenReturn(createRequestHeaders());
        when(input.getBody()).thenReturn(REQUEST_PAYLOAD);
        when(mockSessionItem.getSessionId()).thenReturn(SESSION_ID);
        when(mockSessionService.validateSessionId(SESSION_ID_AS_STRING))
                .thenReturn(mockSessionItem);
        when(mockKBVStorageService.getKBVItem(SESSION_ID)).thenReturn(kbvItemMock);
        when(mockObjectMapper.readValue(kbvItemMock.getQuestionState(), QuestionState.class))
                .thenReturn(questionStateMock);
        when(mockObjectMapper.readValue(REQUEST_PAYLOAD, QuestionAnswer.class))
                .thenReturn(questionAnswerMock);
        when(mockObjectMapper.writeValueAsString(questionStateMock)).thenReturn("question-state");
        when(mockKBVGateway.submitAnswers(any())).thenThrow(InternalServerErrorException.class);
        setupMockEventProbe();

        APIGatewayProxyResponseEvent response =
                questionAnswerHandler.handleRequest(input, mock(Context.class));

        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(mockEventProbe).counterMetric("post_answer", 0d);
    }

    @Test
    void shouldReturn500ErrorWhenExperianServerReturnsAnError() throws JsonProcessingException {
        KBVItem kbvItemMock = mock(KBVItem.class);
        SessionItem mockSessionItem = mock(SessionItem.class);
        QuestionState questionStateMock = mock(QuestionState.class);
        QuestionAnswer questionAnswerMock = mock(QuestionAnswer.class);
        QuestionsResponse questionsResponseMock = mock(QuestionsResponse.class);

        when(input.getHeaders()).thenReturn(createRequestHeaders());
        when(input.getBody()).thenReturn(REQUEST_PAYLOAD);
        when(mockSessionItem.getSessionId()).thenReturn(SESSION_ID);
        when(mockSessionService.validateSessionId(SESSION_ID_AS_STRING))
                .thenReturn(mockSessionItem);
        when(mockKBVStorageService.getKBVItem(SESSION_ID)).thenReturn(kbvItemMock);
        when(mockObjectMapper.readValue(kbvItemMock.getQuestionState(), QuestionState.class))
                .thenReturn(questionStateMock);
        when(mockObjectMapper.readValue(REQUEST_PAYLOAD, QuestionAnswer.class))
                .thenReturn(questionAnswerMock);
        when(questionStateMock.hasAtLeastOneUnAnswered()).thenReturn(false);
        when(mockKBVGateway.submitAnswers(any())).thenReturn(questionsResponseMock);
        when(mockObjectMapper.writeValueAsString(any())).thenReturn("question-response");
        when(questionsResponseMock.getResults()).thenReturn(null);
        when(questionsResponseMock.hasError()).thenReturn(Boolean.TRUE);
        when(questionsResponseMock.getErrorMessage())
                .thenReturn("Third Party Server error occurred.");
        when(mockObjectMapper.writeValueAsString(questionStateMock))
                .thenReturn("question-state-mock-string");
        doNothing().when(mockKBVStorageService).update(any());
        setupMockEventProbe();

        APIGatewayProxyResponseEvent response =
                questionAnswerHandler.handleRequest(input, contextMock);

        assertEquals("{\"error\":\"Third Party Server error occurred.\"}", response.getBody());
        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(mockEventProbe).counterMetric("post_answer", 0d);
    }

    @ParameterizedTest(
            name =
                    "{index} => authenticationResult={0}, answeredCorrectly={1}, answeredInCorrectly={2}, totalQuestionsAsked={3}")
    @CsvSource({"Authenticated, 3, 1, 4", "Not Authenticated, 2, 2, 4"})
    void shouldReturnOkWhenFinalExperianResponseIncludesAQuestionAnswerSummary(
            String authenticationResult,
            int answeredCorrectly,
            int answeredInCorrectly,
            int totalQuestionsAsked)
            throws JsonProcessingException, SqsException {
        KbvQuestion[] kbvQuestions =
                new KbvQuestion[] {getQuestion("First"), getQuestion("Second")};
        QuestionAnswer questionAnswerOne = new QuestionAnswer();
        QuestionAnswer questionAnswerTwo = new QuestionAnswer();
        questionAnswerOne.setQuestionId("First");
        questionAnswerTwo.setQuestionId("Second");

        QuestionAnswer[] questionAnswers =
                new QuestionAnswer[] {questionAnswerOne, questionAnswerTwo};

        KBVItem kbvItem = getKbvItem(getQuestionStateWithAnswer(kbvQuestions, questionAnswers));
        QuestionState questionState = getQuestionStateWithAnswer(kbvQuestions, questionAnswers);

        String questionAnswerTwoAsString = new ObjectMapper().writeValueAsString(questionAnswerTwo);
        SessionItem mockSessionItem = mock(SessionItem.class);
        when(input.getHeaders()).thenReturn(createRequestHeaders());
        when(input.getBody()).thenReturn(questionAnswerTwoAsString);
        when(mockSessionItem.getSessionId()).thenReturn(SESSION_ID);
        when(mockSessionService.validateSessionId(SESSION_ID_AS_STRING))
                .thenReturn(mockSessionItem);
        when(mockKBVStorageService.getKBVItem(SESSION_ID)).thenReturn(kbvItem);
        when(mockObjectMapper.readValue(kbvItem.getQuestionState(), QuestionState.class))
                .thenReturn(questionState);
        when(mockObjectMapper.readValue(questionAnswerTwoAsString, QuestionAnswer.class))
                .thenReturn(questionAnswerTwo);
        when(mockKBVGateway.submitAnswers(any()))
                .thenReturn(
                        getQuestionResponseWithResults(
                                authenticationResult,
                                getKbvQuestionAnswerSummary(
                                        answeredCorrectly,
                                        answeredInCorrectly,
                                        totalQuestionsAsked)));

        APIGatewayProxyResponseEvent result =
                questionAnswerHandler.handleRequest(input, contextMock);

        verify(mockKBVStorageService, times(2)).update(kbvItem);
        verify(mockSessionService).validateSessionId(SESSION_ID_AS_STRING);
        verify(mockSessionService).createAuthorizationCode(mockSessionItem);
        verify(mockAuditService)
                .sendAuditEvent(
                        eq(AuditEventType.THIRD_PARTY_REQUEST_ENDED),
                        any(AuditEventContext.class),
                        any(Object.class));

        assertAll(
                () -> assertNotNull(kbvItem.getQuestionAnswerResultSummary()),
                () -> assertEquals(authenticationResult, kbvItem.getStatus()),
                () ->
                        assertEquals(
                                totalQuestionsAsked,
                                kbvItem.getQuestionAnswerResultSummary().getQuestionsAsked()),
                () ->
                        assertEquals(
                                answeredCorrectly,
                                kbvItem.getQuestionAnswerResultSummary().getAnsweredCorrect()),
                () ->
                        assertEquals(
                                answeredInCorrectly,
                                kbvItem.getQuestionAnswerResultSummary().getAnsweredIncorrect()));

        assertEquals(HttpStatusCode.OK, result.getStatusCode());
        assertNull(result.getBody());
    }

    private void setupMockEventProbe() {
        when(mockEventProbe.counterMetric(anyString(), anyDouble())).thenReturn(mockEventProbe);
        when(mockEventProbe.log(any(Level.class), any(Exception.class))).thenReturn(mockEventProbe);
    }

    private Map<String, String> createRequestHeaders() {
        return Map.of(HEADER_SESSION_ID, SESSION_ID_AS_STRING);
    }

    private QuestionState getQuestionStateWithAnswer(
            KbvQuestion[] kbvQuestions, QuestionAnswer[] questionAnswers) {
        QuestionState questionState = getQuestionState(kbvQuestions);

        for (KbvQuestion question : kbvQuestions) {
            Optional<QuestionAnswer> questionAnswer =
                    Arrays.stream(questionAnswers)
                            .filter(qa -> qa.getQuestionId().equals(question.getQuestionId()))
                            .findFirst();
            questionAnswer.ifPresent(
                    ans -> {
                        ans.setQuestionId(question.getQuestionId());
                        ans.setAnswer(String.format("%s Answer", question.getQuestionId()));
                        questionState.setAnswer(ans);
                    });
        }
        return questionState;
    }

    private QuestionState getQuestionState(KbvQuestion[] kbvQuestions) {
        QuestionState questionState = new QuestionState();
        questionState.setQAPairs(kbvQuestions);
        return questionState;
    }

    private KbvQuestionAnswerSummary getKbvQuestionAnswerSummary(
            int answeredCorrect, int answeredIncorrect, int totalQuestionsAsked) {
        KbvQuestionAnswerSummary kbvQuestionAnswerSummary = new KbvQuestionAnswerSummary();
        kbvQuestionAnswerSummary.setAnsweredCorrect(answeredCorrect);
        kbvQuestionAnswerSummary.setAnsweredIncorrect(answeredIncorrect);
        kbvQuestionAnswerSummary.setQuestionsAsked(totalQuestionsAsked);
        return kbvQuestionAnswerSummary;
    }

    private KBVItem getKbvItem(QuestionState questionState) throws JsonProcessingException {
        KBVItem kbvItem = new KBVItem();
        kbvItem.setQuestionState(new ObjectMapper().writeValueAsString(questionState));
        return kbvItem;
    }

    private KbvQuestion getQuestion(String questionId) {
        KbvQuestionOptions questionOptions = new KbvQuestionOptions();
        questionOptions.setIdentifier(questionId);
        questionOptions.setFieldType("G");

        KbvQuestion question = new KbvQuestion();
        question.setQuestionId(questionId);
        question.setQuestionOptions(questionOptions);

        return question;
    }

    private QuestionsResponse getQuestionResponseWithResults(
            String authenticationResult, KbvQuestionAnswerSummary kbvQuestionAnswerSummary) {
        QuestionsResponse questionsResponse = new QuestionsResponse();
        KbvResult kbvResult = getKbvResult("END");
        kbvResult.setAuthenticationResult(authenticationResult);
        kbvResult.setAnswerSummary(kbvQuestionAnswerSummary);
        questionsResponse.setResults(kbvResult);
        return questionsResponse;
    }

    private KbvResult getKbvResult(String transactionValue) {
        KbvResult kbvResult = new KbvResult();
        kbvResult.setNextTransId(new String[] {transactionValue});
        return kbvResult;
    }
}
