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
import uk.gov.di.ipv.cri.kbv.api.domain.PersonIdentity;
import uk.gov.di.ipv.cri.kbv.api.domain.Question;
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
import java.util.Optional;

public class QuestionHandler extends ApiGatewayResponse {
    public static final String HEADER_SESSION_ID = "session-id";
    private final StorageService storageService;
    private final ExperianService experianService;
    private QuestionStore questionStore;
    private static final Logger LOGGER = LoggerFactory.getLogger(QuestionHandler.class);
    private static final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new JavaTimeModule());

    public QuestionHandler() {
        DataStore<KBVSessionItem> dataStore = DynamoDbFactory.createDataStore();
        this.storageService = new StorageService(dataStore);
        this.experianService = new ExperianService();
    }

    public QuestionHandler(StorageService storageService, ExperianService experianService) {
        this.storageService = storageService;
        this.experianService = experianService;
    }

    @Override
    @Logging(correlationIdPath = CorrelationIdPathConstants.API_GATEWAY_REST)
    @Metrics(captureColdStart = true)
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {
        try {
            processQuestionRequest(input);
        } catch (JsonProcessingException jsonProcessingException) {
            LOGGER.error("Failed to parse object using ObjectMapper");
            error(jsonProcessingException.getMessage());
        } catch (NullPointerException npe) {
            LOGGER.error("Error finding the requested resource");
            badRequest(npe.getMessage());
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Retrieving questions failed: " + e);
            error(e.getMessage());
        } catch (com.amazonaws.AmazonServiceException e) {
            LOGGER.error("AWS Server error occurred.");
            error(e.getMessage());
        }
        return response();
    }

    public void processQuestionRequest(APIGatewayProxyRequestEvent input)
            throws IOException, InterruptedException {

        var kbvSessionItem =
                storageService
                        .getSessionId(input.getHeaders().get(HEADER_SESSION_ID))
                        .orElseThrow(NullPointerException::new);
        var personIdentity =
                objectMapper.readValue(kbvSessionItem.getUserAttributes(), PersonIdentity.class);
        var questionState =
                objectMapper.readValue(kbvSessionItem.getQuestionState(), QuestionState.class);
        var json =
                objectMapper.writeValueAsString(
                        RequestPayLoadFactory.request(
                                kbvSessionItem, personIdentity, questionState));

        questionStore = new QuestionStore(kbvSessionItem, questionState, storageService);
        if (respondWithQuestionFromDbStore(questionState, kbvSessionItem)) return;
        respondWithQuestionFromExperianThenStoreInDb(json, kbvSessionItem, questionState);
    }

    private boolean respondWithQuestionFromDbStore(
            QuestionState questionState, KBVSessionItem kbvSessionItem)
            throws JsonProcessingException {
        // TODO Handle scenario when no questions are available
        if (kbvSessionItem.getAuthorizationCode() != null) {
            noContent();
            return true;
        }

        Optional<Question> nextQuestion = questionState.getNextQuestion();
        if (nextQuestion.isPresent()) {
            ok(objectMapper.writeValueAsString(nextQuestion.get()));

            return nextQuestion.isPresent();
        }
        return nextQuestion.isPresent();
    }

    private void respondWithQuestionFromExperianThenStoreInDb(
            String json, KBVSessionItem kbvSessionItem, QuestionState questionState)
            throws IOException, InterruptedException {
        // we should fall in this block once only
        // fetch a batch of questions from experian kbv wrapper
        // LOGGER.info("This the respondWithQuestionFromExperianThenStoreInDb Branch");
        var body =
                experianService.getResponseFromKBVExperianAPI(
                        json, RequestPayLoadFactory.getEndPoint(kbvSessionItem.getAuthRefNo()));
        LOGGER.info(json);
        LOGGER.info(body);
        var questionsResponse = objectMapper.readValue(body, QuestionsResponse.class);
        if (questionsResponse.hasQuestions()) {
            if (kbvSessionItem.getAuthRefNo() == null) {
                questionStore.setControlInfo(questionsResponse);
                questionStore.saveResponsesToState(questionsResponse);
                ok(objectMapper.writeValueAsString(questionState.getNextQuestion().get()));
                return;
            }
            ok();
            return;
        } else if (questionsResponse.hasQuestionRequestEnded()) {
            System.out.println(
                    "respondWithQuestionFromExperianThenStoreInDb: hasQuestionRequestEnded - saveFinalResponseState");

            noContent();
        } else { // TODO: Alternate flow when first request does not return questions
            System.out.println("respondWithQuestionFromExperianThenStoreInDb - badRequest");
            badRequest(body);
        }
    }
}
