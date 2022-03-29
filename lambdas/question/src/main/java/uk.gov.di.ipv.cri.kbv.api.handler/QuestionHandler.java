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
import uk.gov.di.ipv.cri.kbv.api.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.kbv.api.library.domain.PersonIdentity;
import uk.gov.di.ipv.cri.kbv.api.library.domain.Question;
import uk.gov.di.ipv.cri.kbv.api.library.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.library.domain.QuestionsRequest;
import uk.gov.di.ipv.cri.kbv.api.library.domain.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.library.helpers.EventProbe;
import uk.gov.di.ipv.cri.kbv.api.library.persistence.DataStore;
import uk.gov.di.ipv.cri.kbv.api.library.persistence.item.KBVSessionItem;
import uk.gov.di.ipv.cri.kbv.api.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.kbv.api.library.service.ExperianService;
import uk.gov.di.ipv.cri.kbv.api.library.service.StorageService;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static org.apache.logging.log4j.Level.INFO;

public class QuestionHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    public static final String HEADER_SESSION_ID = "session-id";
    public static final String GET_QUESTION = "get_question";
    public static final String ERROR = "\"error\"";
    private static ObjectMapper objectMapper;
    private final StorageService storageService;
    private final ExperianService experianService;
    private APIGatewayProxyResponseEvent response;
    private EventProbe eventProbe;

    @ExcludeFromGeneratedCoverageReport
    public QuestionHandler() {
        this(
                new ObjectMapper(),
                new StorageService(
                        new DataStore<>(
                                ConfigurationService.getInstance().getKBVSessionTableName(),
                                KBVSessionItem.class,
                                DataStore.getClient(
                                        ConfigurationService.getInstance().isRunningLocally()))),
                new ExperianService(),
                new EventProbe());
    }

    public QuestionHandler(
            ObjectMapper objectMapper,
            StorageService storageService,
            ExperianService experianService,
            EventProbe eventProbe) {
        this.objectMapper = objectMapper;
        this.objectMapper.registerModule(new JavaTimeModule());
        this.storageService = storageService;
        this.experianService = experianService;
        this.response = new APIGatewayProxyResponseEvent();
        this.eventProbe = eventProbe;
    }

    @Override
    @Logging(correlationIdPath = CorrelationIdPathConstants.API_GATEWAY_REST)
    @Metrics(captureColdStart = true)
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {

        try {
            processQuestionRequest(input);
        } catch (JsonProcessingException jsonProcessingException) {
            eventProbe.log(INFO, jsonProcessingException).counterMetric(GET_QUESTION, 0d);
            response.withStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.withBody("{ " + ERROR + ":\"Failed to parse object using ObjectMapper.\" }");
        } catch (NullPointerException npe) {
            eventProbe.log(INFO, npe).counterMetric(GET_QUESTION, 0d);
            response.withStatusCode(HttpStatus.SC_BAD_REQUEST);
            response.withBody("{ " + ERROR + ":\"" + npe + "\" }");
        } catch (IOException | InterruptedException e) {
            eventProbe.log(INFO, e).counterMetric(GET_QUESTION, 0d);
            response.withStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.withBody("{ " + ERROR + ":\"Retrieving questions failed.\" }");
        } catch (AmazonServiceException e) {
            eventProbe.log(INFO, e).counterMetric(GET_QUESTION, 0d);
            response.withStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.withBody("{ " + ERROR + ":\"AWS Server error occurred.\" }");
        }
        return response;
    }

    public void processQuestionRequest(APIGatewayProxyRequestEvent input)
            throws IOException, InterruptedException {
        response.withHeaders(Map.of("Content-Type", "application/json"));
        String sessionId = input.getHeaders().get(HEADER_SESSION_ID);
        KBVSessionItem kbvSessionItem =
                storageService.getSessionId(sessionId).orElseThrow(NullPointerException::new);
        PersonIdentity personIdentity =
                objectMapper.readValue(kbvSessionItem.getUserAttributes(), PersonIdentity.class);
        QuestionState questionState =
                objectMapper.readValue(kbvSessionItem.getQuestionState(), QuestionState.class);

        QuestionsRequest questionsRequest = new QuestionsRequest();
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
            return true;
        }
        return false;
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
        String questionsResponsePayload =
                experianService.getResponseFromKBVExperianAPI(
                        json, "EXPERIAN_API_WRAPPER_SAA_RESOURCE");
        QuestionsResponse questionsResponse =
                objectMapper.readValue(questionsResponsePayload, QuestionsResponse.class);
        if (questionsResponse.hasQuestions()) {
            questionState.setQAPairs(questionsResponse.getQuestions());
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
            response.withBody(questionsResponsePayload);
        }
    }
}
