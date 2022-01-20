package uk.gov.di.cri.experian.kbv.api.resource;

import com.experian.uk.schema.experian.identityiq.services.webservice.Question;
import com.fasterxml.jackson.databind.ObjectMapper;
import spark.Request;
import spark.Response;
import spark.Route;
import uk.gov.di.cri.experian.kbv.api.domain.PersonIdentity;
import uk.gov.di.cri.experian.kbv.api.domain.QuestionAnswer;
import uk.gov.di.cri.experian.kbv.api.domain.QuestionAnswerRequest;
import uk.gov.di.cri.experian.kbv.api.domain.QuestionState;
import uk.gov.di.cri.experian.kbv.api.domain.QuestionsResponse;
import uk.gov.di.cri.experian.kbv.api.service.StorageService;

import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class KBVResource {

    public static final String HEADER_SESSION_ID = "session-id";
    public static final String RESPONSE_TYPE_APPLICATION_JSON = "application/json";
    public static final String EMPTY_JSON = "{}";
    private StorageService storageService;
    private ObjectMapper objectMapper;

    public KBVResource(StorageService storageService, ObjectMapper objectMapper) {
        this.storageService = storageService;
        this.objectMapper = objectMapper;
    }

    public final Route createSession =
            (Request request, Response response) -> {
                PersonIdentity identity =
                        objectMapper.readValue(request.body(), PersonIdentity.class);
                String key = UUID.randomUUID().toString();
                storageService.save(key, new QuestionState(identity));
                response.status(HttpServletResponse.SC_CREATED);
                response.type(RESPONSE_TYPE_APPLICATION_JSON);
                return objectMapper.writeValueAsString(Map.of(HEADER_SESSION_ID, key));
            };

    public final Route question =
            (Request request, Response response) -> {
                String sessionId = request.headers(HEADER_SESSION_ID);
                QuestionState questionState = storageService.get(sessionId);
                PersonIdentity personIdentity = questionState.getPersonIdentity();

                String json = objectMapper.writeValueAsString(personIdentity);

                Optional<Question> nextQuestion = questionState.getNextQuestion();
                if (!nextQuestion.isPresent()) { // we should fall in this block once only
                    // fetch a batch of questions from experian kbv wrapper
                    QuestionsResponse questionsResponse = startGetQuestions(json);
                    if (!questionState.setQuestionsResponse(questionsResponse)) {
                        response.status(HttpServletResponse.SC_BAD_REQUEST);
                        response.type(RESPONSE_TYPE_APPLICATION_JSON);
                        return objectMapper.writeValueAsString(
                                Map.of("error", "no further questions"));
                    }
                    storageService.save(sessionId, questionState);
                    nextQuestion = questionState.getNextQuestion();
                }
                response.status(HttpServletResponse.SC_OK);
                response.type(RESPONSE_TYPE_APPLICATION_JSON);
                return objectMapper.writeValueAsString(nextQuestion.get());
            };

    public final Route answer =
            (Request request, Response response) -> {
                String sessionId = request.headers(HEADER_SESSION_ID);
                QuestionState questionState = storageService.get(sessionId);
                QuestionAnswer answer =
                        objectMapper.readValue(request.body(), QuestionAnswer.class);

                questionState.setAnswer(answer);

                int responseStatus = HttpServletResponse.SC_OK;

                if (questionState.submitAnswers()) {

                    QuestionsResponse questionsResponse = submitAnswers(questionState);

                    boolean moreQuestions = questionState.setQuestionsResponse(questionsResponse);
                    if (!moreQuestions) {
                        responseStatus = HttpServletResponse.SC_NO_CONTENT;
                    }
                }

                response.status(responseStatus);
                response.type(RESPONSE_TYPE_APPLICATION_JSON);
                return EMPTY_JSON;
            };

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

    private URI get_KBV_RTQ_URI() {
        String baseURL = System.getenv("EXPERIAN_API_WRAPPER_URL");
        String resource = System.getenv("EXPERIAN_API_WRAPPER_RTQ_RESOURCE");
        return URI.create(baseURL + resource);
    }
}
