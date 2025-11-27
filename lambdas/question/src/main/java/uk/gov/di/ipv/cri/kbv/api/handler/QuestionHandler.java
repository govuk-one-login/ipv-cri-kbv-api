package uk.gov.di.ipv.cri.kbv.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebServiceSoap;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.lambda.powertools.logging.CorrelationIdPaths;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.FlushMetrics;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventContext;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventType;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentityDetailed;
import uk.gov.di.ipv.cri.common.library.exception.SessionExpiredException;
import uk.gov.di.ipv.cri.common.library.exception.SessionNotFoundException;
import uk.gov.di.ipv.cri.common.library.exception.SqsException;
import uk.gov.di.ipv.cri.common.library.persistence.item.EvidenceRequest;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;
import uk.gov.di.ipv.cri.common.library.service.AuditService;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.service.PersonIdentityService;
import uk.gov.di.ipv.cri.common.library.service.SessionService;
import uk.gov.di.ipv.cri.common.library.util.ApiGatewayResponseGenerator;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.common.library.util.TempCleaner;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestion;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.exception.InvalidStrategyScoreException;
import uk.gov.di.ipv.cri.kbv.api.exception.MissingClientIdException;
import uk.gov.di.ipv.cri.kbv.api.exception.QuestionNotFoundException;
import uk.gov.di.ipv.cri.kbv.api.service.IdentityIQWebServiceSoapCache;
import uk.gov.di.ipv.cri.kbv.api.service.KBVService;
import uk.gov.di.ipv.cri.kbv.api.service.KBVStorageService;
import uk.gov.di.ipv.cri.kbv.api.service.ServiceFactory;
import uk.gov.di.ipv.cri.kbv.api.util.EvidenceUtils;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static java.util.Optional.ofNullable;
import static org.apache.logging.log4j.Level.ERROR;
import static uk.gov.di.ipv.cri.common.library.error.ErrorResponse.SESSION_EXPIRED;
import static uk.gov.di.ipv.cri.common.library.error.ErrorResponse.SESSION_NOT_FOUND;
import static uk.gov.di.ipv.cri.kbv.api.domain.IIQAuditEventType.EXPERIAN_IIQ_STARTED;
import static uk.gov.di.ipv.cri.kbv.api.domain.IIQAuditEventType.THIN_FILE_ENCOUNTERED;
import static uk.gov.di.ipv.cri.kbv.api.domain.KbvResponsesAuditExtension.EXPERIAN_IIQ_RESPONSE;
import static uk.gov.di.ipv.cri.kbv.api.domain.KbvResponsesAuditExtension.createAuditEventExtensions;
import static uk.gov.di.ipv.cri.kbv.api.domain.KbvResponsesAuditExtension.createResponseReceivedAuditEventExtensions;

