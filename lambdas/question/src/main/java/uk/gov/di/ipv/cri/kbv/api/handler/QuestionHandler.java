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
import software.amazon.lambda.powertools.tracing.Tracing;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventContext;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventType;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentityDetailed;
import uk.gov.di.ipv.cri.common.library.exception.SessionExpiredException;
import uk.gov.di.ipv.cri.common.library.exception.SessionNotFoundException;
import uk.gov.di.ipv.cri.common.library.exception.SqsException;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;
import uk.gov.di.ipv.cri.common.library.service.AuditService;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.service.PersonIdentityService;
import uk.gov.di.ipv.cri.common.library.service.SessionService;
import uk.gov.di.ipv.cri.common.library.util.ApiGatewayResponseGenerator;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestion;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.exception.QuestionNotFoundException;
import uk.gov.di.ipv.cri.kbv.api.gateway.KBVGatewayFactory;
import uk.gov.di.ipv.cri.kbv.api.service.KBVService;
import uk.gov.di.ipv.cri.kbv.api.service.KBVStorageService;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.apache.logging.log4j.Level.ERROR;
import static uk.gov.di.ipv.cri.common.library.error.ErrorResponse.SESSION_EXPIRED;
import static uk.gov.di.ipv.cri.common.library.error.ErrorResponse.SESSION_NOT_FOUND;

