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
import uk.gov.di.ipv.cri.kbv.api.domain.NextTransId;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswer;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.domain.Results;
import uk.gov.di.ipv.cri.kbv.api.persistence.item.KBVSessionItem;
import uk.gov.di.ipv.cri.kbv.api.service.ExperianService;
import uk.gov.di.ipv.cri.kbv.api.service.StorageService;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.kbv.api.handler.QuestionAnswerHandler.HEADER_SESSION_ID;

@ExtendWith(MockitoExtension.class)
class QuestionAnswerHandlerTest {

    public static final String REQUEST_PAYLOAD =
            "\"questionID\":\" Q0008 \",\"answer\":\" some-answer \"";
    private QuestionAnswerHandler questionAnswerHandler;
    @Mock private ObjectMapper mockObjectMapper;
    @Mock private StorageService mockStorageService;
    @Mock private ExperianService mockExperianService;
    @Mock private Appender<ILoggingEvent> appender;
    @Mock private APIGatewayProxyRequestEvent input;
    @Mock private Context contextMock;

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
    void shouldReturn200WithWhen1stAnswerIsSubmitted() throws JsonProcessingException {
        KBVSessionItem kbvSessionItemMock = mock(KBVSessionItem.class);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, "new-session-id");
        QuestionState questionStateMock = mock(QuestionState.class);

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockStorageService.getSessionId(sessionHeader.get(HEADER_SESSION_ID)))
                .thenReturn(Optional.ofNullable(kbvSessionItemMock));
        when(mockObjectMapper.readValue(kbvSessionItemMock.getQuestionState(), QuestionState.class))
                .thenReturn(questionStateMock);
        when(input.getBody()).thenReturn(REQUEST_PAYLOAD);
        when(mockObjectMapper.writeValueAsString(questionStateMock)).thenReturn("question-state");
        doNothing().when(mockStorageService).update(kbvSessionItemMock);

        when(questionStateMock.hasAtLeastOneUnAnswered()).thenReturn(true);

        APIGatewayProxyResponseEvent result =
                questionAnswerHandler.handleRequest(input, contextMock);

        verify(questionStateMock).setAnswer(any());
        assertEquals(HttpStatus.SC_OK, result.getStatusCode());
        assertNull(result.getBody());
    }

    @Test
    void shouldReturn200WithFinalResponseFromExperianAPI()
            throws IOException, InterruptedException {

        KBVSessionItem kbvSessionItemMock = mock(KBVSessionItem.class);
        QuestionState questionStateMock = mock(QuestionState.class);
        QuestionAnswer questionAnswerMock = mock(QuestionAnswer.class);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, "new-session-id");
        String responseBodyExperian = "experian-response-body";
        QuestionsResponse questionsResponseMock = mock(QuestionsResponse.class);
        Results resultsMock = mock(Results.class);
        List<String> transactionValue = Collections.singletonList("END");
        NextTransId nextTransIdMock = mock(NextTransId.class);

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockStorageService.getSessionId(sessionHeader.get(HEADER_SESSION_ID)))
                .thenReturn(Optional.ofNullable(kbvSessionItemMock));

        when(mockObjectMapper.readValue(kbvSessionItemMock.getQuestionState(), QuestionState.class))
                .thenReturn(questionStateMock);
        when(input.getBody()).thenReturn(REQUEST_PAYLOAD);
        when(mockObjectMapper.readValue(REQUEST_PAYLOAD, QuestionAnswer.class))
                .thenReturn(questionAnswerMock);
        when(questionStateMock.hasAtLeastOneUnAnswered()).thenReturn(false);

        when(mockExperianService.getResponseFromExperianAPI(
                        any(), eq("EXPERIAN_API_WRAPPER_RTQ_RESOURCE")))
                .thenReturn(responseBodyExperian);
        when(mockObjectMapper.readValue(responseBodyExperian, QuestionsResponse.class))
                .thenReturn(questionsResponseMock);

        when(mockObjectMapper.writeValueAsString(any())).thenReturn("question-response");

        APIGatewayProxyResponseEvent result =
                questionAnswerHandler.handleRequest(input, contextMock);

        verify(mockStorageService).update(kbvSessionItemMock);
        assertEquals(HttpStatus.SC_OK, result.getStatusCode());
        assertNull(result.getBody());
    }

    @Test
    void shouldReturn200WhenNextSetOfQuestionsAreReceivedFromExperian()
            throws IOException, InterruptedException {

        KBVSessionItem kbvSessionItemMock = mock(KBVSessionItem.class);
        QuestionState questionStateMock = mock(QuestionState.class);
        QuestionAnswer questionAnswerMock = mock(QuestionAnswer.class);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, "new-session-id");
        String responseBodyExperian = "experian-response-body";
        QuestionsResponse questionsResponseMock = mock(QuestionsResponse.class);

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockStorageService.getSessionId(sessionHeader.get(HEADER_SESSION_ID)))
                .thenReturn(Optional.ofNullable(kbvSessionItemMock));

        when(mockObjectMapper.readValue(kbvSessionItemMock.getQuestionState(), QuestionState.class))
                .thenReturn(questionStateMock);
        when(input.getBody()).thenReturn(REQUEST_PAYLOAD);
        when(mockObjectMapper.readValue(REQUEST_PAYLOAD, QuestionAnswer.class))
                .thenReturn(questionAnswerMock);
        when(mockObjectMapper.writeValueAsString(questionStateMock)).thenReturn("question-state");
        when(questionStateMock.hasAtLeastOneUnAnswered()).thenReturn(false);

        when(mockExperianService.getResponseFromExperianAPI(
                        any(), eq("EXPERIAN_API_WRAPPER_RTQ_RESOURCE")))
                .thenReturn(responseBodyExperian);
        when(mockObjectMapper.readValue(responseBodyExperian, QuestionsResponse.class))
                .thenReturn(questionsResponseMock);
        when(questionsResponseMock.hasQuestions()).thenReturn(true);
        doNothing().when(questionStateMock).setQAPairs(any());
        when(mockObjectMapper.writeValueAsString(questionStateMock)).thenReturn("question-state");

        APIGatewayProxyResponseEvent result =
                questionAnswerHandler.handleRequest(input, contextMock);

        verify(mockStorageService, times(2)).update(kbvSessionItemMock);
        assertEquals(HttpStatus.SC_OK, result.getStatusCode());
        assertNull(result.getBody());
    }

    @Test
    void shouldReturn500ErrorWhenAWSDynamoDBServiceDown() {
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, "new-session-id");
        ArgumentCaptor<ILoggingEvent> loggingEventArgumentCaptor =
                ArgumentCaptor.forClass(ILoggingEvent.class);

        when(input.getHeaders()).thenReturn(sessionHeader);
        doThrow(InternalServerErrorException.class)
                .when(mockStorageService)
                .getSessionId(anyString());

        APIGatewayProxyResponseEvent response =
                questionAnswerHandler.handleRequest(input, contextMock);

        verify(appender).doAppend(loggingEventArgumentCaptor.capture());
        ILoggingEvent event = loggingEventArgumentCaptor.getValue();
        assertEquals("AWS Server error occurred.", event.getMessage());
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void shouldReturn400ErrorWhenNoSessionIdProvided() {
        ArgumentCaptor<ILoggingEvent> loggingEventArgumentCaptor =
                ArgumentCaptor.forClass(ILoggingEvent.class);

        APIGatewayProxyResponseEvent response =
                questionAnswerHandler.handleRequest(input, contextMock);

        verify(appender).doAppend(loggingEventArgumentCaptor.capture());
        ILoggingEvent event = loggingEventArgumentCaptor.getValue();
        assertEquals("Error finding the requested resource", event.getMessage());
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturn500ErrorWhenQuestionStateCannotBeParsedToJSON() throws IOException {
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
                questionAnswerHandler.handleRequest(input, contextMock);

        verify(appender).doAppend(loggingEventArgumentCaptor.capture());
        ILoggingEvent event = loggingEventArgumentCaptor.getValue();
        assertEquals("Failed to parse object using ObjectMapper", event.getMessage());
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void shouldReturn500ErrorWhenExperianAPIIsDown() throws IOException, InterruptedException {
        ArgumentCaptor<ILoggingEvent> loggingEventArgumentCaptor =
                ArgumentCaptor.forClass(ILoggingEvent.class);

        KBVSessionItem kbvSessionItemMock = mock(KBVSessionItem.class);
        QuestionState questionStateMock = mock(QuestionState.class);

        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, "new-session-id");
        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockStorageService.getSessionId(sessionHeader.get(HEADER_SESSION_ID)))
                .thenReturn(Optional.ofNullable(kbvSessionItemMock));

        QuestionAnswer questionAnswerMock = mock(QuestionAnswer.class);

        when(mockObjectMapper.readValue(kbvSessionItemMock.getQuestionState(), QuestionState.class))
                .thenReturn(questionStateMock);
        when(input.getBody()).thenReturn(REQUEST_PAYLOAD);
        when(mockObjectMapper.readValue(REQUEST_PAYLOAD, QuestionAnswer.class))
                .thenReturn(questionAnswerMock);

        when(mockObjectMapper.writeValueAsString(questionStateMock)).thenReturn("question-state");
        when(mockExperianService.getResponseFromExperianAPI(
                        null, "EXPERIAN_API_WRAPPER_RTQ_RESOURCE"))
                .thenThrow(InterruptedException.class);

        APIGatewayProxyResponseEvent response =
                questionAnswerHandler.handleRequest(input, contextMock);

        verify(appender).doAppend(loggingEventArgumentCaptor.capture());
        ILoggingEvent event = loggingEventArgumentCaptor.getValue();
        assertEquals(
                "Retrieving questions failed: java.lang.InterruptedException", event.getMessage());
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
}
