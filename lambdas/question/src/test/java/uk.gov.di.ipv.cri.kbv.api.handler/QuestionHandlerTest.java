package uk.gov.di.ipv.cri.kbv.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.xray.AWSXRay;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

    private static final String TEST_IPV_SESSION_ID = "a-session-id";
    private QuestionHandler questionHandler;
    @Mock
    private ObjectMapper mockObjectMapper;
    @Mock
    private StorageService mockStorageService;
    @Mock
    private ExperianService mockExperianService;
    @Mock
    private APIGatewayProxyResponseEvent mockapiGatewayProxyResponseEvent;

    @BeforeEach
    void setUp() {
        AWSXRay.beginSegment("handleRequest");
        questionHandler =
                new QuestionHandler(mockObjectMapper, mockStorageService, mockExperianService);
    }

    @AfterEach
    void tearDown() {
        AWSXRay.endSegment();
    }

//    @Test
    void shouldReturn200ResponseWithAQuestion() throws IOException, InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();
        APIGatewayProxyRequestEvent input = new APIGatewayProxyRequestEvent();
        input.setHeaders(Map.of(HEADER_SESSION_ID, TEST_IPV_SESSION_ID));

        PersonIdentity personIdentity = mock(PersonIdentity.class);
        QuestionState initialQuestionState = objectMapper.readValue(TestData.INITIAL_QUESTION_STATE, QuestionState.class);

        KBVSessionItem kbvSessionItem = mock(KBVSessionItem.class);
        when(mockStorageService.getSessionId(TEST_IPV_SESSION_ID)).thenReturn(kbvSessionItem);

        when(mockObjectMapper.readValue(kbvSessionItem.getUserAttributes(), PersonIdentity.class))
                .thenReturn(personIdentity);
        when(mockObjectMapper.readValue(kbvSessionItem.getQuestionState(), QuestionState.class))
                .thenReturn(initialQuestionState);

        when(mockObjectMapper.writeValueAsString(personIdentity))
                .thenReturn("person-identity");

//        Question question = objectMapper.readValue(expectedResult, Question.class);
        QuestionState mockQuestionState = mock(QuestionState.class);
//        when(mockQuestionState.getNextQuestion()).thenReturn(Optional.empty());
//

        QuestionsResponse questionsResponse = objectMapper.readValue(TestData.expectedResult, QuestionsResponse.class);
        when(mockExperianService.getQuestions("person-identity")).thenReturn(questionsResponse);

//        String state = objectMapper.writeValueAsString(questionState);
//        LOGGER.info("questionState:" + state);
//        storageService.update(
//                sessionId,
//                state,
//                questionState.getControl().getAuthRefNo(),
//                questionState.getControl().getURN());
//        nextQuestion = questionState.getNextQuestion();
//        responseBody = objectMapper.writeValueAsString(nextQuestion.get());
//
//        String newState = mockObjectMapper.writeValueAsString(mockQuestionState);
        mockStorageService.update(anyString(), anyString(), anyString(), anyString());
//        when(mockQuestionState.getNextQuestion()).thenReturn(Optional.ofNullable(new Question()));

//        when(mockQuestionState.setQuestionsResponse(questionsResponse)).thenReturn(true);

//        when(mockapiGatewayProxyResponseEvent.getBody()).thenReturn(expectedResult);

         mockapiGatewayProxyResponseEvent = questionHandler.handleRequest(input, mock(Context.class));

//        verify(mockApiGatewayProxyResponseEvent).withHeaders(Map.of("Content-Type",
//                "application/json"));
//        verify(mockApiGatewayProxyResponseEvent).withStatusCode(HttpStatus.SC_OK);
//        assertTrue(apiGatewayProxyResponseEvent.getBody().contains("Q007"));
        System.out.println(mockapiGatewayProxyResponseEvent.getBody());
        assertEquals( HttpStatus.SC_OK, mockapiGatewayProxyResponseEvent.getStatusCode());
    }

    @Test
    void ola_shouldReturn200OkWhen1stCalledAndReturn1stUnAnsweredQuestion() throws IOException, InterruptedException {
        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, "new-session-id");
        ObjectMapper objectMapper = new ObjectMapper();
        Context contextMock = mock(Context.class);
        KBVSessionItem kbvSessionItemMock = mock(KBVSessionItem.class);
        PersonIdentity personIdentityMock = mock(PersonIdentity.class);
//        QuestionState questionStateMock = mock(QuestionState.class);

        QuestionState initialQuestionState = objectMapper.readValue(TestData.INITIAL_QUESTION_STATE, QuestionState.class);
        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockStorageService.getSessionId(sessionHeader.get(HEADER_SESSION_ID))).thenReturn(kbvSessionItemMock);
        when(mockObjectMapper.readValue(kbvSessionItemMock.getUserAttributes(), PersonIdentity.class)).thenReturn(personIdentityMock);
        when(mockObjectMapper.readValue(kbvSessionItemMock.getQuestionState(), QuestionState.class)).thenReturn(initialQuestionState);
        when(mockObjectMapper.writeValueAsString(personIdentityMock)).thenReturn("person-identity");



//        when(questionStateMock.getNextQuestion()).thenReturn(Optional.empty());

        QuestionsResponse questionsResponse = objectMapper.readValue(TestData.expectedResult, QuestionsResponse.class);
        when(mockExperianService.getQuestions("person-identity")).thenReturn(questionsResponse);
//        when(questionStateMock.setQuestionsResponse(questionsResponse)).thenReturn(true);

        QuestionState questionState = objectMapper.readValue(TestData.QUESTION_STATE, QuestionState.class);
        when(mockObjectMapper.writeValueAsString(questionState)).thenReturn(TestData.QUESTION_STATE);
//        Control controlMock = mock(Control.class);
//        when(questionStateMock.getControl()).thenReturn(controlMock);
//        when(controlMock.getAuthRefNo()).thenReturn(questionState.getControl().getAuthRefNo());
//        when(controlMock.getURN()).thenReturn(questionState.getControl().getURN());
        doNothing().when(mockStorageService).update(sessionHeader.get(HEADER_SESSION_ID), TestData.QUESTION_STATE, questionState.getControl().getAuthRefNo(), questionState.getControl().getURN());

        Question nextQuestion = objectMapper.readValue(TestData.EXPECTED_QUESTION, Question.class);
//        when(questionStateMock.getNextQuestion()).thenReturn(Optional.ofNullable(nextQuestion));

//        when(mockObjectMapper.writeValueAsString(nextQuestion)).thenReturn(TestData.EXPECTED_QUESTION);

        APIGatewayProxyResponseEvent response = questionHandler.handleRequest(input, contextMock);
        assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        //assertEquals(null, response.getBody());
    }
}
