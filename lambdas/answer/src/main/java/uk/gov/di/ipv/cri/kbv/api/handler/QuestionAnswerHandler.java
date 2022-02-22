package uk.gov.di.ipv.cri.kbv.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
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
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.persistence.DataStore;
import uk.gov.di.ipv.cri.kbv.api.persistence.item.KBVSessionItem;
import uk.gov.di.ipv.cri.kbv.api.service.ConfigurationService;
import uk.gov.di.ipv.cri.kbv.api.service.ExperianService;
import uk.gov.di.ipv.cri.kbv.api.service.StorageService;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class QuestionAnswerHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuestionAnswerHandler.class);

    private APIGatewayProxyResponseEvent response;
    private static ObjectMapper objectMapper = new ObjectMapper();
    private final StorageService storageService;
    private final ExperianService experianService;
    public static final String HEADER_SESSION_ID = "session-id";

    public QuestionAnswerHandler() {
        this(
                new ObjectMapper(),
                new StorageService(
                        new DataStore<>(
                                ConfigurationService.getInstance().getKBVSessionTableName(),
                                KBVSessionItem.class,
                                DataStore.getClient(
                                        ConfigurationService.getInstance().isRunningLocally()))),
                new ExperianService(objectMapper),
                new APIGatewayProxyResponseEvent());
    }

    public QuestionAnswerHandler(
            ObjectMapper objectMapper,
            StorageService storageService,
            ExperianService experianService,
            APIGatewayProxyResponseEvent apiGatewayProxyResponseEvent) {
        this.objectMapper = objectMapper;
        this.objectMapper.registerModule(new JavaTimeModule());
        this.storageService = storageService;
        this.experianService = experianService;
        this.response = apiGatewayProxyResponseEvent;
    }

    @Override
    @Tracing(captureMode = CaptureMode.DISABLED)
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {

        String responseBody = "{}";
        Map<String, String> responseHeaders = Map.of("Content-Type", "application/json");
        String sessionId = input.getHeaders().get(HEADER_SESSION_ID);
        LOGGER.info("QuestionAnswerHandler handleRequest sessionId: " + sessionId);
        KBVSessionItem kbvSessionItem = storageService.getSessionId(sessionId).orElseThrow();
        QuestionState questionState;
        QuestionAnswer answer;

        try {
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

            questionState.getQaPairs().stream()
                    .map(
                            qaPair -> {
                                if (qaPair.getQuestion().getQuestionID().equals(questionID)) {
                                    qaPair = questionAnswerPair;
                                }
                                return qaPair;
                            })
                    .collect(Collectors.toList());

            storageService.update(
                    sessionId,
                    objectMapper.writeValueAsString(questionState),
                    questionState.getControl().getAuthRefNo(),
                    questionState.getControl().getURN());
            if (questionState.canSubmitAnswers(questionState.getQaPairs())) {
                QuestionsResponse questionsResponse = submitAnswersToExperianAPI(questionState);
                boolean moreQuestions = questionState.setQuestionsResponse(questionsResponse);
                if (moreQuestions) {
                    String state = objectMapper.writeValueAsString(questionState);
                    storageService.update(
                            sessionId,
                            state,
                            questionsResponse.getControl().getAuthRefNo(),
                            questionsResponse.getControl().getURN());
                    Optional<Question> nextQuestion = questionState.getNextQuestion();
                    responseBody = objectMapper.writeValueAsString(nextQuestion.get());
                    response.withStatusCode(HttpStatus.SC_CREATED);
                    response.withBody(responseBody);
                } else {
                    storageService.updateAuthorisationCode(sessionId, responseBody);
                    responseBody = objectMapper.writeValueAsString(questionsResponse);
                    response.withStatusCode(HttpStatus.SC_OK);
                    response.withBody(responseBody);
                }
            } else {
                Optional<Question> nextQuestion = questionState.getNextQuestion();
                responseBody = objectMapper.writeValueAsString(nextQuestion.get());
                response.withStatusCode(HttpStatus.SC_OK);
                response.withBody(responseBody);
            }

        } catch (Exception e) {
            responseBody = "{ \"error\":\"" + e.getMessage() + "\" }";
            response.withStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.withBody(responseBody);
        }

        response.withHeaders(responseHeaders);
        return response;
    }

    private QuestionsResponse submitAnswersToExperianAPI(QuestionState questionState)
            throws IOException, InterruptedException {
        QuestionAnswerRequest questionAnswerRequest = new QuestionAnswerRequest();
        List<QuestionAnswerPair> pairs = questionState.getQaPairs();

        List<QuestionAnswer> collect =
                pairs.stream()
                        .map(
                                pair -> {
                                    QuestionAnswer questionAnswer = new QuestionAnswer();
                                    questionAnswer.setAnswer(pair.getAnswer());
                                    questionAnswer.setQuestionId(
                                            pair.getQuestion().getQuestionID());
                                    return questionAnswer;
                                })
                        .collect(Collectors.toList());

        questionAnswerRequest.setQuestionAnswers(collect);
        questionAnswerRequest.setAuthRefNo(questionState.getControl().getAuthRefNo());
        questionAnswerRequest.setUrn(questionState.getControl().getURN());
        String json = objectMapper.writeValueAsString(questionAnswerRequest);
        QuestionsResponse questionsResponse =
                experianService.getResponseFromExperianAPI(
                        json, "EXPERIAN_API_WRAPPER_RTQ_RESOURCE");

        return questionsResponse;
    }
}
