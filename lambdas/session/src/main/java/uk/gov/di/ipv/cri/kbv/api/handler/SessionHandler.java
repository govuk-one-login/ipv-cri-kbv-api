package uk.gov.di.ipv.cri.kbv.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.lambda.powertools.tracing.CaptureMode;
import software.amazon.lambda.powertools.tracing.Tracing;
import uk.gov.di.ipv.cri.kbv.api.domain.ParseJWT;
import uk.gov.di.ipv.cri.kbv.api.domain.PersonIdentity;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.persistence.DataStore;
import uk.gov.di.ipv.cri.kbv.api.persistence.item.KBVSessionItem;
import uk.gov.di.ipv.cri.kbv.api.service.ConfigurationService;
import uk.gov.di.ipv.cri.kbv.api.service.StorageService;

import java.text.ParseException;
import java.util.Map;
import java.util.UUID;

public class SessionHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final ParseJWT jwtParser;
    private final StorageService storageService;
    private static final String HEADER_SESSION_ID = "session-id";
    private static ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionHandler.class);

    private APIGatewayProxyResponseEvent response;

    public SessionHandler() {
        this(
                new StorageService(
                        new DataStore<>(
                                ConfigurationService.getInstance().getKBVSessionTableName(),
                                KBVSessionItem.class,
                                DataStore.getClient(
                                        ConfigurationService.getInstance().isRunningLocally()))),
                new APIGatewayProxyResponseEvent(),
                new ParseJWT(objectMapper));
    }

    public SessionHandler(
            StorageService storageService,
            APIGatewayProxyResponseEvent apiGatewayProxyResponseEvent,
            ParseJWT jwtParser) {
        this.storageService = storageService;
        this.response = apiGatewayProxyResponseEvent;
        this.jwtParser = jwtParser;
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    @Tracing(captureMode = CaptureMode.DISABLED)
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {

        response.withHeaders(Map.of("Content-Type", "application/json"));
        try {
            PersonIdentity identity =
                    jwtParser.getPersonIdentity(input).orElseThrow(NullPointerException::new);
            String key = UUID.randomUUID().toString();
            storageService.save(
                    key,
                    objectMapper.writeValueAsString(identity),
                    objectMapper.writeValueAsString(new QuestionState()));
            response.withBody(objectMapper.writeValueAsString(Map.of(HEADER_SESSION_ID, key)));
            response.withStatusCode(HttpStatus.SC_CREATED);
        } catch (JsonProcessingException | ParseException | NullPointerException e) {
            LOGGER.error("The supplied JWT was not of the expected format.");
            response.withStatusCode(HttpStatus.SC_BAD_REQUEST);
            response.withBody("{ \"error\":\"" + e + "\" }");
        } catch (com.amazonaws.AmazonServiceException e) {
            LOGGER.error("AWS Server error occurred.");
            response.withStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.withBody("{ \"error\":\"" + e + "\" }");
        }
        return response;
    }
}
