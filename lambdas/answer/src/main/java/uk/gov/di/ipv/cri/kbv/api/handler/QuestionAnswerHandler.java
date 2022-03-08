package uk.gov.di.ipv.cri.kbv.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.lambda.powertools.tracing.CaptureMode;
import software.amazon.lambda.powertools.tracing.Tracing;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswer;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.persistence.DataStore;
import uk.gov.di.ipv.cri.kbv.api.persistence.item.KBVSessionItem;
import uk.gov.di.ipv.cri.kbv.api.service.ConfigurationService;
import uk.gov.di.ipv.cri.kbv.api.service.ExperianService;
import uk.gov.di.ipv.cri.kbv.api.service.StorageService;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class QuestionAnswerHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuestionAnswerHandler.class);
    private final ObjectMapper objectMapper;
    private final StorageService storageService;
    private final ExperianService experianService;

    private APIGatewayProxyResponseEvent response;
    public static final String HEADER_SESSION_ID = "session-id";
    public static final String ERROR = "\"error\"";

    public QuestionAnswerHandler() {
        this(
                new ObjectMapper(),
                new StorageService(
                        new DataStore<>(
                                ConfigurationService.getInstance().getKBVSessionTableName(),
                                KBVSessionItem.class,
                                DataStore.getClient(
                                        ConfigurationService.getInstance().isRunningLocally()))),
                new ExperianService());
    }

    public QuestionAnswerHandler(
            ObjectMapper objectMapper,
            StorageService storageService,
            ExperianService experianService) {
        this.objectMapper = objectMapper;
        this.objectMapper.registerModule(new JavaTimeModule());
        this.storageService = storageService;
        this.experianService = experianService;
        this.response = new APIGatewayProxyResponseEvent();
    }

    @Override
    @Tracing(captureMode = CaptureMode.DISABLED)
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {
        response.withHeaders(Map.of("Content-Type", "application/json"));
        try {
            processAnswerResponse(input);
        } catch (JsonProcessingException jsonProcessingException) {
            LOGGER.error("Failed to parse object using ObjectMapper");
            response.withStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.withBody("{ " + ERROR + ":\"" + jsonProcessingException.getMessage() + "\" }");
        } catch (NullPointerException npe) {
            LOGGER.error("Error finding the requested resource");
            response.withStatusCode(HttpStatus.SC_BAD_REQUEST);
            response.withBody(npe.getMessage());
        } catch (IOException | InterruptedException e) {
            response.withStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.withBody("{ " + ERROR + ":\"" + e.getMessage() + "\" }");
            LOGGER.error(String.format("Retrieving questions failed: %s", e));
            Thread.currentThread().interrupt();
        } catch (com.amazonaws.AmazonServiceException e) {
            LOGGER.error("AWS Server error occurred.");
            response.withStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.withBody("{ " + ERROR + ":\"" + e.getMessage() + "\" }");
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
        String body =
                experianService.getResponseFromKBVExperianAPI(
                        objectMapper.writeValueAsString(questionAnswerRequest),
                        "EXPERIAN_API_WRAPPER_RTQ_RESOURCE");
        QuestionsResponse questionsResponse = objectMapper.readValue(body, QuestionsResponse.class);

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
