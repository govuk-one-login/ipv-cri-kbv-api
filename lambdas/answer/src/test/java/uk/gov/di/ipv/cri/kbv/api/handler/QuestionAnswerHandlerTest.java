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
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.services.dynamodb.model.InternalServerErrorException;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventType;
import uk.gov.di.ipv.cri.common.library.exception.SqsException;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;
import uk.gov.di.ipv.cri.common.library.service.AuditService;
import uk.gov.di.ipv.cri.common.library.service.SessionService;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswer;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.gateway.KBVGateway;
import uk.gov.di.ipv.cri.kbv.api.gateway.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.service.KBVService;
import uk.gov.di.ipv.cri.kbv.api.service.KBVStorageService;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionAnswerHandlerTest {

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

    private KBVService spyKBVService;

    @BeforeEach
    void setUp() {
        spyKBVService = Mockito.spy(new KBVService(mockKBVGateway));

        questionAnswerHandler =
                new QuestionAnswerHandler(
                        mockObjectMapper,
                        mockKBVStorageService,
                        spyKBVService,
                        mockEventProbe,
                        mockSessionService,
                        mockAuditService);
    }

    @Test
    void shouldReturn200WithWhen1stAnswerIsSubmitted() throws JsonProcessingException {
        KBVItem kbvItemMock = mock(KBVItem.class);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, UUID.randomUUID().toString());
        QuestionState questionStateMock = mock(QuestionState.class);

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockKBVStorageService.getKBVItem(
                        UUID.fromString(sessionHeader.get(HEADER_SESSION_ID))))
                .thenReturn(kbvItemMock);
        when(mockObjectMapper.readValue(kbvItemMock.getQuestionState(), QuestionState.class))
                .thenReturn(questionStateMock);
        when(input.getBody()).thenReturn(REQUEST_PAYLOAD);
        when(mockObjectMapper.writeValueAsString(questionStateMock)).thenReturn("question-state");
        doNothing().when(mockKBVStorageService).update(kbvItemMock);

        when(questionStateMock.hasAtLeastOneUnAnswered()).thenReturn(true);

        APIGatewayProxyResponseEvent result =
                questionAnswerHandler.handleRequest(input, contextMock);

        verify(questionStateMock).setAnswer(any());
        assertEquals(HttpStatusCode.OK, result.getStatusCode());
        assertNull(result.getBody());
    }

    @Test
    void shouldReturn200WithFinalResponseFromExperianAPI() throws IOException, SqsException {
        KBVItem kbvItemMock = mock(KBVItem.class);
        QuestionState questionStateMock = mock(QuestionState.class);
        QuestionAnswer questionAnswerMock = mock(QuestionAnswer.class);

        String mockUUID = UUID.randomUUID().toString();
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, mockUUID);
        QuestionsResponse questionsResponseMock = mock(QuestionsResponse.class);

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(kbvItemMock.getSessionId())
                .thenReturn(UUID.fromString(sessionHeader.get(HEADER_SESSION_ID)));
        when(mockKBVStorageService.getKBVItem(
                        UUID.fromString(sessionHeader.get(HEADER_SESSION_ID))))
                .thenReturn(kbvItemMock);

        when(mockObjectMapper.readValue(kbvItemMock.getQuestionState(), QuestionState.class))
                .thenReturn(questionStateMock);
        when(input.getBody()).thenReturn(REQUEST_PAYLOAD);
        when(mockObjectMapper.readValue(REQUEST_PAYLOAD, QuestionAnswer.class))
                .thenReturn(questionAnswerMock);
        when(questionStateMock.hasAtLeastOneUnAnswered()).thenReturn(false);

        when(mockKBVGateway.submitAnswers(any())).thenReturn(questionsResponseMock);

        when(mockObjectMapper.writeValueAsString(any())).thenReturn("question-response");

        when(questionsResponseMock.hasQuestions()).thenReturn(false);
        when(questionsResponseMock.hasQuestionRequestEnded()).thenReturn(true);

        SessionItem mockSessionItem = mock(SessionItem.class);
        when(mockSessionService.getSession(String.valueOf(kbvItemMock.getSessionId())))
                .thenReturn(mockSessionItem);
        doNothing().when(mockSessionService).createAuthorizationCode(mockSessionItem);

        APIGatewayProxyResponseEvent result =
                questionAnswerHandler.handleRequest(input, contextMock);

        verify(mockKBVStorageService, times(2)).update(kbvItemMock);
        verify(mockSessionService).getSession(String.valueOf(kbvItemMock.getSessionId()));
        verify(mockSessionService).createAuthorizationCode(mockSessionItem);
        assertEquals(HttpStatusCode.OK, result.getStatusCode());
        assertNull(result.getBody());
        verify(mockAuditService).sendAuditEvent(AuditEventType.THIRD_PARTY_REQUEST_ENDED);
    }

    @Test
    void shouldReturn200WhenNextSetOfQuestionsAreReceivedFromExperian() throws IOException {
        KBVItem kbvItemMock = mock(KBVItem.class);
        QuestionState questionStateMock = mock(QuestionState.class);
        QuestionAnswer questionAnswerMock = mock(QuestionAnswer.class);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, UUID.randomUUID().toString());

        QuestionsResponse questionsResponseMock = mock(QuestionsResponse.class);

        when(input.getHeaders()).thenReturn(sessionHeader);

        when(mockKBVStorageService.getKBVItem(
                        UUID.fromString(sessionHeader.get(HEADER_SESSION_ID))))
                .thenReturn(kbvItemMock);

        when(mockObjectMapper.readValue(kbvItemMock.getQuestionState(), QuestionState.class))
                .thenReturn(questionStateMock);
        when(input.getBody()).thenReturn(REQUEST_PAYLOAD);
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
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, UUID.randomUUID().toString());

        when(input.getHeaders()).thenReturn(sessionHeader);
        doThrow(InternalServerErrorException.class)
                .when(mockKBVStorageService)
                .getKBVItem(UUID.fromString(sessionHeader.get(HEADER_SESSION_ID)));

        setupEventProbeErrorBehaviour();
        APIGatewayProxyResponseEvent response =
                questionAnswerHandler.handleRequest(input, contextMock);

        assertEquals("{ \"error\":\"AWS Server error occurred.\" }", response.getBody());
        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(mockEventProbe).counterMetric("post_answer", 0d);
    }

    @Test
    void shouldReturn400ErrorWhenNoSessionIdProvided() {
        setupEventProbeErrorBehaviour();
        APIGatewayProxyResponseEvent response =
                questionAnswerHandler.handleRequest(input, contextMock);

        assertEquals("{ \"error\":\"Error finding the requested resource.\" }", response.getBody());

        assertEquals(HttpStatusCode.BAD_REQUEST, response.getStatusCode());
        verify(mockEventProbe).counterMetric("post_answer", 0d);
    }

    @Test
    void shouldReturn500ErrorWhenQuestionStateCannotBeParsedToJSON() throws IOException {
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, UUID.randomUUID().toString());
        KBVItem kbvItemMock = mock(KBVItem.class);

        when(input.getHeaders()).thenReturn(sessionHeader);

        when(mockKBVStorageService.getKBVItem(
                        UUID.fromString(sessionHeader.get(HEADER_SESSION_ID))))
                .thenReturn(kbvItemMock);
        when(mockObjectMapper.readValue(kbvItemMock.getQuestionState(), QuestionState.class))
                .thenThrow(JsonProcessingException.class);
        setupEventProbeErrorBehaviour();
        APIGatewayProxyResponseEvent response =
                questionAnswerHandler.handleRequest(input, contextMock);

        assertEquals(
                "{ \"error\":\"Failed to parse object using ObjectMapper.\" }", response.getBody());
        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(mockEventProbe).counterMetric("post_answer", 0d);
    }

    @Test
    void shouldReturn500ErrorWhenExperianAPIIsDown() throws IOException {
        KBVItem kbvItemMock = mock(KBVItem.class);
        QuestionState questionStateMock = mock(QuestionState.class);

        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, UUID.randomUUID().toString());
        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockKBVStorageService.getKBVItem(
                        UUID.fromString(sessionHeader.get(HEADER_SESSION_ID))))
                .thenReturn(kbvItemMock);

        QuestionAnswer questionAnswerMock = mock(QuestionAnswer.class);

        when(mockObjectMapper.readValue(kbvItemMock.getQuestionState(), QuestionState.class))
                .thenReturn(questionStateMock);
        when(input.getBody()).thenReturn(REQUEST_PAYLOAD);
        when(mockObjectMapper.readValue(REQUEST_PAYLOAD, QuestionAnswer.class))
                .thenReturn(questionAnswerMock);

        when(mockObjectMapper.writeValueAsString(questionStateMock)).thenReturn("question-state");

        when(mockKBVGateway.submitAnswers(any())).thenThrow(InternalServerErrorException.class);

        setupEventProbeErrorBehaviour();
        APIGatewayProxyResponseEvent response =
                questionAnswerHandler.handleRequest(input, mock(Context.class));

        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(mockEventProbe).counterMetric("post_answer", 0d);
    }

    private void setupEventProbeErrorBehaviour() {
        when(mockEventProbe.counterMetric(anyString(), anyDouble())).thenReturn(mockEventProbe);
        when(mockEventProbe.log(any(Level.class), any(Exception.class))).thenReturn(mockEventProbe);
    }
}
