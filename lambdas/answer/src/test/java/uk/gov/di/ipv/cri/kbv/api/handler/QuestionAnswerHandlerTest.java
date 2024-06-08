package uk.gov.di.ipv.cri.kbv.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import uk.gov.di.ipv.cri.common.library.exception.SqsException;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;
import uk.gov.di.ipv.cri.common.library.service.AuditService;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.service.SessionService;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestion;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestionAnswerSummary;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestionOptions;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvResult;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswer;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.gateway.KBVGateway;
import uk.gov.di.ipv.cri.kbv.api.service.KBVService;
import uk.gov.di.ipv.cri.kbv.api.service.KBVStorageService;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.common.library.domain.AuditEventType.REQUEST_SENT;
import static uk.gov.di.ipv.cri.common.library.domain.AuditEventType.RESPONSE_RECEIVED;

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
    @Mock private ConfigurationService mockConfigurationService;
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
                        mockConfigurationService,
                        mockAuditService);
    }

    @Test
    void shouldReturn200WithWhen1stAnswerIsSubmitted() throws JsonProcessingException {
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, SESSION_ID.toString());
        KBVItem kbvItem = new KBVItem();
        kbvItem.setSessionId(UUID.fromString(sessionHeader.get(HEADER_SESSION_ID)));

        KbvQuestion kbvQuestionOne = getQuestion("Q00015");
        KbvQuestion kbvQuestionTwo = getQuestion("Q00040");

        QuestionAnswer submitAnswerToFirstQuestion = new QuestionAnswer();
        submitAnswerToFirstQuestion.setQuestionId(kbvQuestionTwo.getQuestionId());
        submitAnswerToFirstQuestion.setAnswer("Answer One just about to be submitted");

        QuestionState questionState = new QuestionState();
        questionState.setQAPairs(new KbvQuestion[] {kbvQuestionOne, kbvQuestionTwo});

        SessionItem sessionItem = new SessionItem();
        sessionItem.setSessionId(UUID.fromString(sessionHeader.get(HEADER_SESSION_ID)));

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockObjectMapper.readValue(input.getBody(), QuestionAnswer.class))
                .thenReturn(submitAnswerToFirstQuestion);
        when(mockObjectMapper.readValue(kbvItem.getQuestionState(), QuestionState.class))
                .thenReturn(questionState);
        when(mockSessionService.validateSessionId(sessionHeader.get(HEADER_SESSION_ID)))
                .thenReturn(sessionItem);
        when(mockKBVStorageService.getKBVItem(SESSION_ID)).thenReturn(kbvItem);
        doNothing().when(mockKBVStorageService).update(kbvItem);

        APIGatewayProxyResponseEvent result =
                questionAnswerHandler.handleRequest(input, contextMock);

        assertEquals(HttpStatusCode.OK, result.getStatusCode());
        assertNull(result.getBody());

        verify(mockSessionService).validateSessionId(SESSION_ID_AS_STRING);
        verify(mockObjectMapper).readValue(input.getBody(), QuestionAnswer.class);
        verify(mockObjectMapper).readValue(kbvItem.getQuestionState(), QuestionState.class);
        verify(mockSessionService).validateSessionId(sessionHeader.get(HEADER_SESSION_ID));
        verify(mockKBVStorageService).getKBVItem(SESSION_ID);
        verify(mockKBVStorageService).update(kbvItem);
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

        when(mockConfigurationService.getVerifiableCredentialIssuer()).thenReturn("test-issuer");
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
        when(questionStateMock.hasAtLeastOneUnanswered()).thenReturn(false);
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
        for (var eventType : List.of(REQUEST_SENT, RESPONSE_RECEIVED)) {
            verify(mockAuditService)
                    .sendAuditEvent(
                            eq(eventType),
                            auditEventContextArgCaptor.capture(),
                            auditEventExtensionsArgCaptor.capture());
        }

        assertEquals(mockSessionItem, auditEventContextArgCaptor.getValue().getSessionItem());
        assertEquals(
                createRequestHeaders(), auditEventContextArgCaptor.getValue().getRequestHeaders());
        Map<?, ?> auditEventExtensionEntries =
                (Map<?, ?>) auditEventExtensionsArgCaptor.getValue().get("experianIiqResponse");
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
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, SESSION_ID.toString());
        KBVItem kbvItem = new KBVItem();
        kbvItem.setSessionId(UUID.fromString(sessionHeader.get(HEADER_SESSION_ID)));

        KbvQuestion kbvQuestionOne = getQuestion("Q00015");
        KbvQuestion kbvQuestionTwo = getQuestion("Q00040");

        QuestionAnswer existingAnsweredFirstQuestion = new QuestionAnswer();
        existingAnsweredFirstQuestion.setQuestionId(kbvQuestionOne.getQuestionId());
        existingAnsweredFirstQuestion.setAnswer("Answer One");

        QuestionAnswer submitAnswerToSecondQuestion = new QuestionAnswer();
        submitAnswerToSecondQuestion.setQuestionId(kbvQuestionTwo.getQuestionId());
        submitAnswerToSecondQuestion.setAnswer("Answer Two just about to be submitted");

        QuestionState questionState = new QuestionState();
        questionState.setQAPairs(new KbvQuestion[] {kbvQuestionOne, kbvQuestionTwo});
        questionState.setAnswer(submitAnswerToSecondQuestion);
        SessionItem sessionItem = new SessionItem();
        sessionItem.setSessionId(UUID.fromString(sessionHeader.get(HEADER_SESSION_ID)));
        QuestionAnswerRequest questionRequest = new QuestionAnswerRequest();
        questionRequest.setQuestionAnswers(
                List.of(existingAnsweredFirstQuestion, submitAnswerToSecondQuestion));
        QuestionsResponse questionsResponse = new QuestionsResponse();
        questionsResponse.setQuestions(
                new KbvQuestion[] {getQuestion("Q00045"), getQuestion("Q00067")});

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockObjectMapper.readValue(input.getBody(), QuestionAnswer.class))
                .thenReturn(submitAnswerToSecondQuestion);
        when(mockObjectMapper.readValue(kbvItem.getQuestionState(), QuestionState.class))
                .thenReturn(questionState);
        when(mockSessionService.validateSessionId(sessionHeader.get(HEADER_SESSION_ID)))
                .thenReturn(sessionItem);
        when(mockKBVStorageService.getKBVItem(SESSION_ID)).thenReturn(kbvItem);
        doNothing().when(mockKBVStorageService).update(kbvItem);

        APIGatewayProxyResponseEvent result =
                questionAnswerHandler.handleRequest(input, contextMock);

        assertEquals(HttpStatusCode.OK, result.getStatusCode());
        assertNull(result.getBody());

        verify(mockSessionService).validateSessionId(SESSION_ID_AS_STRING);
        verify(mockObjectMapper).readValue(input.getBody(), QuestionAnswer.class);
        verify(mockObjectMapper).readValue(kbvItem.getQuestionState(), QuestionState.class);
        verify(mockSessionService).validateSessionId(sessionHeader.get(HEADER_SESSION_ID));
        verify(mockKBVStorageService).getKBVItem(SESSION_ID);
        verify(mockKBVStorageService).update(kbvItem);
    }

    @Test
    @DisplayName(
            "it should replicate issue where a resubmission of a previously answered question results in IllegalStateException 'Question not found for questionID'")
    void shouldThrowIllegalStateExceptionOnResubmissionOfAlreadyAnsweredQuestions()
            throws JsonProcessingException {
        KBVItem kbvItem = new KBVItem();
        SessionItem sessionItem = new SessionItem();
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, SESSION_ID.toString());
        ArgumentCaptor<IllegalStateException> illegalStateExceptionArgumentCaptor =
                ArgumentCaptor.forClass(IllegalStateException.class);
        ArgumentCaptor<Level> levelArgumentCaptor = ArgumentCaptor.forClass(Level.class);
        sessionItem.setSessionId(UUID.fromString(sessionHeader.get(HEADER_SESSION_ID)));
        kbvItem.setSessionId(UUID.fromString(sessionHeader.get(HEADER_SESSION_ID)));


        QuestionAnswer existingAnsweredSecondQuestion = new QuestionAnswer();
        existingAnsweredSecondQuestion.setQuestionId("Q00040");
        existingAnsweredSecondQuestion.setAnswer("Answer two already answered");
        QuestionState questionState = new QuestionState();
        questionState.setQAPairs(new KbvQuestion[] {getQuestion("Q00015"), getQuestion("Q00040")});
        questionState.setQAPairs(new KbvQuestion[] {getQuestion("Q00045"), getQuestion("Q00067")});

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(input.getBody())
                .thenReturn(new ObjectMapper().writeValueAsString(existingAnsweredSecondQuestion));

        when(mockObjectMapper.readValue(input.getBody(), QuestionAnswer.class))
                .thenReturn(existingAnsweredSecondQuestion);
        when(mockObjectMapper.readValue(kbvItem.getQuestionState(), QuestionState.class))
                .thenReturn(questionState);
        when(mockSessionService.validateSessionId(sessionHeader.get(HEADER_SESSION_ID)))
                .thenReturn(sessionItem);
        when(mockKBVStorageService.getKBVItem(SESSION_ID)).thenReturn(kbvItem);
        when(mockEventProbe.counterMetric(anyString(), anyDouble())).thenReturn(mockEventProbe);
        when(mockEventProbe.log(any(Level.class), any(IllegalStateException.class)))
                .thenReturn(mockEventProbe);

        APIGatewayProxyResponseEvent response =
                questionAnswerHandler.handleRequest(input, contextMock);

        verify(mockEventProbe)
                .log(levelArgumentCaptor.capture(), illegalStateExceptionArgumentCaptor.capture());
        verify(mockEventProbe).counterMetric("post_answer", 0d);
        verify(mockSessionService).validateSessionId(SESSION_ID_AS_STRING);
        verify(mockObjectMapper).readValue(input.getBody(), QuestionAnswer.class);
        verify(mockObjectMapper).readValue(kbvItem.getQuestionState(), QuestionState.class);
        verify(mockSessionService).validateSessionId(sessionHeader.get(HEADER_SESSION_ID));
        verify(mockKBVStorageService).getKBVItem(SESSION_ID);

        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("{\"error\":\"Third Party Server error occurred.\"}", response.getBody());
        assertEquals(Level.ERROR, levelArgumentCaptor.getValue());
        assertEquals(
                "Question not found for questionID: Q00040",
                illegalStateExceptionArgumentCaptor.getValue().getMessage());
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

        when(mockConfigurationService.getVerifiableCredentialIssuer()).thenReturn("test-issuer");
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
    void shouldSendRequestSentAuditEventWhenExperianAPIIsDownNotSendAuditReceivedEvent()
            throws IOException, SqsException {
        KBVItem kbvItemMock = mock(KBVItem.class);
        SessionItem mockSessionItem = mock(SessionItem.class);
        QuestionState questionStateMock = mock(QuestionState.class);
        QuestionAnswer questionAnswerMock = mock(QuestionAnswer.class);

        when(mockConfigurationService.getVerifiableCredentialIssuer()).thenReturn("test-issuer");
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
        verify(mockAuditService)
                .sendAuditEvent(eq(REQUEST_SENT), any(AuditEventContext.class), any(Object.class));
        verify(mockAuditService, never())
                .sendAuditEvent(
                        eq(RESPONSE_RECEIVED), any(AuditEventContext.class), any(Object.class));
    }

    @Test
    void shouldReturn500ErrorWhenExperianServerReturnsAnError() throws JsonProcessingException {
        KBVItem kbvItemMock = mock(KBVItem.class);
        SessionItem mockSessionItem = mock(SessionItem.class);
        QuestionState questionStateMock = mock(QuestionState.class);
        QuestionAnswer questionAnswerMock = mock(QuestionAnswer.class);
        QuestionsResponse questionsResponseMock = mock(QuestionsResponse.class);

        when(mockConfigurationService.getVerifiableCredentialIssuer()).thenReturn("test-issuer");
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
        when(questionStateMock.hasAtLeastOneUnanswered()).thenReturn(false);

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

        when(mockConfigurationService.getVerifiableCredentialIssuer()).thenReturn("test-issuer");
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
                        eq(RESPONSE_RECEIVED), any(AuditEventContext.class), any(Object.class));
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
        KbvResult kbvResult = new KbvResult();
        kbvResult.setNextTransId(new String[] {"END"});
        kbvResult.setAuthenticationResult(authenticationResult);
        kbvResult.setAnswerSummary(kbvQuestionAnswerSummary);
        questionsResponse.setResults(kbvResult);
        return questionsResponse;
    }
}
