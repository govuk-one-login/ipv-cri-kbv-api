package uk.gov.di.ipv.cri.kbv.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.lambda.powertools.logging.CorrelationIdPathConstants;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.tracing.Tracing;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventContext;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventType;
import uk.gov.di.ipv.cri.common.library.exception.SessionExpiredException;
import uk.gov.di.ipv.cri.common.library.exception.SessionNotFoundException;
import uk.gov.di.ipv.cri.common.library.exception.SqsException;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;
import uk.gov.di.ipv.cri.common.library.service.AuditService;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.service.SessionService;
import uk.gov.di.ipv.cri.common.library.util.ApiGatewayResponseGenerator;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestion;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswer;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.gateway.KBVGatewayFactory;
import uk.gov.di.ipv.cri.kbv.api.service.KBVService;
import uk.gov.di.ipv.cri.kbv.api.service.KBVStorageService;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.apache.logging.log4j.Level.ERROR;
import static uk.gov.di.ipv.cri.common.library.error.ErrorResponse.SESSION_EXPIRED;
import static uk.gov.di.ipv.cri.common.library.error.ErrorResponse.SESSION_NOT_FOUND;
import static uk.gov.di.ipv.cri.kbv.api.domain.KbvResponsesAuditExtension.EXPERIAN_IIQ_RESPONSE;
import static uk.gov.di.ipv.cri.kbv.api.domain.KbvResponsesAuditExtension.createAuditEventExtensions;

