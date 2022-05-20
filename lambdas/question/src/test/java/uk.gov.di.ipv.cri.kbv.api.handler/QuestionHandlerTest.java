package uk.gov.di.ipv.cri.kbv.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.experian.uk.schema.experian.identityiq.services.webservice.Control;
import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebService;
import com.experian.uk.schema.experian.identityiq.services.webservice.Question;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
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
import uk.gov.di.ipv.cri.address.library.domain.personidentity.PersonIdentity;
import uk.gov.di.ipv.cri.address.library.service.PersonIdentityService;
import uk.gov.di.ipv.cri.address.library.util.EventProbe;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.gateway.KBVGateway;
import uk.gov.di.ipv.cri.kbv.api.gateway.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.gateway.ResponseToQuestionMapper;
import uk.gov.di.ipv.cri.kbv.api.gateway.StartAuthnAttemptRequestMapper;
import uk.gov.di.ipv.cri.kbv.api.security.HeaderHandlerResolver;
import uk.gov.di.ipv.cri.kbv.api.security.KBVClientFactory;
import uk.gov.di.ipv.cri.kbv.api.service.KBVService;
import uk.gov.di.ipv.cri.kbv.api.service.KBVServiceFactory;
import uk.gov.di.ipv.cri.kbv.api.service.KBVStorageService;
import uk.gov.di.ipv.cri.kbv.api.service.KBVSystemProperty;

import java.io.IOException;
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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.kbv.api.handler.QuestionHandler.HEADER_SESSION_ID;

