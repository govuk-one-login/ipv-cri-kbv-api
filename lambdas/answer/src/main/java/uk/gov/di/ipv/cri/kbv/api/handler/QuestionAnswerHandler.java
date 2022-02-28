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
import uk.gov.di.ipv.cri.kbv.api.domain.Question;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswer;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerPair;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerRequestMapper;
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
import java.util.UUID;

public class QuestionAnswerHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuestionAnswerHandler.class);

    private APIGatewayProxyResponseEvent response;
    private final ObjectMapper objectMapper;
    private final StorageService storageService;
    private final ExperianService experianService;
    public static final String HEADER_SESSION_ID = "session-id";
    public static final String ERROR = "\"error\"";
    public static final String EXPERIAN_API_WRAPPER_RTQ_RESOURCE =
            "EXPERIAN_API_WRAPPER_RTQ_RESOURCE";

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
        QuestionState questionState;
        QuestionAnswer answer;
        String responseBody = "{}";

        try {
            String sessionId = input.getHeaders().get(HEADER_SESSION_ID);
            KBVSessionItem kbvSessionItem =
                    storageService.getSessionId(sessionId).orElseThrow(NullPointerException::new);
            questionState =
                    objectMapper.readValue(kbvSessionItem.getQuestionState(), QuestionState.class);
            answer = objectMapper.readValue(input.getBody(), QuestionAnswer.class);
            String questionID = answer.getQuestionId();
            QuestionAnswerPair questionAnswerPair =
                    questionState.getQaPairs().stream()
                            .filter(pair -> pair.getQuestion().getQuestionID().equals(questionID))
                            .findFirst()
                            .orElseThrow(
                                    () ->
                                            new IllegalStateException(
                                                    "Question not found for questionID: "
                                                            + questionID));
            questionAnswerPair.setAnswer(answer.getAnswer());

            kbvSessionItem.setQuestionState(objectMapper.writeValueAsString(questionState));
            kbvSessionItem.setAuthRefNo(questionState.getControl().getAuthRefNo());
            kbvSessionItem.setUrn(questionState.getControl().getURN());
            storageService.update(kbvSessionItem);

            if (questionState.canSubmitAnswers(questionState.getQaPairs())) {
                QuestionAnswerRequestMapper questionAnswerRequestMapper = new QuestionAnswerRequestMapper();
                QuestionAnswerRequest questionAnswer =
                        questionAnswerRequestMapper.mapFrom(questionState);
                String questionAnswerRequest = objectMapper.writeValueAsString(questionAnswer);
                String questionsResponseReceived = experianService.getResponseFromExperianAPI(questionAnswerRequest, EXPERIAN_API_WRAPPER_RTQ_RESOURCE);

                System.out.println("RESPONSE->>>> "+questionsResponseReceived);
                QuestionsResponse questionsResponse = objectMapper.readValue(questionsResponseReceived, QuestionsResponse.class);
                boolean moreQuestions = questionState.setQuestionsResponse(questionsResponse);
                if (moreQuestions) {
                    String state = objectMapper.writeValueAsString(questionState);
                    kbvSessionItem.setQuestionState(state);
                    kbvSessionItem.setAuthRefNo(questionState.getControl().getAuthRefNo());
                    kbvSessionItem.setUrn(questionState.getControl().getURN());
                    storageService.update(kbvSessionItem);
                    Optional<Question> nextQuestion = questionState.getNextQuestion();
                    responseBody = objectMapper.writeValueAsString(nextQuestion.get());
                    response.withStatusCode(HttpStatus.SC_CREATED);
                    response.withBody(responseBody);
                } else {
                    responseBody = objectMapper.writeValueAsString(questionsResponse);
                    kbvSessionItem.setQuestionState(responseBody);
                    kbvSessionItem.setAuthorizationCode(UUID.randomUUID().toString());
                    storageService.update(kbvSessionItem);
                    response.withStatusCode(HttpStatus.SC_OK);
                    response.withBody(responseBody);
                }
            } else {
                Optional<Question> nextQuestion = questionState.getNextQuestion();
                responseBody = objectMapper.writeValueAsString(nextQuestion.get());
                response.withStatusCode(HttpStatus.SC_OK);
                response.withBody(responseBody);
            }
        } catch (JsonProcessingException jsonProcessingException) {
            LOGGER.error("Failed to parse object using ObjectMapper");
            response.withStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.withBody("{ " + ERROR + ":\"" + jsonProcessingException.getMessage() + "\" }");
        } catch (NullPointerException npe) {
            LOGGER.error("Error finding the requested resource");
            response.withStatusCode(HttpStatus.SC_BAD_REQUEST);
            response.withBody(npe.getMessage());
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Retrieving questions failed: " + e);
            response.withStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.withBody("{ " + ERROR + ":\"" + e.getMessage() + "\" }");
        } catch (com.amazonaws.AmazonServiceException e) {
            LOGGER.error("AWS Server error occurred.");
            response.withStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.withBody("{ " + ERROR + ":\"" + e.getMessage() + "\" }");
        }
        response.withHeaders(Map.of("Content-Type", "application/json"));
        return response;
    }
}
