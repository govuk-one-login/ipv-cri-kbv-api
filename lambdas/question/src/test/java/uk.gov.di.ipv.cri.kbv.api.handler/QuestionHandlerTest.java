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
import uk.gov.di.ipv.cri.kbv.api.domain.PersonIdentity;
import uk.gov.di.ipv.cri.kbv.api.domain.Question;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.persistence.item.KBVSessionItem;
import uk.gov.di.ipv.cri.kbv.api.service.ExperianService;
import uk.gov.di.ipv.cri.kbv.api.service.StorageService;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.kbv.api.handler.QuestionHandler.HEADER_SESSION_ID;

@ExtendWith(MockitoExtension.class)
class QuestionHandlerTest {

    private QuestionHandler questionHandler;
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
        Logger logger = (Logger) LoggerFactory.getLogger(QuestionHandler.class);
        logger.addAppender(appender);
        questionHandler =
                new QuestionHandler(mockObjectMapper, mockStorageService, mockExperianService);
    }

    @Test
    void shouldReturn200OkWhen1stCalledAndReturn1stUnAnsweredQuestion()
            throws IOException, InterruptedException {
        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, "new-session-id");

        Context contextMock = mock(Context.class);
        KBVSessionItem kbvSessionItemMock = mock(KBVSessionItem.class);
        PersonIdentity personIdentityMock = mock(PersonIdentity.class);
        QuestionState questionStateMock = mock(QuestionState.class);

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockStorageService.getSessionId(sessionHeader.get(HEADER_SESSION_ID)))
                .thenReturn(Optional.ofNullable(kbvSessionItemMock));
        when(mockObjectMapper.readValue(
                        kbvSessionItemMock.getUserAttributes(), PersonIdentity.class))
                .thenReturn(personIdentityMock);
        when(mockObjectMapper.readValue(kbvSessionItemMock.getQuestionState(), QuestionState.class))
                .thenReturn(questionStateMock);

        when(mockObjectMapper.writeValueAsString(any())).thenReturn("questions-request");

        QuestionsResponse questionsResponseMock = mock(QuestionsResponse.class);

        when(mockExperianService.getResponseFromExperianAPI(
                        "questions-request", "EXPERIAN_API_WRAPPER_SAA_RESOURCE"))
                .thenReturn("questionsResponseMock");

        //  QuestionsResponse questionsResponse = objectMapper.readValue(questionsResponsePayload,
        // QuestionsResponse.class);
        when(mockObjectMapper.readValue("questionsResponseMock", QuestionsResponse.class))
                .thenReturn(questionsResponseMock);

        when(questionStateMock.setQuestionsResponse(questionsResponseMock)).thenReturn(true);
        String state = "question-state";
        when(mockObjectMapper.writeValueAsString(questionStateMock)).thenReturn(state);
        Control controlMock = mock(Control.class);
        when(questionStateMock.getControl()).thenReturn(controlMock);
        String authRefNo = "auth-ref-no";
        when(controlMock.getAuthRefNo()).thenReturn(authRefNo);
        String ipvSessionId = "ipv-session-id";
        when(controlMock.getURN()).thenReturn(ipvSessionId);
        doNothing().when(mockStorageService).update(kbvSessionItemMock);

        Question expectedQuestion = mock(Question.class);

        when(questionStateMock.getNextQuestion()) // we have to do this to get it to work
                .thenReturn(Optional.empty()) // otherwise the second overrides the first
                .thenReturn(Optional.ofNullable(expectedQuestion));

        when(mockObjectMapper.writeValueAsString(expectedQuestion))
                .thenReturn(TestData.EXPECTED_QUESTION);

        APIGatewayProxyResponseEvent response = questionHandler.handleRequest(input, contextMock);

        assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        assertEquals(TestData.EXPECTED_QUESTION, response.getBody());
    }

    @Test
    void shouldReturn400ErrorWhenNoFurtherQuestions() throws IOException, InterruptedException {
        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, "new-session-id");

        Context contextMock = mock(Context.class);
        KBVSessionItem kbvSessionItemMock = mock(KBVSessionItem.class);
        PersonIdentity personIdentityMock = mock(PersonIdentity.class);
        QuestionState questionStateMock = mock(QuestionState.class);

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockStorageService.getSessionId(sessionHeader.get(HEADER_SESSION_ID)))
                .thenReturn(Optional.ofNullable(kbvSessionItemMock));
        when(mockObjectMapper.readValue(
                        kbvSessionItemMock.getUserAttributes(), PersonIdentity.class))
                .thenReturn(personIdentityMock);
        when(mockObjectMapper.readValue(kbvSessionItemMock.getQuestionState(), QuestionState.class))
                .thenReturn(questionStateMock);

        when(mockObjectMapper.writeValueAsString(any())).thenReturn("questions-request");

        QuestionsResponse questionsResponseMock = mock(QuestionsResponse.class);

        String questionsResponsePayload = "questionsResponse";
        when(mockExperianService.getResponseFromExperianAPI(
                        "questions-request", "EXPERIAN_API_WRAPPER_SAA_RESOURCE"))
                .thenReturn(questionsResponsePayload);

        //        QuestionsResponse questionsResponse =
        // objectMapper.readValue(questionsResponsePayload, QuestionsResponse.class);

        when(mockObjectMapper.readValue(questionsResponsePayload, QuestionsResponse.class))
                .thenReturn(questionsResponseMock);

        when(questionStateMock.setQuestionsResponse(questionsResponseMock)).thenReturn(false);

        APIGatewayProxyResponseEvent response = questionHandler.handleRequest(input, contextMock);
        assertEquals("{ \"error\":\" no further questions \" }", response.getBody());
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturn400ErrorWhenNoSessionIdProvided() {
        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        ArgumentCaptor<ILoggingEvent> loggingEventArgumentCaptor =
                ArgumentCaptor.forClass(ILoggingEvent.class);

        APIGatewayProxyResponseEvent response =
                questionHandler.handleRequest(input, mock(Context.class));

        verify(appender).doAppend(loggingEventArgumentCaptor.capture());
        ILoggingEvent event = loggingEventArgumentCaptor.getValue();
        assertEquals("Error finding the requested resource", event.getMessage());
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
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
                questionHandler.handleRequest(input, mock(Context.class));

        verify(appender).doAppend(loggingEventArgumentCaptor.capture());
        ILoggingEvent event = loggingEventArgumentCaptor.getValue();
        assertEquals("AWS Server error occurred.", event.getMessage());
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void shouldReturn500ErrorWhenPersonIdentityCannotBeParsedToJSON() throws IOException {
        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, "new-session-id");
        KBVSessionItem kbvSessionItemMock = mock(KBVSessionItem.class);
        ArgumentCaptor<ILoggingEvent> loggingEventArgumentCaptor =
                ArgumentCaptor.forClass(ILoggingEvent.class);

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockStorageService.getSessionId(sessionHeader.get(HEADER_SESSION_ID)))
                .thenReturn(Optional.ofNullable(kbvSessionItemMock));
        when(mockObjectMapper.readValue(
                        kbvSessionItemMock.getUserAttributes(), PersonIdentity.class))
                .thenThrow(JsonProcessingException.class);

        APIGatewayProxyResponseEvent response =
                questionHandler.handleRequest(input, mock(Context.class));

        verify(appender).doAppend(loggingEventArgumentCaptor.capture());
        ILoggingEvent event = loggingEventArgumentCaptor.getValue();
        assertEquals("Failed to parse object using ObjectMapper", event.getMessage());
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void shouldReturn500ErrorWhenExperianServiceIsDown() throws IOException, InterruptedException {

        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, "new-session-id");
        ArgumentCaptor<ILoggingEvent> loggingEventArgumentCaptor =
                ArgumentCaptor.forClass(ILoggingEvent.class);
        KBVSessionItem kbvSessionItemMock = mock(KBVSessionItem.class);
        PersonIdentity personIdentityMock = mock(PersonIdentity.class);
        QuestionState questionStateMock = mock(QuestionState.class);

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockStorageService.getSessionId(sessionHeader.get(HEADER_SESSION_ID)))
                .thenReturn(Optional.ofNullable(kbvSessionItemMock));
        when(mockObjectMapper.readValue(
                kbvSessionItemMock.getUserAttributes(), PersonIdentity.class))
                .thenReturn(personIdentityMock);
        when(mockObjectMapper.readValue(kbvSessionItemMock.getQuestionState(), QuestionState.class))
                .thenReturn(questionStateMock);

        when(mockObjectMapper.writeValueAsString(any())).thenReturn("questions-request");

        doThrow(InterruptedException.class)
                .when(mockExperianService)
                .getResponseFromExperianAPI(anyString(), anyString());

        APIGatewayProxyResponseEvent response =
                questionHandler.handleRequest(input, mock(Context.class));

        verify(appender).doAppend(loggingEventArgumentCaptor.capture());
        ILoggingEvent event = loggingEventArgumentCaptor.getValue();
        assertEquals("Retrieving questions failed: java.lang.InterruptedException", event.getMessage());
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
}
