package uk.gov.di.ipv.cri.kbv.api.handler;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.amazonaws.services.dynamodbv2.model.InternalServerErrorException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import uk.gov.di.ipv.cri.kbv.api.domain.Question;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswer;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerPair;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.kbv.api.handler.QuestionAnswerHandler.HEADER_SESSION_ID;

@ExtendWith(MockitoExtension.class)
class QuestionAnswerHandlerTest {

    public static final String INVALID_PAYLOAD =
            "\"questionID\":\" Q00015 \",\"answer\":\" some-answer \"";
    public static final String QUESTION_STATE_WITH_ONE_ANSWER =
            "{\"qaPairs\":[{\"question\":{\"questionID\":\"Question 1\",\"text\":null,\"tooltip\":null,\"answerHeldFlag\":null,\"answerFormat\":null},\"answer\":\"Answered questionOne\"},{\"question\":{\"questionID\":\"Question 2\",\"text\":null,\"tooltip\":null,\"answerHeldFlag\":null,\"answerFormat\":null},\"answer\":null}],\"nextQuestion\":{\"empty\":false,\"present\":true},\"state\":\"\",\"answers\":[{\"questionId\":\"Question 1\",\"answer\":\"Answered questionOne\"},{\"questionId\":\"Question 2\",\"answer\":null}]}";
    public static final String QUESTION_STATE_WITH_ALL_TWO_ANSWERED =
            "{\"qaPairs\":[{\"question\":{\"questionID\":\"Question 1\",\"text\":null,\"tooltip\":null,\"answerHeldFlag\":null,\"answerFormat\":null},\"answer\":\"Answered questionOne\"},{\"question\":{\"questionID\":\"Question 2\",\"text\":null,\"tooltip\":null,\"answerHeldFlag\":null,\"answerFormat\":null},\"answer\":\"Second Answer\"}],\"nextQuestion\":{\"empty\":true,\"present\":false},\"state\":\"\",\"answers\":[{\"questionId\":\"Question 1\",\"answer\":\"Answered questionOne\"},{\"questionId\":\"Question 2\",\"answer\":\"Second Answer\"}]}";
    public static final String EXPERIAN_RTQ_RESPONSE_WITH_QUESTION =
            "{\"control\":{\"urn\":\"f0746bbd-ed6f-44c7-bc3b-82c336c10815\",\"authRefNo\":\"7DCTWBC7LR\",\"dateTime\":null,\"testDatabase\":\"A\",\"clientAccountNo\":\"J8193\",\"clientBranchNo\":null,\"operatorID\":\"GDSCABINETUIIQ01U\",\"parameters\":{\"oneShotAuthentication\":\"N\",\"storeCaseData\":\"P\"}},\"questions\":{\"question\":[{\"questionID\":\"Q00015\",\"text\":\"What is the first James Bond movie?\",\"tooltip\":\"It was released in the 1960s.\",\"answerHeldFlag\":null,\"answerFormat\":{\"identifier\":\"A00004\",\"fieldType\":\"G \",\"answerList\":[\"Movie 1\",\"Movie 2\",\"Movie 3\"]}},{\"questionID\":\"Q00040\",\"text\":\"How much was your recent loan for?\",\"tooltip\":\"The approximate starting balance, in £s, on an active personal loan. Does not include HP Loans, 2nd Mortgages or Home Credit.\",\"answerFormat\":{\"identifier\":\"A00004\",\"fieldType\":\"G \",\"answerList\":[\"UP TO £8,500\",\"OVER £8,500 UP TO £9,000\",\"OVER £9,000 UP TO £9,500\",\"OVER £9,500 UP TO £10,000\",\"NONE OF THE ABOVE / DOES NOT APPLY\"]},\"answerHeldFlag\":null}],\"skipsRemaining\":null,\"skipWarning\":null},\"results\":{\"outcome\":\"Authentication Questions returned\",\"authenticationResult\":null,\"questions\":null,\"alerts\":null,\"nextTransId\":{\"string\":[\"RTQ\"]},\"caseFoundFlag\":null,\"confirmationCode\":null},\"error\":null}";
    public static final String EXPERIAN_END_RESPONSE_WITH_QUESTION =
            "{\"control\":{\"urn\":\"f0746bbd-ed6f-44c7-bc3b-82c336c10815\",\"authRefNo\":\"7DCTWBC7LR\",\"dateTime\":null,\"testDatabase\":\"A\",\"clientAccountNo\":\"J8193\",\"clientBranchNo\":null,\"operatorID\":\"GDSCABINETUIIQ01U\",\"parameters\":{\"oneShotAuthentication\":\"N\",\"storeCaseData\":\"P\"}},\"questions\":{\"question\":[{\"questionID\":\"Q00015\",\"text\":\"What is the first James Bond movie?\",\"tooltip\":\"It was released in the 1960s.\",\"answerHeldFlag\":null,\"answerFormat\":{\"identifier\":\"A00004\",\"fieldType\":\"G \",\"answerList\":[\"Movie 1\",\"Movie 2\",\"Movie 3\"]}},{\"questionID\":\"Q00040\",\"text\":\"How much was your recent loan for?\",\"tooltip\":\"The approximate starting balance, in £s, on an active personal loan. Does not include HP Loans, 2nd Mortgages or Home Credit.\",\"answerFormat\":{\"identifier\":\"A00004\",\"fieldType\":\"G \",\"answerList\":[\"UP TO £8,500\",\"OVER £8,500 UP TO £9,000\",\"OVER £9,000 UP TO £9,500\",\"OVER £9,500 UP TO £10,000\",\"NONE OF THE ABOVE / DOES NOT APPLY\"]},\"answerHeldFlag\":null}],\"skipsRemaining\":null,\"skipWarning\":null},\"results\":{\"outcome\":\"Authentication Questions returned\",\"authenticationResult\":null,\"questions\":null,\"alerts\":null,\"nextTransId\":{\"string\":[\"END\"]},\"caseFoundFlag\":null,\"confirmationCode\":null},\"error\":null}";

