package uk.gov.di.ipv.cri.kbv.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.experian.uk.schema.experian.identityiq.services.webservice.AnswerFormat;
import com.experian.uk.schema.experian.identityiq.services.webservice.Control;
import com.experian.uk.schema.experian.identityiq.services.webservice.Question;
import com.experian.uk.schema.experian.identityiq.services.webservice.Questions;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.dynamodb.model.InternalServerErrorException;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventType;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentityDetailed;
import uk.gov.di.ipv.cri.common.library.exception.SqsException;
import uk.gov.di.ipv.cri.common.library.service.AuditService;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.service.PersonIdentityService;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswer;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.exception.QuestionNotFoundException;
import uk.gov.di.ipv.cri.kbv.api.gateway.KBVGateway;
import uk.gov.di.ipv.cri.kbv.api.gateway.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.service.KBVService;
import uk.gov.di.ipv.cri.kbv.api.service.KBVStorageService;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import static uk.gov.di.ipv.cri.kbv.api.handler.QuestionHandler.GET_QUESTION;
import static uk.gov.di.ipv.cri.kbv.api.handler.QuestionHandler.HEADER_SESSION_ID;

@ExtendWith(MockitoExtension.class)
class QuestionHandlerTest {
    private QuestionHandler questionHandler;
    @Mock private ObjectMapper mockObjectMapper;
    @Mock private KBVStorageService mockKBVStorageService;
    @Mock private PersonIdentityService mockPersonIdentityService;
    @Mock private EventProbe mockEventProbe;
    @Mock private KBVGateway mockKBVGateway;
    @Mock private ConfigurationService mockConfigurationService;
    @Mock private AuditService mockAuditService;
    private KBVService spyKBVService;

    @BeforeEach
    void setUp() {
        spyKBVService = Mockito.spy(new KBVService(mockKBVGateway));
        questionHandler =
                new QuestionHandler(
                        mockObjectMapper,
                        mockKBVStorageService,
                        mockPersonIdentityService,
                        spyKBVService,
                        mockConfigurationService,
                        mockEventProbe,
                        mockAuditService);
    }

    @Nested
    class QuestionHandlerCalled {
        @Test
        void shouldReturn200OkWhen1stCalledAndReturn1stUnAnsweredQuestionFromExperianEndpoint()
                throws IOException, SqsException {
            APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
            Map<String, String> sessionHeader =
                    Map.of(HEADER_SESSION_ID, UUID.randomUUID().toString());

            KBVItem kbvItem = new KBVItem();
            kbvItem.setSessionId(UUID.fromString(sessionHeader.get(HEADER_SESSION_ID)));
            PersonIdentityDetailed personIdentity = mock(PersonIdentityDetailed.class);

            when(input.getHeaders()).thenReturn(sessionHeader);
            when(mockPersonIdentityService.getPersonIdentityDetailed(kbvItem.getSessionId()))
                    .thenReturn(personIdentity);
            doNothing().when(mockKBVStorageService).save(any());

            String expectedQuestion = new ObjectMapper().writeValueAsString(getQuestionOne());

            doReturn(getExperianQuestionResponse(List.of(getQuestionOne(), getQuestionTwo())))
                    .when(spyKBVService)
                    .getQuestions(any());
            when(mockObjectMapper.writeValueAsString(any())).thenReturn(expectedQuestion);
            APIGatewayProxyResponseEvent response =
                    questionHandler.handleRequest(input, mock(Context.class));

            assertEquals(HttpStatusCode.OK, response.getStatusCode());
            assertEquals(expectedQuestion, response.getBody());

            verify(mockPersonIdentityService).getPersonIdentityDetailed(kbvItem.getSessionId());
            verify(mockAuditService).sendAuditEvent(AuditEventType.REQUEST_SENT, personIdentity);
            verify(mockKBVStorageService).save(any());
            verify(mockObjectMapper, times(2)).writeValueAsString(any());
            verify(mockEventProbe).counterMetric(GET_QUESTION);
        }

