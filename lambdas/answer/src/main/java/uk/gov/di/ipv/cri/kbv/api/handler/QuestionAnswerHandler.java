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
import software.amazon.lambda.powertools.parameters.ParamManager;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswer;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.kbv.api.library.helpers.EventProbe;
import uk.gov.di.ipv.cri.kbv.api.library.persistence.DataStore;
import uk.gov.di.ipv.cri.kbv.api.library.persistence.item.KBVSessionItem;
import uk.gov.di.ipv.cri.kbv.api.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.kbv.api.library.service.StorageService;
import uk.gov.di.ipv.cri.kbv.api.service.KBVService;
import uk.gov.di.ipv.cri.kbv.api.service.KBVServiceFactory;
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
    private final StorageService storageService;
    private final KBVService kbvService;

    private APIGatewayProxyResponseEvent response;
    public static final String HEADER_SESSION_ID = "session-id";
    public static final String ERROR_KEY = "\"error\"";
    private EventProbe eventProbe;

    @ExcludeFromGeneratedCoverageReport
    public QuestionAnswerHandler() {
        this(
                new ObjectMapper(),
                new StorageService(
                        new DataStore<>(
                                ConfigurationService.getInstance().getKBVSessionTableName(),
                                KBVSessionItem.class,
                                DataStore.getClient(
                                        ConfigurationService.getInstance().isRunningLocally()))),
                new KBVSystemProperty(new KeyStoreService(ParamManager.getSecretsProvider())),
                new KBVServiceFactory(),
                new EventProbe());
    }

    public QuestionAnswerHandler(
            ObjectMapper objectMapper,
            StorageService storageService,
            KBVSystemProperty systemProperty,
            KBVServiceFactory kbvServiceFactory,
            EventProbe eventProbe) {
        this.objectMapper = objectMapper;
        this.objectMapper.registerModule(new JavaTimeModule());
        this.storageService = storageService;
        this.kbvService = kbvServiceFactory.create();

        this.response = new APIGatewayProxyResponseEvent();
        this.eventProbe = eventProbe;

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
            response.withStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.withBody(
                    "{ " + ERROR_KEY + ":\"Failed to parse object using ObjectMapper.\" }");
        } catch (NullPointerException npe) {
            eventProbe.log(INFO, npe).counterMetric(POST_ANSWER, 0d);
            response.withStatusCode(HttpStatus.SC_BAD_REQUEST);
            response.withBody("{ " + ERROR_KEY + ":\"Error finding the requested resource.\" }");
        } catch (IOException | InterruptedException e) {
            eventProbe.log(ERROR, e).counterMetric(POST_ANSWER, 0d);
            response.withStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.withBody(
                    "{ "
                            + ERROR_KEY
                            + ":\""
                            + String.format("Retrieving questions failed: %s", e)
                            + "\" }");
            Thread.currentThread().interrupt();
        } catch (AmazonServiceException e) {
            eventProbe.log(ERROR, e).counterMetric(POST_ANSWER, 0d);
            response.withStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.withBody("{ " + ERROR_KEY + ":\"AWS Server error occurred.\" }");
        }
        return response;
    }

    public void processAnswerResponse(APIGatewayProxyRequestEvent input)
            throws IOException, InterruptedException {
        QuestionState questionState;
        String sessionId = input.getHeaders().get(HEADER_SESSION_ID);
        KBVSessionItem kbvSessionItem =
                storageService.getSessionId(sessionId).orElseThrow(NullPointerException::new);
        questionState =
                objectMapper.readValue(kbvSessionItem.getQuestionState(), QuestionState.class);
        QuestionAnswer answer = objectMapper.readValue(input.getBody(), QuestionAnswer.class);

        if (respondWithAnswerFromDbStore(answer, questionState, kbvSessionItem)) return;
        respondWithAnswerFromExperianThenStoreInDb(questionState, kbvSessionItem);
    }

    private void respondWithAnswerFromExperianThenStoreInDb(
            QuestionState questionState, KBVSessionItem kbvSessionItem)
            throws IOException, InterruptedException {
        QuestionAnswerRequest questionAnswerRequest = new QuestionAnswerRequest();
        questionAnswerRequest.setUrn(kbvSessionItem.getUrn());
        questionAnswerRequest.setAuthRefNo(kbvSessionItem.getAuthRefNo());
        questionAnswerRequest.setQuestionAnswers(questionState.getAnswers());

        QuestionsResponse questionsResponse = kbvService.submitAnswers(questionAnswerRequest);

        if (questionsResponse.hasQuestions()) {
            questionState.setQAPairs(questionsResponse.getQuestions());
            kbvSessionItem.setQuestionState(objectMapper.writeValueAsString(questionState));
            storageService.update(kbvSessionItem);
        } else if (questionsResponse.hasQuestionRequestEnded()) {
            String state = objectMapper.writeValueAsString(questionState);
            kbvSessionItem.setQuestionState(state);
            kbvSessionItem.setAuthorizationCode(UUID.randomUUID().toString());
            kbvSessionItem.setStatus(questionsResponse.getStatus());
            storageService.update(kbvSessionItem);
        } else {
            // TODO: alternate flow could end of transaction / or others
        }
        response.withStatusCode(HttpStatus.SC_OK);
    }

    private boolean respondWithAnswerFromDbStore(
            QuestionAnswer answer, QuestionState questionState, KBVSessionItem kbvSessionItem)
            throws JsonProcessingException {

        questionState.setAnswer(answer);
        kbvSessionItem.setQuestionState(objectMapper.writeValueAsString(questionState));
        storageService.update(kbvSessionItem);
        response.withStatusCode(HttpStatus.SC_OK);

        return questionState.hasAtLeastOneUnAnswered();
    }
}