    private QuestionAnswerHandler questionAnswerHandler;
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    @Mock private StorageService mockStorageService;
    @Mock private ExperianService mockExperianService;
    @Mock private Appender<ILoggingEvent> appender;
    @Mock private APIGatewayProxyRequestEvent input;
    @Mock private Context contextMock;

    @BeforeEach
    void setUp() {
        Logger logger = (Logger) LoggerFactory.getLogger(QuestionAnswerHandler.class);
        logger.addAppender(appender);

        questionAnswerHandler = new QuestionAnswerHandler(mockStorageService, mockExperianService);
    }

    @Test
    void shouldReturn200WithWhen1stAnswerIsSubmitted() throws JsonProcessingException {
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, "new-session-id");

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
        when(questionStateMock.getQaPairs()).thenReturn(List.of(qaPairOne, qaPairTwo));

        String questionStateValue = objectMapper.writeValueAsString(questionStateMock);
        when(kbvSessionItemMock.getQuestionState()).thenReturn(questionStateValue);
        QuestionAnswer questionAnswer = mock(QuestionAnswer.class);
        when(questionAnswer.getQuestionId()).thenReturn("Question 1");
        when(questionAnswer.getAnswer()).thenReturn("Answered questionOne");

        String questionAnswerValue = objectMapper.writeValueAsString(questionAnswer);
        when(input.getHeaders()).thenReturn(sessionHeader);
        when(input.getBody()).thenReturn(questionAnswerValue);
        when(mockStorageService.getSessionId(sessionHeader.get(HEADER_SESSION_ID)))
                .thenReturn(Optional.ofNullable(kbvSessionItemMock));

        doNothing().when(kbvSessionItemMock).setQuestionState(QUESTION_STATE_WITH_ONE_ANSWER);
        doNothing().when(mockStorageService).update(kbvSessionItemMock);

        APIGatewayProxyResponseEvent result =
                questionAnswerHandler.handleRequest(input, contextMock);

