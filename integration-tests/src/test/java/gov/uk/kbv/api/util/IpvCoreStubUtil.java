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

    public static final String KBV_CRI_DEV = "kbv-cri-dev";
    public static final String ENVIRONMENT = "/dev"; // dev, build, staging, integration
    public static final String SESSION = ENVIRONMENT + "/session";
    public static final String QUESTION = ENVIRONMENT + "/question";
    public static final String ANSWER = "/dev/answer";
    public static final String ABANDON = ENVIRONMENT + "/abandon";
    public static final String AUTHORIZATION = ENVIRONMENT + "/authorization";
    public static final String TOKEN = ENVIRONMENT + "/token";
    public static final String CREDENTIAL_ISSUE = ENVIRONMENT + "/credential/issue";

    private static String getPrivateAPIEndpoint() {
        return getApiEndpoint(
                "API_GATEWAY_ID_PRIVATE", "Environment variable PRIVATE API endpoint is not set");
    }

    private static String getPublicAPIEndpoint() {
        return getApiEndpoint(
                "API_GATEWAY_ID_PUBLIC", "Environment variable PUBLIC API endpoint is not set");
    }

    public static String getApiEndpoint(String apikey, String message) {
        String apiEndpoint = System.getenv(apikey);
        Optional.ofNullable(apiEndpoint).orElseThrow(() -> new IllegalArgumentException(message));

        return "https://" + apiEndpoint + ".execute-api.eu-west-2.amazonaws.com";
    }

    public static String getIPVCoreStubURL() {
        return Optional.ofNullable(System.getenv("IPV_CORE_STUB_URL"))
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Environment variable IPV_CORE_STUB_URL is not set"));
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

    public static String getClaimsForUser(String baseUrl, int userDataRowNumber)
            throws URISyntaxException, IOException, InterruptedException {
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                new URIBuilder(baseUrl)
                                        .setPath("backend/generateInitialClaimsSet")
                                        .addParameter("cri", KBV_CRI_DEV)
                                        .addParameter(
                                                "rowNumber", String.valueOf(userDataRowNumber))
                                        .build())
                        .GET()
                        .build();
        return sendHttpRequest(request).body();
    }

    public static String createRequest(String baseUrl, String jsonString)
            throws URISyntaxException, IOException, InterruptedException {

        var uri =
                new URIBuilder(baseUrl)
                        .setPath("backend/createSessionRequest")
                        .addParameter("cri", KBV_CRI_DEV)
                        .build();

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(uri)
                        .setHeader("Accept", "application/json")
                        .setHeader("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonString))
                        .build();

        return sendHttpRequest(request).body();
    }

    public static String getPublicAPIKey() {
        return Optional.ofNullable(System.getenv("APIGW_API_KEY")).orElseThrow();
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
                        .uri(new URIBuilder(getPrivateAPIEndpoint()).setPath(ANSWER).build())
                        .setHeader("Accept", "application/json")
                        .setHeader("Content-Type", "application/json")
                        .setHeader("session-id", sessionId)
                        .POST(HttpRequest.BodyPublishers.ofString(POST_REQUEST_BODY))
                        .build();
        sendHttpRequest(request);
    }

    public static String getPrivateKeyJWTFormParamsForAuthCode(
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

    public static HttpResponse<String> sendSessionRequest(String sessionRequestBody)
            throws URISyntaxException, IOException, InterruptedException {
        var request =
                HttpRequest.newBuilder()
                        .uri(new URIBuilder(getPrivateAPIEndpoint()).setPath(SESSION).build())
                        .setHeader("Accept", "application/json")
                        .setHeader("Content-Type", "application/json")
                        .setHeader("X-Forwarded-For", "192.168.0.1")
                        .POST(HttpRequest.BodyPublishers.ofString(sessionRequestBody))
                        .build();
        return sendHttpRequest(request);
    }

    public static HttpResponse<String> sendQuestionRequest(String sessionId)
            throws URISyntaxException, IOException, InterruptedException {
        var request =
                HttpRequest.newBuilder()
                        .uri(new URIBuilder(getPrivateAPIEndpoint()).setPath(QUESTION).build())
                        .setHeader("Accept", "application/json")
                        .setHeader("session-id", sessionId)
                        .GET()
                        .build();
        return sendHttpRequest(request);
    }

    public static HttpResponse<String> sendAuthorizationRequest(String sessionId)
            throws IOException, InterruptedException, URISyntaxException {
        var url =
                new URIBuilder(getPrivateAPIEndpoint())
                        .setPath(AUTHORIZATION)
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
                        .setHeader("Accept", "application/json")
                        .setHeader("session-id", sessionId)
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
                        .uri(new URIBuilder(getPublicAPIEndpoint()).setPath(TOKEN).build())
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .header("x-api-key", IpvCoreStubUtil.getPublicAPIKey())
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
                                        .setPath(CREDENTIAL_ISSUE)
                                        .build())
                        .header("x-api-key", IpvCoreStubUtil.getPublicAPIKey())
                        .setHeader("Authorization", "Bearer " + accessToken)
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();
        return sendHttpRequest(request);
    }

    public static HttpResponse<String> sendAbandonRequest(String sessionId)
            throws IOException, InterruptedException, URISyntaxException {
        var request =
                HttpRequest.newBuilder()
                        .uri(new URIBuilder(getPrivateAPIEndpoint()).setPath(ABANDON).build())
                        .setHeader("Accept", "application/json")
                        .setHeader("session-id", sessionId)
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();
        return sendHttpRequest(request);
    }
}
