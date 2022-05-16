package uk.gov.di.ipv.cri.kbv.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.lambda.powertools.logging.CorrelationIdPathConstants;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswer;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.factory.DynamoDbFactory;
import uk.gov.di.ipv.cri.kbv.api.factory.RequestPayLoadFactory;
import uk.gov.di.ipv.cri.kbv.api.helper.ApiGatewayResponse;
import uk.gov.di.ipv.cri.kbv.api.persistence.DataStore;
import uk.gov.di.ipv.cri.kbv.api.persistence.item.KBVSessionItem;
import uk.gov.di.ipv.cri.kbv.api.service.ExperianService;
import uk.gov.di.ipv.cri.kbv.api.service.QuestionStore;
import uk.gov.di.ipv.cri.kbv.api.service.StorageService;

import java.io.IOException;

public class QuestionAnswerHandler extends ApiGatewayResponse {
    public static final String HEADER_SESSION_ID = "session-id";
    private final StorageService storageService;
    private final ExperianService experianService;
    private QuestionStore questionStore;
    private static final Logger LOGGER = LoggerFactory.getLogger(QuestionAnswerHandler.class);
    private static final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new JavaTimeModule());

    public QuestionAnswerHandler() {
        DataStore<KBVSessionItem> dataStore = DynamoDbFactory.createDataStore();
        this.storageService = new StorageService(dataStore);
        this.experianService = new ExperianService();
    }

    public QuestionAnswerHandler(StorageService storageService, ExperianService experianService) {
        this.storageService = storageService;
        this.experianService = experianService;
    }

    @Override
    @Logging(correlationIdPath = CorrelationIdPathConstants.API_GATEWAY_REST)
    @Metrics(captureColdStart = true)
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {
        try {
            processAnswerResponse(input);
        } catch (JsonProcessingException jsonProcessingException) {
            LOGGER.error("Failed to parse object using ObjectMapper");
            error(jsonProcessingException.getMessage());
        } catch (NullPointerException npe) {
            LOGGER.error("Error finding the requested resource");
            badRequest(npe.getMessage());
        } catch (IOException | InterruptedException e) {
            error(e.getMessage());
            LOGGER.error(String.format("Retrieving questions failed: %s", e));
            Thread.currentThread().interrupt();
        } catch (com.amazonaws.AmazonServiceException e) {
            LOGGER.error("AWS Server error occurred.");
            error(e.getMessage());
        }
        return response();
    }

    public void processAnswerResponse(APIGatewayProxyRequestEvent input)
            throws IOException, InterruptedException {

        var kbvSessionItem =
                storageService
                        .getSessionId(input.getHeaders().get(HEADER_SESSION_ID))
                        .orElseThrow(NullPointerException::new);
        var questionState =
                objectMapper.readValue(kbvSessionItem.getQuestionState(), QuestionState.class);
        var answer = objectMapper.readValue(input.getBody(), QuestionAnswer.class);

        questionStore = new QuestionStore(kbvSessionItem, questionState, storageService);
        if (respondWithAnswerFromDbStore(answer, questionState, kbvSessionItem)) return;
        respondWithAnswerFromExperianThenStoreInDb(questionState, kbvSessionItem);
    }

    private boolean respondWithAnswerFromDbStore(
            QuestionAnswer answer, QuestionState questionState, KBVSessionItem kbvSessionItem)
            throws JsonProcessingException {
        if (questionState.hasAtLeastOneUnAnswered()) {
            questionState.setAnswer(answer);
            kbvSessionItem.setQuestionState(objectMapper.writeValueAsString(questionState));
            storageService.update(kbvSessionItem);
            questionState =
                    objectMapper.readValue(kbvSessionItem.getQuestionState(), QuestionState.class);
            ok();
        }
        return questionState.hasAtLeastOneUnAnswered();
    }

    private void respondWithAnswerFromExperianThenStoreInDb(
            QuestionState questionState, KBVSessionItem kbvSessionItem)
            throws IOException, InterruptedException {
        var json =
                objectMapper.writeValueAsString(
                        RequestPayLoadFactory.request(kbvSessionItem, questionState));
        var body =
                experianService.getResponseFromKBVExperianAPI(
                        json, RequestPayLoadFactory.getEndPoint(kbvSessionItem.getAuthRefNo()));

        var questionsResponse = objectMapper.readValue(body, QuestionsResponse.class);

        if (questionsResponse.hasQuestions()) {
            System.out.println("respondWithAnswerFromExperianThenStoreInDb:- hasQuestions");
            questionStore.saveResponsesToState(questionsResponse);

            ok();
        } else if (questionsResponse.hasQuestionRequestEnded()) {
            System.out.println(
                    "respondWithAnswerFromExperianThenStoreInDb:- hasQuestionRequestEnded - saveFinalResponseState");
            questionStore.saveFinalResponseState(questionsResponse);

            ok();
        } else {
            // TODO: alternate flow could end of transaction / or others
            System.out.println("respondWithAnswerFromExperianThenStoreInDb:- badRequest");

            badRequest(body);
        }
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
