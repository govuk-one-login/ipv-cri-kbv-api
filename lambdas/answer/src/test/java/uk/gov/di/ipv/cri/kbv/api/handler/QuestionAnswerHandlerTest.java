package uk.gov.di.ipv.cri.kbv.api.handler;

import com.amazonaws.services.dynamodbv2.model.InternalServerErrorException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.kbv.api.library.domain.NextTransId;
import uk.gov.di.ipv.cri.kbv.api.library.domain.QuestionAnswer;
import uk.gov.di.ipv.cri.kbv.api.library.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.library.domain.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.library.domain.Results;
import uk.gov.di.ipv.cri.kbv.api.library.helpers.EventProbe;
import uk.gov.di.ipv.cri.kbv.api.library.persistence.item.KBVSessionItem;
import uk.gov.di.ipv.cri.kbv.api.library.service.ExperianService;
import uk.gov.di.ipv.cri.kbv.api.library.service.StorageService;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    public static final String REQUEST_PAYLOAD =
            "\"questionID\":\" Q0008 \",\"answer\":\" some-answer \"";
    private QuestionAnswerHandler questionAnswerHandler;
    @Mock private ObjectMapper mockObjectMapper;
    @Mock private StorageService mockStorageService;
    @Mock private ExperianService mockExperianService;
    @Mock private APIGatewayProxyRequestEvent input;
    @Mock private Context contextMock;
    @Mock private EventProbe mockEventProbe;

    @BeforeEach
    void setUp() {
        questionAnswerHandler =
                new QuestionAnswerHandler(
                        mockObjectMapper, mockStorageService, mockExperianService, mockEventProbe);
    }

    @Test
    void shouldReturn200WithWhen1stAnswerIsSubmitted() throws JsonProcessingException {
        KBVSessionItem kbvSessionItemMock = mock(KBVSessionItem.class);
        Map<String, String> sessionHeader =
                Map.of(QuestionAnswerHandler.HEADER_SESSION_ID, "new-session-id");
        QuestionState questionStateMock = mock(QuestionState.class);

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockStorageService.getSessionId(
                        sessionHeader.get(QuestionAnswerHandler.HEADER_SESSION_ID)))
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
        Map<String, String> sessionHeader =
                Map.of(QuestionAnswerHandler.HEADER_SESSION_ID, "new-session-id");
        String responseBodyExperian = "experian-response-body";
        QuestionsResponse questionsResponseMock = mock(QuestionsResponse.class);
        Results resultsMock = mock(Results.class);
        List<String> transactionValue = Collections.singletonList("END");
        NextTransId nextTransIdMock = mock(NextTransId.class);

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockStorageService.getSessionId(
                        sessionHeader.get(QuestionAnswerHandler.HEADER_SESSION_ID)))
                .thenReturn(Optional.ofNullable(kbvSessionItemMock));

        when(mockObjectMapper.readValue(kbvSessionItemMock.getQuestionState(), QuestionState.class))
                .thenReturn(questionStateMock);
        when(input.getBody()).thenReturn(REQUEST_PAYLOAD);
        when(mockObjectMapper.readValue(REQUEST_PAYLOAD, QuestionAnswer.class))
                .thenReturn(questionAnswerMock);
        when(questionStateMock.hasAtLeastOneUnAnswered()).thenReturn(false);

        when(mockExperianService.getResponseFromKBVExperianAPI(
                        any(), eq("EXPERIAN_API_WRAPPER_RTQ_RESOURCE")))
                .thenReturn(responseBodyExperian);
        when(mockObjectMapper.readValue(responseBodyExperian, QuestionsResponse.class))
                .thenReturn(questionsResponseMock);

        when(mockObjectMapper.writeValueAsString(any())).thenReturn("question-response");

        when(questionsResponseMock.hasQuestions()).thenReturn(false);
        when(questionsResponseMock.hasQuestionRequestEnded()).thenReturn(true);

        APIGatewayProxyResponseEvent result =
                questionAnswerHandler.handleRequest(input, contextMock);

        verify(mockStorageService, times(2)).update(kbvSessionItemMock);
        assertEquals(HttpStatus.SC_OK, result.getStatusCode());
        assertNull(result.getBody());
    }

    @Test
    void shouldReturn200WhenNextSetOfQuestionsAreReceivedFromExperian()
            throws IOException, InterruptedException {

        KBVSessionItem kbvSessionItemMock = mock(KBVSessionItem.class);
        QuestionState questionStateMock = mock(QuestionState.class);
        QuestionAnswer questionAnswerMock = mock(QuestionAnswer.class);
        Map<String, String> sessionHeader =
                Map.of(QuestionAnswerHandler.HEADER_SESSION_ID, "new-session-id");
        String responseBodyExperian = "experian-response-body";
        QuestionsResponse questionsResponseMock = mock(QuestionsResponse.class);

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockStorageService.getSessionId(
                        sessionHeader.get(QuestionAnswerHandler.HEADER_SESSION_ID)))
                .thenReturn(Optional.ofNullable(kbvSessionItemMock));

        when(mockObjectMapper.readValue(kbvSessionItemMock.getQuestionState(), QuestionState.class))
                .thenReturn(questionStateMock);
        when(input.getBody()).thenReturn(REQUEST_PAYLOAD);
        when(mockObjectMapper.readValue(REQUEST_PAYLOAD, QuestionAnswer.class))
                .thenReturn(questionAnswerMock);
        when(mockObjectMapper.writeValueAsString(questionStateMock)).thenReturn("question-state");
        when(questionStateMock.hasAtLeastOneUnAnswered()).thenReturn(false);

        when(mockExperianService.getResponseFromKBVExperianAPI(
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
        Map<String, String> sessionHeader =
                Map.of(QuestionAnswerHandler.HEADER_SESSION_ID, "new-session-id");

        when(input.getHeaders()).thenReturn(sessionHeader);
        doThrow(InternalServerErrorException.class)
                .when(mockStorageService)
                .getSessionId(anyString());

        setupEventProbeErrorBehaviour();
        APIGatewayProxyResponseEvent response =
                questionAnswerHandler.handleRequest(input, contextMock);

        assertEquals("{ \"error\":\"AWS Server error occurred.\" }", response.getBody());
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(mockEventProbe).counterMetric("post_answer", 0d);
    }

    @Test
    void shouldReturn400ErrorWhenNoSessionIdProvided() {
        setupEventProbeErrorBehaviour();
        APIGatewayProxyResponseEvent response =
                questionAnswerHandler.handleRequest(input, contextMock);

        assertEquals("{ \"error\":\"Error finding the requested resource.\" }", response.getBody());

        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        verify(mockEventProbe).counterMetric("post_answer", 0d);
    }

    @Test
    void shouldReturn500ErrorWhenQuestionStateCannotBeParsedToJSON() throws IOException {
        Map<String, String> sessionHeader =
                Map.of(QuestionAnswerHandler.HEADER_SESSION_ID, "new-session-id");
        KBVSessionItem kbvSessionItemMock = mock(KBVSessionItem.class);

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockStorageService.getSessionId(
                        sessionHeader.get(QuestionAnswerHandler.HEADER_SESSION_ID)))
                .thenReturn(Optional.ofNullable(kbvSessionItemMock));
        when(mockObjectMapper.readValue(kbvSessionItemMock.getQuestionState(), QuestionState.class))
                .thenThrow(JsonProcessingException.class);
        setupEventProbeErrorBehaviour();
        APIGatewayProxyResponseEvent response =
                questionAnswerHandler.handleRequest(input, contextMock);

        assertEquals(
                "{ \"error\":\"Failed to parse object using ObjectMapper.\" }", response.getBody());
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(mockEventProbe).counterMetric("post_answer", 0d);
    }

    @Test
    void shouldReturn500ErrorWhenExperianAPIIsDown() throws IOException, InterruptedException {

        KBVSessionItem kbvSessionItemMock = mock(KBVSessionItem.class);
        QuestionState questionStateMock = mock(QuestionState.class);

        Map<String, String> sessionHeader =
                Map.of(QuestionAnswerHandler.HEADER_SESSION_ID, "new-session-id");
        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockStorageService.getSessionId(
                        sessionHeader.get(QuestionAnswerHandler.HEADER_SESSION_ID)))
                .thenReturn(Optional.ofNullable(kbvSessionItemMock));

        QuestionAnswer questionAnswerMock = mock(QuestionAnswer.class);

        when(mockObjectMapper.readValue(kbvSessionItemMock.getQuestionState(), QuestionState.class))
                .thenReturn(questionStateMock);
        when(input.getBody()).thenReturn(REQUEST_PAYLOAD);
        when(mockObjectMapper.readValue(REQUEST_PAYLOAD, QuestionAnswer.class))
                .thenReturn(questionAnswerMock);

        when(mockObjectMapper.writeValueAsString(questionStateMock)).thenReturn("question-state");
        when(mockExperianService.getResponseFromKBVExperianAPI(
                        null, "EXPERIAN_API_WRAPPER_RTQ_RESOURCE"))
                .thenThrow(InterruptedException.class);

        setupEventProbeErrorBehaviour();
        APIGatewayProxyResponseEvent response =
                questionAnswerHandler.handleRequest(input, contextMock);
        assertEquals(
                "{ \"error\":\"Retrieving questions failed: java.lang.InterruptedException\" }",
                response.getBody());
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(mockEventProbe).counterMetric("post_answer", 0d);
    }

    private void setupEventProbeErrorBehaviour() {
        when(mockEventProbe.counterMetric(anyString(), anyDouble())).thenReturn(mockEventProbe);
        when(mockEventProbe.log(any(Level.class), any(Exception.class))).thenReturn(mockEventProbe);
    }
}
