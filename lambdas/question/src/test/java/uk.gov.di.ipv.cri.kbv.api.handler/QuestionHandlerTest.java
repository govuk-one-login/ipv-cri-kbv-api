package uk.gov.di.ipv.cri.kbv.api.handler;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.amazonaws.services.dynamodbv2.model.InternalServerErrorException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.xray.AWSXRay;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.gov.di.ipv.cri.kbv.api.domain.*;
import uk.gov.di.ipv.cri.kbv.api.persistence.item.KBVSessionItem;
import uk.gov.di.ipv.cri.kbv.api.service.ExperianService;
import uk.gov.di.ipv.cri.kbv.api.service.StorageService;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static uk.gov.di.ipv.cri.kbv.api.handler.QuestionHandler.HEADER_SESSION_ID;

@ExtendWith(MockitoExtension.class)
public class QuestionHandlerTest {

    private QuestionHandler questionHandler;
    @Mock
    private ObjectMapper mockObjectMapper;
    @Mock
    private StorageService mockStorageService;
    @Mock
    private ExperianService mockExperianService;

    @Mock private Appender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        AWSXRay.beginSegment("handleRequest");
        Logger logger = (Logger) LoggerFactory.getLogger(QuestionHandler.class);
        logger.addAppender(appender);
        questionHandler =
                new QuestionHandler(mockObjectMapper, mockStorageService, mockExperianService);
    }

    @AfterEach
    void tearDown() {
        AWSXRay.endSegment();
    }

    @Test
    void shouldReturn200OkWhen1stCalledAndReturn1stUnAnsweredQuestion() throws IOException, InterruptedException {
        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, "new-session-id");

        Context contextMock = mock(Context.class);
        KBVSessionItem kbvSessionItemMock = mock(KBVSessionItem.class);
        PersonIdentity personIdentityMock = mock(PersonIdentity.class);
        QuestionState questionStateMock = mock(QuestionState.class);

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockStorageService.getSessionId(sessionHeader.get(HEADER_SESSION_ID))).thenReturn(Optional.ofNullable(kbvSessionItemMock));
        when(mockObjectMapper.readValue(kbvSessionItemMock.getUserAttributes(), PersonIdentity.class)).thenReturn(personIdentityMock);
        when(mockObjectMapper.readValue(kbvSessionItemMock.getQuestionState(), QuestionState.class)).thenReturn(questionStateMock);
        when(mockObjectMapper.writeValueAsString(personIdentityMock)).thenReturn("person-identity");

        QuestionsResponse questionsResponseMock = mock(QuestionsResponse.class);
        when(mockExperianService.getQuestions("person-identity")).thenReturn(questionsResponseMock);
        when(questionStateMock.setQuestionsResponse(questionsResponseMock)).thenReturn(true);
        String state = "question-state";
        when(mockObjectMapper.writeValueAsString(questionStateMock)).thenReturn(state);
        Control controlMock = mock(Control.class);
        when(questionStateMock.getControl()).thenReturn(controlMock);
        when(controlMock.getAuthRefNo()).thenReturn("auth-ref-no");
        when(controlMock.getURN()).thenReturn("ipv-session-id");
        doNothing().when(mockStorageService).update(sessionHeader.get(HEADER_SESSION_ID), state, "auth-ref-no", "ipv-session-id");

        Question expectedQuestion = mock(Question.class);

        when(questionStateMock.getNextQuestion()) // we have to do this to get it to work
                .thenReturn(Optional.empty())     // otherwise the second overrides the first
                .thenReturn(Optional.ofNullable(expectedQuestion));

        when(mockObjectMapper.writeValueAsString(expectedQuestion)).thenReturn(TestData.EXPECTED_QUESTION);
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
        when(mockStorageService.getSessionId(sessionHeader.get(HEADER_SESSION_ID))).thenReturn(Optional.ofNullable(kbvSessionItemMock));
        when(mockObjectMapper.readValue(kbvSessionItemMock.getUserAttributes(), PersonIdentity.class)).thenReturn(personIdentityMock);
        when(mockObjectMapper.readValue(kbvSessionItemMock.getQuestionState(), QuestionState.class)).thenReturn(questionStateMock);
        when(mockObjectMapper.writeValueAsString(personIdentityMock)).thenReturn("person-identity");

        QuestionsResponse questionsResponseMock = mock(QuestionsResponse.class);
        when(mockExperianService.getQuestions("person-identity")).thenReturn(questionsResponseMock);
        when(questionStateMock.setQuestionsResponse(questionsResponseMock)).thenReturn(false);

        APIGatewayProxyResponseEvent response = questionHandler.handleRequest(input, contextMock);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        assertEquals("{ \"error\":\" no further questions \" }", response.getBody());
    }

    @Test
    void shouldReturn500ErrorWhenNoSessionIdProvided()  {
        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        ArgumentCaptor<ILoggingEvent> loggingEventArgumentCaptor =
                ArgumentCaptor.forClass(ILoggingEvent.class);

        APIGatewayProxyResponseEvent response = questionHandler.handleRequest(input, mock(Context.class));
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(appender).doAppend(loggingEventArgumentCaptor.capture());
        ILoggingEvent event = loggingEventArgumentCaptor.getValue();
        assertEquals(event.getMessage(), "Retrieving questions failed: java.lang.NullPointerException");
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

        APIGatewayProxyResponseEvent response = questionHandler.handleRequest(input, mock(Context.class));
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(appender).doAppend(loggingEventArgumentCaptor.capture());
        ILoggingEvent event = loggingEventArgumentCaptor.getValue();
        assertEquals(event.getMessage(), "AWS Server error occurred.");
    }

    @Test
    void shouldReturn500ErrorWhenPersonIdentityCannotBeParsedToJSON() throws IOException, InterruptedException {
        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, "new-session-id");
        KBVSessionItem kbvSessionItemMock = mock(KBVSessionItem.class);
        ArgumentCaptor<ILoggingEvent> loggingEventArgumentCaptor =
                ArgumentCaptor.forClass(ILoggingEvent.class);

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockStorageService.getSessionId(sessionHeader.get(HEADER_SESSION_ID))).thenReturn(Optional.ofNullable(kbvSessionItemMock));
        when(mockObjectMapper.readValue(kbvSessionItemMock.getUserAttributes(), PersonIdentity.class)).thenThrow(JsonProcessingException.class);

        APIGatewayProxyResponseEvent response = questionHandler.handleRequest(input, mock(Context.class));
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(appender).doAppend(loggingEventArgumentCaptor.capture());
        ILoggingEvent event = loggingEventArgumentCaptor.getValue();
        assertEquals(event.getMessage(),"Failed to parse object using ObjectMapper");
    }

}
