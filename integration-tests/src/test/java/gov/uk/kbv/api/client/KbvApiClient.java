package gov.uk.kbv.api.client;

import uk.gov.di.ipv.cri.common.library.client.ClientConfigurationService;
import uk.gov.di.ipv.cri.common.library.client.HttpHeaders;
import uk.gov.di.ipv.cri.common.library.util.URIBuilder;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class KbvApiClient {
    private final HttpClient httpClient;
    private final ClientConfigurationService clientConfigurationService;

    public KbvApiClient(ClientConfigurationService clientConfigurationService) {
        this.clientConfigurationService = clientConfigurationService;
        this.httpClient = HttpClient.newBuilder().build();
    }

    private static final String JSON_MIME_MEDIA_TYPE = "application/json";

    public HttpResponse<String> sendAbandonRequest(String sessionId)
            throws IOException, InterruptedException {
        var request =
                HttpRequest.newBuilder()
                        .uri(
                                new URIBuilder(
                                                this.clientConfigurationService
                                                        .getPrivateApiEndpoint())
                                        .setPath(
                                                this.clientConfigurationService.createUriPath(
                                                        "abandon"))
                                        .build())
                        .header(HttpHeaders.ACCEPT, JSON_MIME_MEDIA_TYPE)
                        .header(HttpHeaders.SESSION_ID, sessionId)
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();
        return sendHttpRequest(request);
    }

    public HttpResponse<String> sendIssueCredentialRequest(String accessToken)
            throws IOException, InterruptedException {
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                new URIBuilder(
                                                this.clientConfigurationService
                                                        .getPublicApiEndpoint())
                                        .setPath(
                                                this.clientConfigurationService.createUriPath(
                                                        "credential/issue"))
                                        .build())
                        .header(
                                HttpHeaders.API_KEY,
                                this.clientConfigurationService.getPublicApiKey())
                        .header("Authorization", "Bearer " + accessToken)
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();
        return sendHttpRequest(request);
    }

    public HttpResponse<String> sendQuestionRequest(String sessionId)
            throws IOException, InterruptedException {
        var request =
                HttpRequest.newBuilder()
                        .uri(
                                new URIBuilder(
                                                this.clientConfigurationService
                                                        .getPrivateApiEndpoint())
                                        .setPath(
                                                this.clientConfigurationService.createUriPath(
                                                        "question"))
                                        .build())
                        .header(HttpHeaders.ACCEPT, JSON_MIME_MEDIA_TYPE)
                        .header(HttpHeaders.SESSION_ID, sessionId)
                        .GET()
                        .build();
        return sendHttpRequest(request);
    }

    public void submitCorrectAnswers(String question, String sessionId)
            throws IOException, InterruptedException {
        String answer =
                Map.of(
                                "Q00001", "Correct 1",
                                "Q00002", "Correct 2")
                        .get(question.toUpperCase());

        String POST_REQUEST_BODY =
                "{\"questionId\":\"" + question + "\",\"answer\":\"" + answer + "\"}";

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                new URIBuilder(
                                                this.clientConfigurationService
                                                        .getPrivateApiEndpoint())
                                        .setPath(
                                                this.clientConfigurationService.createUriPath(
                                                        "answer"))
                                        .build())
                        .header(HttpHeaders.ACCEPT, JSON_MIME_MEDIA_TYPE)
                        .header(HttpHeaders.CONTENT_TYPE, JSON_MIME_MEDIA_TYPE)
                        .header(HttpHeaders.SESSION_ID, sessionId)
                        .POST(HttpRequest.BodyPublishers.ofString(POST_REQUEST_BODY))
                        .build();
        sendHttpRequest(request);
    }

    public void submitIncorrectAnswers(String question, String sessionId)
            throws IOException, InterruptedException, URISyntaxException {
        String answer =
                Map.of(
                                "Q00001", "Incorrect 1",
                                "Q00002", "Incorrect 2")
                        .get(question.toUpperCase());

        String POST_REQUEST_BODY =
                "{\"questionId\":\"" + question + "\",\"answer\":\"" + answer + "\"}";

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                new URIBuilder(
                                                this.clientConfigurationService
                                                        .getPrivateApiEndpoint())
                                        .setPath(
                                                this.clientConfigurationService.createUriPath(
                                                        "answer"))
                                        .build())
                        .header(HttpHeaders.ACCEPT, JSON_MIME_MEDIA_TYPE)
                        .header(HttpHeaders.CONTENT_TYPE, JSON_MIME_MEDIA_TYPE)
                        .header(HttpHeaders.SESSION_ID, sessionId)
                        .POST(HttpRequest.BodyPublishers.ofString(POST_REQUEST_BODY))
                        .build();
        sendHttpRequest(request);
    }

    private HttpResponse<String> sendHttpRequest(HttpRequest request)
            throws IOException, InterruptedException {
        return this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