public class QuestionHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    public static final String HEADER_SESSION_ID = "session-id";
    public static final String LAMBDA_NAME = "get_question";
    public static final String ERROR_KEY = "error";
    public static final String IIQ_STRATEGY_PARAM_NAME = "IIQStrategy";
    private static final String IIQ_OPERATOR_ID_PARAM_NAME = "IIQOperatorId";
    public static final String METRIC_DIMENSION_QUESTION_ID = "kbv_question_id";
    public static final String METRIC_DIMENSION_QUESTION_STRATEGY = "question_strategy";
    private final ObjectMapper objectMapper;
    private final KBVStorageService kbvStorageService;
    private final PersonIdentityService personIdentityService;
    private final EventProbe eventProbe;
    private final KBVService kbvService;
    private final AuditService auditService;
    private final ConfigurationService configurationService;
    private final SessionService sessionService;

    @ExcludeFromGeneratedCoverageReport
    public QuestionHandler() {
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.personIdentityService = new PersonIdentityService();
        this.configurationService = new ConfigurationService();
        this.kbvService = new KBVService(new KBVGatewayFactory().create(this.configurationService));
        this.kbvStorageService = new KBVStorageService(this.configurationService);
        this.auditService = new AuditService(this.configurationService);
        this.sessionService = new SessionService(this.configurationService);

        this.eventProbe = new EventProbe();
    }

    public QuestionHandler(
            ObjectMapper objectMapper,
            KBVStorageService kbvStorageService,
            PersonIdentityService personIdentityService,
            KBVService kbvService,
            ConfigurationService configurationService,
            EventProbe eventProbe,
            AuditService auditService,
            SessionService sessionService) {
        this.objectMapper = objectMapper;
        this.kbvStorageService = kbvStorageService;
        this.personIdentityService = personIdentityService;
        this.eventProbe = eventProbe;
        this.auditService = auditService;
        this.kbvService = kbvService;
        this.configurationService = configurationService;
        this.sessionService = sessionService;
    }

    @Override
    @Logging(correlationIdPath = CorrelationIdPathConstants.API_GATEWAY_REST, clearState = true)
    @Metrics(captureColdStart = true)
    @Tracing
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {

        try {
            var sessionId = UUID.fromString(input.getHeaders().get(HEADER_SESSION_ID));
            var sessionItem = sessionService.validateSessionId(String.valueOf(sessionId));
            var kbvItem = kbvStorageService.getKBVItem(sessionId);
            var questionState = new QuestionState();
            if (kbvItem != null) {
                questionState =
                        objectMapper.readValue(kbvItem.getQuestionState(), QuestionState.class);
            } else {
                kbvItem = new KBVItem();
                kbvItem.setSessionId(sessionId);
            }
            if (Objects.nonNull(kbvItem.getStatus())) {
                eventProbe.counterMetric(LAMBDA_NAME);
                return createNoContentResponse();
            }

            KbvQuestion question =
                    processQuestionRequest(questionState, kbvItem, sessionItem, input.getHeaders());
            eventProbe.addDimensions(
                    Map.of(METRIC_DIMENSION_QUESTION_ID, question.getQuestionId()));
            eventProbe.counterMetric(LAMBDA_NAME);
            return ApiGatewayResponseGenerator.proxyJsonResponse(HttpStatusCode.OK, question);
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
            return handleException(HttpStatusCode.BAD_REQUEST, npe, npe.toString());
        } catch (QuestionNotFoundException qe) {
            eventProbe.counterMetric(LAMBDA_NAME, 0d);
            return createNoContentResponse();
        } catch (IOException e) {
            return handleException(
                    HttpStatusCode.INTERNAL_SERVER_ERROR, e, "Retrieving questions failed.");
        } catch (Exception e) {
            return handleException(
                    HttpStatusCode.INTERNAL_SERVER_ERROR, e, "AWS Server error occurred.");
        }
    }

    private void sendQuestionReceivedAuditEvent(
            QuestionsResponse questionsResponse,
            SessionItem sessionItem,
            Map<String, String> requestHeaders)
            throws SqsException {
        auditService.sendAuditEvent(
                AuditEventType.RESPONSE_RECEIVED,
                new AuditEventContext(requestHeaders, sessionItem),
                this.kbvService.createAuditEventExtensions(questionsResponse));
    }

    @Tracing
    KbvQuestion processQuestionRequest(
            QuestionState questionState,
            KBVItem kbvItem,
            SessionItem sessionItem,
            Map<String, String> requestHeaders)
            throws IOException, SqsException {

        Optional<KbvQuestion> questionOptional;
        questionOptional = getQuestionFromDbStore(questionState);
        if (questionOptional.isPresent()) {
            return questionOptional.get();
        }
        var questionsResponse = getQuestionAnswerResponse(kbvItem, sessionItem, requestHeaders);
        questionOptional = getQuestionFromResponse(questionsResponse, questionState);
        sendQuestionReceivedAuditEvent(questionsResponse, sessionItem, requestHeaders);
        saveQuestionStateToKbvItem(kbvItem, questionState, questionsResponse);
        if (questionOptional.isPresent()) {
            return questionOptional.get();
        }
        sessionService.createAuthorizationCode(sessionItem);
        throw new QuestionNotFoundException("No questions available");
    }

    private Optional<KbvQuestion> getQuestionFromDbStore(QuestionState questionState) {
        Objects.requireNonNull(questionState, "questionState cannot be null");
        return questionState.getNextQuestion();
    }

    private Optional<KbvQuestion> getQuestionFromResponse(
            QuestionsResponse questionsResponse, QuestionState questionState) {

        if (questionsResponse != null && questionsResponse.hasQuestions()) {
            questionState.setQAPairs(questionsResponse.getQuestions());
            return questionState.getNextQuestion();
        }
        // Alternate flow when first request does not return questions
        return Optional.empty();
    }

    private void saveQuestionStateToKbvItem(
            KBVItem kbvItem, QuestionState questionState, QuestionsResponse questionsResponse)
            throws JsonProcessingException {
        var state = objectMapper.writeValueAsString(questionState);
        kbvItem.setQuestionState(state);
        kbvItem.setAuthRefNo(questionsResponse.getAuthReference());
        kbvItem.setUrn(questionsResponse.getUniqueReference());
        kbvItem.setExpiryDate(this.configurationService.getSessionExpirationEpoch());

        kbvStorageService.save(kbvItem);
    }

    private QuestionsResponse getQuestionAnswerResponse(
            KBVItem kbvItem, SessionItem sessionItem, Map<String, String> requestHeaders)
            throws SqsException {
        Objects.requireNonNull(kbvItem, "kbvItem cannot be null");

        var personIdentity =
                personIdentityService.getPersonIdentityDetailed(kbvItem.getSessionId());
        var questionRequest = createQuestionRequest(personIdentity);

        eventProbe.addDimensions(
                Map.of(METRIC_DIMENSION_QUESTION_STRATEGY, questionRequest.getStrategy()));

        sendQuestionRequestAuditEvent(sessionItem, personIdentity, requestHeaders);
        var questionResponse = this.kbvService.getQuestions(questionRequest);

        return questionResponse;
    }

    private void sendQuestionRequestAuditEvent(
            SessionItem sessionItem,
            PersonIdentityDetailed personIdentity,
            Map<String, String> requestHeaders)
            throws SqsException {
        auditService.sendAuditEvent(
                AuditEventType.REQUEST_SENT,
                new AuditEventContext(personIdentity, requestHeaders, sessionItem),
                Map.of("component_id", configurationService.getVerifiableCredentialIssuer()));
    }

    private QuestionRequest createQuestionRequest(PersonIdentityDetailed personIdentity) {
        var questionRequest = new QuestionRequest();
        var strategy = this.configurationService.getParameterValue(IIQ_STRATEGY_PARAM_NAME);

        questionRequest.setStrategy(strategy);
        questionRequest.setIiqOperatorId(
                this.configurationService.getParameterValue(IIQ_OPERATOR_ID_PARAM_NAME));
        questionRequest.setPersonIdentity(
                personIdentityService.convertToPersonIdentitySummary(personIdentity));

        return questionRequest;
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
