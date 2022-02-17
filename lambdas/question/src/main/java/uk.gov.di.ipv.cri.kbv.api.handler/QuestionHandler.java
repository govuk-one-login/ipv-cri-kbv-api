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
    private final ObjectMapper objectMapper;
    private final StorageService storageService;
    private final ExperianService experianService;

    public QuestionHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.storageService =
                new StorageService(
                        new DataStore<>(
                                ConfigurationService.getInstance().getKBVSessionTableName(),
                                KBVSessionItem.class,
                                DataStore.getClient(
                                        ConfigurationService.getInstance().isRunningLocally())));
        this.experianService = new ExperianService(this.objectMapper);
    }

    public QuestionHandler(
            ObjectMapper objectMapper,
            StorageService storageService,
            ExperianService experianService) {
        this.objectMapper = objectMapper;
        this.storageService = storageService;
        this.experianService = experianService;
    }

    @Override
    @Tracing(captureMode = CaptureMode.DISABLED)
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {
        String responseBody = "{}";
        Map<String, String> responseHeaders = Map.of("Content-Type", "application/json");
        String json;
        int statusCode;

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
                QuestionsResponse questionsResponse = experianService.getQuestions(json);
                if (questionState != null
                        && !questionState.setQuestionsResponse(questionsResponse)) {
                    statusCode = HttpStatus.SC_BAD_REQUEST;
                    responseBody = "{ \"error\":\" no further questions \" }";
                } else {
                    statusCode = HttpStatus.SC_OK;
                    String state = objectMapper.writeValueAsString(questionState);
                    storageService.update(
                            sessionId,
                            state,
                            questionState.getControl().getAuthRefNo(),
                            questionState.getControl().getURN());
                    nextQuestion = questionState.getNextQuestion();
                    responseBody = objectMapper.writeValueAsString(nextQuestion.get());
                }
            } else {
                // TODO Handle scenario when no questions are available
                statusCode = HttpStatus.SC_OK;
                nextQuestion = questionState.getNextQuestion();
                if (!nextQuestion.isEmpty()) {
                    responseBody = objectMapper.writeValueAsString(nextQuestion.get());
                } else {
                    responseBody = "{\"message\":\"no further questions\"}";
                }
            }
        } catch (JsonProcessingException jsonProcessingException) {
            LOGGER.error("Failed to parse object using ObjectMapper");
            statusCode = HttpStatus.SC_INTERNAL_SERVER_ERROR;
            responseBody = "{ \"error\":\"" + jsonProcessingException + "\" }";
        } catch (IOException | InterruptedException | NullPointerException e) {
            LOGGER.error("Retrieving questions failed: " + e);
            statusCode = HttpStatus.SC_INTERNAL_SERVER_ERROR;
            responseBody = "{ \"error\":\"" + e.getMessage() + "\" }";
        } catch (com.amazonaws.AmazonServiceException e) {
            LOGGER.error("AWS Server error occurred.");
            statusCode = HttpStatus.SC_INTERNAL_SERVER_ERROR;
            responseBody = "{ \"error\":\"" + e + "\" }";
        }
        return new APIGatewayProxyResponseEvent()
                .withHeaders(responseHeaders)
                .withStatusCode(statusCode)
                .withBody(responseBody);
    }
}
