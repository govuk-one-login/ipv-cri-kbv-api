package uk.gov.di.ipv.cri.kbv.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.lambda.powertools.logging.CorrelationIdPathConstants;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventType;
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
import uk.gov.di.ipv.cri.kbv.api.gateway.KBVGatewayFactory;
import uk.gov.di.ipv.cri.kbv.api.gateway.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.service.KBVService;
import uk.gov.di.ipv.cri.kbv.api.service.KBVStorageService;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import static org.apache.logging.log4j.Level.ERROR;

public class QuestionAnswerHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String HEADER_SESSION_ID = "session-id";
    private static final String ERROR_KEY = "\"error\"";
    private static final String POST_ANSWER = "post_answer";
    private final ObjectMapper objectMapper;
    private final KBVService kbvService;
    private final KBVStorageService kbvStorageService;
    private final SessionService sessionService;
    private final EventProbe eventProbe;
    private final AuditService auditService;

    @ExcludeFromGeneratedCoverageReport
    public QuestionAnswerHandler() {
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        ConfigurationService configurationService = new ConfigurationService();
        this.kbvStorageService = new KBVStorageService(configurationService);
        this.kbvService =
                new KBVService(new KBVGatewayFactory(configurationService).getKbvGateway());
        this.eventProbe = new EventProbe();
        this.sessionService = new SessionService();
        this.auditService = new AuditService();
    }

    public QuestionAnswerHandler(
            ObjectMapper objectMapper,
            KBVStorageService kbvStorageService,
            KBVService kbvService,
            EventProbe eventProbe,
            SessionService sessionService,
            AuditService auditService) {
        this.objectMapper = objectMapper;
        this.kbvStorageService = kbvStorageService;
        this.eventProbe = eventProbe;
        this.sessionService = sessionService;
        this.auditService = auditService;
        this.kbvService = kbvService;
    }

    @Override
    @Logging(correlationIdPath = CorrelationIdPathConstants.API_GATEWAY_REST)
    @Metrics(captureColdStart = true)
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        response.withHeaders(Map.of("Content-Type", "application/json"));
        try {
            processAnswerResponse(input, response);
            eventProbe.counterMetric(POST_ANSWER);
        } catch (JsonProcessingException jsonProcessingException) {
            eventProbe.log(ERROR, jsonProcessingException).counterMetric(POST_ANSWER, 0d);
            response.withStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR);
            response.withBody(
                    "{ " + ERROR_KEY + ":\"Failed to parse object using ObjectMapper.\" }");
        } catch (NullPointerException npe) {
            eventProbe.log(ERROR, npe).counterMetric(POST_ANSWER, 0d);
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

    public void processAnswerResponse(
            APIGatewayProxyRequestEvent input, APIGatewayProxyResponseEvent response)
            throws IOException, SqsException {
        QuestionState questionState;
        String sessionId = input.getHeaders().get(HEADER_SESSION_ID);
        KBVItem kbvItem = kbvStorageService.getKBVItem(UUID.fromString(sessionId));

        questionState = objectMapper.readValue(kbvItem.getQuestionState(), QuestionState.class);
        QuestionAnswer answer = objectMapper.readValue(input.getBody(), QuestionAnswer.class);

        if (respondWithAnswerFromDbStore(answer, questionState, kbvItem, response)) return;
        respondWithAnswerFromExperianThenStoreInDb(questionState, kbvItem, response);
    }

    private void respondWithAnswerFromExperianThenStoreInDb(
            QuestionState questionState, KBVItem kbvItem, APIGatewayProxyResponseEvent response)
            throws IOException, SqsException {
        QuestionAnswerRequest questionAnswerRequest = new QuestionAnswerRequest();
        questionAnswerRequest.setUrn(kbvItem.getUrn());
        questionAnswerRequest.setAuthRefNo(kbvItem.getAuthRefNo());
        questionAnswerRequest.setQuestionAnswers(questionState.getAnswers());

        QuestionsResponse questionsResponse = kbvService.submitAnswers(questionAnswerRequest);
        if (questionsResponse.hasQuestions()) {
            questionState.setQAPairs(questionsResponse.getQuestions());
            var serializedQuestionState = objectMapper.writeValueAsString(questionState);
            kbvItem.setQuestionState(serializedQuestionState);
            kbvStorageService.update(kbvItem);
        } else if (questionsResponse.hasQuestionRequestEnded()) {
            var serializedQuestionState = objectMapper.writeValueAsString(questionState);
            kbvItem.setQuestionState(serializedQuestionState);
            kbvItem.setStatus(questionsResponse.getStatus());
            kbvStorageService.update(kbvItem);

            SessionItem sessionItem =
                    sessionService.getSession(String.valueOf(kbvItem.getSessionId()));
            sessionItem.setAuthorizationCode(UUID.randomUUID().toString());
            sessionService.createAuthorizationCode(sessionItem);
            auditService.sendAuditEvent(AuditEventType.THIRD_PARTY_REQUEST_ENDED);
        }
        response.withStatusCode(HttpStatusCode.OK);
    }

    private boolean respondWithAnswerFromDbStore(
            QuestionAnswer answer,
            QuestionState questionState,
            KBVItem kbvItem,
            APIGatewayProxyResponseEvent response)
            throws JsonProcessingException {

        questionState.setAnswer(answer);
        kbvItem.setQuestionState(objectMapper.writeValueAsString(questionState));
        kbvStorageService.update(kbvItem);
        response.withStatusCode(HttpStatusCode.OK);

        return questionState.hasAtLeastOneUnAnswered();
    }
}
