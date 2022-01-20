package uk.gov.di.cri.experian.kbv.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.lambda.powertools.tracing.CaptureMode;
import software.amazon.lambda.powertools.tracing.Tracing;
import uk.gov.di.cri.experian.kbv.api.domain.PersonIdentity;
import uk.gov.di.cri.experian.kbv.api.domain.QuestionState;
import uk.gov.di.cri.experian.kbv.api.service.StorageService;

import java.util.Map;
import java.util.UUID;

public class SessionHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private ObjectMapper objectMapper;
    private StorageService storageService;
    public static final String HEADER_SESSION_ID = "session-id";

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
        return createResponseEvent(statusCode, responseBody, responseHeaders);
    }

    private static APIGatewayProxyResponseEvent createResponseEvent(
            int statusCode, String body, Map<String, String> headers) {
        APIGatewayProxyResponseEvent apiGatewayProxyResponseEvent =
                new APIGatewayProxyResponseEvent();
        apiGatewayProxyResponseEvent.setHeaders(headers);
        apiGatewayProxyResponseEvent.setStatusCode(statusCode);
        apiGatewayProxyResponseEvent.setBody(body);

        return apiGatewayProxyResponseEvent;
    }
}
