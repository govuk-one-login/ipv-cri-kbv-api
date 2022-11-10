package gov.uk.kbv.api.util;

import org.apache.http.client.utils.URIBuilder;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class IpvCoreStubUtil {

    private static class HttpHeaders {
        static final String ACCEPT = "Accept";
        static final String CONTENT_TYPE = "Content-Type";
        static final String SESSION_ID = "session-id";
        static final String API_KEY = "x-api-key"; // pragma: allowlist secret
    }

    private static final String JSON_MIME_MEDIA_TYPE = "application/json";
    private static final String KBV_CRI_DEV = "kbv-cri-dev";
    private static final String ENVIRONMENT = "/dev"; // dev, build, staging, integration

    public static String getClaimsForUser(int userDataRowNumber)
            throws URISyntaxException, IOException, InterruptedException {
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                new URIBuilder(getIPVCoreStubURL())
                                        .setPath("backend/generateInitialClaimsSet")
                                        .addParameter("cri", KBV_CRI_DEV)
                                        .addParameter(
                                                "rowNumber", String.valueOf(userDataRowNumber))
                                        .build())
                        .GET()
                        .build();
        return sendHttpRequest(request).body();
    }

    public static String createSessionRequest(String requestBody)
            throws URISyntaxException, IOException, InterruptedException {

        var uri =
                new URIBuilder(getIPVCoreStubURL())
                        .setPath("backend/createSessionRequest")
                        .addParameter("cri", KBV_CRI_DEV)
                        .build();

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(uri)
                        .header(HttpHeaders.ACCEPT, JSON_MIME_MEDIA_TYPE)
                        .header(HttpHeaders.CONTENT_TYPE, JSON_MIME_MEDIA_TYPE)
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

        return sendHttpRequest(request).body();
    }

    public static void submitCorrectAnswers(String question, String sessionId)
            throws IOException, InterruptedException, URISyntaxException {
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
                                new URIBuilder(getPrivateAPIEndpoint())
                                        .setPath(createUriPath("answer"))
                                        .build())
                        .header(HttpHeaders.ACCEPT, JSON_MIME_MEDIA_TYPE)
                        .header(HttpHeaders.CONTENT_TYPE, JSON_MIME_MEDIA_TYPE)
                        .header(HttpHeaders.SESSION_ID, sessionId)
                        .POST(HttpRequest.BodyPublishers.ofString(POST_REQUEST_BODY))
                        .build();
        sendHttpRequest(request);
    }

    public static HttpResponse<String> sendSessionRequest(String sessionRequestBody)
            throws URISyntaxException, IOException, InterruptedException {
        var request =
                HttpRequest.newBuilder()
                        .uri(
                                new URIBuilder(getPrivateAPIEndpoint())
                                        .setPath(createUriPath("session"))
                                        .build())
                        .header(HttpHeaders.ACCEPT, JSON_MIME_MEDIA_TYPE)
                        .header(HttpHeaders.CONTENT_TYPE, JSON_MIME_MEDIA_TYPE)
                        .header("X-Forwarded-For", "192.168.0.1")
                        .POST(HttpRequest.BodyPublishers.ofString(sessionRequestBody))
                        .build();
        return sendHttpRequest(request);
    }

    public static HttpResponse<String> sendQuestionRequest(String sessionId)
            throws URISyntaxException, IOException, InterruptedException {
        var request =
                HttpRequest.newBuilder()
                        .uri(
                                new URIBuilder(getPrivateAPIEndpoint())
                                        .setPath(createUriPath("question"))
                                        .build())
                        .header(HttpHeaders.ACCEPT, JSON_MIME_MEDIA_TYPE)
                        .header(HttpHeaders.SESSION_ID, sessionId)
                        .GET()
                        .build();
        return sendHttpRequest(request);
    }

    public static HttpResponse<String> sendAuthorizationRequest(String sessionId)
            throws IOException, InterruptedException, URISyntaxException {
        var url =
                new URIBuilder(getPrivateAPIEndpoint())
                        .setPath(createUriPath("authorization"))
                        .addParameter(
                                "redirect_uri",
                                "https://di-ipv-core-stub.london.cloudapps.digital/callback")
                        .addParameter("client_id", "ipv-core-stub")
                        .addParameter("response_type", "code")
                        .addParameter("scope", "openid")
                        .addParameter("state", "state-ipv")
                        .build();

        var request =
                HttpRequest.newBuilder()
                        .uri(url)
                        .header(HttpHeaders.ACCEPT, JSON_MIME_MEDIA_TYPE)
                        .header(HttpHeaders.SESSION_ID, sessionId)
                        .GET()
                        .build();
        return sendHttpRequest(request);
    }

    public static HttpResponse<String> sendTokenRequest(String authorizationCode)
            throws URISyntaxException, IOException, InterruptedException {
        String privateKeyJWT =
                IpvCoreStubUtil.getPrivateKeyJWTFormParamsForAuthCode(
                        getIPVCoreStubURL(), authorizationCode.trim());
        var request =
                HttpRequest.newBuilder()
                        .uri(
                                new URIBuilder(getPublicAPIEndpoint())
                                        .setPath(createUriPath("token"))
                                        .build())
                        .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                        .header(HttpHeaders.API_KEY, getPublicAPIKey())
                        .POST(HttpRequest.BodyPublishers.ofString(privateKeyJWT))
                        .build();
        return sendHttpRequest(request);
    }

    public static HttpResponse<String> sendCredentialsIssuerRequest(String accessToken)
            throws URISyntaxException, IOException, InterruptedException {
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                new URIBuilder(getPublicAPIEndpoint())
                                        .setPath(createUriPath("credential/issue"))
                                        .build())
                        .header(HttpHeaders.API_KEY, getPublicAPIKey())
                        .header("Authorization", "Bearer " + accessToken)
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();
        return sendHttpRequest(request);
    }

    public static HttpResponse<String> sendAbandonRequest(String sessionId)
            throws IOException, InterruptedException, URISyntaxException {
        var request =
                HttpRequest.newBuilder()
                        .uri(
                                new URIBuilder(getPrivateAPIEndpoint())
                                        .setPath(createUriPath("abandon"))
                                        .build())
                        .header(HttpHeaders.ACCEPT, JSON_MIME_MEDIA_TYPE)
                        .header(HttpHeaders.SESSION_ID, sessionId)
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();
        return sendHttpRequest(request);
    }

    private static String getPublicAPIKey() {
        return Optional.ofNullable(System.getenv("APIGW_API_KEY")).orElseThrow();
    }

    private static String getApiEndpoint(String apikey, String message) {
        String apiEndpoint = System.getenv(apikey);
        Optional.ofNullable(apiEndpoint).orElseThrow(() -> new IllegalArgumentException(message));

        return "https://" + apiEndpoint + ".execute-api.eu-west-2.amazonaws.com";
    }

    private static String getPrivateAPIEndpoint() {
        return getApiEndpoint(
                "API_GATEWAY_ID_PRIVATE", "Environment variable API_GATEWAY_ID_PRIVATE is not set");
    }

    private static String getPublicAPIEndpoint() {
        return getApiEndpoint(
                "API_GATEWAY_ID_PUBLIC", "Environment variable API_GATEWAY_ID_PUBLIC is not set");
    }

    private static String getIPVCoreStubURL() {
        return Optional.ofNullable(System.getenv("IPV_CORE_STUB_URL"))
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "Environment variable IPV_CORE_STUB_URL is not set"));
    }

    private static String createUriPath(String endpoint) {
        return String.format("%s/%s", ENVIRONMENT, endpoint);
    }

    private static String getPrivateKeyJWTFormParamsForAuthCode(
            String baseUrl, String authorizationCode)
            throws URISyntaxException, IOException, InterruptedException {
        var url =
                new URIBuilder(baseUrl)
                        .setPath("backend/createTokenRequestPrivateKeyJWT")
                        .addParameter("cri", KBV_CRI_DEV)
                        .addParameter("authorization_code", authorizationCode)
                        .build();

        HttpRequest request = HttpRequest.newBuilder().uri(url).GET().build();
        return sendHttpRequest(request).body();
    }

    private static HttpResponse<String> sendHttpRequest(HttpRequest request)
            throws IOException, InterruptedException {

        String basicAuthUser =
                Objects.requireNonNull(
                        System.getenv("IPV_CORE_STUB_BASIC_AUTH_USER"),
                        "Environment variable IPV_CORE_STUB_BASIC_AUTH_USER is not set");
        String basicAuthPassword =
                Objects.requireNonNull(
                        System.getenv("IPV_CORE_STUB_BASIC_AUTH_PASSWORD"),
                        "Environment variable IPV_CORE_STUB_BASIC_AUTH_PASSWORD is not set");

        HttpClient client =
                HttpClient.newBuilder()
                        .authenticator(
                                new Authenticator() {
                                    @Override
                                    protected PasswordAuthentication getPasswordAuthentication() {
                                        return new PasswordAuthentication(
                                                basicAuthUser, basicAuthPassword.toCharArray());
                                    }
                                })
                        .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
