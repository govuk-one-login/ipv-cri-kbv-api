package uk.gov.di.cri.experian.kbv.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.experian.uk.schema.experian.identityiq.services.webservice.Question;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.lambda.powertools.tracing.CaptureMode;
import software.amazon.lambda.powertools.tracing.Tracing;
import uk.gov.di.cri.experian.kbv.api.domain.PersonIdentity;
import uk.gov.di.cri.experian.kbv.api.domain.QuestionState;
import uk.gov.di.cri.experian.kbv.api.domain.QuestionsResponse;
import uk.gov.di.cri.experian.kbv.api.service.StorageService;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Optional;

public class QuestionHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private ObjectMapper objectMapper;
    private StorageService storageService;
    public static final String HEADER_SESSION_ID = "session-id";
    public static final String RESPONSE_TYPE_APPLICATION_JSON = "application/json";
    public static final String EMPTY_JSON = "{}";

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
        String json = null;
        int statusCode;

        try {
            String sessionId = input.getHeaders().get(HEADER_SESSION_ID);
            QuestionState questionState = storageService.get(sessionId);
            PersonIdentity personIdentity = questionState.getPersonIdentity();
            json = objectMapper.writeValueAsString(personIdentity);
            Optional<Question> nextQuestion = questionState.getNextQuestion();
            if (!nextQuestion.isPresent()) { // we should fall in this block once only
                // fetch a batch of questions from experian kbv wrapper
                QuestionsResponse questionsResponse = startGetQuestions(json);
                if (!questionState.setQuestionsResponse(questionsResponse)) {
                    statusCode = 400;
                    responseBody = "{ \"error\":\" no further questions \" }";
                } else {
                    statusCode = 200;
                    storageService.save(sessionId, questionState);
                    nextQuestion = questionState.getNextQuestion();
                    responseBody = objectMapper.writeValueAsString(nextQuestion.get());
                }
            } else {
                // TODO Handle scenario when no questions are available
                statusCode = 200;
            }
        } catch (Exception e) {
            context.getLogger().log("Retrieving questions failed: " + e.toString());
            statusCode = 500;
            responseBody = "{ \"error\":\"" + e.getMessage() + "\" }";
        }
        return createResponseEvent(statusCode, responseBody, responseHeaders);
    }

    private static APIGatewayProxyResponseEvent createResponseEvent(
            int statusCode, String body, Map<String, String> headers) {
        APIGatewayProxyResponseEvent apiGatewayProxyResponseEvent =
                new APIGatewayProxyResponseEvent();
        apiGatewayProxyResponseEvent.setHeaders(headers);
        apiGatewayProxyResponseEvent.setStatusCode(statusCode);
        apiGatewayProxyResponseEvent.setBody(body);

        return apiGatewayProxyResponseEvent;
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
