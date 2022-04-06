package uk.gov.di.ipv.cri.kbv.api.handler;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpStatus;
import software.amazon.lambda.powertools.logging.CorrelationIdPathConstants;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;
import uk.gov.di.ipv.cri.kbv.api.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.kbv.api.library.domain.ParseJWT;
import uk.gov.di.ipv.cri.kbv.api.library.domain.PersonIdentity;
import uk.gov.di.ipv.cri.kbv.api.library.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.library.domain.SessionRequest;
import uk.gov.di.ipv.cri.kbv.api.library.exception.ClientConfigurationException;
import uk.gov.di.ipv.cri.kbv.api.library.exception.ValidationException;
import uk.gov.di.ipv.cri.kbv.api.library.helpers.EventProbe;
import uk.gov.di.ipv.cri.kbv.api.library.persistence.DataStore;
import uk.gov.di.ipv.cri.kbv.api.library.persistence.item.KBVSessionItem;
import uk.gov.di.ipv.cri.kbv.api.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.kbv.api.library.service.StorageService;
import uk.gov.di.ipv.cri.kbv.api.library.validation.ValidatorService;

import java.text.ParseException;
import java.util.Map;
import java.util.UUID;

import static org.apache.logging.log4j.Level.ERROR;
import static org.apache.logging.log4j.Level.INFO;

public class SessionHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final ParseJWT parseJWT;
    private final StorageService storageService;
    private static final String HEADER_SESSION_ID = "session-id";
    public static final String EVENT_SESSION_CREATED = "session_created";
    private static ObjectMapper objectMapper = new ObjectMapper();
    private final EventProbe eventProbe;
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
                new ParseJWT(objectMapper),
                new EventProbe());
    }

    public SessionHandler(
            ValidatorService validatorService,
            StorageService storageService,
            APIGatewayProxyResponseEvent apiGatewayProxyResponseEvent,
            ParseJWT parseJWT,
            EventProbe eventProbe) {
        this.validatorService = validatorService;
        this.storageService = storageService;
        this.response = apiGatewayProxyResponseEvent;
        this.parseJWT = parseJWT;
        this.objectMapper.registerModule(new JavaTimeModule());
        this.eventProbe = eventProbe;
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
            eventProbe.addDimensions(Map.of("issuer", sessionRequest.getClientId()));
            PersonIdentity identity =
                    parseJWT.getPersonIdentity(sessionRequest.getRequestJWT())
                            .orElseThrow(NullPointerException::new);
            String key = UUID.randomUUID().toString();
            storageService.save(
                    key,
                    objectMapper.writeValueAsString(identity),
                    objectMapper.writeValueAsString(new QuestionState()));
            eventProbe.counterMetric(EVENT_SESSION_CREATED).auditEvent(sessionRequest);

            response.withBody(objectMapper.writeValueAsString(Map.of(HEADER_SESSION_ID, key)));
            response.withStatusCode(HttpStatus.SC_CREATED);
        } catch (JsonProcessingException | ParseException | NullPointerException e) {
            eventProbe.log(INFO, e).counterMetric(EVENT_SESSION_CREATED, 0d);
            response.withStatusCode(HttpStatus.SC_BAD_REQUEST);
            response.withBody("{ \"error\":\"The supplied JWT was not of the expected format.\" }");
        } catch (AmazonServiceException e) {
            eventProbe.log(ERROR, e).counterMetric(EVENT_SESSION_CREATED, 0d);
            response.withStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.withBody("{ \"error\":\"AWS Server error occurred.\" }");
        } catch (ValidationException e) {
            eventProbe.log(INFO, e).counterMetric(EVENT_SESSION_CREATED, 0d);
            response.withStatusCode(HttpStatus.SC_BAD_REQUEST);
            response.withBody("{ \"error\":\"Session Validation Exception.\" }");
        } catch (ClientConfigurationException e) {
            eventProbe.log(ERROR, e).counterMetric(EVENT_SESSION_CREATED, 0d);
            response.withStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.withBody("{ \"error\":\"Server Configuration Error.\" }");
        }
        return response;
    }
}
