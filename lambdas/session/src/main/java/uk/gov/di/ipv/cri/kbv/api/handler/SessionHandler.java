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
import software.amazon.lambda.powertools.logging.CorrelationIdPathConstants;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;
import uk.gov.di.ipv.cri.kbv.api.domain.ParseJWT;
import uk.gov.di.ipv.cri.kbv.api.domain.PersonIdentity;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.domain.SessionRequest;
import uk.gov.di.ipv.cri.kbv.api.exception.ClientConfigurationException;
import uk.gov.di.ipv.cri.kbv.api.exception.ValidationException;
import uk.gov.di.ipv.cri.kbv.api.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.kbv.api.persistence.DataStore;
import uk.gov.di.ipv.cri.kbv.api.persistence.item.KBVSessionItem;
import uk.gov.di.ipv.cri.kbv.api.service.ConfigurationService;
import uk.gov.di.ipv.cri.kbv.api.service.StorageService;
import uk.gov.di.ipv.cri.kbv.api.validation.ValidatorService;

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
    private final ValidatorService validatorService;

    @ExcludeFromGeneratedCoverageReport
    public SessionHandler() {
        this(
                new ValidatorService(),
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
            ValidatorService validatorService,
            StorageService storageService,
            APIGatewayProxyResponseEvent apiGatewayProxyResponseEvent,
            ParseJWT jwtParser) {
        this.validatorService = validatorService;
        this.storageService = storageService;
        this.response = apiGatewayProxyResponseEvent;
        this.jwtParser = jwtParser;
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    @Logging(correlationIdPath = CorrelationIdPathConstants.API_GATEWAY_REST)
    @Metrics(captureColdStart = true)
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {

        response.withHeaders(Map.of("Content-Type", "application/json"));
        try {
            SessionRequest sessionRequest =
                    validatorService.validateSessionRequest(input.getBody());
            PersonIdentity identity =
                    jwtParser
                            .getPersonIdentity(sessionRequest.getRequestJWT())
                            .orElseThrow(NullPointerException::new);
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
        } catch (ValidationException e) {
            LOGGER.error("Session Validation Exception");
            response.withStatusCode(HttpStatus.SC_BAD_REQUEST);
        } catch (ClientConfigurationException e) {
            LOGGER.error("Server Configuration Error");
            response.withStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
        return response;
    }
}
