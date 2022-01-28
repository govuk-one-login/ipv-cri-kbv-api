package uk.gov.di.ipv.cri.kbv.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import software.amazon.lambda.powertools.tracing.CaptureMode;
import software.amazon.lambda.powertools.tracing.Tracing;
import uk.gov.di.ipv.cri.kbv.api.domain.PersonIdentity;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.service.StorageService;

import java.util.Map;
import java.util.UUID;

public class SessionHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private ObjectMapper objectMapper;
    private StorageService storageService;
    public static final String HEADER_SESSION_ID = "session-id";

    public SessionHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.storageService = new StorageService();
    }

    public SessionHandler(ObjectMapper objectMapper, StorageService storageService) {
        this.objectMapper = objectMapper;
        this.storageService = storageService;
    }

    @Override
    @Tracing(captureMode = CaptureMode.DISABLED)
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {
        PersonIdentity identity = null;
        String responseBody = "";
        Map<String, String> responseHeaders = Map.of("Content-Type", "application/json");
        int statusCode;
        try {
            // identity = parseJwt.getPersonIdentity();
            //--- ipv_session_id = parseJwt.getIpvSessionId();
            //--- strategy = parseJwt.getStrategy();
            identity = objectMapper.readValue(input.getBody(), PersonIdentity.class);
            String key = UUID.randomUUID().toString();
            storageService.save(key, new QuestionState(identity));
            responseBody = objectMapper.writeValueAsString(Map.of(HEADER_SESSION_ID, key));
            statusCode = 201;

        } catch (JsonProcessingException e) {
            e.printStackTrace();
            statusCode = 500;
            responseBody = "{ \"error\":\"" + e.getMessage() + "\" }";
        }
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(responseHeaders)
                .withBody(responseBody);
    }
}
