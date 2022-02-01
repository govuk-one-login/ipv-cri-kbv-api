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
import uk.gov.di.ipv.cri.kbv.api.persistence.DataStore;
import uk.gov.di.ipv.cri.kbv.api.persistence.item.KBVSessionItem;
import uk.gov.di.ipv.cri.kbv.api.service.ConfigurationService;
import uk.gov.di.ipv.cri.kbv.api.service.StorageService;

import java.util.Map;
import java.util.UUID;

public class SessionHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final ObjectMapper objectMapper;
    private final StorageService storageService;
    public static final String HEADER_SESSION_ID = "session-id";

    private APIGatewayProxyResponseEvent response;

    public SessionHandler() {
        this(
                new ObjectMapper(),
                new StorageService(
                        new DataStore<KBVSessionItem>(
                                ConfigurationService.getInstance().getKBVSessionTableName(),
                                KBVSessionItem.class,
                                DataStore.getClient(false),
                                false)),
                new APIGatewayProxyResponseEvent());

        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public SessionHandler(
            ObjectMapper objectMapper,
            StorageService storageService,
            APIGatewayProxyResponseEvent response) {
        this.objectMapper = objectMapper;
        this.storageService = storageService;
        this.response = response;
    }

    @Override
    @Tracing(captureMode = CaptureMode.DISABLED)
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {
        PersonIdentity identity;

        response.withHeaders(Map.of("Content-Type", "application/json"));
        try {
            identity = objectMapper.readValue(input.getBody(), PersonIdentity.class);
            String questionState = objectMapper.writeValueAsString(new QuestionState(identity));
            String key = UUID.randomUUID().toString();
            storageService.save(key, questionState);
            response.withBody(objectMapper.writeValueAsString(Map.of(HEADER_SESSION_ID, key)));
            response.withStatusCode(201);

        } catch (JsonProcessingException e) {
            e.printStackTrace();
            response.withStatusCode(500);
            response.withBody("{ \"error\":\"" + e.getMessage() + "\" }");
        }
        return response;
    }
}
