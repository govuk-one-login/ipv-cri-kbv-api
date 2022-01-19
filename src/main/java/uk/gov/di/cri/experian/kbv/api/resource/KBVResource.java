package uk.gov.di.cri.experian.kbv.api.resource;

import com.experian.uk.schema.experian.identityiq.services.webservice.Question;
import com.fasterxml.jackson.databind.ObjectMapper;
import spark.Request;
import spark.Response;
import spark.Route;
import uk.gov.di.cri.experian.kbv.api.domain.PersonIdentity;
import uk.gov.di.cri.experian.kbv.api.domain.QuestionState;
import uk.gov.di.cri.experian.kbv.api.domain.QuestionsResponse;
import uk.gov.di.cri.experian.kbv.api.service.StorageService;

import javax.servlet.http.HttpServletResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;

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

                System.out.println(json);

                URI wrapperResourceURI = getKBVWrapperURI();
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
                                httpReq, java.net.http.HttpResponse.BodyHandlers.ofString());

                // we get x questions back
                // deser

                QuestionsResponse questionsResponse =
                        objectMapper.readValue(res.body(), QuestionsResponse.class);
                questionState.setQuestionsResponse(questionsResponse);
                storageService.save(sessionId, questionState);
                Question question = questionState.getNextQuestion();
                response.status(HttpServletResponse.SC_OK);
                response.type("application/json;charset=UTF-8");
                return objectMapper.writeValueAsString(question);
            };

    private URI getKBVWrapperURI() {
        String baseURL = System.getenv("EXPERIAN_API_WRAPPER_URL");
        String resource = System.getenv("EXPERIAN_API_WRAPPER_SAA_RESOURCE");
        return URI.create(baseURL + resource);
    }
}