        verify(mockStorageService, times(1)).update(kbvSessionItemMock);
        assertEquals(HttpStatus.SC_OK, result.getStatusCode());
        assertNull(result.getBody());
    }

    @Test
    void shouldReturn200WithFinalResponseFromExperianAPI()
            throws IOException, InterruptedException {

        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, "new-session-id");

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
        when(questionStateMock.getQaPairs()).thenReturn(List.of(qaPairOne, qaPairTwo));

        QuestionAnswer questionAnswer = mock(QuestionAnswer.class);
        when(questionAnswer.getQuestionId()).thenReturn("Question 2");
        when(questionAnswer.getAnswer()).thenReturn("Second Answer");

        String questionStateValue = objectMapper.writeValueAsString(questionStateMock);
        when(kbvSessionItemMock.getAuthRefNo()).thenReturn("authRefNo");
        when(kbvSessionItemMock.getQuestionState()).thenReturn(questionStateValue);
        String questionAnswerValue = objectMapper.writeValueAsString(questionAnswer);
        when(input.getHeaders()).thenReturn(sessionHeader);
        when(input.getBody()).thenReturn(questionAnswerValue);
        when(mockStorageService.getSessionId(sessionHeader.get(HEADER_SESSION_ID)))
                .thenReturn(Optional.ofNullable(kbvSessionItemMock));

        doNothing().when(kbvSessionItemMock).setQuestionState(QUESTION_STATE_WITH_ALL_TWO_ANSWERED);
        doNothing().when(mockStorageService).update(kbvSessionItemMock);

        when(mockExperianService.getResponseFromKBVExperianAPI(
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

        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, "new-session-id");

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
        when(questionStateMock.getQaPairs()).thenReturn(List.of(qaPairOne, qaPairTwo));

        QuestionAnswer questionAnswer = mock(QuestionAnswer.class);
        when(questionAnswer.getQuestionId()).thenReturn("Question 2");
        when(questionAnswer.getAnswer()).thenReturn("Second Answer");

        String questionStateValue = objectMapper.writeValueAsString(questionStateMock);
        when(kbvSessionItemMock.getAuthRefNo()).thenReturn("authRefno");
        when(kbvSessionItemMock.getQuestionState()).thenReturn(questionStateValue);
        String questionAnswerValue = objectMapper.writeValueAsString(questionAnswer);
        when(input.getHeaders()).thenReturn(sessionHeader);
        when(input.getBody()).thenReturn(questionAnswerValue);
        when(mockStorageService.getSessionId(sessionHeader.get(HEADER_SESSION_ID)))
                .thenReturn(Optional.ofNullable(kbvSessionItemMock));

        doNothing().when(kbvSessionItemMock).setQuestionState(QUESTION_STATE_WITH_ALL_TWO_ANSWERED);
        doNothing().when(mockStorageService).update(kbvSessionItemMock);

        when(mockExperianService.getResponseFromKBVExperianAPI(
                        any(), eq("EXPERIAN_API_WRAPPER_RTQ_RESOURCE")))
                .thenReturn(EXPERIAN_RTQ_RESPONSE_WITH_QUESTION);

        APIGatewayProxyResponseEvent response =
                questionAnswerHandler.handleRequest(input, contextMock);

        assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        assertNull(response.getBody());
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
        when(kbvSessionItemMock.getQuestionState()).thenReturn(INVALID_PAYLOAD);

        APIGatewayProxyResponseEvent response =
                questionAnswerHandler.handleRequest(input, contextMock);

        verify(appender).doAppend(loggingEventArgumentCaptor.capture());
        ILoggingEvent event = loggingEventArgumentCaptor.getValue();
        assertEquals("Failed to parse object using ObjectMapper", event.getMessage());
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // @Test
    void shouldReturn500ErrorWhenExperianAPIIsDown() throws IOException, InterruptedException {
        ArgumentCaptor<ILoggingEvent> loggingEventArgumentCaptor =
                ArgumentCaptor.forClass(ILoggingEvent.class);

        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, "new-session-id");

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
        when(questionStateMock.getQaPairs()).thenReturn(List.of(qaPairOne, qaPairTwo));

        QuestionAnswer questionAnswer = mock(QuestionAnswer.class);
        when(questionAnswer.getQuestionId()).thenReturn("Question 2");
        when(questionAnswer.getAnswer()).thenReturn("Second Answer");

        String questionStateValue = objectMapper.writeValueAsString(questionStateMock);
        when(kbvSessionItemMock.getQuestionState()).thenReturn(questionStateValue);
        when(kbvSessionItemMock.getAuthRefNo()).thenReturn("authRefNo");

        String questionAnswerValue = objectMapper.writeValueAsString(questionAnswer);
        when(input.getHeaders()).thenReturn(sessionHeader);
        when(input.getBody()).thenReturn(questionAnswerValue);
        when(mockStorageService.getSessionId(sessionHeader.get(HEADER_SESSION_ID)))
                .thenReturn(Optional.ofNullable(kbvSessionItemMock));

        doNothing().when(kbvSessionItemMock).setQuestionState(QUESTION_STATE_WITH_ALL_TWO_ANSWERED);
        doNothing().when(mockStorageService).update(kbvSessionItemMock);

        when(mockExperianService.getResponseFromKBVExperianAPI(
                        any(), eq("EXPERIAN_API_WRAPPER_RTQ_RESOURCE")))
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
