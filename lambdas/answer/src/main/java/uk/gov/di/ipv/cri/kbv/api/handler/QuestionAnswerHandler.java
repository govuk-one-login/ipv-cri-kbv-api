package uk.gov.di.ipv.cri.kbv.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.lambda.powertools.logging.CorrelationIdPathConstants;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.parameters.ParamManager;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventTypes;
import uk.gov.di.ipv.cri.common.library.exception.SqsException;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;
import uk.gov.di.ipv.cri.common.library.service.AuditService;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.service.SessionService;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswer;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.gateway.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.service.KBVService;
import uk.gov.di.ipv.cri.kbv.api.service.KBVServiceFactory;
import uk.gov.di.ipv.cri.kbv.api.service.KBVStorageService;
import uk.gov.di.ipv.cri.kbv.api.service.KBVSystemProperty;
import uk.gov.di.ipv.cri.kbv.api.service.KeyStoreService;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import static org.apache.logging.log4j.Level.ERROR;
import static org.apache.logging.log4j.Level.INFO;

public class QuestionAnswerHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    public static final String HEADER_SESSION_ID = "session-id";
    public static final String ERROR_KEY = "\"error\"";
    private static final String POST_ANSWER = "post_answer";
    private final ObjectMapper objectMapper;
    private final KBVService kbvService;
    private final KBVStorageService kbvStorageService;
    private final SessionService sessionService;
    private final APIGatewayProxyResponseEvent response;
    private final EventProbe eventProbe;
    private final AuditService auditService;

    @ExcludeFromGeneratedCoverageReport
    public QuestionAnswerHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.kbvStorageService = new KBVStorageService();
        this.kbvService = new KBVServiceFactory().create();
        this.response = new APIGatewayProxyResponseEvent();
        this.eventProbe = new EventProbe();
        this.sessionService = new SessionService();
        this.auditService =
                new AuditService(
                        SqsClient.builder().build(),
                        new ConfigurationService(),
                        new ObjectMapper());

        var kbvSystemProperty =
                new KBVSystemProperty(new KeyStoreService(ParamManager.getSecretsProvider()));

        kbvSystemProperty.save();
    }

    public QuestionAnswerHandler(
            ObjectMapper objectMapper,
            KBVStorageService kbvStorageService,
            KBVSystemProperty systemProperty,
            KBVServiceFactory kbvServiceFactory,
            EventProbe eventProbe,
            SessionService sessionService,
            AuditService auditService) {
        this.objectMapper = objectMapper;
        this.objectMapper.registerModule(new JavaTimeModule());
        this.kbvStorageService = kbvStorageService;
        this.response = new APIGatewayProxyResponseEvent();
        this.eventProbe = eventProbe;
        this.sessionService = sessionService;
        this.auditService = auditService;
        this.kbvService = kbvServiceFactory.create();
        systemProperty.save();
    }

    @Override
    @Logging(correlationIdPath = CorrelationIdPathConstants.API_GATEWAY_REST)
    @Metrics(captureColdStart = true)
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {
        response.withHeaders(Map.of("Content-Type", "application/json"));
        try {
            processAnswerResponse(input);
        } catch (JsonProcessingException jsonProcessingException) {
            eventProbe.log(ERROR, jsonProcessingException).counterMetric(POST_ANSWER, 0d);
            response.withStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR);
            response.withBody(
                    "{ " + ERROR_KEY + ":\"Failed to parse object using ObjectMapper.\" }");
        } catch (NullPointerException npe) {
            eventProbe.log(INFO, npe).counterMetric(POST_ANSWER, 0d);
            response.withStatusCode(HttpStatusCode.BAD_REQUEST);
            response.withBody("{ " + ERROR_KEY + ":\"Error finding the requested resource.\" }");
        } catch (IOException e) {
            eventProbe.log(ERROR, e).counterMetric(POST_ANSWER, 0d);
            response.withStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR);
            response.withBody(
                    "{ "
                            + ERROR_KEY
                            + ":\""
                            + String.format("Retrieving questions failed: %s", e)
                            + "\" }");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            eventProbe.log(ERROR, e).counterMetric(POST_ANSWER, 0d);
            response.withStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR);
            response.withBody("{ " + ERROR_KEY + ":\"AWS Server error occurred.\" }");
        }
        return response;
    }

    public void processAnswerResponse(APIGatewayProxyRequestEvent input)
            throws IOException, SqsException {
        QuestionState questionState;
        String sessionId = input.getHeaders().get(HEADER_SESSION_ID);

        KBVItem kbvItem = kbvStorageService.getKBVItem(UUID.fromString(sessionId));

        questionState = objectMapper.readValue(kbvItem.getQuestionState(), QuestionState.class);
        QuestionAnswer answer = objectMapper.readValue(input.getBody(), QuestionAnswer.class);

        if (respondWithAnswerFromDbStore(answer, questionState, kbvItem)) return;
        respondWithAnswerFromExperianThenStoreInDb(questionState, kbvItem);
    }

    private void respondWithAnswerFromExperianThenStoreInDb(
            QuestionState questionState, KBVItem kbvItem) throws IOException, SqsException {
        QuestionAnswerRequest questionAnswerRequest = new QuestionAnswerRequest();
        questionAnswerRequest.setUrn(kbvItem.getUrn());
        questionAnswerRequest.setAuthRefNo(kbvItem.getAuthRefNo());
        questionAnswerRequest.setQuestionAnswers(questionState.getAnswers());

        QuestionsResponse questionsResponse = kbvService.submitAnswers(questionAnswerRequest);

        if (questionsResponse.hasQuestions()) {
            questionState.setQAPairs(questionsResponse.getQuestions());
            kbvItem.setQuestionState(objectMapper.writeValueAsString(questionState));
            kbvStorageService.update(kbvItem);
        } else if (questionsResponse.hasQuestionRequestEnded()) {
            String state = objectMapper.writeValueAsString(questionState);
            kbvItem.setQuestionState(state);
            kbvItem.setStatus(questionsResponse.getStatus());
            kbvStorageService.update(kbvItem);

            SessionItem sessionItem =
                    sessionService.getSession(String.valueOf(kbvItem.getSessionId()));
            sessionItem.setAuthorizationCode(UUID.randomUUID().toString());
            sessionService.createAuthorizationCode(sessionItem);
            auditService.sendAuditEvent(AuditEventTypes.IPV_KBV_CRI_THIRD_PARTY_REQUEST_ENDED);
        } else {
            // TODO: alternate flow could end of transaction / or others
        }
        response.withStatusCode(HttpStatusCode.OK);
    }

    private boolean respondWithAnswerFromDbStore(
            QuestionAnswer answer, QuestionState questionState, KBVItem kbvItem)
            throws JsonProcessingException {

        questionState.setAnswer(answer);
        kbvItem.setQuestionState(objectMapper.writeValueAsString(questionState));
        kbvStorageService.update(kbvItem);
        response.withStatusCode(HttpStatusCode.OK);

        return questionState.hasAtLeastOneUnAnswered();
    }
}
