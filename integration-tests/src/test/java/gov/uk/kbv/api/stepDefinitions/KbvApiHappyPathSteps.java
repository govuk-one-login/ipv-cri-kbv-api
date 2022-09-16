package gov.uk.kbv.api.stepDefinitions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.http.client.utils.URIBuilder;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class KbvApiHappyPathSteps {

    public final String KBV_CRI_DEV = "kbv-cri-dev";
    public final String ENVIRONMENT = "/dev"; // dev, build, staging, integration
    public final String SESSION = ENVIRONMENT + "/session";
    public final String QUESTION = ENVIRONMENT + "/question";
    public final String ANSWER = ENVIRONMENT + "/answer";
    public final String AUTHORIZATION = ENVIRONMENT + "/authorization";
    public final String TOKEN = ENVIRONMENT + "/token";
    public final String CREDENTIAL_ISSUE = ENVIRONMENT + "/credential/issue";
    private String sessionRequestBody;
    private String sessionId;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private String questionId;
    private String authorizationCode;
    private String userIdentityJson;
    private HttpResponse<String> response;

    private String getPrivateAPIEndpoint() {
        return getApiEndpoint(
                "API_GATEWAY_ID_PRIVATE", "Environment variable PRIVATE API endpoint is not set");
    }

    private String getPublicAPIEndpoint() {
        return getApiEndpoint(
                "API_GATEWAY_ID_PUBLIC", "Environment variable PUBLIC API endpoint is not set");
    }

    private String getApiEndpoint(String apikey, String message) {
        String apiEndpoint = System.getenv(apikey);
        Optional.ofNullable(apiEndpoint).orElseThrow(() -> new IllegalArgumentException(message));

        return "https://" + apiEndpoint + ".execute-api.eu-west-2.amazonaws.com";
    }

    @Given("user has the user identity in the form of a signed JWT string")
    public void userHasTheUserIdentityInTheFormOfASignedJWTString()
            throws URISyntaxException, IOException, InterruptedException {
        int experianRowNumber = 681;
        userIdentityJson = getClaimsForUser(getIPVCoreStubURL(), experianRowNumber);
        sessionRequestBody = createRequest(getIPVCoreStubURL(), userIdentityJson);
    }

    private String getIPVCoreStubURL() {
        return Optional.ofNullable(System.getenv("IPV_CORE_STUB_URL"))
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Environment variable IPV_CORE_STUB_URL is not set"));
    }

    private String getClaimsForUser(String baseUrl, int userDataRowNumber)
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

    private String getPrivateKeyJWTFormParamsForAuthCode(String baseUrl, String authorizationCode)
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

    private String createRequest(String baseUrl, String jsonString)
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

    @When("user sends a POST request to session end point")
    public void user_sends_a_post_request_to_session_end_point()
            throws IOException, InterruptedException, URISyntaxException {

        var request =
                HttpRequest.newBuilder()
                        .uri(new URIBuilder(getPrivateAPIEndpoint()).setPath(SESSION).build())
                        .setHeader("Accept", "application/json")
                        .setHeader("Content-Type", "application/json")
                        .setHeader("X-Forwarded-For", "192.168.0.1")
                        .POST(HttpRequest.BodyPublishers.ofString(sessionRequestBody))
                        .build();
        this.response = sendHttpRequest(request);

        Map<String, String> deserializedResponse =
                objectMapper.readValue(this.response.body(), new TypeReference<>() {});
        sessionId = deserializedResponse.get("session_id");
    }

    @Then("user gets a session-id")
    public void user_gets_a_session_id() {
        assertNotNull(sessionId);
    }

    @When("user sends a GET request to question end point")
    public void user_sends_a_get_request_to_question_end_point()
            throws IOException, InterruptedException, URISyntaxException {

        var request =
                HttpRequest.newBuilder()
                        .uri(new URIBuilder(getPrivateAPIEndpoint()).setPath(QUESTION).build())
                        .setHeader("Accept", "application/json")
                        .setHeader("session-id", sessionId)
                        .GET()
                        .build();

        response = sendHttpRequest(request);
        Map<String, String> deserializeGetResponse =
                objectMapper.readValue(response.body(), new TypeReference<>() {});
        makeQuestionAssertions(deserializeGetResponse);
    }

    private void makeQuestionAssertions(Map<String, String> deserialisedGETResponse) {
        if (!deserialisedGETResponse.isEmpty()) {
            assertNotNull(deserialisedGETResponse.get("text"));
            assertNotNull(deserialisedGETResponse.get("questionID"));
            questionId = deserialisedGETResponse.get("questionID");
        }
    }

    private HttpResponse<String> sendHttpRequest(HttpRequest request)
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

    @And("user answers the question correctly")
    public void userAnswersTheQuestionCorrectly()
            throws IOException, URISyntaxException, InterruptedException {
        answerCorrectly(questionId);
    }

    @When("user sends a GET request to question end point when there are no questions left")
    public void userSendsAGETRequestToQuestionEndPointWhenThereAreNoQuestionsLeft()
            throws IOException, InterruptedException, URISyntaxException {
        var request =
                HttpRequest.newBuilder()
                        .uri(new URIBuilder(getPrivateAPIEndpoint()).setPath(QUESTION).build())
                        .setHeader("Accept", "application/json")
                        .setHeader("session-id", sessionId)
                        .GET()
                        .build();

        response = sendHttpRequest(request);
    }

    @Then("user gets status code {int}")
    public void user_gets_status_code(Integer statusCode) {
        assertEquals(statusCode, response.statusCode());
    }

    @When("user sends a GET request to authorization end point")
    public void user_sends_a_get_request_to_authorization_end_point()
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

        response = sendHttpRequest(request);
    }

    @And("a valid authorization code is returned in the response")
    public void aValidAuthorizationCodeIsReturnedInTheResponse() throws IOException {
        JsonNode jsonNode = objectMapper.readTree(response.body());
        authorizationCode = jsonNode.get("authorizationCode").get("value").textValue();
        assertNotNull(authorizationCode);
    }

    @When("user sends a POST request to token end point")
    public void user_sends_a_post_request_to_token_end_point()
            throws URISyntaxException, IOException, InterruptedException {
        String privateKeyJWT =
                getPrivateKeyJWTFormParamsForAuthCode(
                        getIPVCoreStubURL(), authorizationCode.trim());
        var request =
                HttpRequest.newBuilder()
                        .uri(new URIBuilder(getPublicAPIEndpoint()).setPath(TOKEN).build())
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .header("x-api-key", getPublicAPIKey())
                        .POST(HttpRequest.BodyPublishers.ofString(privateKeyJWT))
                        .build();

        response = sendHttpRequest(request);
    }

    @And("a valid access token code is returned in the response")
    public void aValidAccessTokenCodeIsReturnedInTheResponse() throws IOException {
        JsonNode jsonNode = objectMapper.readTree(response.body());
        var accessToken = jsonNode.get("access_token").asText();
        var expiresIn = jsonNode.get("expires_in").asInt();
        var tokenType = jsonNode.get("token_type").asText();
        assertEquals(3600, expiresIn);
        assertEquals("Bearer", tokenType);
        assertFalse(accessToken.isEmpty());
    }

    @When("user sends a POST request to Credential Issue end point with a valid access token")
    public void user_sends_a_post_request_to_credential_issue_end_point_with_a_valid_access_token()
            throws IOException, InterruptedException, URISyntaxException {
        JsonNode jsonNode = objectMapper.readTree(response.body());
        var accessToken = jsonNode.get("access_token").asText();
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                new URIBuilder(getPublicAPIEndpoint())
                                        .setPath(CREDENTIAL_ISSUE)
                                        .build())
                        .header("x-api-key", getPublicAPIKey())
                        .setHeader("Authorization", "Bearer " + accessToken)
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();
        response = sendHttpRequest(request);
    }

    @And("a valid JWT is returned in the response")
    public void aValidJWTIsReturnedInTheResponse() throws ParseException, IOException {
        assertNotNull(response.body());
        makeAssertions(SignedJWT.parse(response.body()));
    }

    private void makeAssertions(SignedJWT decodedJWT) throws IOException {
        var header = decodedJWT.getHeader().toString();
        var payload = objectMapper.readTree(decodedJWT.getPayload().toString());
        JsonNode identityJSON = objectMapper.readTree(userIdentityJson);

        assertEquals("{\"typ\":\"JWT\",\"alg\":\"ES256\"}", header);
        assertNotNull(payload);
        assertEquals(2, payload.get("vc").get("evidence").get(0).get("verificationScore").asInt());

        assertEquals(
                "IdentityCheck", payload.get("vc").get("evidence").get(0).get("type").asText());
        assertNotNull(payload.get("vc").get("evidence").get(0).get("txn").asText());
        assertEquals("VerifiableCredential", payload.get("vc").get("type").get(0).asText());
        assertEquals("IdentityCheckCredential", payload.get("vc").get("type").get(1).asText());
        assertEquals(
                payload.get("vc")
                        .get("credentialSubject")
                        .get("birthDate")
                        .get(0)
                        .get("value")
                        .asText(),
                identityJSON.get("shared_claims").get("birthDate").get(0).get("value").asText());
        assertEquals(
                payload.get("vc")
                        .get("credentialSubject")
                        .get("name")
                        .get(0)
                        .get("nameParts")
                        .get(0)
                        .get("value")
                        .asText(),
                identityJSON
                        .get("shared_claims")
                        .get("name")
                        .get(0)
                        .get("nameParts")
                        .get(0)
                        .get("value")
                        .asText());
    }

    private String getPublicAPIKey() {
        return Optional.ofNullable(System.getenv("APIGW_API_KEY")).orElseThrow();
    }

    private void answerCorrectly(String question)
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
        response = sendHttpRequest(request);
    }
}
