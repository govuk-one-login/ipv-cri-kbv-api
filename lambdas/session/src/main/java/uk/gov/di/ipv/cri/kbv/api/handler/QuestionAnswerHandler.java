package uk.gov.di.ipv.cri.kbv.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.lambda.powertools.tracing.CaptureMode;
import software.amazon.lambda.powertools.tracing.Tracing;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswer;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.persistence.item.KBVSessionItem;
import uk.gov.di.ipv.cri.kbv.api.service.StorageService;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QuestionAnswerHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final ObjectMapper objectMapper;
    private final StorageService storageService;
    public static final String HEADER_SESSION_ID = "session-id";
    public static final String RESPONSE_TYPE_APPLICATION_JSON = "application/json";
    public static final String EMPTY_JSON = "{}";

    public QuestionAnswerHandler(ObjectMapper objectMapper, StorageService storageService) {
        this.objectMapper = objectMapper;
        this.storageService = storageService;
    }

    @Override
    @Tracing(captureMode = CaptureMode.DISABLED)
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {
        String responseBody = "{}";
        Map<String, String> responseHeaders = Map.of("Content-Type", "application/json");
        String sessionId = input.getHeaders().get(HEADER_SESSION_ID);
        KBVSessionItem kbvSessionItem = storageService.getSessionId(sessionId);
        QuestionState questionState = null;
        try {
            questionState =
                    objectMapper.readValue(kbvSessionItem.getQuestionState(), QuestionState.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        QuestionAnswer answer = null;
        int statusCode;
        try {
            answer = objectMapper.readValue(input.getBody(), QuestionAnswer.class);

            questionState.setAnswer(answer);
            statusCode = 200;

            if (questionState.submitAnswers()) {
                QuestionsResponse questionsResponse = submitAnswers(questionState);
                boolean moreQuestions = questionState.setQuestionsResponse(questionsResponse);
                if (!moreQuestions) {
                    statusCode = 204;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            context.getLogger().log("Retrieving questions failed: " + e);
            statusCode = 500;
            responseBody = "{ \"error\":\"" + e.getMessage() + "\" }";
        }

        return createResponseEvent(statusCode, responseBody, responseHeaders);
    }

    private QuestionsResponse submitAnswers(QuestionState questionState)
            throws IOException, InterruptedException {
        QuestionAnswerRequest questionAnswerRequest = new QuestionAnswerRequest();
        List<QuestionState.QuestionAnswerPair> pairs = questionState.getQaPairs();

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

        URI wrapperResourceURI = get_KBV_RTQ_URI();
        System.out.println("👍 wrapper uri:" + wrapperResourceURI);
        HttpRequest httpReq =
                HttpRequest.newBuilder()
                        .uri(wrapperResourceURI)
                        .setHeader("Accept", RESPONSE_TYPE_APPLICATION_JSON)
                        .setHeader("Content-Type", RESPONSE_TYPE_APPLICATION_JSON)
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpResponse<String> res = httpClient.send(httpReq, HttpResponse.BodyHandlers.ofString());

        String body = res.body();
        System.out.println("😁 got rtq response: " + body);
        QuestionsResponse questionsResponse = objectMapper.readValue(body, QuestionsResponse.class);
        return questionsResponse;
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

    private URI get_KBV_RTQ_URI() {
        String baseURL = System.getenv("EXPERIAN_API_WRAPPER_URL");
        String resource = System.getenv("EXPERIAN_API_WRAPPER_RTQ_RESOURCE");
        return URI.create(baseURL + resource);
    }
}
