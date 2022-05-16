package uk.gov.di.ipv.cri.kbv.api.handler;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.amazonaws.services.dynamodbv2.model.InternalServerErrorException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.gov.di.ipv.cri.kbv.api.domain.PersonIdentity;
import uk.gov.di.ipv.cri.kbv.api.domain.Question;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerPair;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsRequest;
import uk.gov.di.ipv.cri.kbv.api.persistence.item.KBVSessionItem;
import uk.gov.di.ipv.cri.kbv.api.service.ExperianService;
import uk.gov.di.ipv.cri.kbv.api.service.StorageService;

import java.io.IOException;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.kbv.api.handler.QuestionHandler.HEADER_SESSION_ID;

@ExtendWith(MockitoExtension.class)
class QuestionHandlerTest {

    private QuestionHandler questionHandler;
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    @Mock private StorageService mockStorageService;
    @Mock private ExperianService mockExperianService;
    @Mock private Appender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        Logger logger = (Logger) LoggerFactory.getLogger(QuestionHandler.class);
        logger.addAppender(appender);
        questionHandler = new QuestionHandler(mockStorageService, mockExperianService);
    }

    @Test
    void shouldReturn200OkWhen1stCalledAndReturn1stUnAnsweredQuestionFromExperianEndpoint()
            throws IOException, InterruptedException {
        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, "new-session-id");

        Context contextMock = mock(Context.class);
        KBVSessionItem kbvSessionItemMock = mock(KBVSessionItem.class);
        PersonIdentity personIdentityMock = mock(PersonIdentity.class);
        QuestionState questionStateMock = mock(QuestionState.class);
        String questionsResponse =
                "{\"control\":{\"urn\":\"f0746bbd-ed6f-44c7-bc3b-82c336c10815\",\"authRefNo\":\"7DCTWBC7LR\",\"dateTime\":null,\"testDatabase\":\"A\",\"clientAccountNo\":\"J8193\",\"clientBranchNo\":null,\"operatorID\":\"GDSCABINETUIIQ01U\",\"parameters\":{\"oneShotAuthentication\":\"N\",\"storeCaseData\":\"P\"}},\"questions\":{\"question\":[{\"questionID\":\"Q00015\",\"text\":\"What is the first James Bond movie?\",\"tooltip\":\"It was released in the 1960s.\",\"answerHeldFlag\":null,\"answerFormat\":{\"identifier\":\"A00004\",\"fieldType\":\"G \",\"answerList\":[\"Movie 1\",\"Movie 2\",\"Movie 3\"]}},{\"questionID\":\"Q00040\",\"text\":\"How much was your recent loan for?\",\"tooltip\":\"The approximate starting balance, in £s, on an active personal loan. Does not include HP Loans, 2nd Mortgages or Home Credit.\",\"answerFormat\":{\"identifier\":\"A00004\",\"fieldType\":\"G \",\"answerList\":[\"UP TO £8,500\",\"OVER £8,500 UP TO £9,000\",\"OVER £9,000 UP TO £9,500\",\"OVER £9,500 UP TO £10,000\",\"NONE OF THE ABOVE / DOES NOT APPLY\"]},\"answerHeldFlag\":null}],\"skipsRemaining\":null,\"skipWarning\":null},\"results\":{\"outcome\":\"Authentication Questions returned\",\"authenticationResult\":null,\"questions\":null,\"alerts\":null,\"nextTransId\":{\"string\":[\"RTQ\"]},\"caseFoundFlag\":null,\"confirmationCode\":null},\"error\":null}";

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockStorageService.getSessionId(sessionHeader.get(HEADER_SESSION_ID)))
                .thenReturn(Optional.ofNullable(kbvSessionItemMock));
        String personIdentity = objectMapper.writeValueAsString(personIdentityMock);
        String questionState = objectMapper.writeValueAsString(questionStateMock);

        when(kbvSessionItemMock.getUserAttributes()).thenReturn(personIdentity);
        when(kbvSessionItemMock.getQuestionState()).thenReturn(questionState);

        QuestionsRequest questionsRequest = new QuestionsRequest();
        questionsRequest.setPersonIdentity(personIdentityMock);

        String json = objectMapper.writeValueAsString(questionsRequest);

        when(mockExperianService.getResponseFromKBVExperianAPI(
                        json, "EXPERIAN_API_WRAPPER_SAA_RESOURCE"))
                .thenReturn(questionsResponse);

        doNothing().when(mockStorageService).update(kbvSessionItemMock);

        APIGatewayProxyResponseEvent response = questionHandler.handleRequest(input, contextMock);

        assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        assertEquals(TestData.EXPECTED_QUESTION_ONE, response.getBody());
    }

    @Test
    void shouldReturn200OkWhenCalledAgainAndReturnNextUnAnsweredQuestionFromStorage()
            throws IOException {
        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, "new-session-id");

        Context contextMock = mock(Context.class);
        KBVSessionItem kbvSessionItemMock = mock(KBVSessionItem.class);
        PersonIdentity personIdentityMock = mock(PersonIdentity.class);

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockStorageService.getSessionId(sessionHeader.get(HEADER_SESSION_ID)))
                .thenReturn(Optional.ofNullable(kbvSessionItemMock));
        String personIdentity = objectMapper.writeValueAsString(personIdentityMock);
        String questionState =
                "{\"qaPairs\":[{\"question\":{\"questionID\":\"Q00015\",\"text\":\"What is the outstanding balance of your current mortgage?\",\"tooltip\":\"The approximate amount in £s, including interest. A loan to buy property (or land) where the loan is secured by a charge on that property.\",\"answerHeldFlag\":null,\"answerFormat\":{\"identifier\":\"A00004\",\"fieldType\":\"G \",\"answerList\":[\"UP TO £10,000\",\"OVER £10,000 UP TO £35,000\",\"OVER £35,000 UP TO £60,000\",\"OVER £60,000 UP TO £85,000\",\"NONE OF THE ABOVE / DOES NOT APPLY\"]}},\"answer\":\"Answer given here\"},{\"question\":{\"questionID\":\"Q00040\",\"text\":\"How much was your recent loan for?\",\"tooltip\":\"The approximate starting balance, in £s, on an active personal loan. Does not include HP Loans, 2nd Mortgages or Home Credit.\",\"answerHeldFlag\":null,\"answerFormat\":{\"identifier\":\"A00004\",\"fieldType\":\"G \",\"answerList\":[\"UP TO £8,500\",\"OVER £8,500 UP TO £9,000\",\"OVER £9,000 UP TO £9,500\",\"OVER £9,500 UP TO £10,000\",\"NONE OF THE ABOVE / DOES NOT APPLY\"]}},\"answer\":null}],\"nextQuestion\":{\"empty\":false,\"present\":true},\"state\":\"RTQ\",\"answers\":[{\"questionId\":\"Q00040\",\"answer\":null},{\"questionId\":\"Q00015\",\"answer\":null}]}";

        Question question2 = mock(Question.class);

        when(questionStateMock.getNextQuestion()).thenReturn(Optional.ofNullable(question2));

        when(mockObjectMapper.writeValueAsString(question2)).thenReturn(TestData.EXPECTED_QUESTION);

        APIGatewayProxyResponseEvent response = questionHandler.handleRequest(input, contextMock);

        assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        assertEquals(TestData.EXPECTED_QUESTION_TWO, response.getBody());
    }

    @Test
    void shouldReturn204ErrorWhenNoFurtherQuestions() throws IOException, InterruptedException {
        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, "new-session-id");

        Context contextMock = mock(Context.class);
        KBVSessionItem kbvSessionItemMock = mock(KBVSessionItem.class);
        PersonIdentity personIdentityMock = mock(PersonIdentity.class);

        String personIdentity = objectMapper.writeValueAsString(personIdentityMock);
        String questionState =
                "{\"qaPairs\":[{\"question\":{\"questionID\":\"Q00015\",\"text\":\"What is the outstanding balance of your current mortgage?\",\"tooltip\":\"The approximate amount in £s, including interest. A loan to buy property (or land) where the loan is secured by a charge on that property.\",\"answerHeldFlag\":null,\"answerFormat\":{\"identifier\":\"A00004\",\"fieldType\":\"G \",\"answerList\":[\"UP TO £10,000\",\"OVER £10,000 UP TO £35,000\",\"OVER £35,000 UP TO £60,000\",\"OVER £60,000 UP TO £85,000\",\"NONE OF THE ABOVE / DOES NOT APPLY\"]}},\"answer\":\"Answer given here\"},{\"question\":{\"questionID\":\"Q00040\",\"text\":\"How much was your recent loan for?\",\"tooltip\":\"The approximate starting balance, in £s, on an active personal loan. Does not include HP Loans, 2nd Mortgages or Home Credit.\",\"answerHeldFlag\":null,\"answerFormat\":{\"identifier\":\"A00004\",\"fieldType\":\"G \",\"answerList\":[\"UP TO £8,500\",\"OVER £8,500 UP TO £9,000\",\"OVER £9,000 UP TO £9,500\",\"OVER £9,500 UP TO £10,000\",\"NONE OF THE ABOVE / DOES NOT APPLY\"]}},\"answer\":\"another answer given\"}],\"nextQuestion\":{\"empty\":false,\"present\":true},\"state\":\"RTQ\",\"answers\":[{\"questionId\":\"Q00040\",\"answer\":null},{\"questionId\":\"Q00015\",\"answer\":null}]}";

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockStorageService.getSessionId(sessionHeader.get(HEADER_SESSION_ID)))
                .thenReturn(Optional.ofNullable(kbvSessionItemMock));
        when(kbvSessionItemMock.getUserAttributes()).thenReturn(personIdentity);
        when(kbvSessionItemMock.getQuestionState()).thenReturn(questionState);

        String questionsResponse =
                "{\"control\":{\"urn\":\"e57adf7a-f4d9-4cae-a071-2742fb72c42b\",\"authRefNo\":\"7DCTTAC7HT\",\"dateTime\":null,\"testDatabase\":null,\"clientAccountNo\":null,\"clientBranchNo\":null,\"operatorID\":null,\"parameters\":null},\"questions\":null,\"results\":{\"outcome\":\"Insufficient Questions (Unable to Authenticate)\",\"authenticationResult\":\"Unable to Authenticate\",\"questions\":null,\"alerts\":null,\"nextTransId\":{\"string\":[\"END\"]},\"caseFoundFlag\":null,\"confirmationCode\":null},\"error\":null}";
        when(mockExperianService.getResponseFromKBVExperianAPI(
                        any(), eq("EXPERIAN_API_WRAPPER_SAA_RESOURCE")))
                .thenReturn(questionsResponse);

        APIGatewayProxyResponseEvent response = questionHandler.handleRequest(input, contextMock);
        // assertEquals(questionsResponse, response.getBody());
        assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());
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
    void shouldReturn500ErrorWhenAWSDynamoDBServiceDown() {
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
    void shouldReturn500ErrorWhenPersonIdentityCannotBeParsedToJSON() {
        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, "new-session-id");
        ArgumentCaptor<ILoggingEvent> loggingEventArgumentCaptor =
                ArgumentCaptor.forClass(ILoggingEvent.class);

        KBVSessionItem kbvSessionItemMock = mock(KBVSessionItem.class);
        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockStorageService.getSessionId(sessionHeader.get(HEADER_SESSION_ID)))
                .thenReturn(Optional.ofNullable(kbvSessionItemMock));
        when(kbvSessionItemMock.getUserAttributes()).thenReturn(TestData.EXPECTED_QUESTION_ONE);
        when(input.getHeaders()).thenReturn(sessionHeader);

        APIGatewayProxyResponseEvent response =
                questionHandler.handleRequest(input, mock(Context.class));

        verify(appender).doAppend(loggingEventArgumentCaptor.capture());
        ILoggingEvent event = loggingEventArgumentCaptor.getValue();
        assertEquals("Failed to parse object using ObjectMapper", event.getMessage());
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void shouldReturn500ErrorWhenExperianServiceIsDown() throws IOException, InterruptedException {
        ArgumentCaptor<ILoggingEvent> loggingEventArgumentCaptor =
                ArgumentCaptor.forClass(ILoggingEvent.class);
        KBVSessionItem kbvSessionItemMock = mock(KBVSessionItem.class);
        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, "new-session-id");

        PersonIdentity personIdentityMock = mock(PersonIdentity.class);
        QuestionState questionStateMock = mock(QuestionState.class);
        String personIdentity = objectMapper.writeValueAsString(personIdentityMock);
        String questionState = objectMapper.writeValueAsString(questionStateMock);

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockStorageService.getSessionId(sessionHeader.get(HEADER_SESSION_ID)))
                .thenReturn(Optional.ofNullable(kbvSessionItemMock));
        when(kbvSessionItemMock.getUserAttributes()).thenReturn(personIdentity);
        when(kbvSessionItemMock.getQuestionState()).thenReturn(questionState);

        doThrow(InterruptedException.class)
                .when(mockExperianService)
                .getResponseFromKBVExperianAPI(anyString(), anyString());

        APIGatewayProxyResponseEvent response =
                questionHandler.handleRequest(input, mock(Context.class));

        verify(appender).doAppend(loggingEventArgumentCaptor.capture());
        ILoggingEvent event = loggingEventArgumentCaptor.getValue();
        assertEquals(
                "Retrieving questions failed: java.lang.InterruptedException", event.getMessage());
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void shouldReturn204WhenAGivenSessionHasReceivedFinalResponseFromExperian() throws IOException {
        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, "new-session-id");

        Context contextMock = mock(Context.class);

        PersonIdentity personIdentityMock = mock(PersonIdentity.class);
        KBVSessionItem kbvSessionItemMock = mock(KBVSessionItem.class);
        Question questionOne = mock(Question.class);
        Question questionTwo = mock(Question.class);
        QuestionState questionStateMock = mock(QuestionState.class);
        QuestionAnswerPair qaPairOne = mock(QuestionAnswerPair.class);
        QuestionAnswerPair qaPairTwo = mock(QuestionAnswerPair.class);

        when(questionOne.getQuestionID()).thenReturn("Question 1");
        when(questionTwo.getQuestionID()).thenReturn("Question 2");

        when(qaPairOne.getQuestion()).thenReturn(questionOne);
        when(qaPairTwo.getQuestion()).thenReturn(questionTwo);
        when(qaPairOne.getAnswer()).thenReturn("Answered questionOne");
        when(qaPairTwo.getAnswer()).thenReturn("Answered questionTwo");
        when(questionStateMock.getQaPairs()).thenReturn(List.of(qaPairOne, qaPairTwo));
        // when(questionStateMock.getState()).thenReturn("END");
        String questionStateValue = objectMapper.writeValueAsString(questionStateMock);
        when(kbvSessionItemMock.getQuestionState()).thenReturn(questionStateValue);
        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockStorageService.getSessionId(sessionHeader.get(HEADER_SESSION_ID)))
                .thenReturn(Optional.ofNullable(kbvSessionItemMock));
        String personIdentity = objectMapper.writeValueAsString(personIdentityMock);
        String questionState = objectMapper.writeValueAsString(questionStateMock);
        when(kbvSessionItemMock.getUserAttributes()).thenReturn(personIdentity);
        when(kbvSessionItemMock.getQuestionState()).thenReturn(questionState);
        when(kbvSessionItemMock.getAuthorizationCode()).thenReturn("some-authorization-code");

        APIGatewayProxyResponseEvent response = questionHandler.handleRequest(input, contextMock);

        assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
    }
}
