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
import java.util.UUID;
import java.util.stream.Collectors;

public class KBVResource {

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
                response.type("application/json;charset=UTF-8");
                return objectMapper.writeValueAsString(Map.of("session-id", key));
            };

    public final Route question =
            (Request request, Response response) -> {
                String sessionId = request.headers("session-id");

                QuestionState questionState = storageService.get(sessionId);
                PersonIdentity personIdentity = questionState.getPersonIdentity();

                String json = objectMapper.writeValueAsString(personIdentity);

                Question question = questionState.getNextQuestion();
                if (question == null) {
                    // fetch a batch of questions
                    HttpResponse<String> res = getMoreQuestions(json);
                    QuestionsResponse questionsResponse =
                            objectMapper.readValue(res.body(), QuestionsResponse.class);
                    questionState.setQuestionsResponse(questionsResponse);
                    storageService.save(sessionId, questionState);
                    question = questionState.getNextQuestion();

                }
                response.status(HttpServletResponse.SC_OK);
                response.type("application/json;charset=UTF-8");
                return objectMapper.writeValueAsString(question);

            };


    public final Route answer =
            (Request request, Response response) -> {

                String sessionId = request.headers("session-id");
                QuestionState questionState = storageService.get(sessionId);


                QuestionAnswer answer = objectMapper.readValue(request.body(), QuestionAnswer.class);

                questionState.setAnswer(answer);

                if (questionState.submitAnswers()) {

                    QuestionAnswerRequest questionAnswerRequest = new QuestionAnswerRequest();
                    List<QuestionState.QuestionAnswerPair> pairs = questionState.getQas();

                    List<QuestionAnswer> collect = pairs.stream().map(pair ->
                            {
                                QuestionAnswer questionAnswer = new QuestionAnswer();
                                questionAnswer.setAnswer(pair.getAnswer());
                                questionAnswer.setQuestionId(pair.getQuestion().getQuestionID());
                                return questionAnswer;
                            }
                    ).collect(Collectors.toList());


                    questionAnswerRequest.setQuestionAnswers(collect);
                    questionAnswerRequest.setAuthRefNo(questionState.getControl().getAuthRefNo());
                    questionAnswerRequest.setUrn(questionState.getControl().getURN());
                    String json = objectMapper.writeValueAsString(questionAnswerRequest);

                    URI wrapperResourceURI = get_KBV_RTQ_URI();
                    System.out.println("👍 wrapper uri:" + wrapperResourceURI);
                    HttpRequest httpReq =
                            HttpRequest.newBuilder()
                                    .uri(wrapperResourceURI)
                                    .setHeader("Accept", "application/json")
                                    .setHeader("Content-Type", "application/json")
                                    .POST(HttpRequest.BodyPublishers.ofString(json))
                                    .build();

                    HttpClient httpClient = HttpClient.newHttpClient();
                    HttpResponse<String> res =
                            httpClient.send(
                                    httpReq, HttpResponse.BodyHandlers.ofString());

                    QuestionsResponse questionsResponse =
                            objectMapper.readValue(res.body(), QuestionsResponse.class);

                    questionState.setQuestionsResponse(questionsResponse);

                }


                response.status(HttpServletResponse.SC_NO_CONTENT);
                response.type("application/json;charset=UTF-8");
                return null;

            };


    private HttpResponse<String> getMoreQuestions(String json) throws IOException, InterruptedException {
        URI wrapperResourceURI = get_KBV_SAA_URI();
        System.out.println("👍 wrapper uri:" + wrapperResourceURI);
        HttpRequest httpReq =
                HttpRequest.newBuilder()
                        .uri(wrapperResourceURI)
                        .setHeader("Accept", "application/json")
                        .setHeader("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpResponse<String> res =
                httpClient.send(
                        httpReq, HttpResponse.BodyHandlers.ofString());
        return res;
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