public class QuestionHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger LOGGER = LogManager.getLogger(QuestionHandler.class);
    public static final String HEADER_SESSION_ID = "session-id";
    public static final String LAMBDA_NAME = "get_question";
    public static final String ERROR_KEY = "error";
    public static final String IIQ_STRATEGY_PARAM_NAME = "IIQStrategy";
    private static final String IIQ_OPERATOR_ID_PARAM_NAME = "IIQOperatorId";
    public static final String METRIC_DIMENSION_QUESTION_ID = "kbv_question_id";
    public static final String METRIC_DIMENSION_QUESTION_STRATEGY = "question_strategy";
    public static final String METRIC_KBV_JOURNEY_TYPE = "kbv_journey_type";
    public static final String METRIC_REQUESTED_VERIFICATION_SCORE = "requested_verification_score";
    private final ObjectMapper objectMapper;
    private final KBVStorageService kbvStorageService;
    private final PersonIdentityService personIdentityService;
    private final EventProbe eventProbe;
    private final KBVService kbvService;
    private final AuditService auditService;
    private final ConfigurationService configurationService;
    private final SessionService sessionService;

    private final ServiceFactory serviceFactory;
    private final IdentityIQWebServiceSoapCache identityIQWebServiceSoapCache;

    @ExcludeFromGeneratedCoverageReport
    public QuestionHandler() {
        TempCleaner.clean();

        this.identityIQWebServiceSoapCache = new IdentityIQWebServiceSoapCache();

        this.serviceFactory = new ServiceFactory();

        DynamoDbEnhancedClient dynamoDbEnhancedClient = serviceFactory.getDynamoDbEnhancedClient();
        this.configurationService = serviceFactory.getConfigurationService();

        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.personIdentityService =
                new PersonIdentityService(
                        serviceFactory.getConfigurationService(),
                        serviceFactory.getDynamoDbEnhancedClient());

        this.kbvService = new KBVService(serviceFactory.getKbvGateway());
        this.kbvStorageService = new KBVStorageService(dynamoDbEnhancedClient);
        this.auditService = serviceFactory.getAuditService();
        this.sessionService = serviceFactory.getSessionService();
        this.eventProbe = new EventProbe();
    }

    public QuestionHandler( // NOSONAR
            ServiceFactory serviceFactory,
            ObjectMapper objectMapper,
            KBVStorageService kbvStorageService,
            PersonIdentityService personIdentityService,
            KBVService kbvService,
            ConfigurationService configurationService,
            EventProbe eventProbe,
            AuditService auditService,
            SessionService sessionService,
            IdentityIQWebServiceSoapCache identityIQWebServiceSoapCache) {
        this.serviceFactory = serviceFactory;
        this.objectMapper = objectMapper;
        this.kbvStorageService = kbvStorageService;
        this.personIdentityService = personIdentityService;
        this.eventProbe = eventProbe;
        this.auditService = auditService;
        this.kbvService = kbvService;
        this.configurationService = configurationService;
        this.sessionService = sessionService;
        this.identityIQWebServiceSoapCache = identityIQWebServiceSoapCache;
    }

    @Override
    @Logging(correlationIdPath = CorrelationIdPaths.API_GATEWAY_REST, clearState = true)
    @FlushMetrics(namespace = "kbv-cri-api", captureColdStart = true)
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

            String clientId = sessionItem.getClientId();

            if (clientId == null || clientId.isBlank()) {
                throw new MissingClientIdException();
            }

            KbvQuestion question =
                    processQuestionRequest(
                            identityIQWebServiceSoapCache.get(clientId, serviceFactory),
                            questionState,
                            kbvItem,
                            sessionItem,
                            input.getHeaders());

            eventProbe.addDimensions(
                    Map.of(
                            METRIC_DIMENSION_QUESTION_ID,
                            EventProbe.clean(question.getQuestionId())));

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
        } catch (MissingClientIdException ex) {
            return handleException(
                    HttpStatusCode.INTERNAL_SERVER_ERROR, ex, "Missing client identifier");
        } catch (QuestionNotFoundException qe) {
            decrementCounterMetrics();
            return createNoContentResponse();
        } catch (InvalidStrategyScoreException e) {
            return handleException(HttpStatusCode.INTERNAL_SERVER_ERROR, e, e.getMessage());
        } catch (IOException e) {
            return handleException(
                    HttpStatusCode.INTERNAL_SERVER_ERROR, e, "Retrieving questions failed.");
        } catch (Exception e) {
            return handleException(
                    HttpStatusCode.INTERNAL_SERVER_ERROR, e, "AWS Server error occurred.");
        }
    }

    KbvQuestion processQuestionRequest(
            IdentityIQWebServiceSoap identityIQWebServiceSoap,
            QuestionState questionState,
            KBVItem kbvItem,
            SessionItem sessionItem,
            Map<String, String> requestHeaders)
            throws IOException, SqsException, InvalidStrategyScoreException {

        var questionOptional = getQuestionFromDbStore(questionState);
        if (questionOptional.isPresent()) {
            return questionOptional.get();
        }
        var questionsResponse =
                getQuestionAnswerResponse(
                        identityIQWebServiceSoap, kbvItem, sessionItem, requestHeaders);
        questionOptional = getQuestionFromResponse(questionsResponse, questionState);
        sendQuestionReceivedAuditEvent(questionsResponse, sessionItem, requestHeaders);
        sendAuditEventIfThinFileEncountered(questionsResponse, sessionItem, requestHeaders);

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
            LOGGER.info(
                    "QUESTION HANDLER: QuestionIds from 3rd-party {}",
                    questionsResponse.getAllQuestionIds());
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
            IdentityIQWebServiceSoap identityIQWebServiceSoap,
            KBVItem kbvItem,
            SessionItem sessionItem,
            Map<String, String> requestHeaders)
            throws SqsException, JsonProcessingException, InvalidStrategyScoreException {
        Objects.requireNonNull(kbvItem, "kbvItem cannot be null");

        var personIdentity =
                personIdentityService.getPersonIdentityDetailed(kbvItem.getSessionId());
        var questionRequest =
                createQuestionRequest(personIdentity, sessionItem.getEvidenceRequest());

        sendQuestionRequestSentAuditEvent(sessionItem, personIdentity, requestHeaders);
        sendExperianIIQStartedAuditEvent(sessionItem, requestHeaders);
        return this.kbvService.getQuestions(identityIQWebServiceSoap, questionRequest);
    }

    private QuestionRequest createQuestionRequest(
            PersonIdentityDetailed personIdentity, EvidenceRequest evidenceRequest)
            throws JsonProcessingException, InvalidStrategyScoreException {
        var questionRequest = new QuestionRequest();
        var strategyParam = this.configurationService.getParameterValue(IIQ_STRATEGY_PARAM_NAME);

        Map<String, String> strategyMap =
                objectMapper.readValue(strategyParam, new TypeReference<>() {});

        int requestedVerificationScore = EvidenceUtils.getVerificationScoreForPass(evidenceRequest);
        questionRequest.setStrategy(
                getKbvQuestionStrategy(strategyMap, requestedVerificationScore));
        eventProbe.addDimensions(
                Map.of(
                        METRIC_REQUESTED_VERIFICATION_SCORE,
                        EventProbe.clean(String.valueOf(requestedVerificationScore)),
                        METRIC_DIMENSION_QUESTION_STRATEGY,
                        EventProbe.clean(questionRequest.getStrategy())));
        eventProbe.counterMetric(METRIC_KBV_JOURNEY_TYPE);

        questionRequest.setIiqOperatorId(
                this.configurationService.getParameterValue(IIQ_OPERATOR_ID_PARAM_NAME));

        questionRequest.setPersonIdentity(
                personIdentityService.convertToPersonIdentitySummary(personIdentity));

        return questionRequest;
    }

    private String getKbvQuestionStrategy(Map<String, String> strategyMap, int verificationScore) {
        String strategy =
                ofNullable(strategyMap.get(String.valueOf(verificationScore)))
                        .orElseThrow(InvalidStrategyScoreException::new);
        LOGGER.info("Using IIQStrategy: {}", strategy);
        return strategy;
    }

    private APIGatewayProxyResponseEvent handleException(
            int httpStatusCode, Throwable throwable, String errorMessage) {
        eventProbe.log(ERROR, throwable);
        decrementCounterMetrics();
        return httpStatusCode == HttpStatusCode.NO_CONTENT
                ? createNoContentResponse()
                : ApiGatewayResponseGenerator.proxyJsonResponse(
                        httpStatusCode, Map.of(ERROR_KEY, errorMessage));
    }

    private void decrementCounterMetrics() {
        eventProbe.counterMetric(LAMBDA_NAME, 0d);
        eventProbe.counterMetric(METRIC_KBV_JOURNEY_TYPE, 0d);
    }

    private APIGatewayProxyResponseEvent createNoContentResponse() {
        return new APIGatewayProxyResponseEvent().withStatusCode(HttpStatusCode.NO_CONTENT);
    }

    private void sendExperianIIQStartedAuditEvent(
            SessionItem sessionItem, Map<String, String> requestHeaders) throws SqsException {
        auditService.sendAuditEvent(
                EXPERIAN_IIQ_STARTED.toString(),
                new AuditEventContext(requestHeaders, sessionItem),
                getComponentId());
    }

    private void sendQuestionRequestSentAuditEvent(
            SessionItem sessionItem,
            PersonIdentityDetailed personIdentity,
            Map<String, String> requestHeaders)
            throws SqsException {
        auditService.sendAuditEvent(
                AuditEventType.REQUEST_SENT,
                new AuditEventContext(personIdentity, requestHeaders, sessionItem),
                getComponentId());
    }

    private void sendQuestionReceivedAuditEvent(
            QuestionsResponse questionsResponse,
            SessionItem sessionItem,
            Map<String, String> requestHeaders)
            throws SqsException {
        auditService.sendAuditEvent(
                AuditEventType.RESPONSE_RECEIVED,
                new AuditEventContext(requestHeaders, sessionItem),
                Map.of(
                        EXPERIAN_IIQ_RESPONSE,
                        createResponseReceivedAuditEventExtensions(questionsResponse)));
    }

    private void sendAuditEventIfThinFileEncountered(
            QuestionsResponse questionsResponse,
            SessionItem sessionItem,
            Map<String, String> requestHeaders)
            throws SqsException {
        if (Objects.nonNull(questionsResponse) && questionsResponse.isThinFile()) {
            auditService.sendAuditEvent(
                    THIN_FILE_ENCOUNTERED.toString(),
                    new AuditEventContext(requestHeaders, sessionItem),
                    Map.of(EXPERIAN_IIQ_RESPONSE, createAuditEventExtensions(questionsResponse)));
        }
    }

    private Map<String, String> getComponentId() {
        return Map.of("component_id", configurationService.getVerifiableCredentialIssuer());
    }
}
