package uk.gov.di.ipv.cri.kbv.api.handler;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.amazonaws.services.dynamodbv2.model.InternalServerErrorException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.strategy.LogErrorContextMissingStrategy;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.gov.di.ipv.cri.kbv.api.domain.Control;
import uk.gov.di.ipv.cri.kbv.api.domain.Question;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswer;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerPair;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.persistence.item.KBVSessionItem;
import uk.gov.di.ipv.cri.kbv.api.service.ExperianService;
import uk.gov.di.ipv.cri.kbv.api.service.StorageService;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.kbv.api.handler.QuestionAnswerHandler.HEADER_SESSION_ID;

@ExtendWith(MockitoExtension.class)
public class QuestionAnswerHandlerTest {

    public static final String REQUEST_PAYLOAD =
            "\"questionID\":\" Q0008 \",\"answer\":\" some-answer \"";
    public static final String QUESTION_ID = "questionID";
    public static final String SECOND_QUESTION = "second-question";
    public static final String ANSWER = "answer";
    private QuestionAnswerHandler questionAnswerHandler;
    @Mock private ObjectMapper mockObjectMapper;
    @Mock private StorageService mockStorageService;
    @Mock private ExperianService mockExperianService;
    @Mock private Appender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {

        AWSXRay.setGlobalRecorder(
                AWSXRayRecorderBuilder.standard()
                        .withContextMissingStrategy(new LogErrorContextMissingStrategy())
                        .build());
        Logger logger = (Logger) LoggerFactory.getLogger(QuestionAnswerHandler.class);
        logger.addAppender(appender);

        questionAnswerHandler =
                new QuestionAnswerHandler(
                        mockObjectMapper, mockStorageService, mockExperianService);
    }

    @Test
    void shouldReturn200WithNextQuestionWhen1stAnswerIsSubmitted() throws JsonProcessingException {
        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        Context contextMock = mock(Context.class);
        KBVSessionItem kbvSessionItemMock = mock(KBVSessionItem.class);
        QuestionState questionStateMock = mock(QuestionState.class);
        Control controlMock = mock(Control.class);
        when(controlMock.getAuthRefNo()).thenReturn("some-auth-ref");
        when(controlMock.getURN()).thenReturn("some-urn");

        when(questionStateMock.getControl()).thenReturn(controlMock);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, "new-session-id");

