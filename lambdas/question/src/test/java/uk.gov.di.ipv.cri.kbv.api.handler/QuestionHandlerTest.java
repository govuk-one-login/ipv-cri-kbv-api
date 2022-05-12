package uk.gov.di.ipv.cri.kbv.api.handler;

import com.amazonaws.services.dynamodbv2.model.InternalServerErrorException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.experian.uk.schema.experian.identityiq.services.webservice.Control;
import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebService;
import com.experian.uk.schema.experian.identityiq.services.webservice.Question;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.kbv.api.domain.PersonIdentity;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.gateway.KBVGateway;
import uk.gov.di.ipv.cri.kbv.api.gateway.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.gateway.ResponseToQuestionMapper;
import uk.gov.di.ipv.cri.kbv.api.gateway.StartAuthnAttemptRequestMapper;
import uk.gov.di.ipv.cri.kbv.api.library.helpers.EventProbe;
import uk.gov.di.ipv.cri.kbv.api.library.persistence.item.KBVSessionItem;
import uk.gov.di.ipv.cri.kbv.api.security.HeaderHandlerResolver;
import uk.gov.di.ipv.cri.kbv.api.security.KBVClientFactory;
import uk.gov.di.ipv.cri.kbv.api.service.KBVService;
import uk.gov.di.ipv.cri.kbv.api.service.KBVServiceFactory;
import uk.gov.di.ipv.cri.kbv.api.service.KBVSystemProperty;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

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
    @Mock private StorageService mockStorageService;
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
                        mockStorageService,
                        mockSystemProperty,
                        mockKbvServiceFactory,
                        mockEventProbe);
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

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockStorageService.getSessionId(sessionHeader.get(HEADER_SESSION_ID)))
                .thenReturn(Optional.ofNullable(kbvSessionItemMock));
        when(mockObjectMapper.readValue(
                        kbvSessionItemMock.getUserAttributes(), PersonIdentity.class))
                .thenReturn(personIdentityMock);
        when(mockObjectMapper.readValue(kbvSessionItemMock.getQuestionState(), QuestionState.class))
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
        doNothing().when(mockStorageService).update(kbvSessionItemMock);

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
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, "new-session-id");
        var userAttributes =
                "{\"title\":\"Mr\",\"firstName\":\"Jack\",\"middleNames\":null,\"surname\":\"Reacher\",\"dateOfBirth\":null,\"addresses\":[{\"houseNumber\":null,\"houseName\":null,\"flat\":null,\"street\":null,\"townCity\":null,\"postcode\":null,\"district\":null,\"addressType\":null,\"dateMovedOut\":null}]}";
        PersonIdentity personIdentity =
                new ObjectMapper().readValue(userAttributes, PersonIdentity.class);

        KBVSessionItem kbvSessionItem = new KBVSessionItem();
        kbvSessionItem.setUserAttributes(userAttributes);
        QuestionState questionStateMock = mock(QuestionState.class);

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockStorageService.getSessionId(sessionHeader.get(HEADER_SESSION_ID)))
                .thenReturn(Optional.ofNullable(kbvSessionItem));
        when(mockObjectMapper.readValue(kbvSessionItem.getUserAttributes(), PersonIdentity.class))
                .thenReturn(personIdentity);
        when(mockObjectMapper.readValue(kbvSessionItem.getQuestionState(), QuestionState.class))
                .thenReturn(questionStateMock);

        QuestionsResponse questionsResponseMock = mock(QuestionsResponse.class);

        when(mockKbvService.getQuestions(questionRequestCaptor.capture()))
                .thenReturn(questionsResponseMock);

        APIGatewayProxyResponseEvent response = questionHandler.handleRequest(input, contextMock);

        verify(mockStorageService).getSessionId(sessionHeader.get(HEADER_SESSION_ID));
        verify(mockObjectMapper)
                .readValue(kbvSessionItem.getUserAttributes(), PersonIdentity.class);
        verify(mockObjectMapper).readValue(kbvSessionItem.getQuestionState(), QuestionState.class);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
    }

    @Test
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
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, "new-session-id");

        when(input.getHeaders()).thenReturn(sessionHeader);
        doThrow(InternalServerErrorException.class)
                .when(mockStorageService)
                .getSessionId(anyString());

        setupEventProbeErrorBehaviour();
        APIGatewayProxyResponseEvent response =
                questionHandler.handleRequest(input, mock(Context.class));

        assertEquals("{ \"error\":\"AWS Server error occurred.\" }", response.getBody());
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(mockEventProbe).counterMetric("get_question", 0d);
    }

    @Test
    void shouldReturn500ErrorWhenPersonIdentityCannotBeParsedToJSON() throws IOException {
        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, "new-session-id");
        KBVSessionItem kbvSessionItemMock = mock(KBVSessionItem.class);

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockStorageService.getSessionId(sessionHeader.get(HEADER_SESSION_ID)))
                .thenReturn(Optional.ofNullable(kbvSessionItemMock));
        when(mockObjectMapper.readValue(
                        kbvSessionItemMock.getUserAttributes(), PersonIdentity.class))
                .thenThrow(JsonProcessingException.class);

        setupEventProbeErrorBehaviour();
        APIGatewayProxyResponseEvent response =
                questionHandler.handleRequest(input, mock(Context.class));

        assertEquals(
                "{ \"error\":\"Failed to parse object using ObjectMapper.\" }", response.getBody());
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(mockEventProbe).counterMetric("get_question", 0d);
    }

    // @Test
    void shouldReturn500ErrorWhenExperianServiceIsDown() throws IOException {

        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, "new-session-id");

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

        // HeaderHandler headerHandler = mock(HeaderHandler.class);
        // when(headerHandler.handleMessage(any())).thenThrow(RuntimeException.class);

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

        when(kbvSessionItemMock.getAuthorizationCode()).thenReturn("authorisation-code");
        APIGatewayProxyResponseEvent response = questionHandler.handleRequest(input, contextMock);

        assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
    }

    private void setupEventProbeErrorBehaviour() {
        when(mockEventProbe.counterMetric(anyString(), anyDouble())).thenReturn(mockEventProbe);
        when(mockEventProbe.log(any(Level.class), any(Exception.class))).thenReturn(mockEventProbe);
    }
}
