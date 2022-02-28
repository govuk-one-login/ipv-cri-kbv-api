package uk.gov.di.ipv.cri.kbv.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerRequestMapper;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ExperianService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExperianService.class);
    public static final String RESPONSE_TYPE_APPLICATION_JSON = "application/json";
    public static final String EXPERIAN_API_WRAPPER_URL = "EXPERIAN_API_WRAPPER_URL";

    public String getResponseFromExperianAPI(String payload, String uri)
            throws IOException, InterruptedException {
        URI wrapperResourceURI = createExperianUri(uri);
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

        LOGGER.info("getResponseFromExperianAPI response status code: " + res.statusCode());
        String body = res.body();
        LOGGER.info("getResponseFromExperianAPI response: " + body);

        return body;
    }

    private URI createExperianUri(String uri) {
        String baseURL = System.getenv(EXPERIAN_API_WRAPPER_URL);
        String resource = System.getenv(uri);
        return URI.create(baseURL + resource);
    }
}