        @Test
        void shouldReturn200OkWhenCalledAgainAndReturnNextUnAnsweredQuestionFromStorage()
                throws IOException {
            APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
            Map<String, String> sessionHeader =
                    Map.of(HEADER_SESSION_ID, UUID.randomUUID().toString());

            KBVItem kbvItem = new KBVItem();
            kbvItem.setSessionId(UUID.fromString(sessionHeader.get(HEADER_SESSION_ID)));

            QuestionState questionState = new QuestionState();
            Questions questions = new Questions();

            Question answeredQuestion = getQuestionOne();
            QuestionAnswer questionAnswer = new QuestionAnswer();
            questionAnswer.setQuestionId(answeredQuestion.getQuestionID());
            questionAnswer.setAnswer("OVER £35,000 UP TO £60,000");

            Question unAnsweredQuestion = getQuestionTwo();
            questions.getQuestion().addAll(List.of(answeredQuestion, unAnsweredQuestion));

            questionState.setQAPairs(questions);
            questionState.setAnswer(questionAnswer);

            when(input.getHeaders()).thenReturn(sessionHeader);
            when(mockKBVStorageService.getKBVItem(
                            UUID.fromString(sessionHeader.get(HEADER_SESSION_ID))))
                    .thenReturn(kbvItem);
            when(mockObjectMapper.readValue(kbvItem.getQuestionState(), QuestionState.class))
                    .thenReturn(questionState);
            String expectedQuestion = new ObjectMapper().writeValueAsString(unAnsweredQuestion);
            when(mockObjectMapper.writeValueAsString(unAnsweredQuestion))
                    .thenReturn(expectedQuestion);

            APIGatewayProxyResponseEvent response =
                    questionHandler.handleRequest(input, mock(Context.class));

            assertEquals(HttpStatusCode.OK, response.getStatusCode());
            assertEquals(expectedQuestion, response.getBody());
            verify(mockKBVStorageService)
                    .getKBVItem(UUID.fromString(sessionHeader.get(HEADER_SESSION_ID)));
            verify(mockObjectMapper).readValue(kbvItem.getQuestionState(), QuestionState.class);
            verify(mockObjectMapper).writeValueAsString(unAnsweredQuestion);
            verify(mockEventProbe).counterMetric(GET_QUESTION);
        }

        @Test
        void shouldReturn500ErrorWhenThereAreNoFurtherQuestions() throws IOException {
            Context contextMock = mock(Context.class);
            APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
            Map<String, String> sessionHeader =
                    Map.of(HEADER_SESSION_ID, UUID.randomUUID().toString());
            setupEventProbeErrorBehaviour();

            PersonIdentityDetailed personIdentity = mock(PersonIdentityDetailed.class);
            KBVItem kbvItem = new KBVItem();
            kbvItem.setSessionId(UUID.fromString(sessionHeader.get(HEADER_SESSION_ID)));
            QuestionState questionStateMock = mock(QuestionState.class);

            when(input.getHeaders()).thenReturn(sessionHeader);
            when(mockKBVStorageService.getKBVItem(
                            UUID.fromString(sessionHeader.get(HEADER_SESSION_ID))))
                    .thenReturn(kbvItem);
            when(mockPersonIdentityService.getPersonIdentityDetailed(kbvItem.getSessionId()))
                    .thenReturn(personIdentity);

            when(mockObjectMapper.readValue(kbvItem.getQuestionState(), QuestionState.class))
                    .thenReturn(questionStateMock);

            APIGatewayProxyResponseEvent response =
                    questionHandler.handleRequest(input, contextMock);

            assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR, response.getStatusCode());

            verify(mockKBVStorageService)
                    .getKBVItem(UUID.fromString(sessionHeader.get(HEADER_SESSION_ID)));

            verify(mockPersonIdentityService).getPersonIdentityDetailed(kbvItem.getSessionId());
            verify(mockObjectMapper).readValue(kbvItem.getQuestionState(), QuestionState.class);
            verify(mockEventProbe).counterMetric(GET_QUESTION, 0d);
            assertEquals("{ \"error\":\"Question not Found\" }", response.getBody());
        }

        @Test
        void shouldReturn400ErrorWhenNoSessionIdProvided() {
            APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
            setupEventProbeErrorBehaviour();

            APIGatewayProxyResponseEvent response =
                    questionHandler.handleRequest(input, mock(Context.class));

            String expectedMessage = "java.lang.NullPointerException";
            assertTrue(response.getBody().contains(expectedMessage));
            assertEquals(HttpStatusCode.BAD_REQUEST, response.getStatusCode());
            verify(mockEventProbe).counterMetric(GET_QUESTION, 0d);
        }