@ExcludeFromGeneratedCoverageReport
public class QuestionAnswerHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String HEADER_SESSION_ID = "session-id";
    private static final String ERROR_KEY = "error";
    private static final String LAMBDA_NAME = "post_answer";
    private final ObjectMapper objectMapper;
    private final KBVService kbvService;
    private final KBVStorageService kbvStorageService;
    private final SessionService sessionService;
    private final EventProbe eventProbe;
    private final AuditService auditService;
    private final ConfigurationService configurationService;
    private static final Logger LOGGER = LogManager.getLogger(QuestionAnswerHandler.class);

    @ExcludeFromGeneratedCoverageReport
    public QuestionAnswerHandler() {
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.configurationService = new ConfigurationService();
        this.kbvStorageService = new KBVStorageService(configurationService);
        this.kbvService = new KBVService(new KBVGatewayFactory().create(configurationService));
        this.sessionService = new SessionService(configurationService);
        this.auditService = new AuditService(configurationService);

        this.eventProbe = new EventProbe();
    }

    public QuestionAnswerHandler(
            ObjectMapper objectMapper,
            KBVStorageService kbvStorageService,
            KBVService kbvService,
            EventProbe eventProbe,
            SessionService sessionService,
            ConfigurationService configurationService,
            AuditService auditService) {
        this.objectMapper = objectMapper;
        this.kbvStorageService = kbvStorageService;
        this.sessionService = sessionService;
        this.auditService = auditService;
        this.kbvService = kbvService;
        this.configurationService = configurationService;

        this.eventProbe = eventProbe;
    }

    @Override
    @Logging(correlationIdPath = CorrelationIdPathConstants.API_GATEWAY_REST, clearState = true)
    @Metrics(captureColdStart = true)
    @Tracing
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {
        try {
            handleRequest(input.getBody(), input.getHeaders());
            eventProbe.counterMetric(LAMBDA_NAME);
            return new APIGatewayProxyResponseEvent()
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withStatusCode(HttpStatusCode.OK);
        } catch (SessionExpiredException sessionExpiredException) {
            return handleException(
                    HttpStatusCode.FORBIDDEN,
                    sessionExpiredException,
                    "Access denied by resource owner or authorization server - "
                            + SESSION_EXPIRED.getErrorSummary());
        } catch (SessionNotFoundException sessionNotFoundException) {
            return handleException(
                    HttpStatusCode.FORBIDDEN,
                    sessionNotFoundException,
                    "Access denied by resource owner or authorization server - "
                            + SESSION_NOT_FOUND.getErrorSummary());
        } catch (JsonProcessingException jsonProcessingException) {
            return handleException(
                    HttpStatusCode.INTERNAL_SERVER_ERROR,
                    jsonProcessingException,
                    "Failed to parse object using ObjectMapper.");
        } catch (NullPointerException npe) {
            return handleException(
                    HttpStatusCode.BAD_REQUEST, npe, "Error finding the requested resource.");
        } catch (IOException e) {
            Thread.currentThread().interrupt();
            return handleException(
                    HttpStatusCode.INTERNAL_SERVER_ERROR,
                    e,
                    String.format("Retrieving questions failed: %s", e));
        } catch (IllegalStateException ise) {
            return handleException(
                    HttpStatusCode.INTERNAL_SERVER_ERROR,
                    ise,
                    "Third Party Server error occurred.");
        } catch (Exception e) {
            return handleException(
                    HttpStatusCode.INTERNAL_SERVER_ERROR, e, "AWS Server error occurred.");
        }
    }

    public void handleRequest(String requestBody, Map<String, String> requestHeaders)
            throws IOException, SqsException {
        var sessionItem = sessionService.validateSessionId(requestHeaders.get(HEADER_SESSION_ID));
        var kbvItem = kbvStorageService.getKBVItem(sessionItem.getSessionId());

        var questionState = objectMapper.readValue(kbvItem.getQuestionState(), QuestionState.class);
        var submittedAnswer = objectMapper.readValue(requestBody, QuestionAnswer.class);

        String questionIdsAllQaPairs =
                questionState.getQuestionIdsFromQAPairs().collect(Collectors.joining(","));
        String questionIdsQaPairs =
                questionState.getQaPairs().stream()
                        .map(item -> item.getQuestion().getQuestionId())
                        .collect(Collectors.joining(","));
        LOGGER.info(
                "respondWithAnswerFromDbStore to questionId: {} questionState: allQaPair questionIds {}",
                submittedAnswer.getQuestionId(),
                questionIdsAllQaPairs);
        LOGGER.info(
                "respondWithAnswerFromDbStore to questionId: {} questionState: qaPair questionIds {}",
                submittedAnswer.getQuestionId(),
                questionIdsQaPairs);
        if (respondWithAnswerFromDbStore(submittedAnswer, questionState, kbvItem)) return;
        respondWithAnswerFromExperianThenStoreInDb(
                questionState, kbvItem, sessionItem, requestHeaders);
    }

    private void respondWithAnswerFromExperianThenStoreInDb(
            QuestionState questionState,
            KBVItem kbvItem,
            SessionItem sessionItem,
            Map<String, String> requestHeaders)
            throws IOException, SqsException {
        var questionAnswerRequest = new QuestionAnswerRequest();
        questionAnswerRequest.setUrn(kbvItem.getUrn());
        questionAnswerRequest.setAuthRefNo(kbvItem.getAuthRefNo());
        questionAnswerRequest.setQuestionAnswers(questionState.getAnswers());
        auditService.sendAuditEvent(
                AuditEventType.REQUEST_SENT,
                new AuditEventContext(requestHeaders, sessionItem),
                Map.of("component_id", configurationService.getVerifiableCredentialIssuer()));
        String questionAnswerIds =
                questionAnswerRequest.getQuestionAnswers().stream()
                        .map(QuestionAnswer::getQuestionId)
                        .collect(Collectors.joining(","));
        LOGGER.info(
                "QUESTION ANSWER HANDLER: questionId: {} answered and sent to 3rd-party",
                questionAnswerIds);
        var questionsResponse = kbvService.submitAnswers(questionAnswerRequest);
        auditService.sendAuditEvent(
                AuditEventType.RESPONSE_RECEIVED,
                new AuditEventContext(requestHeaders, sessionItem),
                Map.of(EXPERIAN_IIQ_RESPONSE, createAuditEventExtensions(questionsResponse)));
        if (questionsResponse.hasQuestions()) {
            String questionIdsInResponse =
                    Arrays.stream(questionsResponse.getQuestions())
                            .map(KbvQuestion::getQuestionId)
                            .collect(Collectors.joining(","));
            LOGGER.info(
                    "QUESTION ANSWER HANDLER: questionId: {} received from 3rd-party",
                    questionIdsInResponse);
            questionState.setQAPairs(questionsResponse.getQuestions());
            var serializedQuestionState = objectMapper.writeValueAsString(questionState);
            kbvItem.setQuestionState(serializedQuestionState);
            kbvStorageService.update(kbvItem);
        } else if (Objects.nonNull(questionsResponse.getResults())
                && questionsResponse.hasQuestionRequestEnded()) {
            var serializedQuestionState = objectMapper.writeValueAsString(questionState);
            kbvItem.setQuestionState(serializedQuestionState);
            kbvItem.setStatus(questionsResponse.getStatus());
            kbvItem.setQuestionAnswerResultSummary(
                    questionsResponse.getQuestionAnswerResultSummary());
            kbvStorageService.update(kbvItem);

            sessionService.createAuthorizationCode(sessionItem);
        } else if (questionsResponse.hasError()) {
            var serializedQuestionState = objectMapper.writeValueAsString(questionState);
            kbvItem.setQuestionState(serializedQuestionState);
            kbvItem.setStatus(questionsResponse.getErrorMessage());
            kbvStorageService.update(kbvItem);
            throw new IllegalStateException(
                    String.format(
                            "Third party API invocation error, code: %s, message: %s",
                            questionsResponse.getErrorCode(), questionsResponse.getErrorMessage()));
        }
    }

    private boolean respondWithAnswerFromDbStore(
            QuestionAnswer answer, QuestionState questionState, KBVItem kbvItem)
            throws JsonProcessingException {

        questionState.setAnswer(answer);
        kbvItem.setQuestionState(objectMapper.writeValueAsString(questionState));
        kbvStorageService.update(kbvItem);

        return questionState.hasAtLeastOneUnanswered();
    }

    private APIGatewayProxyResponseEvent handleException(
            int httpStatusCode, Throwable throwable, String errorMessage) {
        eventProbe.log(ERROR, throwable).counterMetric(LAMBDA_NAME, 0d);
        return httpStatusCode == HttpStatusCode.NO_CONTENT
                ? createNoContentResponse()
                : ApiGatewayResponseGenerator.proxyJsonResponse(
                        httpStatusCode, Map.of(ERROR_KEY, errorMessage));
    }

    private APIGatewayProxyResponseEvent createNoContentResponse() {
        return new APIGatewayProxyResponseEvent().withStatusCode(HttpStatusCode.NO_CONTENT);
    }
}
