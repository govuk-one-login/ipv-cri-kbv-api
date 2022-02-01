package uk.gov.di.ipv.cri.kbv.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.xray.AWSXRay;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.kbv.api.domain.PersonIdentity;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.service.StorageService;

import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SessionHandlerTest {
    private SessionHandler sessionHandler;
    private ObjectMapper mockObjectMapper = mock(ObjectMapper.class);
    private StorageService mockStorageService = mock(StorageService.class);

    @BeforeEach
    void setUp() {
        AWSXRay.beginSegment("handleRequest");
        this.sessionHandler =
                new SessionHandler(
                        this.mockObjectMapper,
                        this.mockStorageService,
                        mock(APIGatewayProxyResponseEvent.class));
    }

    @Test
    void shouldReturn201ResponseWhenRequestIsValid() throws JsonProcessingException {
        String requestBody =
                "{\"firstName\":\"KENNETH\",\"surname\":\"DECERQUEIRA\",\"title\":\"MR\",\"dateOfBirth\":\"1964-06-18\",\"addresses\":[{\"houseNumber\":8,\"street\":\"HADLEY ROAD\",\"townCity\":\"BATH\",\"postcode\":\"BA2 5AA\",\"addressType\":\"CURRENT\"}]}";
        String questionState =
                "{\"personIdentity\":{\"title\":\"MR\",\"firstName\":\"KENNETH\",\"middleNames\":null,\"surname\":\"DECERQUEIRA\",\"dateOfBirth\":[1964,6,18],\"addresses\":[{\"houseNumber\":\"8\",\"houseName\":null,\"flat\":null,\"street\":\"HADLEY ROAD\",\"townCity\":\"BATH\",\"postcode\":\"BA2 5AA\",\"district\":null,\"addressType\":\"CURRENT\",\"dateMovedOut\":null}]},\"control\":null,\"qaPairs\":[],\"nextQuestion\":{\"empty\":true,\"present\":false}}";
        APIGatewayProxyRequestEvent mockRequest = mock(APIGatewayProxyRequestEvent.class);
        PersonIdentity mockPersonIdentity = mock(PersonIdentity.class);
        QuestionState mockQuestionState = new QuestionState(mockPersonIdentity);

        when(mockRequest.getBody()).thenReturn(requestBody);
        when(mockObjectMapper.readValue(requestBody, PersonIdentity.class))
                .thenReturn(mock(PersonIdentity.class));
        when(mockObjectMapper.writeValueAsString(mockQuestionState)).thenReturn(questionState);
        doNothing().when(mockStorageService).save("new-session-id", questionState);

        APIGatewayProxyResponseEvent result =
                sessionHandler.handleRequest(mockRequest, mock(Context.class));

        verify(result).withStatusCode(201);
        verify(result).withHeaders(Map.of("Content-Type", "application/json"));
        verify(result).withBody("{\"session-id\":\"new-session-id\"}");
    }

    @Test
    void shouldReturn500ResponseWhenJsonProcessingExceptionOccurs() {}
}
