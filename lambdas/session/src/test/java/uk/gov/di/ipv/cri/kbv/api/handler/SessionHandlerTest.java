package uk.gov.di.ipv.cri.kbv.api.handler;

import com.amazonaws.services.dynamodbv2.model.InternalServerErrorException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.kbv.api.domain.ParseJWT;
import uk.gov.di.ipv.cri.kbv.api.domain.PersonIdentity;
import uk.gov.di.ipv.cri.kbv.api.library.domain.SessionRequest;
import uk.gov.di.ipv.cri.kbv.api.library.exception.ClientConfigurationException;
import uk.gov.di.ipv.cri.kbv.api.library.exception.ValidationException;
import uk.gov.di.ipv.cri.kbv.api.library.helpers.EventProbe;
import uk.gov.di.ipv.cri.kbv.api.library.service.StorageService;
import uk.gov.di.ipv.cri.kbv.api.library.validation.ValidatorService;

import java.text.ParseException;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SessionHandlerTest {
    private SessionHandler sessionHandler;
    private ObjectMapper objectMapper = new ObjectMapper();
    @Mock private StorageService mockStorageService;
    @Mock private ParseJWT parseJWTMock;
    @Mock private APIGatewayProxyResponseEvent mockApiGatewayProxyResponseEvent;
    @Mock private Context mockContext;
    @Mock private ValidatorService mockValidatorService;
    @Mock private EventProbe mockEventProbe;

    @BeforeEach
    void setUp() {
        this.sessionHandler =
                new SessionHandler(
                        mockValidatorService,
                        this.mockStorageService,
                        this.mockApiGatewayProxyResponseEvent,
                        this.parseJWTMock,
                        this.mockEventProbe);
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void shouldReturn201ResponseWhenRequestIsValid()
            throws JsonProcessingException, ParseException, ValidationException,
                    ClientConfigurationException {

        when(mockEventProbe.counterMetric(anyString())).thenReturn(mockEventProbe);

        APIGatewayProxyRequestEvent mockRequest = mock(APIGatewayProxyRequestEvent.class);
        when(mockRequest.getBody()).thenReturn(TestData.REQUEST_PAYLOAD);
        SessionRequest sessionRequest =
                objectMapper.readValue(mockRequest.getBody(), SessionRequest.class);

        when(mockValidatorService.validateSessionRequest(anyString())).thenReturn(sessionRequest);

        PersonIdentity personIdentityMock = mock(PersonIdentity.class);
        when(parseJWTMock.getPersonIdentity(sessionRequest.getRequestJWT()))
                .thenReturn(Optional.of(personIdentityMock));

        String expectedBody = "{\"session-id\":\"new-session-id\"}";
        when(mockApiGatewayProxyResponseEvent.getBody()).thenReturn(expectedBody);
        mockApiGatewayProxyResponseEvent = sessionHandler.handleRequest(mockRequest, mockContext);

        verify(mockApiGatewayProxyResponseEvent)
                .withHeaders(Map.of("Content-Type", "application/json"));
        verify(mockApiGatewayProxyResponseEvent).withStatusCode(HttpStatus.SC_CREATED);
        assertTrue(mockApiGatewayProxyResponseEvent.getBody().contains("session-id"));
        verify(mockEventProbe).addDimensions(Map.of("issuer", "some-stub"));
        verify(mockEventProbe).counterMetric("session_created");
    }

    @Test
    void shouldReturn500ErrorWhenIncorrectStorageServiceIsNotResponsive()
            throws ParseException, JsonProcessingException, ValidationException,
                    ClientConfigurationException {
        APIGatewayProxyRequestEvent mockRequest = mock(APIGatewayProxyRequestEvent.class);

        when(mockRequest.getBody()).thenReturn(TestData.REQUEST_PAYLOAD);

        SessionRequest sessionRequestMock =
                objectMapper.readValue(mockRequest.getBody(), SessionRequest.class);

        when(mockValidatorService.validateSessionRequest(mockRequest.getBody()))
                .thenReturn(sessionRequestMock);

        PersonIdentity personIdentityMock = mock(PersonIdentity.class);

        when(parseJWTMock.getPersonIdentity(sessionRequestMock.getRequestJWT()))
                .thenReturn(Optional.ofNullable(personIdentityMock));

        doThrow(InternalServerErrorException.class)
                .when(mockStorageService)
                .save(anyString(), anyString(), anyString());

        setupEventProbeErrorBehaviour();

        mockApiGatewayProxyResponseEvent = sessionHandler.handleRequest(mockRequest, mockContext);

        verify(mockApiGatewayProxyResponseEvent)
                .withStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        verify(mockEventProbe).counterMetric("session_created", 0d);
    }

    @Test
    void shouldReturn400BadRequestWhenIncorrectJWTIsProvided()
            throws ValidationException, ClientConfigurationException, ParseException,
                    JsonProcessingException {
        APIGatewayProxyRequestEvent mockRequest = mock(APIGatewayProxyRequestEvent.class);
        when(mockRequest.getBody()).thenReturn("some json");

        doThrow(ValidationException.class)
                .when(mockValidatorService)
                .validateSessionRequest("some json");

        setupEventProbeErrorBehaviour();
        APIGatewayProxyResponseEvent response =
                sessionHandler.handleRequest(mockRequest, mockContext);

        verify(response).withHeaders(Map.of("Content-Type", "application/json"));
        verify(response).withStatusCode(HttpStatus.SC_BAD_REQUEST);
        verify(mockEventProbe).counterMetric("session_created", 0d);

        verify(parseJWTMock, never()).getPersonIdentity(anyString());
        verify(mockStorageService, never()).save(anyString(), anyString(), anyString());
    }

    @Test
    void shouldCatchServerConfigurationExceptionAndReturn400Response()
            throws ValidationException, ClientConfigurationException, JsonProcessingException,
                    ParseException {

        APIGatewayProxyRequestEvent mockRequest = mock(APIGatewayProxyRequestEvent.class);
        String mockRequestBody = "{invalid:json-jwt}";
        when(mockRequest.getBody()).thenReturn(mockRequestBody);

        setupEventProbeErrorBehaviour();
        doThrow(ClientConfigurationException.class)
                .when(mockValidatorService)
                .validateSessionRequest(mockRequestBody);

        APIGatewayProxyResponseEvent response =
                sessionHandler.handleRequest(mockRequest, mockContext);

        verify(response).withStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);

        verify(mockEventProbe).counterMetric("session_created", 0d);

        verify(parseJWTMock, never()).getPersonIdentity(anyString());
        verify(mockStorageService, never()).save(anyString(), anyString(), anyString());
    }

    private void setupEventProbeErrorBehaviour() {
        when(mockEventProbe.counterMetric(anyString(), anyDouble())).thenReturn(mockEventProbe);
        when(mockEventProbe.log(any(Level.class), any(Exception.class))).thenReturn(mockEventProbe);
    }
}
