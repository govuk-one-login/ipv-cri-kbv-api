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
import uk.gov.di.ipv.cri.kbv.api.domain.ParseJWT;
import uk.gov.di.ipv.cri.kbv.api.domain.PersonIdentity;
import uk.gov.di.ipv.cri.kbv.api.domain.SessionRequest;
import uk.gov.di.ipv.cri.kbv.api.exception.ClientConfigurationException;
import uk.gov.di.ipv.cri.kbv.api.exception.ValidationException;
import uk.gov.di.ipv.cri.kbv.api.service.StorageService;
import uk.gov.di.ipv.cri.kbv.api.validation.ValidatorService;

import java.text.ParseException;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    @Mock private APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent;
    @Mock private APIGatewayProxyResponseEvent mockApiGatewayProxyResponseEvent;
    @Mock private Context mockContext;
    @Mock private Appender<ILoggingEvent> appender;
    @Mock private ValidatorService validatorServiceMock;

    @BeforeEach
    void setUp() {
        Logger logger = (Logger) LoggerFactory.getLogger(SessionHandler.class);
        logger.addAppender(appender);
        this.sessionHandler =
                new SessionHandler(
                        validatorServiceMock,
                        this.mockStorageService,
                        this.mockApiGatewayProxyResponseEvent,
                        this.parseJWTMock);
        objectMapper.registerModule(new JavaTimeModule());
    }

        @Test
    void shouldReturn201ResponseWhenRequestIsValid()
            throws JsonProcessingException, ParseException, ValidationException,
                    ClientConfigurationException {

        APIGatewayProxyRequestEvent mockRequest = mock(APIGatewayProxyRequestEvent.class);
        when(mockRequest.getBody()).thenReturn(TestData.REQUEST_PAYLOAD);
        SessionRequest sessionRequest = objectMapper.readValue(mockRequest.getBody(), SessionRequest.class);

        when(validatorServiceMock.validateSessionRequest(anyString())).thenReturn(sessionRequest);

        PersonIdentity personIdentityMock = mock(PersonIdentity.class);
        when(parseJWTMock.getPersonIdentity(sessionRequest.getRequestJWT())).thenReturn(Optional.of(personIdentityMock));

        String expectedBody = "{\"session-id\":\"new-session-id\"}";
        when(mockApiGatewayProxyResponseEvent.getBody()).thenReturn(expectedBody);
        mockApiGatewayProxyResponseEvent = sessionHandler.handleRequest(mockRequest, mockContext);

        verify(mockApiGatewayProxyResponseEvent)
                .withHeaders(Map.of("Content-Type", "application/json"));
        verify(mockApiGatewayProxyResponseEvent).withStatusCode(HttpStatus.SC_CREATED);
        assertTrue(mockApiGatewayProxyResponseEvent.getBody().contains("session-id"));
    }

    @Test
    void shouldReturn500ErrorWhenIncorrectStorageServiceIsNotResponsive()
            throws ParseException, JsonProcessingException, ValidationException, ClientConfigurationException {
        APIGatewayProxyRequestEvent mockRequest = mock(APIGatewayProxyRequestEvent.class);

        when(mockRequest.getBody()).thenReturn(TestData.REQUEST_PAYLOAD);

        ArgumentCaptor<ILoggingEvent> loggingEventArgumentCaptor =
                ArgumentCaptor.forClass(ILoggingEvent.class);

        SessionRequest sessionRequestMock = objectMapper.readValue(mockRequest.getBody(), SessionRequest.class);

        when(validatorServiceMock.validateSessionRequest(mockRequest.getBody())).thenReturn(sessionRequestMock);

        PersonIdentity personIdentityMock = mock(PersonIdentity.class);

        when(parseJWTMock.getPersonIdentity(sessionRequestMock.getRequestJWT())).thenReturn(Optional.ofNullable(personIdentityMock));

        doThrow(InternalServerErrorException.class)
                .when(mockStorageService)
                .save(anyString(), anyString(), anyString());

        mockApiGatewayProxyResponseEvent = sessionHandler.handleRequest(mockRequest, mockContext);

        verify(mockApiGatewayProxyResponseEvent)
                .withStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        verify(appender).doAppend(loggingEventArgumentCaptor.capture());

        ILoggingEvent event = loggingEventArgumentCaptor.getValue();
        assertEquals(event.getMessage(), "AWS Server error occurred.");
    }

    @Test
    void shouldReturn400BadRequestWhenIncorrectJWTIsProvided() {
        APIGatewayProxyRequestEvent mockRequest = mock(APIGatewayProxyRequestEvent.class);
        when(mockRequest.getBody()).thenReturn(TestData.A_PERSONIDENTITY);
        ArgumentCaptor<ILoggingEvent> loggingEventArgumentCaptor =
                ArgumentCaptor.forClass(ILoggingEvent.class);
        APIGatewayProxyResponseEvent response =
                sessionHandler.handleRequest(mockRequest, mockContext);

        verify(response).withHeaders(Map.of("Content-Type", "application/json"));
        verify(response).withStatusCode(HttpStatus.SC_BAD_REQUEST);
        verify(response).withBody("{ \"error\":\"java.lang.NullPointerException\" }");
        verify(appender).doAppend(loggingEventArgumentCaptor.capture());
        ILoggingEvent event = loggingEventArgumentCaptor.getValue();

        assertEquals(event.getMessage(), "The supplied JWT was not of the expected format.");
    }

    @Test
    void shouldCatchValidationExceptionAndReturn400Response()
            throws ValidationException, ClientConfigurationException,
            JsonProcessingException, ParseException {

        APIGatewayProxyRequestEvent mockRequest = mock(APIGatewayProxyRequestEvent.class);
        String mockRequestBody = "{invalid:json-jwt}";
       when(mockRequest.getBody()).thenReturn(mockRequestBody);
        ArgumentCaptor<ILoggingEvent> loggingEventArgumentCaptor =
                ArgumentCaptor.forClass(ILoggingEvent.class);

        doThrow(ValidationException.class)
                .when(validatorServiceMock)
                .validateSessionRequest(mockRequestBody);

        APIGatewayProxyResponseEvent response = sessionHandler.handleRequest(mockRequest, mockContext);

        verify(response)
                .withStatusCode(HttpStatus.SC_BAD_REQUEST);
        verify(appender).doAppend(loggingEventArgumentCaptor.capture());

        ILoggingEvent event = loggingEventArgumentCaptor.getValue();
        assertEquals(event.getMessage(), "Session Validation Exception");

        verify(parseJWTMock, never()).getPersonIdentity(anyString());
        verify(mockStorageService, never()).save(anyString(), anyString(), anyString());
    }

    @Test
    void shouldCatchServerConfigurationExceptionAndReturn400Response()
            throws ValidationException, ClientConfigurationException,
            JsonProcessingException, ParseException {

        APIGatewayProxyRequestEvent mockRequest = mock(APIGatewayProxyRequestEvent.class);
        String mockRequestBody = "{invalid:json-jwt}";
        when(mockRequest.getBody()).thenReturn(mockRequestBody);
        ArgumentCaptor<ILoggingEvent> loggingEventArgumentCaptor =
                ArgumentCaptor.forClass(ILoggingEvent.class);

        doThrow(ClientConfigurationException.class)
                .when(validatorServiceMock)
                .validateSessionRequest(mockRequestBody);

        APIGatewayProxyResponseEvent response = sessionHandler.handleRequest(mockRequest, mockContext);

        verify(response)
                .withStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        verify(appender).doAppend(loggingEventArgumentCaptor.capture());

        ILoggingEvent event = loggingEventArgumentCaptor.getValue();
        assertEquals(event.getMessage(), "Server Configuration Error");

        verify(parseJWTMock, never()).getPersonIdentity(anyString());
        verify(mockStorageService, never()).save(anyString(), anyString(), anyString());
    }
}
