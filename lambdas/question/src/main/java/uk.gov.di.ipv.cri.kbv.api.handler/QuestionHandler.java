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
import uk.gov.di.ipv.cri.kbv.api.domain.PersonIdentity;
import uk.gov.di.ipv.cri.kbv.api.domain.Question;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.persistence.DataStore;
import uk.gov.di.ipv.cri.kbv.api.persistence.item.KBVSessionItem;
import uk.gov.di.ipv.cri.kbv.api.service.ConfigurationService;
import uk.gov.di.ipv.cri.kbv.api.service.ExperianService;
import uk.gov.di.ipv.cri.kbv.api.service.StorageService;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

public class QuestionHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuestionHandler.class);

    public static final String HEADER_SESSION_ID = "session-id";
    public static final String ERROR = "\"error\"";
    private static ObjectMapper objectMapper = new ObjectMapper();
    private final StorageService storageService;
    private final ExperianService experianService;
    private APIGatewayProxyResponseEvent response;

    public QuestionHandler() {
        this(
                new ObjectMapper(),
                new StorageService(
                        new DataStore<>(
                                ConfigurationService.getInstance().getKBVSessionTableName(),
                                KBVSessionItem.class,
                                DataStore.getClient(
                                        ConfigurationService.getInstance().isRunningLocally()))),
                new ExperianService(objectMapper));
    }

    public QuestionHandler(
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
        String json;

        try {
            String sessionId = input.getHeaders().get(HEADER_SESSION_ID);
            KBVSessionItem kbvSessionItem =
                    storageService.getSessionId(sessionId).orElseThrow(NullPointerException::new);
            PersonIdentity personIdentity =
                    objectMapper.readValue(
                            kbvSessionItem.getUserAttributes(), PersonIdentity.class);

            QuestionState questionState =
                    objectMapper.readValue(kbvSessionItem.getQuestionState(), QuestionState.class);
            json = objectMapper.writeValueAsString(personIdentity);
            Optional<Question> nextQuestion = questionState.getNextQuestion();
            if (nextQuestion.isEmpty()) { // we should fall in this block once only
                // fetch a batch of questions from experian kbv wrapper
                QuestionsResponse questionsResponse =
                        experianService.getResponseFromExperianAPI(
                                json, "EXPERIAN_API_WRAPPER_SAA_RESOURCE");
                if (!questionState.setQuestionsResponse(questionsResponse)) {
                    response.withStatusCode(HttpStatus.SC_BAD_REQUEST);
                    response.withBody("{ " + ERROR + ":\" no further questions \" }");
                } else {
                    response.withStatusCode(HttpStatus.SC_OK);
                    String state = objectMapper.writeValueAsString(questionState);
                    kbvSessionItem.setQuestionState(state);
                    kbvSessionItem.setAuthRefNo(questionState.getControl().getAuthRefNo());
                    kbvSessionItem.setUrn(questionState.getControl().getURN());
                    storageService.update(kbvSessionItem);
                    nextQuestion = questionState.getNextQuestion();
                    response.withBody(objectMapper.writeValueAsString(nextQuestion.get()));
                }
            } else {
                // TODO Handle scenario when no questions are available
                response.withStatusCode(HttpStatus.SC_OK);
                nextQuestion = questionState.getNextQuestion();
                if (nextQuestion.isPresent()) {
                    response.withBody(objectMapper.writeValueAsString(nextQuestion.get()));
                } else {
                    response.withBody("{\"message\":\"no further questions\"}");
                }
            }
        } catch (JsonProcessingException jsonProcessingException) {
            LOGGER.error("Failed to parse object using ObjectMapper");
            response.withStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.withBody("{ " + ERROR + ":\"" + jsonProcessingException.getMessage() + "\" }");
        } catch (NullPointerException npe) {
            LOGGER.error("Error finding the requested resource");
            response.withStatusCode(HttpStatus.SC_BAD_REQUEST);
            response.withBody("{ " + ERROR + ":\"" + npe.getMessage() + "\" }");
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Retrieving questions failed: " + e);
            response.withStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.withBody("{ " + ERROR + ":\"" + e.getMessage() + "\" }");
        } catch (com.amazonaws.AmazonServiceException e) {
            LOGGER.error("AWS Server error occurred.");
            response.withStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.withBody("{ " + ERROR + ":\"" + e.getMessage() + "\" }");
        }
        return response;
    }
}
