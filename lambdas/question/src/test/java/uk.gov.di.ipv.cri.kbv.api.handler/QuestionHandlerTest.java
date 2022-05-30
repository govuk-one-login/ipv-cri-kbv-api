package uk.gov.di.ipv.cri.kbv.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.experian.uk.schema.experian.identityiq.services.webservice.Control;
import com.experian.uk.schema.experian.identityiq.services.webservice.Question;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.dynamodb.model.InternalServerErrorException;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentity;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.service.PersonIdentityService;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.gateway.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.service.KBVService;
import uk.gov.di.ipv.cri.kbv.api.service.KBVServiceFactory;
import uk.gov.di.ipv.cri.kbv.api.service.KBVStorageService;
import uk.gov.di.ipv.cri.kbv.api.service.KBVSystemProperty;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
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

    private final Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());

    @Mock private ObjectMapper mockObjectMapper;
    @Mock private KBVStorageService mockKBVStorageService;
    @Mock private PersonIdentityService mockPersonIdentityService;
    @Mock private EventProbe mockEventProbe;
    @Mock private KBVServiceFactory mockKbvServiceFactory;
    @Mock private KBVService mockKbvService;
    @Mock private ConfigurationService mockConfigurationService;
    @Mock private KBVSystemProperty mockSystemProperty;

    @BeforeEach
    void setUp() {
        when(mockKbvServiceFactory.create()).thenReturn(mockKbvService);
        doNothing().when(mockSystemProperty).save();
        questionHandler =
                new QuestionHandler(
                        mockObjectMapper,
                        mockKBVStorageService,
                        mockPersonIdentityService,
                        mockSystemProperty,
                        mockKbvServiceFactory,
                        mockConfigurationService,
                        mockEventProbe,
                        clock);
    }

    @Test
    void shouldReturn200OkWhen1stCalledAndReturn1stUnAnsweredQuestionFromExperianEndpoint()
            throws IOException {
        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, UUID.randomUUID().toString());

        Context contextMock = mock(Context.class);
        KBVItem kbvItemMock = mock(KBVItem.class);
        PersonIdentity personIdentityMock = mock(PersonIdentity.class);
        QuestionState questionStateMock = mock(QuestionState.class);

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockPersonIdentityService.getPersonIdentity(
                        UUID.fromString(sessionHeader.get(HEADER_SESSION_ID))))
                .thenReturn(personIdentityMock);

        when(mockKBVStorageService.getKBVItem(
                        UUID.fromString(sessionHeader.get(HEADER_SESSION_ID))))
                .thenReturn(kbvItemMock);

        when(mockObjectMapper.readValue(kbvItemMock.getQuestionState(), QuestionState.class))
                .thenReturn(questionStateMock);

        QuestionsResponse questionsResponseMock = mock(QuestionsResponse.class);

        when(questionsResponseMock.hasQuestions()).thenReturn(true);
        String state = "question-state";
        when(mockObjectMapper.writeValueAsString(questionStateMock)).thenReturn(state);
        com.experian.uk.schema.experian.identityiq.services.webservice.Control controlMock =
                mock(Control.class);
        when(questionsResponseMock.getControl()).thenReturn(controlMock);
        String authRefNo = "auth-ref-no";
        when(controlMock.getAuthRefNo()).thenReturn(authRefNo);
        String ipvSessionId = "ipv-session-id";
        when(controlMock.getURN()).thenReturn(ipvSessionId);

        Question expectedQuestion = mock(Question.class);
        when(mockKbvService.getQuestions(any())).thenReturn(questionsResponseMock);
        when(questionStateMock.getNextQuestion()) // we have to do this to get it to work
                .thenReturn(Optional.empty()) // otherwise the second overrides the first
                .thenReturn(Optional.ofNullable(expectedQuestion));

        when(mockObjectMapper.writeValueAsString(expectedQuestion))
                .thenReturn(TestData.EXPECTED_QUESTION);

        APIGatewayProxyResponseEvent response = questionHandler.handleRequest(input, contextMock);

        assertEquals(HttpStatusCode.OK, response.getStatusCode());
        assertEquals(TestData.EXPECTED_QUESTION, response.getBody());
    }

    @Test
    void shouldReturn200OkWhenCalledAgainAndReturnNextUnAnsweredQuestionFromStorage()
            throws IOException {
        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, UUID.randomUUID().toString());

        Context contextMock = mock(Context.class);
        KBVItem kbvItemMock = mock(KBVItem.class);
        PersonIdentity personIdentityMock = mock(PersonIdentity.class);
        QuestionState questionStateMock = mock(QuestionState.class);

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockKBVStorageService.getKBVItem(
                        UUID.fromString(sessionHeader.get(HEADER_SESSION_ID))))
                .thenReturn(kbvItemMock);
        when(mockPersonIdentityService.getPersonIdentity(
                        UUID.fromString(sessionHeader.get(HEADER_SESSION_ID))))
                .thenReturn(personIdentityMock);
        when(mockObjectMapper.readValue(kbvItemMock.getQuestionState(), QuestionState.class))
                .thenReturn(questionStateMock);

        Question question2 = mock(Question.class);

        when(questionStateMock.getNextQuestion()).thenReturn(Optional.ofNullable(question2));

        when(mockObjectMapper.writeValueAsString(question2)).thenReturn(TestData.EXPECTED_QUESTION);

        APIGatewayProxyResponseEvent response = questionHandler.handleRequest(input, contextMock);

        assertEquals(HttpStatusCode.OK, response.getStatusCode());
        assertEquals(TestData.EXPECTED_QUESTION, response.getBody());
    }

    @Test
    void shouldReturn400ErrorWhenNoFurtherQuestions() throws IOException, InterruptedException {
        Context contextMock = mock(Context.class);
        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        ArgumentCaptor<QuestionRequest> questionRequestCaptor =
                ArgumentCaptor.forClass(QuestionRequest.class);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, UUID.randomUUID().toString());

        var userAttributes =
                "{\"firstName\":\"Jack\",\"middleNames\":null,\"surname\":\"Reacher\",\"dateOfBirth\":null,\"addresses\":[{\"buildingNumber\":null,\"buildingName\":null,\"street\":null,\"townCity\":null,\"postcode\":null,\"district\":null,\"addressType\":null,\"dateMovedOut\":null}]}";
        PersonIdentity personIdentity =
                new ObjectMapper().readValue(userAttributes, PersonIdentity.class);

        KBVItem kbvItem = new KBVItem();
        QuestionState questionStateMock = mock(QuestionState.class);

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockKBVStorageService.getKBVItem(
                        UUID.fromString(sessionHeader.get(HEADER_SESSION_ID))))
                .thenReturn(kbvItem);
        when(mockPersonIdentityService.getPersonIdentity(
                        UUID.fromString(sessionHeader.get(HEADER_SESSION_ID))))
                .thenReturn(personIdentity);

        when(mockObjectMapper.readValue(kbvItem.getQuestionState(), QuestionState.class))
                .thenReturn(questionStateMock);

        QuestionsResponse questionsResponseMock = mock(QuestionsResponse.class);

        when(mockKbvService.getQuestions(questionRequestCaptor.capture()))
                .thenReturn(questionsResponseMock);

        APIGatewayProxyResponseEvent response = questionHandler.handleRequest(input, contextMock);

        verify(mockKBVStorageService)
                .getKBVItem(UUID.fromString(sessionHeader.get(HEADER_SESSION_ID)));
        verify(mockObjectMapper).readValue(kbvItem.getQuestionState(), QuestionState.class);

        assertEquals(HttpStatusCode.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturn400ErrorWhenNoSessionIdProvided() {
        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        setupEventProbeErrorBehaviour();

        APIGatewayProxyResponseEvent response =
                questionHandler.handleRequest(input, mock(Context.class));

        assertEquals("{ \"error\":\"java.lang.NullPointerException\" }", response.getBody());
        assertEquals(HttpStatusCode.BAD_REQUEST, response.getStatusCode());
        verify(mockEventProbe).counterMetric("get_question", 0d);
    }

    @Test
    void shouldReturn500ErrorWhenAWSDynamoDBServiceDown() {
        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, UUID.randomUUID().toString());

        when(input.getHeaders()).thenReturn(sessionHeader);
        doThrow(InternalServerErrorException.class)
                .when(mockKBVStorageService)
                .getKBVItem(UUID.fromString(sessionHeader.get(HEADER_SESSION_ID)));

        setupEventProbeErrorBehaviour();
        APIGatewayProxyResponseEvent response =
                questionHandler.handleRequest(input, mock(Context.class));

        assertEquals("{ \"error\":\"AWS Server error occurred.\" }", response.getBody());
        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(mockEventProbe).counterMetric("get_question", 0d);
    }

    @Test
    void shouldReturn500ErrorWhenPersonIdentityCannotBeRetrievedDueToAnAwsError() {
        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, UUID.randomUUID().toString());

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
        when(mockPersonIdentityService.getPersonIdentity(
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
                .getPersonIdentity(UUID.fromString(sessionHeader.get(HEADER_SESSION_ID)));
        verify(mockEventProbe).counterMetric("get_question", 0d);
    }

    @Test
    void shouldReturn500ErrorWhenExperianServiceIsDown() throws IOException {

        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, UUID.randomUUID().toString());

        KBVItem kbvItemMock = mock(KBVItem.class);
        PersonIdentity personIdentityMock = mock(PersonIdentity.class);
        QuestionState questionStateMock = mock(QuestionState.class);

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockKBVStorageService.getKBVItem(
                        UUID.fromString(sessionHeader.get(HEADER_SESSION_ID))))
                .thenReturn(kbvItemMock);
        when(mockPersonIdentityService.getPersonIdentity(
                        UUID.fromString(sessionHeader.get(HEADER_SESSION_ID))))
                .thenReturn(personIdentityMock);

        when(mockObjectMapper.readValue(kbvItemMock.getQuestionState(), QuestionState.class))
                .thenReturn(questionStateMock);

        doThrow(RuntimeException.class)
                .when(mockKbvService)
                .getQuestions(any(QuestionRequest.class));

        setupEventProbeErrorBehaviour();
        APIGatewayProxyResponseEvent response =
                questionHandler.handleRequest(input, mock(Context.class));

        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(mockEventProbe).counterMetric("get_question", 0d);
    }

    @Test
    void shouldReturn204WhenAGivenSessionHasReceivedFinalResponseFromExperian() throws IOException {

        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, UUID.randomUUID().toString());

        Context contextMock = mock(Context.class);
        KBVItem SessionItemMock = mock(KBVItem.class);
        PersonIdentity personIdentityMock = mock(PersonIdentity.class);
        QuestionState questionStateMock = mock(QuestionState.class);

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockKBVStorageService.getKBVItem(
                        UUID.fromString(sessionHeader.get(HEADER_SESSION_ID))))
                .thenReturn(SessionItemMock);
        when(mockPersonIdentityService.getPersonIdentity(
                        UUID.fromString(sessionHeader.get(HEADER_SESSION_ID))))
                .thenReturn(personIdentityMock);

        when(mockObjectMapper.readValue(SessionItemMock.getQuestionState(), QuestionState.class))
                .thenReturn(questionStateMock);

        when(SessionItemMock.getStatus()).thenReturn("status-code");
        APIGatewayProxyResponseEvent response = questionHandler.handleRequest(input, contextMock);

        assertEquals(HttpStatusCode.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
    }

    private void setupEventProbeErrorBehaviour() {
        when(mockEventProbe.counterMetric(anyString(), anyDouble())).thenReturn(mockEventProbe);
        when(mockEventProbe.log(any(Level.class), any(Exception.class))).thenReturn(mockEventProbe);
    }
}
