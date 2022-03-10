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
import uk.gov.di.ipv.cri.kbv.api.service.StorageService;

import java.text.ParseException;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SessionHandlerTest {
    private SessionHandler sessionHandler;
    private ObjectMapper objectMapper = new ObjectMapper();
    @Mock private StorageService mockStorageService;
    @Mock private ParseJWT mockParseJWT;
    @Mock private APIGatewayProxyResponseEvent mockApiGatewayProxyResponseEvent;
    @Mock private Context mockContext;
    @Mock private Appender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        Logger logger = (Logger) LoggerFactory.getLogger(SessionHandler.class);
        logger.addAppender(appender);
        this.sessionHandler =
                new SessionHandler(
                        this.mockStorageService,
                        this.mockApiGatewayProxyResponseEvent,
                        this.mockParseJWT);
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void shouldReturn201ResponseWhenRequestIsValid()
            throws JsonProcessingException, ParseException {

        PersonIdentity person =
                objectMapper.readValue(TestData.APERSONIDENTITY, PersonIdentity.class);
        APIGatewayProxyRequestEvent mockRequest = mock(APIGatewayProxyRequestEvent.class);

        when(mockParseJWT.getPersonIdentity(mockRequest)).thenReturn(Optional.of(person));
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
            throws ParseException, JsonProcessingException {
        APIGatewayProxyRequestEvent mockRequest = mock(APIGatewayProxyRequestEvent.class);
        ArgumentCaptor<ILoggingEvent> loggingEventArgumentCaptor =
                ArgumentCaptor.forClass(ILoggingEvent.class);
        PersonIdentity person =
                objectMapper.readValue(TestData.APERSONIDENTITY, PersonIdentity.class);

        when(mockParseJWT.getPersonIdentity(mockRequest)).thenReturn(Optional.of(person));
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
}
