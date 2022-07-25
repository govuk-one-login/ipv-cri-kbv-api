package uk.gov.di.ipv.cri.kbv.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.dynamodb.model.InternalServerErrorException;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventContext;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventType;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentityDetailed;
import uk.gov.di.ipv.cri.common.library.exception.SqsException;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;
import uk.gov.di.ipv.cri.common.library.service.AuditService;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.service.PersonIdentityService;
import uk.gov.di.ipv.cri.common.library.service.SessionService;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestion;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestionOptions;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswer;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.exception.QuestionNotFoundException;
import uk.gov.di.ipv.cri.kbv.api.gateway.KBVGateway;
import uk.gov.di.ipv.cri.kbv.api.service.KBVService;
import uk.gov.di.ipv.cri.kbv.api.service.KBVStorageService;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.kbv.api.handler.QuestionHandler.HEADER_SESSION_ID;
import static uk.gov.di.ipv.cri.kbv.api.handler.QuestionHandler.IIQ_STRATEGY_PARAM_NAME;
import static uk.gov.di.ipv.cri.kbv.api.handler.QuestionHandler.LAMBDA_NAME;
import static uk.gov.di.ipv.cri.kbv.api.handler.QuestionHandler.METRIC_DIMENSION_QUESTION_ID;
import static uk.gov.di.ipv.cri.kbv.api.handler.QuestionHandler.METRIC_DIMENSION_QUESTION_STRATEGY;

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
    @Mock private SessionService sessionService;
    @Captor private ArgumentCaptor<Map<String, Object>> auditEventMap;
    @Captor private ArgumentCaptor<AuditEventContext> auditEventContextArgCaptor;

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
                        mockAuditService,
                        sessionService);
    }

    @Nested
    class QuestionHandlerCalled {
        @Test
        void shouldReturn200OkWhen1stCalledAndReturn1stUnAnsweredQuestionFromExperianEndpoint()
                throws IOException, SqsException {
            APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
            Map<String, String> requestHeaders =
                    Map.of(HEADER_SESSION_ID, UUID.randomUUID().toString());

            KBVItem kbvItem = new KBVItem();
            kbvItem.setSessionId(UUID.fromString(requestHeaders.get(HEADER_SESSION_ID)));
            PersonIdentityDetailed personIdentity = mock(PersonIdentityDetailed.class);
            SessionItem sessionItem = mock(SessionItem.class);

            when(input.getHeaders()).thenReturn(requestHeaders);
            when(sessionService.validateSessionId(requestHeaders.get(HEADER_SESSION_ID)))
                    .thenReturn(sessionItem);
            when(mockPersonIdentityService.getPersonIdentityDetailed(kbvItem.getSessionId()))
                    .thenReturn(personIdentity);
            doNothing().when(mockKBVStorageService).save(any());

            String expectedQuestion = new ObjectMapper().writeValueAsString(getQuestionOne());

            doReturn(
                            getExperianQuestionResponse(
                                    new KbvQuestion[] {getQuestionOne(), getQuestionTwo()}))
                    .when(spyKBVService)
                    .getQuestions(any());
            when(mockObjectMapper.writeValueAsString(any())).thenReturn(expectedQuestion);
            when(mockConfigurationService.getParameterValue(IIQ_STRATEGY_PARAM_NAME))
                    .thenReturn("3 out of 4");
            APIGatewayProxyResponseEvent response =
                    questionHandler.handleRequest(input, mock(Context.class));

            assertEquals(HttpStatusCode.OK, response.getStatusCode());
            assertEquals(expectedQuestion, response.getBody());

            verify(mockPersonIdentityService).getPersonIdentityDetailed(kbvItem.getSessionId());
            verify(mockAuditService)
                    .sendAuditEvent(
                            eq(AuditEventType.REQUEST_SENT), auditEventContextArgCaptor.capture());
            verify(mockKBVStorageService).save(any());
            verify(mockConfigurationService).getParameterValue("IIQStrategy");
            verify(mockConfigurationService).getParameterValue("IIQOperatorId");
            verify(mockObjectMapper).writeValueAsString(any());
            verify(mockEventProbe).counterMetric(LAMBDA_NAME);
            verify(mockEventProbe)
                    .addDimensions(Map.of(METRIC_DIMENSION_QUESTION_STRATEGY, "3 out of 4"));
            verify(mockEventProbe).addDimensions(Map.of(METRIC_DIMENSION_QUESTION_ID, "Q00015"));
            verifyNoMoreInteractions(mockEventProbe);

            assertEquals(sessionItem, auditEventContextArgCaptor.getValue().getSessionItem());
            assertEquals(requestHeaders, auditEventContextArgCaptor.getValue().getRequestHeaders());
            assertEquals(personIdentity, auditEventContextArgCaptor.getValue().getPersonIdentity());
        }

        @Test
        void shouldReturn200OkWhenCalledAgainAndReturnNextUnAnsweredQuestionFromStorage()
                throws IOException {
            APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
            Map<String, String> sessionHeader =
                    Map.of(HEADER_SESSION_ID, UUID.randomUUID().toString());

            KBVItem kbvItem = new KBVItem();
            kbvItem.setSessionId(UUID.fromString(sessionHeader.get(HEADER_SESSION_ID)));

            KbvQuestion answeredQuestion = getQuestionOne();
            QuestionAnswer questionAnswer = new QuestionAnswer();
            questionAnswer.setQuestionId(answeredQuestion.getQuestionId());
            questionAnswer.setAnswer("OVER £35,000 UP TO £60,000");

            KbvQuestion unAnsweredQuestion = getQuestionTwo();

            QuestionState questionState = new QuestionState();
            questionState.setQAPairs(new KbvQuestion[] {answeredQuestion, unAnsweredQuestion});
            questionState.setAnswer(questionAnswer);

            when(input.getHeaders()).thenReturn(sessionHeader);
            when(mockKBVStorageService.getKBVItem(
                            UUID.fromString(sessionHeader.get(HEADER_SESSION_ID))))
                    .thenReturn(kbvItem);
            when(mockObjectMapper.readValue(kbvItem.getQuestionState(), QuestionState.class))
                    .thenReturn(questionState);
            String expectedQuestion = new ObjectMapper().writeValueAsString(unAnsweredQuestion);

            APIGatewayProxyResponseEvent response =
                    questionHandler.handleRequest(input, mock(Context.class));

            assertEquals(HttpStatusCode.OK, response.getStatusCode());
            assertEquals(expectedQuestion, response.getBody());
            verify(mockKBVStorageService)
                    .getKBVItem(UUID.fromString(sessionHeader.get(HEADER_SESSION_ID)));
            verify(mockConfigurationService, times(0)).getParameterValue("IIQOperatorId");
            verify(mockObjectMapper).readValue(kbvItem.getQuestionState(), QuestionState.class);
            verify(mockConfigurationService, times(0)).getParameterValue("IIQStrategy");
            verify(mockEventProbe).counterMetric(LAMBDA_NAME);
        }

        @Test
        void shouldReturn204WhenThereAreNoQuestions() throws IOException {
            Context contextMock = mock(Context.class);
            QuestionsResponse questionsResponse = mock(QuestionsResponse.class);
            APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
            Map<String, String> sessionHeader =
                    Map.of(HEADER_SESSION_ID, UUID.randomUUID().toString());
            setupEventProbeErrorBehaviour();

            PersonIdentityDetailed personIdentity = mock(PersonIdentityDetailed.class);
            KBVItem kbvItem = new KBVItem();
            kbvItem.setSessionId(UUID.fromString(sessionHeader.get(HEADER_SESSION_ID)));
            QuestionState questionStateMock = mock(QuestionState.class);
            when(mockKBVGateway.getQuestions(any(QuestionRequest.class)))
                    .thenReturn(questionsResponse);

            when(input.getHeaders()).thenReturn(sessionHeader);
            when(mockKBVStorageService.getKBVItem(
                            UUID.fromString(sessionHeader.get(HEADER_SESSION_ID))))
                    .thenReturn(kbvItem);
            when(mockPersonIdentityService.getPersonIdentityDetailed(kbvItem.getSessionId()))
                    .thenReturn(personIdentity);

            when(mockObjectMapper.readValue(kbvItem.getQuestionState(), QuestionState.class))
                    .thenReturn(questionStateMock);

            when(mockConfigurationService.getParameterValue(IIQ_STRATEGY_PARAM_NAME))
                    .thenReturn("3 out of 4");

            APIGatewayProxyResponseEvent response =
                    questionHandler.handleRequest(input, contextMock);

            assertEquals(HttpStatusCode.NO_CONTENT, response.getStatusCode());
            assertNull(response.getBody());

            verify(mockKBVStorageService)
                    .getKBVItem(UUID.fromString(sessionHeader.get(HEADER_SESSION_ID)));

            verify(mockPersonIdentityService).getPersonIdentityDetailed(kbvItem.getSessionId());
            verify(mockObjectMapper).readValue(kbvItem.getQuestionState(), QuestionState.class);
            verify(mockConfigurationService).getParameterValue("IIQStrategy");
            verify(mockConfigurationService).getParameterValue("IIQOperatorId");
            verify(mockEventProbe).counterMetric(LAMBDA_NAME, 0d);
            verify(mockEventProbe)
                    .addDimensions(Map.of(METRIC_DIMENSION_QUESTION_STRATEGY, "3 out of 4"));
            verifyNoMoreInteractions(mockEventProbe);
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
            verify(mockEventProbe).counterMetric(LAMBDA_NAME, 0d);
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

            assertEquals("{\"error\":\"AWS Server error occurred.\"}", response.getBody());
            assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR, response.getStatusCode());
            verify(mockEventProbe).counterMetric(LAMBDA_NAME, 0d);
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

            assertEquals("{\"error\":\"AWS Server error occurred.\"}", response.getBody());
            assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR, response.getStatusCode());

            verify(mockPersonIdentityService)
                    .getPersonIdentityDetailed(
                            UUID.fromString(sessionHeader.get(HEADER_SESSION_ID)));
            verify(mockEventProbe).counterMetric(LAMBDA_NAME, 0d);
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
            verify(mockEventProbe).counterMetric(LAMBDA_NAME, 0d);
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

            when(mockEventProbe.counterMetric(LAMBDA_NAME)).thenReturn(mockEventProbe);
            when(kbvItemMock.getStatus()).thenReturn("status-code");
            APIGatewayProxyResponseEvent response =
                    questionHandler.handleRequest(input, contextMock);

            assertEquals(HttpStatusCode.NO_CONTENT, response.getStatusCode());
            assertNull(response.getBody());
            verify(mockEventProbe).counterMetric(LAMBDA_NAME);
            verify(mockConfigurationService, times(0)).getParameterValue("IIQStrategy");
            verify(mockConfigurationService, times(0)).getParameterValue("IIQOperatorId");
        }
    }

    @Nested
    class ProcessQuestionRequest {
        @Test
        void shouldThrowQuestionNotFoundExceptionWhenQuestionStateAndKbvItemEmptyObjects()
                throws SqsException {
            String expectedOutcome = "Insufficient Questions (Unable to Authenticate)";
            UUID sessionId = UUID.randomUUID();
            KBVItem kbvItem = mock(KBVItem.class);
            SessionItem sessionItem = mock(SessionItem.class);
            Map<String, String> requestHeaders = new HashMap<>();

            QuestionsResponse questionsResponse = mock(QuestionsResponse.class);
            when(kbvItem.getSessionId()).thenReturn(sessionId);
            when(questionsResponse.getStatus()).thenReturn(expectedOutcome);
            when(questionsResponse.getAuthReference()).thenReturn("an auth ref no");
            when(questionsResponse.getUniqueReference()).thenReturn("a urn");

            when(mockKBVGateway.getQuestions(any(QuestionRequest.class)))
                    .thenReturn(questionsResponse);
            when(mockConfigurationService.getParameterValue(IIQ_STRATEGY_PARAM_NAME))
                    .thenReturn("3 out of 4");
            assertThrows(
                    QuestionNotFoundException.class,
                    () ->
                            questionHandler.processQuestionRequest(
                                    new QuestionState(), kbvItem, sessionItem, requestHeaders),
                    "Question not Found");
            verify(sessionService).createAuthorizationCode(sessionItem);
            verify(mockAuditService)
                    .sendAuditEvent(
                            eq(AuditEventType.THIRD_PARTY_REQUEST_ENDED),
                            auditEventContextArgCaptor.capture(),
                            auditEventMap.capture());
            verify(mockKBVStorageService).save(kbvItem);
            verify(mockEventProbe)
                    .addDimensions(Map.of(METRIC_DIMENSION_QUESTION_STRATEGY, "3 out of 4"));
            verifyNoMoreInteractions(mockEventProbe);
            verify(kbvItem).setAuthRefNo("an auth ref no");
            verify(kbvItem).setUrn("a urn");

            Map<String, Object> response =
                    (Map<String, Object>) auditEventMap.getValue().get("experianIiqResponse");
            String outcome = (String) response.get("outcome");
            assertThat(outcome, equalTo(expectedOutcome));
            verify(mockConfigurationService).getParameterValue("IIQStrategy");
            verify(mockConfigurationService).getParameterValue("IIQOperatorId");
            assertEquals(sessionItem, auditEventContextArgCaptor.getValue().getSessionItem());
            assertEquals(requestHeaders, auditEventContextArgCaptor.getValue().getRequestHeaders());
        }

        @Test
        void shouldReturnThrowErrorWhenQuestionStateIsNull() {
            var kbvItem = new KBVItem();
            SessionItem sessionItem = new SessionItem();
            assertThrows(
                    NullPointerException.class,
                    () -> questionHandler.processQuestionRequest(null, kbvItem, sessionItem, null),
                    "questionState cannot be null");
        }

        @Test
        void shouldReturnThrowErrorWhenKbvItemIsNull() {
            var questionState = new QuestionState();
            NullPointerException expectedException =
                    assertThrows(
                            NullPointerException.class,
                            () ->
                                    questionHandler.processQuestionRequest(
                                            questionState, null, mock(SessionItem.class), null));

            assertEquals("kbvItem cannot be null", expectedException.getMessage());
        }

        @Test
        void shouldReturnNextQuestionFromDbStoreWhenThereIsAnUnansweredQuestionInStorage()
                throws IOException, SqsException {
            QuestionState questionState = new QuestionState();

            KbvQuestion answeredQuestion = getQuestionOne();
            QuestionAnswer questionAnswer = new QuestionAnswer();
            questionAnswer.setQuestionId(answeredQuestion.getQuestionId());
            questionAnswer.setAnswer("OVER £35,000 UP TO £60,000");

            KbvQuestion unAnsweredQuestion = getQuestionTwo();

            questionState.setQAPairs(new KbvQuestion[] {answeredQuestion, unAnsweredQuestion});
            questionState.setAnswer(questionAnswer);

            KbvQuestion nextQuestion =
                    questionHandler.processQuestionRequest(
                            questionState,
                            mock(KBVItem.class),
                            mock(SessionItem.class),
                            new HashMap<>());

            assertEquals(nextQuestion.getQuestionId(), unAnsweredQuestion.getQuestionId());
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
            when(mockConfigurationService.getParameterValue(IIQ_STRATEGY_PARAM_NAME))
                    .thenReturn("3 out of 4");
            KbvQuestion nextQuestionFromExperian =
                    questionHandler.processQuestionRequest(
                            questionState, kbvItem, mock(SessionItem.class), new HashMap<>());

            assertEquals(
                    nextQuestionFromExperian.getQuestionId(),
                    getExperianQuestionResponse().getQuestions()[0].getQuestionId());
        }
    }

    private void setupEventProbeErrorBehaviour() {
        when(mockEventProbe.counterMetric(anyString(), anyDouble())).thenReturn(mockEventProbe);
        when(mockEventProbe.log(any(Level.class), any(Exception.class))).thenReturn(mockEventProbe);
    }

    private KbvQuestion getQuestionOne() {
        KbvQuestionOptions questionOptions = new KbvQuestionOptions();
        questionOptions.setIdentifier("A00004");
        questionOptions.setFieldType("G");
        questionOptions.setOptions(
                List.of(
                        "UP TO £10,000",
                        "OVER £35,000 UP TO £60,000",
                        "OVER £60,000 UP TO £85,000",
                        "NONE OF THE ABOVE / DOES NOT APPLY"));

        KbvQuestion question = new KbvQuestion();
        question.setQuestionId("Q00015");
        question.setText("What is the outstanding balance ");
        question.setTooltip("outstanding balance tooltip");
        question.setQuestionOptions(questionOptions);

        return question;
    }

    private KbvQuestion getQuestionTwo() {
        KbvQuestionOptions questionOptions = new KbvQuestionOptions();
        questionOptions.setIdentifier("A00005");
        questionOptions.setFieldType("G");
        questionOptions.setOptions(
                List.of("Blue", "Red", "Green", "NONE OF THE ABOVE / DOES NOT APPLY"));

        KbvQuestion question = new KbvQuestion();
        question.setQuestionId("Q00040");
        question.setText("What your favorite color");
        question.setTooltip("favorite color tooltip");
        question.setQuestionOptions(questionOptions);

        return question;
    }

    private QuestionsResponse getExperianQuestionResponse() {
        return getExperianQuestionResponse(new KbvQuestion[] {new KbvQuestion()});
    }

    private QuestionsResponse getExperianQuestionResponse(KbvQuestion[] kbvQuestions) {
        QuestionsResponse questionsResponse = new QuestionsResponse();
        questionsResponse.setAuthReference("authrefno");
        questionsResponse.setUniqueReference("urn");
        questionsResponse.setQuestions(kbvQuestions);

        return questionsResponse;
    }
}
