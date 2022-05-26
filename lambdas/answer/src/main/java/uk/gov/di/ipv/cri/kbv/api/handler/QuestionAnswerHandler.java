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
import software.amazon.lambda.powertools.parameters.ParamManager;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;
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

    private static final String POST_ANSWER = "post_answer";
    private final ObjectMapper objectMapper;
    private final KBVService kbvService;
    private final KBVStorageService kbvStorageService;
    private final SessionService sessionService;
    private APIGatewayProxyResponseEvent response;
    public static final String HEADER_SESSION_ID = "session-id";
    public static final String ERROR_KEY = "\"error\"";
    private EventProbe eventProbe;

    @ExcludeFromGeneratedCoverageReport
    public QuestionAnswerHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.kbvStorageService = new KBVStorageService();
        this.kbvService = new KBVServiceFactory().create();

        this.response = new APIGatewayProxyResponseEvent();
        this.eventProbe = new EventProbe();
        this.sessionService = new SessionService();

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
            SessionService sessionService) {
        this.objectMapper = objectMapper;
        this.objectMapper.registerModule(new JavaTimeModule());
        this.kbvStorageService = kbvStorageService;

        this.response = new APIGatewayProxyResponseEvent();
        this.eventProbe = eventProbe;
        this.sessionService = sessionService;
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
        } catch (IOException | InterruptedException e) {
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
        //        catch (Exception e) {
        //            eventProbe.log(ERROR, e).counterMetric(POST_ANSWER, 0d);
        //            response.withStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR);
        //            response.withBody("{ " + ERROR_KEY + ":\"AWS Server error occurred.\" }");
        //        }
        return response;
    }

    public void processAnswerResponse(APIGatewayProxyRequestEvent input)
            throws IOException, InterruptedException {
        QuestionState questionState;
        String sessionId = input.getHeaders().get(HEADER_SESSION_ID);

        KBVItem kbvItem = kbvStorageService.getKBVItem(sessionId);

        questionState = objectMapper.readValue(kbvItem.getQuestionState(), QuestionState.class);
        QuestionAnswer answer = objectMapper.readValue(input.getBody(), QuestionAnswer.class);

        if (respondWithAnswerFromDbStore(answer, questionState, kbvItem)) return;
        respondWithAnswerFromExperianThenStoreInDb(questionState, kbvItem);
    }

    private void respondWithAnswerFromExperianThenStoreInDb(
            QuestionState questionState, KBVItem kbvItem) throws IOException {
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

            SessionItem sessionItem = sessionService.getSession(kbvItem.getSessionId());
            sessionItem.setAuthorizationCode(UUID.randomUUID().toString());
            sessionService.createAuthorizationCode(sessionItem);
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