        QuestionAnswer questionAnswerMock = mock(QuestionAnswer.class);
        when(questionAnswerMock.getQuestionId()).thenReturn(QUESTION_ID);
        when(questionAnswerMock.getAnswer()).thenReturn(ANSWER);

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockStorageService.getSessionId(sessionHeader.get(HEADER_SESSION_ID)))
                .thenReturn(Optional.ofNullable(kbvSessionItemMock));

        when(mockObjectMapper.readValue(kbvSessionItemMock.getQuestionState(), QuestionState.class))
                .thenReturn(questionStateMock);

        when(input.getBody()).thenReturn(REQUEST_PAYLOAD);
        when(mockObjectMapper.readValue(REQUEST_PAYLOAD, QuestionAnswer.class))
                .thenReturn(questionAnswerMock);

        Question questionMock1 = mock(Question.class);
        when(questionMock1.getQuestionID()).thenReturn(QUESTION_ID);

        QuestionAnswerPair questionAnswerPairMock1 = mock(QuestionAnswerPair.class);
        when(questionAnswerPairMock1.getQuestion()).thenReturn(questionMock1);

        Question questionMock2 = mock(Question.class);
        QuestionAnswerPair questionAnswerPairMock2 = mock(QuestionAnswerPair.class);
        when(questionStateMock.getQaPairs())
                .thenReturn(List.of(questionAnswerPairMock1, questionAnswerPairMock2));

        when(mockObjectMapper.writeValueAsString(questionStateMock)).thenReturn("question-state");
        doNothing().when(mockStorageService).update(kbvSessionItemMock);
        when(questionStateMock.getNextQuestion()).thenReturn(Optional.of(questionMock2));
        when(mockObjectMapper.writeValueAsString(questionMock2)).thenReturn(SECOND_QUESTION);

        APIGatewayProxyResponseEvent result =
                questionAnswerHandler.handleRequest(input, contextMock);

        assertEquals(HttpStatus.SC_OK, result.getStatusCode());
        assertEquals(SECOND_QUESTION, result.getBody());
    }

    @Test
    void shouldReturn200WithFinalResponseFromExperianAPI()
            throws IOException, InterruptedException {
        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        Context contextMock = mock(Context.class);
        KBVSessionItem kbvSessionItemMock = mock(KBVSessionItem.class);
        QuestionState questionStateMock = mock(QuestionState.class);
        Control controlMock = mock(Control.class);
        when(controlMock.getAuthRefNo()).thenReturn("some-auth-ref");
        when(controlMock.getURN()).thenReturn("some-urn");

        when(questionStateMock.getControl()).thenReturn(controlMock);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, "new-session-id");

        QuestionAnswer questionAnswerMock = mock(QuestionAnswer.class);

        when(questionAnswerMock.getQuestionId()).thenReturn(QUESTION_ID);
        when(questionAnswerMock.getAnswer()).thenReturn(ANSWER);

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockStorageService.getSessionId(sessionHeader.get(HEADER_SESSION_ID)))
                .thenReturn(Optional.ofNullable(kbvSessionItemMock));

        when(mockObjectMapper.readValue(kbvSessionItemMock.getQuestionState(), QuestionState.class))
                .thenReturn(questionStateMock);

        when(input.getBody()).thenReturn(REQUEST_PAYLOAD);
        when(mockObjectMapper.readValue(REQUEST_PAYLOAD, QuestionAnswer.class))
                .thenReturn(questionAnswerMock);

        Question questionMock1 = mock(Question.class);
        when(questionMock1.getQuestionID()).thenReturn(QUESTION_ID);
        QuestionAnswerPair questionAnswerPairMock1 = mock(QuestionAnswerPair.class);
        when(questionAnswerPairMock1.getQuestion()).thenReturn(questionMock1);

        QuestionAnswerPair questionAnswerPairMock2 = mock(QuestionAnswerPair.class);

        when(questionStateMock.getQaPairs())
                .thenReturn(List.of(questionAnswerPairMock1, questionAnswerPairMock2));

        when(mockObjectMapper.writeValueAsString(questionStateMock)).thenReturn("question-state");

        when(questionStateMock.canSubmitAnswers(questionStateMock.getQaPairs())).thenReturn(true);

        QuestionsResponse questionsResponse = mock(QuestionsResponse.class);
        when(mockExperianService.submitAnswersToExperianAPI(questionStateMock))
                .thenReturn(questionsResponse);

        doReturn(false).when(questionStateMock).setQuestionsResponse(questionsResponse);

        String responsePayload = "authenticated-response-received";
        when(mockObjectMapper.writeValueAsString(questionsResponse))
                .thenReturn("authenticated-response-received");
        doNothing().when(mockStorageService).update(kbvSessionItemMock);
        APIGatewayProxyResponseEvent result =
                questionAnswerHandler.handleRequest(input, contextMock);

        assertEquals(HttpStatus.SC_OK, result.getStatusCode());
        assertEquals(responsePayload, result.getBody());
    }

    @Test
    void shouldReturn201WhenNextSetOfQuestionsReceivedFromExperian()
            throws IOException, InterruptedException {
        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        Context contextMock = mock(Context.class);
        KBVSessionItem kbvSessionItemMock = mock(KBVSessionItem.class);
        QuestionState questionStateMock = mock(QuestionState.class);
        Control controlMock = mock(Control.class);
        when(controlMock.getAuthRefNo()).thenReturn("some-auth-ref");
        when(controlMock.getURN()).thenReturn("some-urn");

        when(questionStateMock.getControl()).thenReturn(controlMock);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, "new-session-id");

        QuestionAnswer questionAnswerMock = mock(QuestionAnswer.class);
        when(questionAnswerMock.getQuestionId()).thenReturn(QUESTION_ID);
        when(questionAnswerMock.getAnswer()).thenReturn(ANSWER);

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockStorageService.getSessionId(sessionHeader.get(HEADER_SESSION_ID)))
                .thenReturn(Optional.ofNullable(kbvSessionItemMock));

        when(mockObjectMapper.readValue(kbvSessionItemMock.getQuestionState(), QuestionState.class))
                .thenReturn(questionStateMock);

        when(input.getBody()).thenReturn(REQUEST_PAYLOAD);
        when(mockObjectMapper.readValue(REQUEST_PAYLOAD, QuestionAnswer.class))
                .thenReturn(questionAnswerMock);

        Question questionMock1 = mock(Question.class);
        Question questionMock2 = mock(Question.class);
        when(questionMock1.getQuestionID()).thenReturn(QUESTION_ID);
        QuestionAnswerPair questionAnswerPairMock1 = mock(QuestionAnswerPair.class);
        when(questionAnswerPairMock1.getQuestion()).thenReturn(questionMock1);

        QuestionAnswerPair questionAnswerPairMock2 = mock(QuestionAnswerPair.class);

        when(questionStateMock.getQaPairs())
                .thenReturn(List.of(questionAnswerPairMock1, questionAnswerPairMock2));

        when(mockObjectMapper.writeValueAsString(questionStateMock)).thenReturn("question-state");

        when(questionStateMock.canSubmitAnswers(questionStateMock.getQaPairs())).thenReturn(true);

        QuestionsResponse questionsResponse = mock(QuestionsResponse.class);
        when(mockExperianService.submitAnswersToExperianAPI(questionStateMock))
                .thenReturn(questionsResponse);

        doReturn(true).when(questionStateMock).setQuestionsResponse(questionsResponse);

        doNothing().when(mockStorageService).update(kbvSessionItemMock);

        when(questionStateMock.getNextQuestion()).thenReturn(Optional.of(questionMock2));
        when(mockObjectMapper.writeValueAsString(questionMock2)).thenReturn(SECOND_QUESTION);

        APIGatewayProxyResponseEvent result =
                questionAnswerHandler.handleRequest(input, contextMock);

        assertEquals(HttpStatus.SC_CREATED, result.getStatusCode());
        assertEquals(SECOND_QUESTION, result.getBody());
    }

    @Test
    void shouldReturn500ErrorWhenAWSDynamoDBServiceDown() throws IOException, InterruptedException {
        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, "new-session-id");
        ArgumentCaptor<ILoggingEvent> loggingEventArgumentCaptor =
                ArgumentCaptor.forClass(ILoggingEvent.class);

        when(input.getHeaders()).thenReturn(sessionHeader);
        doThrow(InternalServerErrorException.class)
                .when(mockStorageService)
                .getSessionId(anyString());

        APIGatewayProxyResponseEvent response =
                questionAnswerHandler.handleRequest(input, mock(Context.class));

        verify(appender).doAppend(loggingEventArgumentCaptor.capture());
        ILoggingEvent event = loggingEventArgumentCaptor.getValue();
        assertEquals("AWS Server error occurred.", event.getMessage());
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void shouldReturn400ErrorWhenNoSessionIdProvided() {
        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        ArgumentCaptor<ILoggingEvent> loggingEventArgumentCaptor =
                ArgumentCaptor.forClass(ILoggingEvent.class);

        APIGatewayProxyResponseEvent response =
                questionAnswerHandler.handleRequest(input, mock(Context.class));

        verify(appender).doAppend(loggingEventArgumentCaptor.capture());
        ILoggingEvent event = loggingEventArgumentCaptor.getValue();
        assertEquals("Error finding the requested resource", event.getMessage());
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturn500ErrorWhenQuestionStateCannotBeParsedToJSON() throws IOException {
        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, "new-session-id");
        KBVSessionItem kbvSessionItemMock = mock(KBVSessionItem.class);
        ArgumentCaptor<ILoggingEvent> loggingEventArgumentCaptor =
                ArgumentCaptor.forClass(ILoggingEvent.class);

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockStorageService.getSessionId(sessionHeader.get(HEADER_SESSION_ID)))
                .thenReturn(Optional.ofNullable(kbvSessionItemMock));
        when(mockObjectMapper.readValue(kbvSessionItemMock.getQuestionState(), QuestionState.class))
                .thenThrow(JsonProcessingException.class);

        APIGatewayProxyResponseEvent response =
                questionAnswerHandler.handleRequest(input, mock(Context.class));

        verify(appender).doAppend(loggingEventArgumentCaptor.capture());
        ILoggingEvent event = loggingEventArgumentCaptor.getValue();
        assertEquals("Failed to parse object using ObjectMapper", event.getMessage());
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void shouldReturn500ErrorWhenExperianAPIIsDown() throws IOException, InterruptedException {
        ArgumentCaptor<ILoggingEvent> loggingEventArgumentCaptor =
                ArgumentCaptor.forClass(ILoggingEvent.class);
        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        Context contextMock = mock(Context.class);
        KBVSessionItem kbvSessionItemMock = mock(KBVSessionItem.class);
        QuestionState questionStateMock = mock(QuestionState.class);
        Control controlMock = mock(Control.class);
        when(controlMock.getAuthRefNo()).thenReturn("some-auth-ref");
        when(controlMock.getURN()).thenReturn("some-urn");

        when(questionStateMock.getControl()).thenReturn(controlMock);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, "new-session-id");

        QuestionAnswer questionAnswerMock = mock(QuestionAnswer.class);

        when(questionAnswerMock.getQuestionId()).thenReturn(QUESTION_ID);
        when(questionAnswerMock.getAnswer()).thenReturn(ANSWER);

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockStorageService.getSessionId(sessionHeader.get(HEADER_SESSION_ID)))
                .thenReturn(Optional.ofNullable(kbvSessionItemMock));

        when(mockObjectMapper.readValue(kbvSessionItemMock.getQuestionState(), QuestionState.class))
                .thenReturn(questionStateMock);

        when(input.getBody()).thenReturn(REQUEST_PAYLOAD);
        when(mockObjectMapper.readValue(REQUEST_PAYLOAD, QuestionAnswer.class))
                .thenReturn(questionAnswerMock);

        Question questionMock1 = mock(Question.class);
        when(questionMock1.getQuestionID()).thenReturn(QUESTION_ID);
        QuestionAnswerPair questionAnswerPairMock1 = mock(QuestionAnswerPair.class);
        when(questionAnswerPairMock1.getQuestion()).thenReturn(questionMock1);

        QuestionAnswerPair questionAnswerPairMock2 = mock(QuestionAnswerPair.class);

        when(questionStateMock.getQaPairs())
                .thenReturn(List.of(questionAnswerPairMock1, questionAnswerPairMock2));

        when(mockObjectMapper.writeValueAsString(questionStateMock)).thenReturn("question-state");
        when(questionStateMock.canSubmitAnswers(questionStateMock.getQaPairs())).thenReturn(true);
        when(mockExperianService.submitAnswersToExperianAPI(questionStateMock))
                .thenThrow(InterruptedException.class);

        APIGatewayProxyResponseEvent response =
                questionAnswerHandler.handleRequest(input, mock(Context.class));

        verify(appender).doAppend(loggingEventArgumentCaptor.capture());
        ILoggingEvent event = loggingEventArgumentCaptor.getValue();
        assertEquals(
                "Retrieving questions failed: java.lang.InterruptedException", event.getMessage());
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
}
