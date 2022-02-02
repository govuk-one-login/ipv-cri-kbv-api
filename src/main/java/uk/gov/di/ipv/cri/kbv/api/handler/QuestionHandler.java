package uk.gov.di.ipv.cri.kbv.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import software.amazon.lambda.powertools.tracing.CaptureMode;
import software.amazon.lambda.powertools.tracing.Tracing;
import uk.gov.di.ipv.cri.kbv.api.domain.PersonIdentity;
import uk.gov.di.ipv.cri.kbv.api.domain.Question;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.persistence.DataStore;
import uk.gov.di.ipv.cri.kbv.api.persistence.item.KBVSessionItem;
import uk.gov.di.ipv.cri.kbv.api.service.ConfigurationService;
import uk.gov.di.ipv.cri.kbv.api.service.StorageService;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Optional;

public class QuestionHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    public static final String HEADER_SESSION_ID = "session-id";
    public static final String RESPONSE_TYPE_APPLICATION_JSON = "application/json";
    private final ObjectMapper objectMapper;
    private final StorageService storageService;

    public QuestionHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.storageService =
                new StorageService(
                        new DataStore<>(
                                ConfigurationService.getInstance().getKBVSessionTableName(),
                                KBVSessionItem.class,
                                DataStore.getClient(false)));
    }

    public QuestionHandler(ObjectMapper objectMapper, StorageService storageService) {
        this.objectMapper = objectMapper;
        this.storageService = storageService;
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
            KBVSessionItem kbvSessionItem = storageService.getSessionId(sessionId);
            QuestionState questionState =
                    objectMapper.readValue(kbvSessionItem.getQuestionState(), QuestionState.class);
            PersonIdentity personIdentity = questionState.getPersonIdentity();
            json = objectMapper.writeValueAsString(personIdentity);
            Optional<Question> nextQuestion = questionState.getNextQuestion();
            if (nextQuestion.isEmpty()) { // we should fall in this block once only
                // fetch a batch of questions from experian kbv wrapper
                QuestionsResponse questionsResponse = startGetQuestions(json);
                if (!questionState.setQuestionsResponse(questionsResponse)) {
                    statusCode = 400;
                    responseBody = "{ \"error\":\" no further questions \" }";
                } else {
                    statusCode = 200;
                    String state = objectMapper.writeValueAsString(questionState);
                    storageService.save(sessionId, state);
                    nextQuestion = questionState.getNextQuestion();
                    responseBody = objectMapper.writeValueAsString(nextQuestion.get());
                }
            } else {
                // TODO Handle scenario when no questions are available
                statusCode = 200;
            }
        } catch (Exception e) {
            context.getLogger().log("Retrieving questions failed: " + e);
            statusCode = 500;
            responseBody = "{ \"error\":\"" + e.getMessage() + "\" }";
        }

        return new APIGatewayProxyResponseEvent()
                .withHeaders(responseHeaders)
                .withStatusCode(statusCode)
                .withBody(responseBody);
    }

    private QuestionsResponse startGetQuestions(String json)
            throws IOException, InterruptedException {
        URI wrapperResourceURI = get_KBV_SAA_URI();
        System.out.println("wrapper uri:" + wrapperResourceURI);
        HttpRequest httpReq =
                HttpRequest.newBuilder()
                        .uri(wrapperResourceURI)
                        .setHeader("Accept", RESPONSE_TYPE_APPLICATION_JSON)
                        .setHeader("Content-Type", RESPONSE_TYPE_APPLICATION_JSON)
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpResponse<String> res = httpClient.send(httpReq, HttpResponse.BodyHandlers.ofString());
        System.out.println("saa response status code: " + res.statusCode());
        String body = res.body();
        System.out.println("saa response: " + body);
        return objectMapper.readValue(body, QuestionsResponse.class);
    }

    private URI get_KBV_SAA_URI() {
        String baseURL = System.getenv("EXPERIAN_API_WRAPPER_URL");
        String resource = System.getenv("EXPERIAN_API_WRAPPER_SAA_RESOURCE");
        return URI.create(baseURL + resource);
    }
}