@ExtendWith(MockitoExtension.class)
class QuestionHandlerTest {
    private QuestionHandler questionHandler;
    @Mock private ObjectMapper mockObjectMapper;
    @Mock private KBVStorageService mockKBVStorageService;
    @Mock private PersonIdentityService mockPersonIdentityService;
    @Mock private EventProbe mockEventProbe;
    @Mock private KBVServiceFactory mockKbvServiceFactory;
    @Mock private KBVService mockKbvService;
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
                        mockEventProbe);
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

        when(mockKBVStorageService.getSessionId(sessionHeader.get(HEADER_SESSION_ID)))
                .thenReturn(Optional.ofNullable(kbvItemMock));

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
        doNothing().when(mockKBVStorageService).update(kbvItemMock);

        Question expectedQuestion = mock(Question.class);
        when(mockKbvService.getQuestions(any())).thenReturn(questionsResponseMock);
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
    void shouldReturn200OkWhenCalledAgainAndReturnNextUnAnsweredQuestionFromStorage()
            throws IOException {
        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, UUID.randomUUID().toString());

        Context contextMock = mock(Context.class);
        KBVItem SessionItemMock = mock(KBVItem.class);
        PersonIdentity personIdentityMock = mock(PersonIdentity.class);
        QuestionState questionStateMock = mock(QuestionState.class);

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockKBVStorageService.getSessionId(sessionHeader.get(HEADER_SESSION_ID)))
                .thenReturn(Optional.ofNullable(SessionItemMock));
        when(mockPersonIdentityService.getPersonIdentity(
                        UUID.fromString(sessionHeader.get(HEADER_SESSION_ID))))
                .thenReturn(personIdentityMock);
        when(mockObjectMapper.readValue(SessionItemMock.getQuestionState(), QuestionState.class))
                .thenReturn(questionStateMock);

        Question question2 = mock(Question.class);

        when(questionStateMock.getNextQuestion()).thenReturn(Optional.ofNullable(question2));

        when(mockObjectMapper.writeValueAsString(question2)).thenReturn(TestData.EXPECTED_QUESTION);

        APIGatewayProxyResponseEvent response = questionHandler.handleRequest(input, contextMock);

        assertEquals(HttpStatus.SC_OK, response.getStatusCode());
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
                "{\"firstName\":\"Jack\",\"middleNames\":null,\"surname\":\"Reacher\",\"dateOfBirth\":null,\"addresses\":[{\"buildingNumber\":null,\"buildingName\":null,\"flat\":null,\"street\":null,\"townCity\":null,\"postcode\":null,\"district\":null,\"addressType\":null,\"dateMovedOut\":null}]}";
        PersonIdentity personIdentity =
                new ObjectMapper().readValue(userAttributes, PersonIdentity.class);

        KBVItem kbvItem = new KBVItem();
        QuestionState questionStateMock = mock(QuestionState.class);

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockKBVStorageService.getSessionId(sessionHeader.get(HEADER_SESSION_ID)))
                .thenReturn(Optional.ofNullable(kbvItem));
        when(mockPersonIdentityService.getPersonIdentity(
                        UUID.fromString(sessionHeader.get(HEADER_SESSION_ID))))
                .thenReturn(personIdentity);

        when(mockObjectMapper.readValue(kbvItem.getQuestionState(), QuestionState.class))
                .thenReturn(questionStateMock);

        QuestionsResponse questionsResponseMock = mock(QuestionsResponse.class);

        when(mockKbvService.getQuestions(questionRequestCaptor.capture()))
                .thenReturn(questionsResponseMock);

        APIGatewayProxyResponseEvent response = questionHandler.handleRequest(input, contextMock);

        verify(mockKBVStorageService).getSessionId(sessionHeader.get(HEADER_SESSION_ID));
        verify(mockObjectMapper).readValue(kbvItem.getQuestionState(), QuestionState.class);

        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
    }

    @Test //TODO this is flakky
    void shouldReturn400ErrorWhenNoSessionIdProvided() {
        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        setupEventProbeErrorBehaviour();

        APIGatewayProxyResponseEvent response =
                questionHandler.handleRequest(input, mock(Context.class));

        assertEquals("{ \"error\":\"java.lang.NullPointerException\" }", response.getBody());
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        verify(mockEventProbe).counterMetric("get_question", 0d);
    }

    @Test
    void shouldReturn500ErrorWhenAWSDynamoDBServiceDown() {
        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, UUID.randomUUID().toString());

        when(input.getHeaders()).thenReturn(sessionHeader);
        doThrow(InternalServerErrorException.class)
                .when(mockKBVStorageService)
                .getSessionId(anyString());

        setupEventProbeErrorBehaviour();
        APIGatewayProxyResponseEvent response =
                questionHandler.handleRequest(input, mock(Context.class));

        assertEquals("{ \"error\":\"AWS Server error occurred.\" }", response.getBody());
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
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
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatusCode());

        verify(mockPersonIdentityService)
                .getPersonIdentity(UUID.fromString(sessionHeader.get(HEADER_SESSION_ID)));
        verify(mockEventProbe).counterMetric("get_question", 0d);
    }

    // TODO @Test
    void shouldReturn500ErrorWhenExperianServiceIsDown() throws IOException {

        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, UUID.randomUUID().toString());

        KBVItem SessionItemMock = mock(KBVItem.class);
        PersonIdentity personIdentityMock = mock(PersonIdentity.class);
        QuestionState questionStateMock = mock(QuestionState.class);

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockKBVStorageService.getSessionId(sessionHeader.get(HEADER_SESSION_ID)))
                .thenReturn(Optional.ofNullable(SessionItemMock));
        when(mockPersonIdentityService.getPersonIdentity(
                        UUID.fromString(sessionHeader.get(HEADER_SESSION_ID))))
                .thenReturn(personIdentityMock);

        when(mockObjectMapper.readValue(SessionItemMock.getQuestionState(), QuestionState.class))
                .thenReturn(questionStateMock);

        //         HeaderHandler headerHandler = mock(HeaderHandler.class);
        //         when(headerHandler.handleMessage(any())).thenThrow(RuntimeException.class);

        KBVGateway kbvGateway =
                new KBVGateway(
                        mock(StartAuthnAttemptRequestMapper.class),
                        mock(ResponseToQuestionMapper.class),
                        new KBVClientFactory(
                                        new IdentityIQWebService(), new HeaderHandlerResolver(null))
                                .createClient());

        mockKbvService = spy(new KBVService(kbvGateway));

        doThrow(RuntimeException.class)
                .when(mockKbvService)
                .getQuestions(any(QuestionRequest.class));

        setupEventProbeErrorBehaviour();
        APIGatewayProxyResponseEvent response =
                questionHandler.handleRequest(input, mock(Context.class));

        assertEquals("{ \"error\":\"Retrieving questions failed.\" }", response.getBody());
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
        // verify(mockEventProbe).counterMetric("get_question", 0d);
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
        when(mockKBVStorageService.getSessionId(sessionHeader.get(HEADER_SESSION_ID)))
                .thenReturn(Optional.ofNullable(SessionItemMock));
        when(mockPersonIdentityService.getPersonIdentity(
                        UUID.fromString(sessionHeader.get(HEADER_SESSION_ID))))
                .thenReturn(personIdentityMock);

        when(mockObjectMapper.readValue(SessionItemMock.getQuestionState(), QuestionState.class))
                .thenReturn(questionStateMock);

        when(SessionItemMock.getAuthorizationCode()).thenReturn("authorisation-code");
        APIGatewayProxyResponseEvent response = questionHandler.handleRequest(input, contextMock);

        assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
    }

    private void setupEventProbeErrorBehaviour() {
        when(mockEventProbe.counterMetric(anyString(), anyDouble())).thenReturn(mockEventProbe);
        when(mockEventProbe.log(any(Level.class), any(Exception.class))).thenReturn(mockEventProbe);
    }
}
