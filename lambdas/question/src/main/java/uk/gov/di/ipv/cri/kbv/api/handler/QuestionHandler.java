package uk.gov.di.ipv.cri.kbv.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.experian.uk.schema.experian.identityiq.services.webservice.Question;
import com.experian.uk.schema.experian.identityiq.services.webservice.ResultsQuestions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.lambda.powertools.logging.CorrelationIdPathConstants;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventType;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentityDetailed;
import uk.gov.di.ipv.cri.common.library.exception.SqsException;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;
import uk.gov.di.ipv.cri.common.library.service.AuditService;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.service.PersonIdentityService;
import uk.gov.di.ipv.cri.common.library.service.SessionService;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.exception.QuestionNotFoundException;
import uk.gov.di.ipv.cri.kbv.api.gateway.KBVGatewayFactory;
import uk.gov.di.ipv.cri.kbv.api.gateway.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.service.KBVService;
import uk.gov.di.ipv.cri.kbv.api.service.KBVStorageService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.apache.logging.log4j.Level.ERROR;
import static software.amazon.awssdk.http.HttpStatusCode.BAD_REQUEST;
import static software.amazon.awssdk.http.HttpStatusCode.OK;

public class QuestionHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    public static final String HEADER_SESSION_ID = "session-id";
    public static final String GET_QUESTION = "get_question";
    public static final String ERROR_KEY = "\"error\"";
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
        this.kbvService =
                new KBVService(new KBVGatewayFactory(this.configurationService).getKbvGateway());
        this.kbvStorageService = new KBVStorageService(this.configurationService);
        this.eventProbe = new EventProbe();
        this.auditService = new AuditService();
        this.sessionService = new SessionService();
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
    @Logging(correlationIdPath = CorrelationIdPathConstants.API_GATEWAY_REST)
    @Metrics(captureColdStart = true)
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.withHeaders(Map.of("Content-Type", "application/json"));
        try {
            UUID sessionId = UUID.fromString(input.getHeaders().get(HEADER_SESSION_ID));
            KBVItem kbvItem = kbvStorageService.getKBVItem(sessionId);
            QuestionState questionState = new QuestionState();
            if (kbvItem != null) {
                questionState =
                        objectMapper.readValue(kbvItem.getQuestionState(), QuestionState.class);
            } else {
                kbvItem = new KBVItem();
                kbvItem.setSessionId(sessionId);
            }
            if (Objects.nonNull(kbvItem.getStatus())) {
                response.withStatusCode(HttpStatusCode.NO_CONTENT);
                eventProbe.counterMetric(GET_QUESTION);
                return response;
            }

            Question question = processQuestionRequest(questionState, kbvItem);
            response.withBody(objectMapper.writeValueAsString(question));
            response.withStatusCode(OK);
            eventProbe.counterMetric(GET_QUESTION);
        } catch (JsonProcessingException jsonProcessingException) {
            eventProbe.log(ERROR, jsonProcessingException).counterMetric(GET_QUESTION, 0d);
            response.withStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR);
            response.withBody(
                    "{ " + ERROR_KEY + ":\"Failed to parse object using ObjectMapper.\" }");
        } catch (NullPointerException npe) {
            eventProbe.log(ERROR, npe).counterMetric(GET_QUESTION, 0d);
            response.withStatusCode(BAD_REQUEST);
            response.withBody("{ " + ERROR_KEY + ":\"" + npe + "\" }");
        } catch (QuestionNotFoundException qe) {
            eventProbe.log(ERROR, qe).counterMetric(GET_QUESTION, 0d);
            response.withStatusCode(HttpStatusCode.NO_CONTENT);
        } catch (IOException e) {
            eventProbe.log(ERROR, e).counterMetric(GET_QUESTION, 0d);
            response.withStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR);
            response.withBody("{ " + ERROR_KEY + ":\"Retrieving questions failed.\" }");
        } catch (Exception e) {
            eventProbe.log(ERROR, e).counterMetric(GET_QUESTION, 0d);
            response.withStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR);
            response.withBody("{ " + ERROR_KEY + ":\"AWS Server error occurred.\" }");
        }
        return response;
    }

    private void sendNoQuestionAuditEvent(QuestionsResponse questionsResponse) throws SqsException {
        auditService.sendAuditEvent(
                AuditEventType.THIRD_PARTY_REQUEST_ENDED,
                createAuditEventContext(questionsResponse));
    }

    private void setAuthCode(KBVItem kbvItem) {
        SessionItem sessionItem = sessionService.getSession(String.valueOf(kbvItem.getSessionId()));
        sessionService.createAuthorizationCode(sessionItem);
    }

    private Map<String, Object> createAuditEventContext(QuestionsResponse questionsResponse) {
        Map<String, Object> contextEntries = new HashMap<>();
        contextEntries.put("outcome", questionsResponse.getStatus());
        if (Objects.nonNull(questionsResponse.getResults())
                && Objects.nonNull(questionsResponse.getResults().getQuestions())) {
            ResultsQuestions questionSummary = questionsResponse.getResults().getQuestions();
            contextEntries.put("totalQuestionsAsked", questionSummary.getAsked());
            contextEntries.put("totalQuestionsAnsweredCorrect", questionSummary.getCorrect());
            contextEntries.put("totalQuestionsAnsweredIncorrect", questionSummary.getIncorrect());
        }
        return Map.of("experianIiqResponse", contextEntries);
    }

    public Question processQuestionRequest(QuestionState questionState, KBVItem kbvItem)
            throws IOException, SqsException {

        Question question;
        if ((question = getQuestionFromDbStore(questionState)) != null) {
            return question;
        }
        var questionsResponse = getQuestionAnswerResponse(kbvItem);
        if ((question = getQuestionFromResponse(questionsResponse, questionState)) != null) {
            saveQuestionStateToKbvItem(kbvItem, questionState, questionsResponse);
            return question;
        }
        setAuthCode(kbvItem);
        sendNoQuestionAuditEvent(questionsResponse);
        throw new QuestionNotFoundException("No questions available");
    }

    private Question getQuestionFromDbStore(QuestionState questionState) {
        Objects.requireNonNull(questionState, "questionState cannot be null");
        // TODO Handle scenario when no questions are available
        return questionState.getNextQuestion().orElse(null);
    }

    private Question getQuestionFromResponse(
            QuestionsResponse questionsResponse, QuestionState questionState) {

        if (questionsResponse != null && questionsResponse.hasQuestions()) {
            questionState.setQAPairs(questionsResponse.getQuestions());
            return questionState.getNextQuestion().orElse(null);
        } else { // Alternate flow when first request does not return questions
            return null;
        }
    }

    private void saveQuestionStateToKbvItem(
            KBVItem kbvItem, QuestionState questionState, QuestionsResponse questionsResponse)
            throws JsonProcessingException {
        String state = objectMapper.writeValueAsString(questionState);
        kbvItem.setQuestionState(state);
        kbvItem.setAuthRefNo(questionsResponse.getControl().getAuthRefNo());
        kbvItem.setUrn(questionsResponse.getControl().getURN());
        kbvItem.setExpiryDate(this.configurationService.getSessionExpirationEpoch());

        kbvStorageService.save(kbvItem);
    }

    private QuestionsResponse getQuestionAnswerResponse(KBVItem kbvItem)
            throws JsonProcessingException, SqsException {
        Objects.requireNonNull(kbvItem, "kbvItem cannot be null");

        if (kbvItem.getExpiryDate() == 0L) { // first request for questions for a given session
            PersonIdentityDetailed personIdentity =
                    personIdentityService.getPersonIdentityDetailed(kbvItem.getSessionId());
            QuestionRequest questionRequest = new QuestionRequest();
            questionRequest.setPersonIdentity(
                    personIdentityService.convertToPersonIdentitySummary(personIdentity));
            QuestionsResponse questionsResponse = this.kbvService.getQuestions(questionRequest);
            auditService.sendAuditEvent(AuditEventType.REQUEST_SENT, personIdentity);
            return questionsResponse;
        }
        QuestionState questionState =
                objectMapper.readValue(kbvItem.getQuestionState(), QuestionState.class);
        QuestionAnswerRequest questionAnswerRequest = new QuestionAnswerRequest();
        questionAnswerRequest.setUrn(kbvItem.getUrn());
        questionAnswerRequest.setAuthRefNo(kbvItem.getAuthRefNo());
        questionAnswerRequest.setQuestionAnswers(questionState.getAnswers());
        return this.kbvService.submitAnswers(questionAnswerRequest);
    }
}
