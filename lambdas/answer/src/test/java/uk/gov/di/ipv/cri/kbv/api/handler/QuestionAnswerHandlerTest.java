package uk.gov.di.ipv.cri.kbv.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.experian.uk.schema.experian.identityiq.services.webservice.Question;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.services.dynamodb.model.InternalServerErrorException;
import uk.gov.di.ipv.cri.common.library.exception.SqsException;
import uk.gov.di.ipv.cri.common.library.service.AuditService;
import uk.gov.di.ipv.cri.common.library.service.SessionService;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswer;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerPair;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.gateway.KBVGateway;
import uk.gov.di.ipv.cri.kbv.api.gateway.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.service.KBVService;
import uk.gov.di.ipv.cri.kbv.api.service.KBVStorageService;
import uk.gov.di.ipv.cri.kbv.api.service.KBVSystemProperty;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionAnswerHandlerTest {

    private static final String HEADER_SESSION_ID = "session-id";
    private QuestionAnswerHandler questionAnswerHandler;
    private final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new JavaTimeModule());
    @Mock private KBVStorageService mockKBVStorageService;
    @Mock private APIGatewayProxyRequestEvent input;
    @Mock private Context contextMock;
    @Mock private EventProbe mockEventProbe;
    @Mock private SessionService mockSessionService;
    @Mock private AuditService mockAuditService;
    @Mock private KBVGateway mockKBVGateway;
    @Mock private KBVSystemProperty mockSystemProperty;

    public static final String EXPERIAN_END_RESPONSE_WITH_QUESTION =
            "{\"control\":{\"urn\":\"f0746bbd-ed6f-44c7-bc3b-82c336c10815\",\"authRefNo\":\"7DCTWBC7LR\",\"dateTime\":null,\"testDatabase\":\"A\",\"clientAccountNo\":\"J8193\",\"clientBranchNo\":null,\"operatorID\":\"GDSCABINETUIIQ01U\",\"parameters\":{\"oneShotAuthentication\":\"N\",\"storeCaseData\":\"P\"}},\"questions\":{\"question\":[{\"questionID\":\"Q00015\",\"text\":\"What is the first James Bond movie?\",\"tooltip\":\"It was released in the 1960s.\",\"answerHeldFlag\":null,\"answerFormat\":{\"identifier\":\"A00004\",\"fieldType\":\"G \",\"answerList\":[\"Movie 1\",\"Movie 2\",\"Movie 3\"]}},{\"questionID\":\"Q00040\",\"text\":\"How much was your recent loan for?\",\"tooltip\":\"The approximate starting balance, in £s, on an active personal loan. Does not include HP Loans, 2nd Mortgages or Home Credit.\",\"answerFormat\":{\"identifier\":\"A00004\",\"fieldType\":\"G \",\"answerList\":[\"UP TO £8,500\",\"OVER £8,500 UP TO £9,000\",\"OVER £9,000 UP TO £9,500\",\"OVER £9,500 UP TO £10,000\",\"NONE OF THE ABOVE / DOES NOT APPLY\"]},\"answerHeldFlag\":null}],\"skipsRemaining\":null,\"skipWarning\":null},\"results\":{\"outcome\":\"Authentication Questions returned\",\"authenticationResult\":null,\"questions\":null,\"alerts\":null,\"nextTransId\":{\"string\":[\"END\"]},\"caseFoundFlag\":null,\"confirmationCode\":null},\"error\":null}";
    public static final String EXPERIAN_RTQ_RESPONSE_WITH_QUESTION =
            "{\"control\":{\"urn\":\"f0746bbd-ed6f-44c7-bc3b-82c336c10815\",\"authRefNo\":\"7DCTWBC7LR\",\"dateTime\":null,\"testDatabase\":\"A\",\"clientAccountNo\":\"J8193\",\"clientBranchNo\":null,\"operatorID\":\"GDSCABINETUIIQ01U\",\"parameters\":{\"oneShotAuthentication\":\"N\",\"storeCaseData\":\"P\"}},\"questions\":{\"question\":[{\"questionID\":\"Q00015\",\"text\":\"What is the first James Bond movie?\",\"tooltip\":\"It was released in the 1960s.\",\"answerHeldFlag\":null,\"answerFormat\":{\"identifier\":\"A00004\",\"fieldType\":\"G \",\"answerList\":[\"Movie 1\",\"Movie 2\",\"Movie 3\"]}},{\"questionID\":\"Q00040\",\"text\":\"How much was your recent loan for?\",\"tooltip\":\"The approximate starting balance, in £s, on an active personal loan. Does not include HP Loans, 2nd Mortgages or Home Credit.\",\"answerFormat\":{\"identifier\":\"A00004\",\"fieldType\":\"G \",\"answerList\":[\"UP TO £8,500\",\"OVER £8,500 UP TO £9,000\",\"OVER £9,000 UP TO £9,500\",\"OVER £9,500 UP TO £10,000\",\"NONE OF THE ABOVE / DOES NOT APPLY\"]},\"answerHeldFlag\":null}],\"skipsRemaining\":null,\"skipWarning\":null},\"results\":{\"outcome\":\"Authentication Questions returned\",\"authenticationResult\":null,\"questions\":null,\"alerts\":null,\"nextTransId\":{\"string\":[\"RTQ\"]},\"caseFoundFlag\":null,\"confirmationCode\":null},\"error\":null}";

    private KBVService spyKBVService;

    @BeforeEach
    void setUp() {
        doNothing().when(mockSystemProperty).save();
        spyKBVService = Mockito.spy(new KBVService(mockKBVGateway));

        questionAnswerHandler =
                new QuestionAnswerHandler(
                        mockKBVStorageService,
                        mockSystemProperty,
                        spyKBVService,
                        mockEventProbe,
                        mockSessionService,
                        mockAuditService);
    }

    @Test
    void shouldReturn200WithWhen1stAnswerIsSubmitted() throws JsonProcessingException {
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, UUID.randomUUID().toString());

        KBVItem kbvItemMock = mock(KBVItem.class);
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
        when(kbvItemMock.getQuestionState()).thenReturn(questionStateValue);
        QuestionAnswer questionAnswer = mock(QuestionAnswer.class);
        when(questionAnswer.getQuestionId()).thenReturn("Question 1");
        when(questionAnswer.getAnswer()).thenReturn("Answered questionOne");

        String questionAnswerValue = objectMapper.writeValueAsString(questionAnswer);
        when(input.getHeaders()).thenReturn(sessionHeader);
        when(input.getBody()).thenReturn(questionAnswerValue);
        when(mockKBVStorageService.getKBVItem(
                        UUID.fromString(sessionHeader.get(HEADER_SESSION_ID))))
                .thenReturn(kbvItemMock);

        doNothing().when(mockKBVStorageService).update(kbvItemMock);

        APIGatewayProxyResponseEvent result =
                questionAnswerHandler.handleRequest(input, contextMock);

        verify(input, times(2)).getHeaders();
        verify(input, times(2)).getBody();
        verify(mockKBVStorageService, times(1)).update(kbvItemMock);

        assertEquals(HttpStatusCode.OK, result.getStatusCode());
        assertNull(result.getBody());
    }

    @Test
    void shouldReturn200WithFinalResponseFromExperianAPI() throws IOException, SqsException {
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, UUID.randomUUID().toString());

        KBVItem kbvItemMock = mock(KBVItem.class);
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
        when(kbvItemMock.getQuestionState()).thenReturn(questionStateValue);
        String questionAnswerValue = objectMapper.writeValueAsString(questionAnswer);

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(input.getBody()).thenReturn(questionAnswerValue);
        when(mockKBVStorageService.getKBVItem(
                        UUID.fromString(sessionHeader.get(HEADER_SESSION_ID))))
                .thenReturn(kbvItemMock);

        doNothing().when(mockKBVStorageService).update(kbvItemMock);

        QuestionsResponse questionsResponse =
                objectMapper.readValue(
                        EXPERIAN_END_RESPONSE_WITH_QUESTION, QuestionsResponse.class);
        doReturn(questionsResponse).when(spyKBVService).submitAnswers(any());

        APIGatewayProxyResponseEvent result =
                questionAnswerHandler.handleRequest(input, contextMock);

        verify(mockKBVStorageService, times(2)).update(kbvItemMock);
        assertEquals(HttpStatusCode.OK, result.getStatusCode());
        assertNull(result.getBody());
    }

    @Test
    void shouldReturn200WhenNextSetOfQuestionsAreReceivedFromExperian() throws IOException {
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, UUID.randomUUID().toString());

        KBVItem kbvItemMock = mock(KBVItem.class);
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
        when(kbvItemMock.getQuestionState()).thenReturn(questionStateValue);
        String questionAnswerValue = objectMapper.writeValueAsString(questionAnswer);
        when(input.getHeaders()).thenReturn(sessionHeader);
        when(input.getBody()).thenReturn(questionAnswerValue);
        when(mockKBVStorageService.getKBVItem(
                        UUID.fromString(sessionHeader.get(HEADER_SESSION_ID))))
                .thenReturn(kbvItemMock);

        doNothing().when(mockKBVStorageService).update(kbvItemMock);

        QuestionsResponse questionsResponse =
                objectMapper.readValue(
                        EXPERIAN_RTQ_RESPONSE_WITH_QUESTION, QuestionsResponse.class);
        doReturn(questionsResponse).when(spyKBVService).submitAnswers(any());

        APIGatewayProxyResponseEvent response =
                questionAnswerHandler.handleRequest(input, contextMock);

        assertEquals(HttpStatusCode.OK, response.getStatusCode());
        assertNull(response.getBody());
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
        setupEventProbeErrorBehaviour();
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, UUID.randomUUID().toString());
        KBVItem kbvItemMock = mock(KBVItem.class);

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockKBVStorageService.getKBVItem(
                        UUID.fromString(sessionHeader.get(HEADER_SESSION_ID))))
                .thenReturn(kbvItemMock);
        when(kbvItemMock.getQuestionState()).thenReturn("Bad-json");

        APIGatewayProxyResponseEvent response =
                questionAnswerHandler.handleRequest(input, contextMock);

        assertEquals(
                "{ \"error\":\"Failed to parse object using ObjectMapper.\" }", response.getBody());
        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(mockEventProbe).counterMetric("post_answer", 0d);
    }

    @Test
    void shouldReturn500ErrorWhenExperianAPIIsDown() throws IOException {
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, UUID.randomUUID().toString());

        KBVItem kbvItemMock = mock(KBVItem.class);
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
        when(kbvItemMock.getQuestionState()).thenReturn(questionStateValue);
        String questionAnswerValue = objectMapper.writeValueAsString(questionAnswer);
        when(input.getHeaders()).thenReturn(sessionHeader);
        when(input.getBody()).thenReturn(questionAnswerValue);
        when(mockKBVStorageService.getKBVItem(
                        UUID.fromString(sessionHeader.get(HEADER_SESSION_ID))))
                .thenReturn(kbvItemMock);

        doNothing().when(mockKBVStorageService).update(kbvItemMock);
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
