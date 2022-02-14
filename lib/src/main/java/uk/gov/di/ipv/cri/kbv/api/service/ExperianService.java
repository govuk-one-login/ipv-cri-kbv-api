package uk.gov.di.ipv.cri.kbv.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ExperianService {

    public static final String RESPONSE_TYPE_APPLICATION_JSON = "application/json";
    private ObjectMapper objectMapper;

    public ExperianService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public QuestionsResponse getQuestions(String payload) throws IOException, InterruptedException {
        URI wrapperResourceURI = get_KBV_SAA_URI();
        System.out.println("wrapper uri:" + wrapperResourceURI);
        HttpRequest httpReq =
                HttpRequest.newBuilder()
                        .uri(wrapperResourceURI)
                        .setHeader("Accept", RESPONSE_TYPE_APPLICATION_JSON)
                        .setHeader("Content-Type", RESPONSE_TYPE_APPLICATION_JSON)
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build();

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpResponse<String> res = null;

        res = httpClient.send(httpReq, HttpResponse.BodyHandlers.ofString());

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
