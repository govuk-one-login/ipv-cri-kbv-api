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
import software.amazon.lambda.powertools.logging.CorrelationIdPathConstants;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;
import uk.gov.di.ipv.cri.kbv.api.domain.PersonIdentity;
import uk.gov.di.ipv.cri.kbv.api.domain.Question;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.persistence.DataStore;
import uk.gov.di.ipv.cri.kbv.api.persistence.item.KBVSessionItem;
import uk.gov.di.ipv.cri.kbv.api.service.ConfigurationService;
import uk.gov.di.ipv.cri.kbv.api.service.ExperianService;
import uk.gov.di.ipv.cri.kbv.api.service.StorageService;

import java.io.IOException;
import java.util.Optional;

public class QuestionHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuestionHandler.class);
    public static final String HEADER_SESSION_ID = "session-id";
    public static final String ERROR = "\"error\"";
    private static ObjectMapper objectMapper;
    private final StorageService storageService;
    private final ExperianService experianService;
    private APIGatewayProxyResponseEvent response;

    public QuestionHandler() {
        this(
                new StorageService(
                        new DataStore<>(
                                ConfigurationService.getInstance().getKBVSessionTableName(),
                                KBVSessionItem.class,
                                DataStore.getClient(
                                        ConfigurationService.getInstance().isRunningLocally()))),
                new ExperianService());
    }

    public QuestionHandler(StorageService storageService, ExperianService experianService) {
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.storageService = storageService;
        this.experianService = experianService;
        this.response = new APIGatewayProxyResponseEvent();
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

    public void processQuestionRequest(APIGatewayProxyRequestEvent input)
            throws IOException, InterruptedException {
        String sessionId = input.getHeaders().get(HEADER_SESSION_ID);
        KBVSessionItem kbvSessionItem =
                storageService.getSessionId(sessionId).orElseThrow(NullPointerException::new);
        PersonIdentity personIdentity =
                objectMapper.readValue(kbvSessionItem.getUserAttributes(), PersonIdentity.class);
        QuestionState questionState =
                objectMapper.readValue(kbvSessionItem.getQuestionState(), QuestionState.class);

        QuestionsRequest questionsRequest = new QuestionsRequest();
        questionsRequest.setUrn(kbvSessionItem.getUrn());
        questionsRequest.setPersonIdentity(personIdentity);
        String json = objectMapper.writeValueAsString(questionsRequest);

        if (respondWithQuestionFromDbStore(questionState)) return;
        respondWithQuestionFromExperianThenStoreInDb(json, kbvSessionItem, questionState);
    }

    private boolean respondWithQuestionFromDbStore(QuestionState questionState)
            throws JsonProcessingException {
        // TODO Handle scenario when no questions are available
        Optional<Question> nextQuestion = questionState.getNextQuestion();
        if (nextQuestion.isPresent()) {
            response.withBody(objectMapper.writeValueAsString(nextQuestion.get()));
            response.withStatusCode(HttpStatus.SC_OK);
            return nextQuestion.isPresent();
        }
        return nextQuestion.isPresent();
    }

    private void respondWithQuestionFromExperianThenStoreInDb(
            String json, KBVSessionItem kbvSessionItem, QuestionState questionState)
            throws IOException, InterruptedException {
        // we should fall in this block once only
        // fetch a batch of questions from experian kbv wrapper
        if (kbvSessionItem.getAuthorizationCode() != null) {
            response.withStatusCode(HttpStatus.SC_NO_CONTENT);
            return;
        }
        String body =
                experianService.getResponseFromKBVExperianAPI(
                        json, "EXPERIAN_API_WRAPPER_SAA_RESOURCE");
        QuestionsResponse questionsResponse = objectMapper.readValue(body, QuestionsResponse.class);
        if (questionsResponse.hasQuestions()) {
            questionState.setQAPairs(questionsResponse.getQuestions());
            questionState.setState(questionsResponse.getQuestionStatus());
            Optional<Question> nextQuestion = questionState.getNextQuestion();
            response.withStatusCode(HttpStatus.SC_OK);
            response.withBody(objectMapper.writeValueAsString(nextQuestion.get()));

            String state = objectMapper.writeValueAsString(questionState);
            kbvSessionItem.setQuestionState(state);
            kbvSessionItem.setAuthRefNo(questionsResponse.getControl().getAuthRefNo());
            kbvSessionItem.setUrn(questionsResponse.getControl().getURN());
            storageService.update(kbvSessionItem);
        } else { // TODO: Alternate flow when first request does not return questions
            response.withStatusCode(HttpStatus.SC_BAD_REQUEST);
            response.withBody(body);
        }
    }
}
