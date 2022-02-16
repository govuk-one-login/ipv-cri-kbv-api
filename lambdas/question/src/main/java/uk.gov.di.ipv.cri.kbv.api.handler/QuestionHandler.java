package uk.gov.di.ipv.cri.kbv.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.lambda.powertools.tracing.CaptureMode;
import software.amazon.lambda.powertools.tracing.Tracing;
import uk.gov.di.ipv.cri.kbv.api.domain.*;
import uk.gov.di.ipv.cri.kbv.api.persistence.DataStore;
import uk.gov.di.ipv.cri.kbv.api.persistence.item.KBVSessionItem;
import uk.gov.di.ipv.cri.kbv.api.service.ConfigurationService;
import uk.gov.di.ipv.cri.kbv.api.service.ExperianService;
import uk.gov.di.ipv.cri.kbv.api.service.StorageService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

public class QuestionHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuestionHandler.class);

    public static final String HEADER_SESSION_ID = "session-id";
    public static final String RESPONSE_TYPE_APPLICATION_JSON = "application/json";
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
        LOGGER.info("QuestionHandler.handleRequest() at: " + LocalDateTime.now());
        String responseBody = "{}";
        Map<String, String> responseHeaders = Map.of("Content-Type", "application/json");
        String json;
        int statusCode;
        try {
            String sessionId = input.getHeaders().get(HEADER_SESSION_ID);
            LOGGER.info("sessionId: " + sessionId);
            KBVSessionItem kbvSessionItem = storageService.getSessionId(sessionId);
            PersonIdentity personIdentity =
                    objectMapper.readValue(
                            kbvSessionItem.getUserAttributes(), PersonIdentity.class);

            QuestionState questionState =
                    objectMapper.readValue(
                            kbvSessionItem.getQuestionState(), QuestionState.class);
            json = objectMapper.writeValueAsString(personIdentity);

            questionState
                    .getQaPairs()
                    .forEach(
                            p -> System.out.println("Question Answer pairs => " +
                                            p.getQuestion().getQuestionID()
                                                    + " <-QA-> "
                                                    + p.getAnswer()));

            Optional<Question> nextQuestion = questionState.getNextQuestion();
            if (nextQuestion.isEmpty()) { // we should fall in this block once only
                // fetch a batch of questions from experian kbv wrapper
                QuestionsResponse questionsResponse = experianService.getQuestions(json);
                if (questionState != null
                        && !questionState.setQuestionsResponse(questionsResponse)) {
                    statusCode = 400;
                    responseBody = "{ \"error\":\" no further questions \" }";
                } else {
                    statusCode = 200;
                    String state = objectMapper.writeValueAsString(questionState);
                    LOGGER.info("questionState:" + state);
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
                statusCode = 201;
            }
        } catch (JsonProcessingException jsonProcessingException) {
            context.getLogger()
                    .log(
                            "JSON processing exception- failed to get questions: "
                                    + jsonProcessingException);
            statusCode = 500;
            responseBody = "{ \"error\":\"" + jsonProcessingException.getMessage() + "\" }";
        } catch (IOException | InterruptedException e) {
            context.getLogger().log("Retrieving questions failed: " + e);
            statusCode = 500;
            responseBody = "{ \"error\":\"" + e.getMessage() + "\" }";
        }

        return new APIGatewayProxyResponseEvent()
                .withHeaders(responseHeaders)
                .withStatusCode(statusCode)
                .withBody(responseBody);
    }
}