        @Test
        void shouldReturn500ErrorWhenAWSDynamoDBServiceDown() {
            APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
            Map<String, String> sessionHeader =
                    Map.of(HEADER_SESSION_ID, UUID.randomUUID().toString());

            when(input.getHeaders()).thenReturn(sessionHeader);
            doThrow(InternalServerErrorException.class)
                    .when(mockKBVStorageService)
                    .getKBVItem(UUID.fromString(sessionHeader.get(HEADER_SESSION_ID)));

            setupEventProbeErrorBehaviour();
            APIGatewayProxyResponseEvent response =
                    questionHandler.handleRequest(input, mock(Context.class));

            assertEquals("{ \"error\":\"AWS Server error occurred.\" }", response.getBody());
            assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR, response.getStatusCode());
            verify(mockEventProbe).counterMetric(GET_QUESTION, 0d);
        }

        @Test
        void shouldReturn500ErrorWhenPersonIdentityCannotBeRetrievedDueToAnAwsError() {
            APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
            Map<String, String> sessionHeader =
                    Map.of(HEADER_SESSION_ID, UUID.randomUUID().toString());

            when(input.getHeaders()).thenReturn(sessionHeader);

            AwsErrorDetails awsErrorDetails =
                    AwsErrorDetails.builder()
                            .errorCode("")
                            .sdkHttpResponse(
                                    SdkHttpResponse.builder()
                                            .statusCode(HttpStatusCode.INTERNAL_SERVER_ERROR)
                                            .build())
                            .errorMessage("AWS DynamoDbException Occurred")
                            .build();
            when(mockPersonIdentityService.getPersonIdentityDetailed(
                            UUID.fromString(sessionHeader.get(HEADER_SESSION_ID))))
                    .thenThrow(
                            AwsServiceException.builder()
                                    .statusCode(500)
                                    .awsErrorDetails(awsErrorDetails)
                                    .build());

            setupEventProbeErrorBehaviour();
            APIGatewayProxyResponseEvent response =
                    questionHandler.handleRequest(input, mock(Context.class));

            assertEquals("{ \"error\":\"AWS Server error occurred.\" }", response.getBody());
            assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR, response.getStatusCode());

            verify(mockPersonIdentityService)
                    .getPersonIdentityDetailed(
                            UUID.fromString(sessionHeader.get(HEADER_SESSION_ID)));
            verify(mockEventProbe).counterMetric(GET_QUESTION, 0d);
        }

        @Test
        void shouldReturn500ErrorWhenExperianServiceIsDown() throws IOException {

            APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
            Map<String, String> sessionHeader =
                    Map.of(HEADER_SESSION_ID, UUID.randomUUID().toString());

            KBVItem kbvItemMock = mock(KBVItem.class);
            PersonIdentityDetailed personIdentityMock = mock(PersonIdentityDetailed.class);
            QuestionState questionStateMock = mock(QuestionState.class);

            when(input.getHeaders()).thenReturn(sessionHeader);
            when(mockKBVStorageService.getKBVItem(
                            UUID.fromString(sessionHeader.get(HEADER_SESSION_ID))))
                    .thenReturn(kbvItemMock);
            when(mockPersonIdentityService.getPersonIdentityDetailed(
                            UUID.fromString(sessionHeader.get(HEADER_SESSION_ID))))
                    .thenReturn(personIdentityMock);

            when(mockObjectMapper.readValue(kbvItemMock.getQuestionState(), QuestionState.class))
                    .thenReturn(questionStateMock);

            doThrow(RuntimeException.class)
                    .when(spyKBVService)
                    .getQuestions(any(QuestionRequest.class));

            setupEventProbeErrorBehaviour();
            APIGatewayProxyResponseEvent response =
                    questionHandler.handleRequest(input, mock(Context.class));

            assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR, response.getStatusCode());
            verify(mockEventProbe).counterMetric(GET_QUESTION, 0d);
        }

        @Test
        void shouldReturn204WhenAGivenSessionHasReceivedFinalResponseFromExperian()
                throws IOException {

            APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
            Map<String, String> sessionHeader =
                    Map.of(HEADER_SESSION_ID, UUID.randomUUID().toString());

            Context contextMock = mock(Context.class);
            KBVItem kbvItemMock = mock(KBVItem.class);
            QuestionState questionStateMock = mock(QuestionState.class);

            when(input.getHeaders()).thenReturn(sessionHeader);
            when(mockKBVStorageService.getKBVItem(
                            UUID.fromString(sessionHeader.get(HEADER_SESSION_ID))))
                    .thenReturn(kbvItemMock);

            when(mockObjectMapper.readValue(kbvItemMock.getQuestionState(), QuestionState.class))
                    .thenReturn(questionStateMock);

            when(mockEventProbe.counterMetric(GET_QUESTION)).thenReturn(mockEventProbe);
            when(kbvItemMock.getStatus()).thenReturn("status-code");
            APIGatewayProxyResponseEvent response =
                    questionHandler.handleRequest(input, contextMock);

            assertEquals(HttpStatusCode.NO_CONTENT, response.getStatusCode());
            assertNull(response.getBody());
            verify(mockEventProbe).counterMetric(GET_QUESTION);
        }
    }

    @Nested
    class ProcessQuestionRequest {
        @Test
        void shouldThrowQuestionNotFoundExceptionWhenQuestionStateAndKbvItemEmptyObjects() {
            assertThrows(
                    QuestionNotFoundException.class,
                    () ->
                            questionHandler.processQuestionRequest(
                                    new QuestionState(), new KBVItem()),
                    "Question not Found");
        }

        @Test
        void shouldReturnThrowErrorWhenQuestionStateIsNull() {
            var kbvItem = new KBVItem();
            NullPointerException expectedException =
                    assertThrows(
                            NullPointerException.class,
                            () -> questionHandler.processQuestionRequest(null, kbvItem));

            assertEquals("questionState cannot be null", expectedException.getMessage());
        }

        @Test
        void shouldReturnThrowErrorWhenKbvItemIsNull() {
            var questionState = new QuestionState();
            NullPointerException expectedException =
                    assertThrows(
                            NullPointerException.class,
                            () -> questionHandler.processQuestionRequest(questionState, null));

            assertEquals("kbvItem cannot be null", expectedException.getMessage());
        }

        @Test
        void shouldReturnNextQuestionFromDbStoreWhenThereIsAnUnansweredQuestionInStorage()
                throws IOException, SqsException {
            QuestionState questionState = new QuestionState();
            Questions questions = new Questions();

            Question answeredQuestion = getQuestionOne();
            QuestionAnswer questionAnswer = new QuestionAnswer();
            questionAnswer.setQuestionId(answeredQuestion.getQuestionID());
            questionAnswer.setAnswer("OVER £35,000 UP TO £60,000");

            Question unAnsweredQuestion = getQuestionTwo();
            questions.getQuestion().addAll(List.of(answeredQuestion, unAnsweredQuestion));

            questionState.setQAPairs(questions);
            questionState.setAnswer(questionAnswer);

            Question nextQuestion =
                    questionHandler.processQuestionRequest(questionState, mock(KBVItem.class));

            assertEquals(nextQuestion.getQuestionID(), unAnsweredQuestion.getQuestionID());
        }

        @Test
        void shouldReturnNextQuestionFromExperianWhenFirstCalled()
                throws IOException, SqsException {
            QuestionState questionState = new QuestionState();
            PersonIdentityDetailed personIdentity = mock(PersonIdentityDetailed.class);

            KBVItem kbvItem = new KBVItem();
            UUID sessionId = UUID.randomUUID();
            kbvItem.setSessionId(sessionId);

            when(mockPersonIdentityService.getPersonIdentityDetailed(sessionId))
                    .thenReturn(personIdentity);
            doReturn(getExperianQuestionResponse()).when(spyKBVService).getQuestions(any());
            Question nextQuestionFromExperian =
                    questionHandler.processQuestionRequest(questionState, kbvItem);

            assertEquals(
                    nextQuestionFromExperian.getQuestionID(),
                    getExperianQuestionResponse()
                            .getQuestions()
                            .getQuestion()
                            .get(0)
                            .getQuestionID());
        }

        @Test
        void shouldReturnNextQuestionFromExperianWhenBothAnQuestionsInStorageAreAnswered()
                throws IOException, SqsException {
            QuestionState questionState = new QuestionState();
            Questions questions = new Questions();

            Question firstAnsweredQuestion = getQuestionOne();
            QuestionAnswer questionAnswerOne = new QuestionAnswer();
            questionAnswerOne.setQuestionId(firstAnsweredQuestion.getQuestionID());
            questionAnswerOne.setAnswer("OVER £35,000 UP TO £60,000");

            Question secondAnsweredQuestion = getQuestionTwo();
            QuestionAnswer questionAnswerTwo = new QuestionAnswer();
            questionAnswerTwo.setQuestionId(secondAnsweredQuestion.getQuestionID());
            questionAnswerTwo.setAnswer("Blue");

            questions.getQuestion().addAll(List.of(firstAnsweredQuestion, secondAnsweredQuestion));

            questionState.setQAPairs(questions);
            questionState.setAnswer(questionAnswerOne);
            questionState.setAnswer(questionAnswerTwo);

            KBVItem kbvItem =
                    getADummyKBVItemThatRepresentAnItemInStorage(
                            new ObjectMapper().writeValueAsString(questionState));
            when(mockObjectMapper.readValue(kbvItem.getQuestionState(), QuestionState.class))
                    .thenReturn(questionState);
            doReturn(getExperianQuestionResponse()).when(spyKBVService).submitAnswers(any());
            Question nextQuestionFromExperian =
                    questionHandler.processQuestionRequest(questionState, kbvItem);

            assertEquals(
                    nextQuestionFromExperian.getQuestionID(),
                    getExperianQuestionResponse()
                            .getQuestions()
                            .getQuestion()
                            .get(0)
                            .getQuestionID());
        }
    }

    private KBVItem getADummyKBVItemThatRepresentAnItemInStorage(String questionStateString) {
        KBVItem kbvItem = new KBVItem();
        kbvItem.setQuestionState(questionStateString);
        kbvItem.setExpiryDate(9090L); // this implies kbvItem already exists
        return kbvItem;
    }

    private void setupEventProbeErrorBehaviour() {
        when(mockEventProbe.counterMetric(anyString(), anyDouble())).thenReturn(mockEventProbe);
        when(mockEventProbe.log(any(Level.class), any(Exception.class))).thenReturn(mockEventProbe);
    }

    private Question getQuestionOne() {
        Question question = new Question();
        question.setQuestionID("Q00015");
        question.setText("What is the outstanding balance ");
        question.setTooltip("outstanding balance tooltip");
        AnswerFormat answerFormat = new AnswerFormat();
        answerFormat.setIdentifier("A00004");
        answerFormat.setIdentifier("G");
        answerFormat
                .getAnswerList()
                .addAll(
                        List.of(
                                "UP TO £10,000",
                                "OVER £35,000 UP TO £60,000",
                                "OVER £60,000 UP TO £85,000",
                                "NONE OF THE ABOVE / DOES NOT APPLY"));
        question.setAnswerFormat(answerFormat);
        return question;
    }

    private Question getQuestionTwo() {
        Question question = new Question();
        question.setQuestionID("Q00040");
        question.setText("What your favorite color");
        question.setTooltip("favorite color tooltip");

        AnswerFormat answerFormat = new AnswerFormat();
        answerFormat.setIdentifier("A00005");
        answerFormat.setIdentifier("G");
        answerFormat
                .getAnswerList()
                .addAll(List.of("Blue", "Red", "Green", "NONE OF THE ABOVE / DOES NOT APPLY"));
        question.setAnswerFormat(answerFormat);
        return question;
    }

    private QuestionsResponse getExperianQuestionResponse() {
        return getExperianQuestionResponse(Collections.singletonList(new Question()));
    }

    private QuestionsResponse getExperianQuestionResponse(List<Question> questionList) {
        QuestionsResponse questionsResponse = new QuestionsResponse();
        Questions questions = new Questions();
        Control control = new Control();
        control.setAuthRefNo("authrefno");
        control.setURN("urn");
        questions.getQuestion().addAll(questionList);
        questionsResponse.setQuestions(questions);
        questionsResponse.setControl(control);

        return questionsResponse;
    }
}
