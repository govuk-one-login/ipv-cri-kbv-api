package uk.gov.di.ipv.cri.kbv.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ExperianService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExperianService.class);
    public static final String RESPONSE_TYPE_APPLICATION_JSON = "application/json";
    public static final String EXPERIAN_API_WRAPPER_URL = "EXPERIAN_API_WRAPPER_URL";
    private final HttpClient httpClient;

    ExperianService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public ExperianService() {
        this(HttpClient.newHttpClient());
    }

    public String getResponseFromKBVExperianAPI(String payload, String uriEndpoint)
            throws IOException, InterruptedException {
        URI wrapperResourceURI = createExperianUri(uriEndpoint);
        HttpRequest httpReq =
                HttpRequest.newBuilder()
                        .uri(wrapperResourceURI)
                        .setHeader("Accept", RESPONSE_TYPE_APPLICATION_JSON)
                        .setHeader("Content-Type", RESPONSE_TYPE_APPLICATION_JSON)
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build();

        HttpResponse<String> res =
                this.httpClient.send(httpReq, HttpResponse.BodyHandlers.ofString());

        LOGGER.info(String.format("KBV Experian API response status code: %s", res.statusCode()));
        String body = res.body();
        LOGGER.info(String.format("KBV Experian API response: %s", body));

        return body;
    }

    private URI createExperianUri(String uriEndpoint) {
        String baseURL = System.getenv(EXPERIAN_API_WRAPPER_URL);
        String resource = System.getenv(uriEndpoint);
        return URI.create(baseURL + resource);
    }
}
